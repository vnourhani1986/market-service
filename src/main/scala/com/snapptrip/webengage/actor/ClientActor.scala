package com.snapptrip.webengage.actor

import akka.actor.SupervisorStrategy.Resume
import akka.actor.{Actor, ActorRef, ActorSystem, OneForOneStrategy, PoisonPill, Props, SupervisorStrategy}
import akka.dispatch.{PriorityGenerator, UnboundedStablePriorityMailbox}
import akka.routing.FromConfig
import akka.util.Timeout
import com.snapptrip.api.Messages.{WebEngageEvent, WebEngageUserInfo}
import com.snapptrip.webengage.Converter
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration._

class ClientActor(
                   dbRouter: => ActorRef,
                   kafkaActor: => ActorRef
                 )(
                   implicit timeout: Timeout
                 ) extends Actor with Converter with LazyLogging {

  type E <: Throwable

  import ClientActor._

  lazy val userActorRef: ActorRef = context.actorOf(FromConfig.props(UserActor(dbRouter, kafkaActor)), s"user-router")
  lazy val eventActorRef: ActorRef = context.actorOf(FromConfig.props(EventActor(dbRouter, kafkaActor)), s"event-router")

  override def preStart(): Unit = {
    super.preStart()
  }

  override def supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy(10, 60 seconds, loggingEnabled = true) {
      case _: E => Resume
    }

  override def receive(): Receive = {

    case RegisterUser(user) =>
      userActorRef ! UserActor.RegisterUser(user, sender())

    case CheckUser(user) =>
      userActorRef ! UserActor.RegisterUser(user, sender())

    case RegisterUserResult(result, ref) =>
      ref ! result

    case CheckUserResult(result, ref) =>
      ref ! result

    case TrackEvent(event) =>
      val ref = sender()
      eventActorRef ! EventActor.TrackEvent(event, ref)
      ref ! "Ok"

  }

}

object ClientActor {

  private implicit val timeout: Timeout = Timeout(30.second)

  def apply(dbActor: => ActorRef, kafkaActor: => ActorRef): Props = {
    Props(new ClientActor(dbActor, kafkaActor))
  }

  case class RegisterUser(userInfo: WebEngageUserInfo) extends Message

  case class CheckUser(userInfo: WebEngageUserInfo) extends Message

  case class RegisterUserResult(result: Either[Exception, String], ref: ActorRef)

  case class CheckUserResult(result: Either[Exception, String], ref: ActorRef)

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
      case _ => 5
      case _: PoisonPill => 6
    })

}
