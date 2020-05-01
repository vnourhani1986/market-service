package com.snapptrip.service.actor

import akka.actor.{Actor, ActorRef, ActorSystem, Cancellable, PoisonPill, Props}
import akka.dispatch.{PriorityGenerator, UnboundedStablePriorityMailbox}
import akka.http.scaladsl.model.StatusCodes
import akka.util.Timeout
import com.snapptrip.api.Messages.WebEngageUserInfoWithUserId
import com.snapptrip.formats.Formats._
import com.snapptrip.kafka.Setting.Key
import com.snapptrip.service.api.WebEngageApi
import com.snapptrip.utils.Exceptions.{ErrorCodes, ExtendedException}
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{FiniteDuration, _}

class WebEngageActor(
                      publisherActor: ActorRef,
                      errorPublisherActor: ActorRef
                    )(
                      implicit
                      system: ActorSystem,
                      ec: ExecutionContext,
                      timeout: Timeout
                    ) extends Actor with LazyLogging {

  import WebEngageActor._

  override def receive(): Receive = {

    case SendUserInfo(user, retryCount) =>

      WebEngageApi.trackUser(user.toJson)
        .map {
          case (status, _) if status == StatusCodes.Created =>
            self ! PoisonPill

          case (status, entity) if status == StatusCodes.BadRequest =>
            errorPublisherActor ! (Key(user.userId, "track-user"), entity)
            throw ExtendedException("bad request to webengage", ErrorCodes.BadRequestError)

          case (status, entity) if status == StatusCodes.Unauthorized =>
            errorPublisherActor ! (Key(user.userId, "track-user"), entity)
            throw ExtendedException("webengage authentication fail", ErrorCodes.AuthenticationError)

          case (status, entity) if status == StatusCodes.NotFound =>
            errorPublisherActor ! (Key(user.userId, "track-user"), entity)
            throw ExtendedException("route not found", ErrorCodes.InvalidURL)

        }.recover {
        case error: ExtendedException => Future.failed(error)
        case error =>
          if (retryCount < retryMax) {
            retry(user, (retryCount * retryStep).second, retryCount + 1)
          } else {
            errorPublisherActor ! (Key(user.userId, "track-user"), JsString(error.getMessage))
            throw ExtendedException(error.getMessage, ErrorCodes.RestServiceError)
          }
      }

    case SendEventInfo(userId, event, retryCount) =>

      logger.error(s"send event info $userId, $event")
      WebEngageApi.trackEvent(event)
        .map {

          case (status, _) if status == StatusCodes.Created =>
            logger.error(s"send event Created")
            self ! PoisonPill

          case (status, entity) if status == StatusCodes.BadRequest =>
            logger.error(s"bad request $entity")
            errorPublisherActor ! (Key(userId, "track-event"), entity)
            throw ExtendedException("bad request to webengage", ErrorCodes.BadRequestError)

          case (status, entity) if status == StatusCodes.Unauthorized =>
            logger.error(s"un auth $entity")
            errorPublisherActor ! (Key(userId, "track-event"), entity)
            throw ExtendedException("webengage authentication fail", ErrorCodes.AuthenticationError)

          case (status, entity) if status == StatusCodes.NotFound =>
            logger.error(s"not found $entity")
            errorPublisherActor ! (Key(userId, "track-event"), entity)
            throw ExtendedException("route not found", ErrorCodes.InvalidURL)

        }.recover {
        case error: ExtendedException => Future.failed(error)
        case error =>
          if (retryCount < retryMax) {
            retry(userId, event, (retryCount * retryStep).second, retryCount + 1)
          } else {
            errorPublisherActor ! (Key(userId, "track-event"), JsString(error.getMessage))
            throw ExtendedException(error.getMessage, ErrorCodes.RestServiceError)
          }
      }

  }

  def retry(issueRequest: WebEngageUserInfoWithUserId, time: FiniteDuration, retryCount: Int): Cancellable = {
    context.system.scheduler.scheduleOnce(time, self, SendUserInfo(issueRequest, retryCount))
  }

  def retry(userId: String, issueRequest: JsValue, time: FiniteDuration, retryCount: Int): Cancellable = {
    context.system.scheduler.scheduleOnce(time, self, SendEventInfo(userId, issueRequest, retryCount))
  }

}

object WebEngageActor {

  def apply(
             publisherActor: ActorRef,
             errorPublisherActor: ActorRef
           )(
             implicit
             system: ActorSystem,
             ec: ExecutionContext,
             timeout: Timeout
           ): Props = Props(new WebEngageActor(publisherActor, errorPublisherActor))

  val retryStep = 10
  val retryMax = 5

  case class SendUserInfo(user: WebEngageUserInfoWithUserId, retryCount: Int) extends Message

  case class SendEventInfo(userId: String, event: JsValue, retryCount: Int) extends Message

  class Mailbox(
                 setting: ActorSystem.Settings,
                 config: Config
               ) extends UnboundedStablePriorityMailbox(
    PriorityGenerator {
      case _: SendUserInfo => 0
      case _: SendEventInfo => 1
      case _: PoisonPill => 3
      case _ => 2
    })


}
