package com.snapptrip.service.actor

import java.util.UUID

import akka.actor.SupervisorStrategy.Resume
import akka.actor.{Actor, ActorRef, ActorSystem, OneForOneStrategy, Props, SupervisorStrategy}
import akka.util.Timeout
import com.snapptrip.DI._
import com.snapptrip.api.Messages.{EventUserInfo, WebEngageEvent, WebEngageUserInfo}
import com.snapptrip.kafka.Setting.Key
import com.snapptrip.models.User
import com.snapptrip.service.Converter
import com.snapptrip.service.actor.DBActor.{Find, Save, Update}
import com.snapptrip.utils.Exceptions.{ErrorCodes, ExtendedException}
import com.typesafe.scalalogging.LazyLogging
import spray.json._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class EventActor(
                  dbRouter: => ActorRef,
                  publisherActor: => ActorRef
                )(implicit
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
      dbRouter ! Find(WebEngageUserInfo(mobile_no = user.mobile_no, email = user.email), ref, Some(event))

    case DBActor.FindResult(newUser: WebEngageUserInfo, oldUserOpt: Option[User], ref, eventOpt: Option[JsValue]) =>
      oldUserOpt match {
        case userOpt: Some[User] =>
          val user = converter(newUser, userOpt)
          dbRouter ! Update(user, ref, eventOpt)
        case None =>
          val newUserId = UUID.randomUUID().toString
          val user = converter(newUser, newUserId)
          dbRouter ! Save(user, ref, eventOpt)
      }

    case DBActor.UpdateResult(user: User, updated, ref, eventOpt: Option[JsValue]) =>
      logger.error(s"""update result from db-> $eventOpt""")
      if (updated) {
        val event = eventOpt.get
        val (userId, modifiedEvent) = modifyEvent(user.userId, event) match {
          case Right(value) =>
            logger.error(s"""update result modify event-> $value""")
            value
          case Left(exception) => throw ExtendedException(exception.getMessage, ErrorCodes.JsonParseError, ref)
        }
        self ! SendToKafka(Key(userId, "track-event"), modifiedEvent)
      }

    case DBActor.SaveResult(user: User, ref, eventOpt: Option[JsValue]) =>
      val event = eventOpt.get
      val (userId, modifiedEvent) = modifyEvent(user.userId, event) match {
        case Right(value) => value
        case Left(exception) => throw ExtendedException(exception.getMessage, ErrorCodes.JsonParseError, ref)
      }
      self ! SendToKafka(Key(userId, "track-event"), modifiedEvent)

    case SendToKafka(key, value) =>
      publisherActor ! (key, value)

  }

}


object EventActor extends Converter {

  private implicit val timeout: Timeout = Timeout(30.second)

  def apply(dbActor: => ActorRef, kafkaActor: => ActorRef): Props = {
    Props(new EventActor(dbActor, kafkaActor))
  }

  case class FindUser(user: EventUserInfo, event: JsValue, ref: ActorRef) extends Message

  case class TrackEvent(event: WebEngageEvent, ref: ActorRef) extends Message

  case class SendToKafka(key: Key, value: JsValue) extends Message

}

