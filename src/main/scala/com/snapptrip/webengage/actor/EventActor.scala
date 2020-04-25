package com.snapptrip.webengage.actor

import akka.Done
import akka.actor.SupervisorStrategy.Resume
import akka.actor.{Actor, ActorRef, ActorSystem, OneForOneStrategy, Props, SupervisorStrategy}
import akka.util.Timeout
import com.snapptrip.DI._
import com.snapptrip.api.Messages.{EventUserInfo, WebEngageEvent, WebEngageUserInfo}
import com.snapptrip.formats.Formats._
import com.snapptrip.kafka.Setting.Key
import com.snapptrip.kafka.Publisher
import com.snapptrip.models.User
import com.snapptrip.utils.WebEngageConfig
import com.snapptrip.webengage.Converter
import com.snapptrip.webengage.actor.DBActor.{Find, Save, Update}
import com.snapptrip.webengage.actor.SubscriberActor.NewRequest
import com.typesafe.scalalogging.LazyLogging
import spray.json._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class EventActor(
                  dbRouter: => ActorRef,
                  kafkaActor: => ActorRef
                )(
                  implicit
                  system: ActorSystem,
                  ec: ExecutionContext,
                  timeout: Timeout
                ) extends Actor with Converter with LazyLogging {

  type E <: Throwable

  import EventActor._

  override def preStart(): Unit = {
    super.preStart()
    context.watch(dbRouter)
  }

  override def supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy(10, 60 seconds, loggingEnabled = true) {
      case _: E => Resume
    }

  override def receive(): Receive = {

    case TrackEvent(eventInfo, ref) =>
      self ! FindUser(eventInfo.user, eventInfo.event, ref)

    case FindUser(user, event, ref) =>
      dbRouter ! Find(user, ref, Some(event))

    case DBActor.FindResult(newUser: WebEngageUserInfo, oldUserOpt: Option[User], ref, eventOpt) =>
      oldUserOpt match {
        case userOpt: Some[User] =>
          val webEngageUser = converter(newUser, userOpt)
          dbRouter ! Update(webEngageUser, ref, eventOpt)
        case None =>
          val webEngageUser = converter(newUser)
          dbRouter ! Save(webEngageUser, ref, eventOpt)
      }

    case DBActor.UpdateResult(user: User, updated, _, eventOpt) =>
      if (updated) {
        val event = eventOpt.asInstanceOf[Option[JsValue]].get
        val (userId, modifiedEvent) = modifyEvent(user, event)
        self ! SendToKafka(Key(userId, "track-event"), List(modifiedEvent))
      }

    case DBActor.SaveResult(user: User, _, eventOpt) =>
      val event = eventOpt.map(_.asInstanceOf[JsValue]).get
      val (userId, modifiedEvent) = modifyEvent(user, event)
      self ! SendToKafka(Key(userId, "track-event"), List(modifiedEvent))

    case SendToKafka(key, value) =>
      kafkaActor ! (key, value)

//      Publisher.publish(key, value)
//        .recover {
//          case error: Throwable =>
//            logger.info(s"""publish data to kafka with error: ${error.getMessage}""")
//            SubscriberActor.subscriberActor ! NewRequest(key.toJson.compactPrint, value.head.compactPrint)
//            Done
//        }

  }

  //    def trackEventWithoutUserId(request: WebEngageEvent): Future[(Boolean, JsObject)] = {
  //
  //      val user = request.user
  //      val event = request.event
  //      (for {
  //        _ <- if (user.email.isEmpty && user.mobile_no.isEmpty) {
  //          Future.failed(new Exception("must define one of email or mobile number"))
  //        } else {
  //          Future.successful("")
  //        }
  //        provider = event.asJsObject.fields.filterKeys(_ == "eventData").headOption.flatMap(_._2.asJsObject.fields.filterKeys(_ == "provider").headOption).map(_._2.compactPrint.replace(s""""""", ""))
  //        (userId, newRequest) <- userCheck(WebEngageUserInfo(mobile_no = user.mobile_no, email = user.email, provider = provider)).map { response =>
  //          val (body, _) = response
  //          val lContent = JsObject("userId" -> JsString(body.userId)).fields.toList :::
  //            event.asJsObject.fields.filterKeys(_ == "eventTime").toList.flatMap(x => JsObject(x._1 -> JsString(x._2.compactPrint.replace(s""""""", "").concat(WebEngageConfig.timeOffset))).fields.toList) :::
  //            event.asJsObject.fields.filterKeys(x => x != "email" && x != "mobile_no" && x != "eventTime").toList
  //          val jContent = JsObject(lContent.toMap)
  //          (body.userId, jContent)
  //        }
  //      } yield {
  //        self ! SendToKafka(Key(userId, "track-event"), List(newRequest))
  //        (true, JsObject("status" -> JsString("success")))
  //      }).recover {
  //        case error: Throwable =>
  //          logger.info(s"""client actor track event with error : ${error.getMessage}""")
  //          (false, JsObject("status" -> JsString("failed"), "error" -> JsString(error.getMessage)))
  //      }
  //
  //    }

}


object EventActor extends Converter {

  private implicit val timeout: Timeout = Timeout(30.second)

  def apply(dbActor: => ActorRef, kafkaActor: => ActorRef): Props = {
    Props(new EventActor(dbActor, kafkaActor))
  }

  def modifyEvent(user: User, event: JsValue): (String, JsValue) = {
    val birthDate = user.birthDate.flatMap(dateTimeFormatter)
    val wUser = converter(user, birthDate)
    val lContent = JsObject("userId" -> JsString(wUser.userId)).fields.toList :::
      event.asJsObject.fields.filterKeys(_ == "eventTime").toList.flatMap(x => JsObject(x._1 -> JsString(x._2.compactPrint.replace(s""""""", "").concat(WebEngageConfig.timeOffset))).fields.toList) :::
      event.asJsObject.fields.filterKeys(x => x != "email" && x != "mobile_no" && x != "eventTime").toList
    (wUser.userId, JsObject(lContent.toMap))
  }

  case class FindUser(user: EventUserInfo, event: JsValue, ref: ActorRef) extends Message

  case class TrackEvent(event: WebEngageEvent, ref: ActorRef) extends Message

  case class SendToKafka(key: Key, value: List[JsValue]) extends Message

}

