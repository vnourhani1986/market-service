package com.snapptrip.service.actor

import java.time.LocalDate

import akka.actor.SupervisorStrategy.{Escalate, Restart, Stop}
import akka.actor.{Actor, ActorRef, ActorSystem, Props, _}
import akka.util.Timeout
import com.snapptrip.api.Messages.{WebEngageUserInfo, WebEngageUserInfoWithUserId}
import com.snapptrip.formats.Formats._
import com.snapptrip.kafka.Setting.{DeleteCancelKey, Key}
import com.snapptrip.service.actor.ClientActor.CheckUser
import com.snapptrip.service.actor.WebEngageActor.{SendCancelDeleteUser, SendDeleteUser, SendEventInfo, SendUserInfo}
import com.snapptrip.utils.Exceptions.{ErrorCodes, ExtendedException}
import com.typesafe.scalalogging.LazyLogging
import spray.json.JsonParser
import spray.json._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Try

class SubscriberActor(
                       publisherActor: ActorRef,
                       errorPublisherActor: ActorRef,
                       deleteUserResultPublisherActor: ActorRef,
                       clientActor: ActorRef
                     )(
                       implicit
                       system: ActorSystem,
                       ec: ExecutionContext,
                       timeout: Timeout
                     ) extends Actor with LazyLogging {

  import SubscriberActor._

  override def supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy(10, 60 seconds, loggingEnabled = true) {
      case _: ActorInitializationException => Stop
      case _: ActorKilledException => Stop
      case ex: ExtendedException if ex.errorCode == ErrorCodes.BadRequestError => Stop
      case ex: ExtendedException if ex.errorCode == ErrorCodes.AuthenticationError => Escalate
      case ex: ExtendedException if ex.errorCode == ErrorCodes.InvalidURL => Escalate
      case ex: ExtendedException if ex.errorCode == ErrorCodes.RestServiceError => Escalate
      case _: Exception => Restart
    }


  override def receive(): Receive = {

    case (key: String, value: String) =>

      val (keyId, keyType, entity) = Try {
        val keyId = Try {
          JsonParser(key).convertTo[Key].userId
        }.toOption.orElse(
          Try {
            JsonParser(key).convertTo[DeleteCancelKey].requestId
          }.toOption
        ).get
        val keyType = JsonParser(key).convertTo[Key].keyType
        (keyId, keyType, JsonParser(value))
      }.toEither match {
        case Right(v) => v
        case Left(exception) => throw ExtendedException(exception.getMessage, ErrorCodes.JsonParseError)
      }

      if (keyType == "track-user") {
        val user = entity.convertTo[WebEngageUserInfoWithUserId]
        if (context.child(s"webengage-actor-$keyId").isDefined) {
          context.child(s"webengage-actor-$keyId").foreach(_ ! SendUserInfo(user, 1))
        } else {
          context.actorOf(WebEngageActor(publisherActor, errorPublisherActor, deleteUserResultPublisherActor), s"webengage-actor-$keyId") ! SendUserInfo(user, 1)
        }
      } else if (keyType == "track-event") {
        val event = entity
        if (context.child(s"webengage-actor-$keyId").isDefined) {
          context.child(s"webengage-actor-$keyId").foreach(_ ! SendEventInfo(keyId, event, 1))
        } else {
          context.actorOf(WebEngageActor(publisherActor, errorPublisherActor, deleteUserResultPublisherActor), s"webengage-actor-$keyId") ! SendEventInfo(keyId, event, 1)
        }
      } else if (keyType == "check-user") {
        clientActor ! CheckUser(entity)
      } else if (keyType == "delete-user") {
        if (context.child(s"webengage-actor-$keyId").isDefined) {
          context.child(s"webengage-actor-$keyId").foreach(_ ! SendDeleteUser(keyId, entity, 1))
        } else {
          context.actorOf(WebEngageActor(publisherActor, errorPublisherActor, deleteUserResultPublisherActor), s"webengage-actor-$keyId") ! SendDeleteUser(keyId, entity, 1)
        }
      } else if (keyType == "cancel-delete-user") {
        if (context.child(s"webengage-actor-$keyId").isDefined) {
          context.child(s"webengage-actor-$keyId").foreach(_ ! SendCancelDeleteUser(keyId, 1))
        } else {
          context.actorOf(WebEngageActor(publisherActor, errorPublisherActor, deleteUserResultPublisherActor), s"webengage-actor-$keyId") ! SendCancelDeleteUser(keyId, 1)
        }
      }

  }

}

object SubscriberActor {

  def apply(
             publisherActor: ActorRef,
             errorPublisherActor: ActorRef,
             deleteUserResultPublisherActor: ActorRef,
             clientActor: ActorRef
           )(
             implicit
             system: ActorSystem,
             ec: ExecutionContext,
             timeout: Timeout
           ): Props = Props(new SubscriberActor(publisherActor, errorPublisherActor, deleteUserResultPublisherActor, clientActor))

  final implicit val stringReader: JsonReader[String] = JsonReader.func2Reader(stringFormatter)
  final implicit val booleanReader: JsonReader[Boolean] = JsonReader.func2Reader(booleanFormatter)
  final implicit val stringOptReader: Option[JsValue] => Option[String] = _.map(_.convertTo[String])
  final implicit val booleanOptReader: Option[JsValue] => Option[Boolean] = _.map(_.convertTo[Boolean])
  final implicit val LocalDateOptReader: Option[JsValue] => Option[LocalDate] = _.map(_.convertTo[LocalDate])

  final implicit val biAttributesToUserInfo: JsValue => WebEngageUserInfo = { json =>

    val jsonFields = json.asJsObject.fields

    WebEngageUserInfo(
      user_name = jsonFields.get("user_name"),
      name = jsonFields.get("first_name"),
      family = jsonFields.get("last_name"),
      email = jsonFields.get("email"),
      mobile_no = jsonFields.get("mobile"),
      birth_date = jsonFields.get("birth_date"),
      gender = jsonFields.get("gender"),
      provider = jsonFields.get("provider"),
      anonymous_id = jsonFields.get("anonymous_id"),
      email_opt_in = jsonFields.get("email_opt_in"),
      sms_opt_in = jsonFields.get("sms_opt_in"),
      whatsapp_opt_in = jsonFields.get("whatsapp_opt_in"),
      company = jsonFields.get("company"),
      hashed_email = jsonFields.get("hashed_email"),
      hashed_phone = jsonFields.get("hashed_phone"),
      attributes = Option(jsonFields.filterKeys(attribute =>
        attribute != "user_name" &&
          attribute != "first_name" &&
          attribute != "last_name" &&
          attribute != "email" &&
          attribute != "mobile" &&
          attribute != "birth_date" &&
          attribute != "gender" &&
          attribute != "provider" &&
          attribute != "anonymous_id" &&
          attribute != "email_opt_in" &&
          attribute != "sms_opt_in" &&
          attribute != "whatsapp_opt_in" &&
          attribute != "company" &&
          attribute != "hashed_email" &&
          attribute != "hashed_phone"
      ).toJson)
    )

  }


}
