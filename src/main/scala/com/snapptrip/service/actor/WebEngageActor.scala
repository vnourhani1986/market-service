package com.snapptrip.service.actor

import akka.actor.{Actor, ActorRef, ActorSystem, Cancellable, PoisonPill, Props}
import akka.dispatch.{PriorityGenerator, UnboundedStablePriorityMailbox}
import akka.http.scaladsl.model.StatusCodes
import akka.util.Timeout
import com.snapptrip.api.Messages.WebEngageUserInfoWithUserId
import com.snapptrip.formats.Formats._
import com.snapptrip.kafka.Setting.Key
import com.snapptrip.service.api.WebEngageApi
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import spray.json._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, _}

class WebEngageActor(
                      publisherActor: ActorRef
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
          case (status, _) if status == StatusCodes.InternalServerError =>
            if (retryCount < retryMax) {
              val rt = retryCount + 1
              retry(user, (retryCount * retryStep).second, rt)
            } else {
              publisherActor ! (Key(user.userId, "track-user"), user.toJson)
            }

          case _ =>

            if (retryCount < retryMax) {
              val rt = retryCount + 1
              retry(user, (retryCount * retryStep).second, rt)
            } else {
              publisherActor ! (Key(user.userId, "track-user"), user.toJson)
            }
        }

    case SendEventInfo(userId, event, retryCount) =>

      WebEngageApi.trackEventWithUserId(event)
        .map {
          case (status, _) if status == StatusCodes.Created =>
            self ! PoisonPill
          case (status, _) if status == StatusCodes.InternalServerError =>
            if (retryCount < retryMax) {
              val rt = retryCount + 1
              retry(userId, event, (retryCount * retryStep).second, rt)
            } else {
              publisherActor ! (Key(userId, "track-event"), event)
            }

          case _ =>

            if (retryCount < retryMax) {
              val rt = retryCount + 1
              retry(userId, event, (retryCount * retryStep).second, rt)
            } else {
              publisherActor ! (Key(userId, "track-event"), event)
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
             publisherActor: ActorRef
           )(
             implicit
             system: ActorSystem,
             ec: ExecutionContext,
             timeout: Timeout
           ): Props = Props(new WebEngageActor(publisherActor))

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
