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
import com.snapptrip.utils.WebEngageConfig
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import spray.json._

import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{ExecutionContext, Future}

class WebEngageActor(
                      publisherActor: ActorRef,
                      errorPublisherActor: ActorRef,
                      deleteUserResultPublisherActor: ActorRef
                    )(
                      implicit
                      system: ActorSystem,
                      ec: ExecutionContext,
                      timeout: Timeout
                    ) extends Actor with LazyLogging {

  import WebEngageActor._

  override def receive(): Receive = {

    case SendUserInfo(user, retryCount) =>

      WebEngageApi.post(user.toJson, WebEngageConfig.usersUrl)
        .map {
          case (status, _) if status == StatusCodes.Created =>
            self ! PoisonPill

          case (status, entity) if status == StatusCodes.BadRequest =>
            errorPublisherActor ! (Key(user.userId.get, "track-user"), entity)
            throw ExtendedException("bad request to webengage", ErrorCodes.BadRequestError)

          case (status, entity) if status == StatusCodes.Unauthorized =>
            errorPublisherActor ! (Key(user.userId.get, "track-user"), entity)
            throw ExtendedException("webengage authentication fail", ErrorCodes.AuthenticationError)

          case (status, entity) if status == StatusCodes.NotFound =>
            errorPublisherActor ! (Key(user.userId.get, "track-user"), entity)
            throw ExtendedException("route not found", ErrorCodes.InvalidURL)

        }.recover {
        case error: ExtendedException => Future.failed(error)
        case error =>
          if (retryCount < retryMax) {
            retryTrackUser(user, (retryCount * retryStep).second, retryCount + 1)
          } else {
            errorPublisherActor ! (Key(user.userId.get, "track-user"), JsString(error.getMessage))
            throw ExtendedException(error.getMessage, ErrorCodes.RestServiceError)
          }
      }

    case SendEventInfo(userId, event, retryCount) =>

      WebEngageApi.post(event, WebEngageConfig.eventsUrl)
        .map {

          case (status, _) if status == StatusCodes.Created =>
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
            retryTrackEvent(userId, event, (retryCount * retryStep).second, retryCount + 1)
          } else {
            errorPublisherActor ! (Key(userId, "track-event"), JsString(error.getMessage))
            throw ExtendedException(error.getMessage, ErrorCodes.RestServiceError)
          }
      }

    case SendDeleteUser(userId, opengdprBody, retryCount) =>

      WebEngageApi.post(opengdprBody, WebEngageConfig.opengdprRequestsUrl)
        .map {

          case (status, entity) if status == StatusCodes.Created =>
            deleteUserResultPublisherActor ! (Key(userId, "delete-user-result"), entity)
            self ! PoisonPill

          case (status, entity) if status == StatusCodes.BadRequest =>
            logger.error(s"bad request $entity")
            errorPublisherActor ! (Key(userId, "delete-user"), entity)
            throw ExtendedException("bad request to webengage", ErrorCodes.BadRequestError)

          case (status, entity) if status == StatusCodes.Unauthorized =>
            logger.error(s"un auth $entity")
            errorPublisherActor ! (Key(userId, "delete-user"), entity)
            throw ExtendedException("webengage authentication fail", ErrorCodes.AuthenticationError)

          case (status, entity) if status == StatusCodes.NotFound =>
            logger.error(s"not found $entity")
            errorPublisherActor ! (Key(userId, "delete-user"), entity)
            throw ExtendedException("route not found", ErrorCodes.InvalidURL)

          case (status, entity) if status == StatusCodes.InternalServerError =>
            logger.error(s"not found $entity")
            errorPublisherActor ! (Key(userId, "delete-user"), entity)
            throw ExtendedException("unforeseen service issues", ErrorCodes.InternalSeverError)

        }.recover {
        case error: ExtendedException => Future.failed(error)
        case error =>
          if (retryCount < retryMax) {
            retrySendDeleteUser(userId, opengdprBody, (retryCount * retryStep).second, retryCount + 1)
          } else {
            errorPublisherActor ! (Key(userId, "delete-user"), JsString(error.getMessage))
            throw ExtendedException(error.getMessage, ErrorCodes.RestServiceError)
          }
      }

    case SendCancelDeleteUser(requestId, retryCount) =>

      WebEngageApi.delete(WebEngageConfig.opengdprRequestsUrl + "/" + requestId)
        .map {

          case (status, entity) if status == StatusCodes.Created =>
            deleteUserResultPublisherActor ! (Key(requestId, "delete-user-cancel-result"), entity)
            self ! PoisonPill

          case (status, entity) if status == StatusCodes.BadRequest =>
            logger.error(s"bad request $entity")
            errorPublisherActor ! (Key(requestId, "cancel-delete-user"), entity)
            throw ExtendedException("bad request to webengage", ErrorCodes.BadRequestError)

          case (status, entity) if status == StatusCodes.Unauthorized =>
            logger.error(s"un auth $entity")
            errorPublisherActor ! (Key(requestId, "cancel-delete-user"), entity)
            throw ExtendedException("webengage authentication fail", ErrorCodes.AuthenticationError)

          case (status, entity) if status == StatusCodes.NotFound =>
            logger.error(s"not found $entity")
            errorPublisherActor ! (Key(requestId, "cancel-delete-user"), entity)
            throw ExtendedException("route not found", ErrorCodes.InvalidURL)

          case (status, entity) if status == StatusCodes.InternalServerError =>
            logger.error(s"not found $entity")
            errorPublisherActor ! (Key(requestId, "cancel-delete-user"), entity)
            throw ExtendedException("unforeseen service issues", ErrorCodes.InternalSeverError)

        }.recover {
        case error: ExtendedException => Future.failed(error)
        case error =>
          if (retryCount < retryMax) {
            retrySendCancelDeleteUser(requestId, (retryCount * retryStep).second, retryCount + 1)
          } else {
            errorPublisherActor ! (Key(requestId, "cancel-delete-user"), JsString(error.getMessage))
            throw ExtendedException(error.getMessage, ErrorCodes.RestServiceError)
          }
      }

  }

  def retryTrackUser(issueRequest: WebEngageUserInfoWithUserId, time: FiniteDuration, retryCount: Int): Cancellable = {
    context.system.scheduler.scheduleOnce(time, self, SendUserInfo(issueRequest, retryCount))
  }

  def retryTrackEvent(userId: String, issueRequest: JsValue, time: FiniteDuration, retryCount: Int): Cancellable = {
    context.system.scheduler.scheduleOnce(time, self, SendEventInfo(userId, issueRequest, retryCount))
  }

  def retrySendDeleteUser(userId: String, opengdprBody: JsValue, time: FiniteDuration, retryCount: Int): Cancellable = {
    context.system.scheduler.scheduleOnce(time, self, SendDeleteUser(userId, opengdprBody, retryCount))
  }

  def retrySendCancelDeleteUser(requestId: String, time: FiniteDuration, retryCount: Int): Cancellable = {
    context.system.scheduler.scheduleOnce(time, self, SendCancelDeleteUser(requestId, retryCount))
  }

}

object WebEngageActor {

  def apply(
             publisherActor: ActorRef,
             errorPublisherActor: ActorRef,
             deleteUserResultPublisherActor: ActorRef
           )(
             implicit
             system: ActorSystem,
             ec: ExecutionContext,
             timeout: Timeout
           ): Props = Props(new WebEngageActor(publisherActor, errorPublisherActor, deleteUserResultPublisherActor))

  val retryStep = 10
  val retryMax = 5

  case class SendUserInfo(user: WebEngageUserInfoWithUserId, retryCount: Int) extends Message

  case class SendEventInfo(userId: String, event: JsValue, retryCount: Int) extends Message

  case class SendDeleteUser(userId: String, opengdprBody: JsValue, retryCount: Int) extends Message

  case class SendCancelDeleteUser(requestId: String, retryCount: Int) extends Message

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
