package com.snapptrip.service.actor

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorRef, ActorSystem, Props, _}
import akka.util.Timeout
import com.snapptrip.api.Messages.WebEngageUserInfoWithUserId
import com.snapptrip.formats.Formats._
import com.snapptrip.kafka.Setting.Key
import com.snapptrip.service.actor.WebEngageActor.{SendEventInfo, SendUserInfo}
import com.snapptrip.utils.Exceptions.{ErrorCodes, ExtendedException}
import com.typesafe.scalalogging.LazyLogging
import spray.json.JsonParser

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Try

class SubscriberActor(
                       publisherActor: ActorRef,
                       errorPublisherActor: ActorRef
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

      val (userId, keyType, user, event) = Try {
        val userId = JsonParser(key).convertTo[Key].userId
        val keyType = JsonParser(key).convertTo[Key].keyType
        val (user, event) = if (keyType == "track-user") {
          (Some(JsonParser(value).convertTo[WebEngageUserInfoWithUserId]), None)
        } else {
          (None, Some(JsonParser(value)))
        }
        (userId, keyType, user, event)
      }.toEither match {
        case Right(v) => v
        case Left(exception) => throw ExtendedException(exception.getMessage, ErrorCodes.JsonParseError)
      }

      if (keyType == "track-user") {
        if (context.child(s"webengage-actor-$userId").isDefined) {
          context.child(s"webengage-actor-$userId").foreach(_ ! SendUserInfo(user.get, 1))
        } else {
          context.actorOf(WebEngageActor(publisherActor, errorPublisherActor), s"webengage-actor-$userId") ! SendUserInfo(user.get, 1)
        }
      } else if (keyType == "track-event") {
        if (context.child(s"webengage-actor-$userId").isDefined) {
          context.child(s"webengage-actor-$userId").foreach(_ ! SendEventInfo(userId, event.get, 1))
        } else {
          context.actorOf(WebEngageActor(publisherActor, errorPublisherActor), s"webengage-actor-$userId") ! SendEventInfo(userId, event.get, 1)
        }
      }

  }

}

object SubscriberActor {

  def apply(
             publisherActor: ActorRef,
             errorPublisherActor: ActorRef
           )(
             implicit
             system: ActorSystem,
             ec: ExecutionContext,
             timeout: Timeout
           ): Props = Props(new SubscriberActor(publisherActor, errorPublisherActor))

}
