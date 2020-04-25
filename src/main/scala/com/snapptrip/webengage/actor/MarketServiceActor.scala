package com.snapptrip.webengage.actor

import akka.actor.SupervisorStrategy.Resume
import akka.actor.{Actor, ActorRef, ActorSystem, OneForOneStrategy, Props, SupervisorStrategy}
import akka.routing.FromConfig
import akka.util.Timeout
import com.snapptrip.repos.WebEngageUserRepoImpl
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class MarketServiceActor(
                          implicit
                          system: ActorSystem,
                          ex: ExecutionContext,
                          timeout: Timeout
                        ) extends Actor with LazyLogging {

  private lazy val dbActorRef: ActorRef = context.actorOf(FromConfig.props(DBActor(WebEngageUserRepoImpl))
    .withMailbox("mailbox.db-actor"), s"db-router")
  lazy val clientActorRef: ActorRef = context.actorOf(FromConfig.props(ClientActor(dbActorRef))
    .withMailbox("mailbox.client-actor"), s"client-actor")

  type E <: Throwable

  override def preStart(): Unit = {
    super.preStart()
  }

  override def supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy(10, 60 seconds, loggingEnabled = true) {
      case _: E => Resume
    }

  override def receive(): Receive = {

    case _ =>

  }

  def getChild(name: String): Option[ActorRef] = {
    context.child(name)
  }

}

object MarketServiceActor {

  def apply()(
    implicit
    system: ActorSystem,
    ex: ExecutionContext,
    timeout: Timeout
  ): (Props, ActorRef) = {
    val instance = new MarketServiceActor()
    (Props(instance), instance.clientActorRef)
  }


}

