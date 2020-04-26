package com.snapptrip.service.actor

import akka.actor.SupervisorStrategy.Resume
import akka.actor.{Actor, ActorRef, ActorSystem, OneForOneStrategy, Props, SupervisorStrategy}
import akka.routing.FromConfig
import akka.util.Timeout
import com.snapptrip.DI._
import com.snapptrip.kafka.{Publisher, Subscriber}
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

  val publisherActor: ActorRef = Publisher("webengage")
  private val subscriberActorRef: ActorRef = context.actorOf(SubscriberActor(publisherActor)(system, ex, timeout), "subscriber-actor")
  private val subscriber = Subscriber("webengage", subscriberActorRef)
  private lazy val dbActorRef: ActorRef = context.actorOf(FromConfig.props(DBActor(WebEngageUserRepoImpl))
    .withMailbox("mailbox.db-actor"), s"db-router")
  lazy val clientActorRef: ActorRef = context.actorOf(FromConfig.props(ClientActor(dbActorRef, publisherActor))
    .withMailbox("mailbox.client-actor"), s"client-actor")

  override def preStart(): Unit = {
    super.preStart()
  }

  override def supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy(10, 60 seconds, loggingEnabled = true) {
      case _: Exception => Resume
    }

  override def receive(): Receive = {

    case message: Any => clientActorRef.forward(message)

  }

}

object MarketServiceActor {

  def apply()(
    implicit
    system: ActorSystem,
    ex: ExecutionContext,
    timeout: Timeout
  ) = Props(new MarketServiceActor())

}

