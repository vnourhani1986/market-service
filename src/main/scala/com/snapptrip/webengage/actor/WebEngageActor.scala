package com.snapptrip.webengage.actor

import akka.Done
import akka.actor.{Actor, ActorRef, ActorSystem, Cancellable, PoisonPill, Props}
import akka.dispatch.ControlMessage
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.ask
import akka.util.Timeout
import com.snapptrip.formats.Formats._
import com.snapptrip.kafka.Core.Key
import com.snapptrip.kafka.Publisher
import com.snapptrip.webengage.actor.WebEngageActor.{SendEventInfo, SendUserInfo}
import com.snapptrip.webengage.api.WebEngageApi
import com.typesafe.scalalogging.LazyLogging
import spray.json._
import com.snapptrip.DI._
import com.snapptrip.api.Messages.WebEngageUserInfoWithUserId

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, _}
import scala.util.Random

object WebEngageActor {

  private implicit val timeout: Timeout = Timeout(3.minute)
  val webEngageActor: ActorRef = system.actorOf(Props(new WebEngageActor), s"webengage-actor-${Random.nextInt}")

  case class SendUserInfo(user: WebEngageUserInfoWithUserId, retryCount: Int) extends ControlMessage

  case class SendEventInfo(userId: String, event: JsValue, retryCount: Int) extends ControlMessage

}

class WebEngageActor(
                      implicit
                      system: ActorSystem,
                      ec: ExecutionContext,
                      timeout: Timeout
                    ) extends Actor with LazyLogging {

  val retryStep = 10
  val retryMax = 5

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
              Publisher.publish(Key(user.userId, "track-user"), List(user.toJson)).map { _ =>
                self ! PoisonPill
              }.recover {
                case error: Throwable =>
                  logger.info(s"""publish user data to kafka with error: ${error.getMessage}""")
                  self ! SendUserInfo(user, retryCount)
                  Done
              }
            }

          case _ =>

            if (retryCount < retryMax) {
              val rt = retryCount + 1
              retry(user, (retryCount * retryStep).second, rt)
            } else {
              Publisher.publish(Key(user.userId, "track-user"), List(user.toJson)).map { _ =>
                self ! PoisonPill
              }.recover {
                case error: Throwable =>
                  logger.info(s"""publish user data to kafka with error: ${error.getMessage}""")
                  self ! SendUserInfo(user, retryCount)
                  Done
              }
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
              Publisher.publish(Key(userId, "track-event"), List(event)).map { _ =>
                self ! PoisonPill
              }.recover {
                case error: Throwable =>
                  logger.info(s"""publish event data to kafka with error: ${error.getMessage}""")
                  self ! SendEventInfo(userId, event, retryCount)
                  Done
              }
            }

          case _ =>

            if (retryCount < retryMax) {
              val rt = retryCount + 1
              retry(userId, event, (retryCount * retryStep).second, rt)
            } else {
              Publisher.publish(Key(userId, "track-event"), List(event)).map { _ =>
                self ! PoisonPill
              }.recover {
                case error: Throwable =>
                  logger.info(s"""publish event data to kafka with error: ${error.getMessage}""")
                  self ! SendEventInfo(userId, event, retryCount)
                  Done
              }
            }
        }

    case _ =>
      logger.info(s"""other messages""")
      sender ? JsObject("status" -> JsString("success"), "message" -> JsString("other messages"))
      self ! PoisonPill

  }

  def retry(issueRequest: WebEngageUserInfoWithUserId, time: FiniteDuration, retryCount: Int): Cancellable = {
    context.system.scheduler.scheduleOnce(time, self, SendUserInfo(issueRequest, retryCount))
  }

  def retry(userId: String, issueRequest: JsValue, time: FiniteDuration, retryCount: Int): Cancellable = {
    context.system.scheduler.scheduleOnce(time, self, SendEventInfo(userId, issueRequest, retryCount))
  }

}


