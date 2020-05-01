package com.snapptrip.service.actor

import akka.actor.SupervisorStrategy.{Restart, Resume, Stop}
import akka.actor.{Actor, ActorInitializationException, ActorKilledException, ActorRef, ActorSystem, OneForOneStrategy, PoisonPill, Props, SupervisorStrategy}
import akka.dispatch.{PriorityGenerator, UnboundedStablePriorityMailbox}
import akka.routing.FromConfig
import akka.util.Timeout
import com.snapptrip.api.Messages.{WebEngageEvent, WebEngageUserInfo}
import com.snapptrip.service.Converter
import com.snapptrip.utils.Exceptions.{ErrorCodes, ExtendedException}
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration._

class ClientActor(
                   dbRouter: => ActorRef,
                   publisherActor: => ActorRef
                 )(
                   implicit timeout: Timeout
                 ) extends Actor with Converter with LazyLogging {

  import ClientActor._

  lazy val userActorRef: ActorRef = context.actorOf(FromConfig.props(UserActor(dbRouter, self, publisherActor)), s"user-router")
  lazy val eventActorRef: ActorRef = context.actorOf(FromConfig.props(EventActor(dbRouter, publisherActor)), s"event-router")

  override def preStart(): Unit = {
    super.preStart()
  }

  override def supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy(10, 60 seconds, loggingEnabled = true) {
      case _: ActorInitializationException => Stop
      case _: ActorKilledException => Stop
      case ex: ExtendedException if ex.errorCode == ErrorCodes.TimeFormatError =>
        if (ex.ref != null) self ! CheckUserResult(Left(ex), ex.ref)
        Resume
      case ex: ExtendedException if ex.errorCode == ErrorCodes.JsonParseError =>
        if (ex.ref != null) self ! CheckUserResult(Left(ex), ex.ref)
        Resume
      case _: Throwable => Restart
    }

  override def receive(): Receive = {

    case RegisterUser(user) =>
      userActorRef ! UserActor.RegisterUser(user, sender())

    case CheckUser(user) =>
      userActorRef ! UserActor.RegisterUser(user, sender())

    case RegisterUserResult(result, ref) =>
      logger.error(result.toString)
      ref ! result

    case CheckUserResult(result, ref) =>
      ref ! result

    case TrackEvent(event) =>
      val ref = sender()
      eventActorRef ! EventActor.TrackEvent(event, null)
      ref ! "Ok"

  }

}

object ClientActor {

  import com.snapptrip.DI.timeout

  def apply(dbActor: => ActorRef, kafkaActor: => ActorRef): Props = {
    Props(new ClientActor(dbActor, kafkaActor))
  }

  case class RegisterUser(userInfo: WebEngageUserInfo) extends Message

  case class CheckUser(userInfo: WebEngageUserInfo) extends Message

  case class RegisterUserResult(result: Either[Exception, String], ref: ActorRef)

  case class CheckUserResult(result: Either[ExtendedException, String], ref: ActorRef)

  case class TrackEvent(event: WebEngageEvent) extends Message

  class Mailbox(
                 setting: ActorSystem.Settings,
                 config: Config
               ) extends UnboundedStablePriorityMailbox(
    PriorityGenerator {
      case _: CheckUser => 0
      case _: RegisterUser => 1
      case _: CheckUserResult => 2
      case _: RegisterUserResult => 3
      case _: TrackEvent => 4
      case _: PoisonPill => 6
      case _ => 5
    })

}
