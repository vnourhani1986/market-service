package com.snapptrip.webengage

import akka.actor.{Actor, ActorRef, ActorSystem, Cancellable, Props}
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.snapptrip.api.Messages.WebEngageUserInfoWithUserId
import com.snapptrip.formats.Formats._
import com.snapptrip.services.WebEngage
import com.snapptrip.utils.DeleteWebEngageUsersUtil
import com.typesafe.scalalogging.LazyLogging
import spray.json._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, _}
import scala.util.Random

class WebengageService(
                        implicit
                        system: ActorSystem,
                        ec: ExecutionContext,
                        timeout: Timeout,
                        mat: ActorMaterializer
                      ) {

  val props = Props(new WebEngageActor)
  val actor: ActorRef = system.actorOf(props, s"WebengageActor-${Random.nextInt}")
  val props1 = Props(new DeleteWebEngageUserActor())
  val actor1: ActorRef = system.actorOf(props1, s"delete-webengage-user-actor-${Random.nextInt}")

}

case class SendUserInfo(user: WebEngageUserInfoWithUserId, retryCount: Int)

case class SendEventInfo(event: JsValue, retryCount: Int)

case class Start()

private class WebEngageActor(
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
      WebEngage.trackUser(user.toJson)
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
      WebEngage.trackEventWithUserId(event)
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

private class DeleteWebEngageUserActor(
                                        implicit
                                        system: ActorSystem,
                                        ec: ExecutionContext,
                                        timeout: Timeout
                                      ) extends Actor with LazyLogging {

  val retryStep = 10
  val retryMax = 5
  self ! Start()
  println("hi")

  override def receive(): Receive = {

    case Start() =>
      logger.info(s"""delete user actor start""")
      DeleteWebEngageUsersUtil.deleteWebEngageUsersTable().map(_ => self ! Start())

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
