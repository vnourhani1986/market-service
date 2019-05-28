package com.snapptrip.webengage

import akka.actor.{Actor, ActorSystem, Cancellable, PoisonPill, Props}
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.snapptrip.api.Messages.{WebEngageUserInfo, WebEngageUserInfoWithUserId}
import com.snapptrip.formats.Formats._
import com.snapptrip.services.WebEngage
import com.typesafe.scalalogging.LazyLogging
import spray.json._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.util.Random
import scala.concurrent.duration._

class WebengageService(
                        implicit
                        system: ActorSystem,
                        ec: ExecutionContext,
                        timeout: Timeout,
                        mat: ActorMaterializer
                      ) {

  val props = Props(new WebEngageActor)
  val actor = system.actorOf(props, s"WebengageActor-${Random.nextInt}")

}

case class SendUserInfo(user: WebEngageUserInfoWithUserId)

private class WebEngageActor(
                              implicit
                              system: ActorSystem,
                              ec: ExecutionContext,
                              timeout: Timeout
                            ) extends Actor with LazyLogging {

  var retryCount = 1
  val retryStep = 10

  override def receive(): Receive = {

    case SendUserInfo(user) =>
      logger.info(s"""send user info to webengage retry for $retryCount""")
      sender ? JsObject("status" -> JsString("success"))
      WebEngage.trackUser(user.toJson)
        .map {
        case (status, _) if status == StatusCodes.Created =>
          logger.info(s"""receive user info from webengage status $status""")
          self ? PoisonPill
        case (status, _) if status == StatusCodes.InternalServerError =>
          logger.info(s"""receive user info from webengage status $status""")
          retryCount += 1
          retry(user, (retryCount * retryStep).second)
        case _ =>
          logger.info(s"""receive user info from webengage with invalid response""")
          retryCount += 1
          retry(user, (retryCount * retryStep).second)
      }

    case _ =>
      logger.info(s"""other messages""")
      sender ? JsObject("status" -> JsString("success"))
      self ? PoisonPill

  }

  def retry(issueRequest: WebEngageUserInfoWithUserId, time: FiniteDuration): Cancellable = {
    context.system.scheduler.scheduleOnce(time, self, SendUserInfo(issueRequest))
  }


}
