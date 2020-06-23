package com.snapptrip.service.actor

import akka.actor.SupervisorStrategy.{Escalate, Restart, Stop}
import akka.actor.{Actor, ActorRef, ActorSystem, Props, _}
import akka.util.Timeout
import com.snapptrip.api.Messages.WebEngageUserInfoWithUserId
import com.snapptrip.formats.Formats._
import com.snapptrip.kafka.Setting.{DeleteCancelKey, Key}
import com.snapptrip.service.actor.WebEngageActor.{SendCancelDeleteUser, SendDeleteUser, SendEventInfo, SendUserInfo}
import com.snapptrip.utils.Exceptions.{ErrorCodes, ExtendedException}
import com.typesafe.scalalogging.LazyLogging
import spray.json.JsonParser

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Try

class SubscriberActor(
                       publisherActor: ActorRef,
                       errorPublisherActor: ActorRef,
                       deleteUserResultPublisherActor: ActorRef
                     )(
                       implicit
                       system: ActorSystem,
                       ec: ExecutionContext,
                       timeout: Timeout
                     ) extends Actor with LazyLogging {

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
             deleteUserResultPublisherActor: ActorRef
           )(
             implicit
             system: ActorSystem,
             ec: ExecutionContext,
             timeout: Timeout
           ): Props = Props(new SubscriberActor(publisherActor, errorPublisherActor, deleteUserResultPublisherActor))

}
