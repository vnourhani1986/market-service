package com.snapptrip.webengage.actor

import akka.actor.{Actor, ActorRef, ActorSystem, Cancellable, Props}
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.ask
import akka.util.Timeout
import com.snapptrip.formats.Formats._
import com.snapptrip.api.Messages.WebEngageUserInfoWithUserId
import com.snapptrip.webengage.actor.WebEngageActor.{SendEventInfo, SendUserInfo}
import com.snapptrip.webengage.api.WebEngageApi
import com.typesafe.scalalogging.LazyLogging
import spray.json._
import com.snapptrip.DI._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, _}
import scala.util.Random

object WebEngageActor {

  private implicit val timeout: Timeout = Timeout(1.minute)
  val webEngageActor: ActorRef = system.actorOf(Props(new WebEngageActor), s"webengage-actor-${Random.nextInt}")

  case class SendUserInfo(user: WebEngageUserInfoWithUserId, retryCount: Int)

  case class SendEventInfo(event: JsValue, retryCount: Int)

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
      logger.info(s"""send user info actor to webengage retry for $retryCount""")
      if (sender != self) sender ? (200, JsObject("status" -> JsString("success")))
      WebEngageApi.trackUser(user.toJson)
        .map {
          case (status, _) if status == StatusCodes.Created =>
            logger.info(s"""receive user info actor from webengage status $status""")
          case (status, _) if status == StatusCodes.InternalServerError =>
            logger.info(s"""receive user info from webengage status $status""")
            if (retryCount < retryMax) {
              val rt = retryCount + 1
              retry(user, (retryCount * retryStep).second, rt)
            }
          case _ =>
            logger.info(s"""receive user info actor from webengage with invalid response""")
            if (retryCount < retryMax) {
              val rt = retryCount + 1
              retry(user, (retryCount * retryStep).second, rt)
            }
        }

    case SendEventInfo(event, retryCount) =>
      logger.info(s"""send event info actor to webengage retry for $retryCount""")
      if (sender != self) sender ? (200, JsObject("status" -> JsString("success")))
      WebEngageApi.trackEventWithUserId(event)
        .map {
          case (status, _) if status == StatusCodes.Created =>
            logger.info(s"""receive event info actor from webengage status $status""")
          case (status, _) if status == StatusCodes.InternalServerError =>
            logger.info(s"""receive event info actor from webengage status $status""")
            if (retryCount < retryMax) {
              val rt = retryCount + 1
              retry(event, (retryCount * retryStep).second, rt)
            }
          case _ =>
            logger.info(s"""receive event info actor from webengage with invalid response""")
            if (retryCount < retryMax) {
              val rt = retryCount + 1
              retry(event, (retryCount * retryStep).second, rt)
            }
        }

    case _ =>
      logger.info(s"""other messages""")
      sender ? JsObject("status" -> JsString("success"))

  }

  def retry(issueRequest: WebEngageUserInfoWithUserId, time: FiniteDuration, retryCount: Int): Cancellable = {
    context.system.scheduler.scheduleOnce(time, self, SendUserInfo(issueRequest, retryCount))
  }

  def retry(issueRequest: JsValue, time: FiniteDuration, retryCount: Int): Cancellable = {
    context.system.scheduler.scheduleOnce(time, self, SendEventInfo(issueRequest, retryCount))
  }

}
