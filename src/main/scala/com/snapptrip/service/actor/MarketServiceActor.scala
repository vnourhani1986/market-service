package com.snapptrip.service.actor

import akka.actor.SupervisorStrategy.{Restart, Resume, Stop}
import akka.actor.{Actor, ActorInitializationException, ActorKilledException, ActorRef, ActorSystem, OneForOneStrategy, Props, SupervisorStrategy}
import akka.routing.FromConfig
import akka.util.Timeout
import com.snapptrip.DI._
import com.snapptrip.api.Messages.WebEngageUserInfo
import com.snapptrip.kafka.{Publisher, Setting, Subscriber}
import com.snapptrip.models.User
import com.snapptrip.repos.UserRepoImpl
import com.snapptrip.utils.Exceptions.{ErrorCodes, ExtendedException}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class MarketServiceActor(
                          implicit
                          system: ActorSystem,
                          ex: ExecutionContext,
                          timeout: Timeout
                        ) extends Actor with LazyLogging {

  val publisherActor: ActorRef = Publisher(Setting.topic)
  val errorPublisherActor: ActorRef = Publisher(Setting.errorTopic)
  private val subscriberActorRef: ActorRef = context.actorOf(
    SubscriberActor(publisherActor, errorPublisherActor)(system, ex, timeout), "subscriber-actor")
  private val subscriber = Subscriber(Setting.topic, subscriberActorRef)
  private val dbActorRef: ActorRef = context.actorOf(FromConfig.props(DBActor[User, WebEngageUserInfo](UserRepoImpl))
    .withMailbox("mailbox.db-actor"), s"db-router")
  lazy val clientActorRef: ActorRef = context.actorOf(ClientActor(dbActorRef, publisherActor)
    .withMailbox("mailbox.client-actor"), s"client-actor")

  override def preStart(): Unit = {
    super.preStart()
  }

  override def supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy(10, 60 seconds, loggingEnabled = true) {
      case _: ActorInitializationException => Stop
      case _: ActorKilledException => Stop
      case ex: ExtendedException if ex.errorCode == ErrorCodes.DatabaseError => Resume
      case ex: ExtendedException if ex.errorCode == ErrorCodes.AuthenticationError => Stop
      case ex: ExtendedException if ex.errorCode == ErrorCodes.InvalidURL => Stop
      case ex: ExtendedException if ex.errorCode == ErrorCodes.RestServiceError => Stop
      case ex: ExtendedException if ex.errorCode == ErrorCodes.JsonParseError => Resume
      case _: Exception => Restart
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

