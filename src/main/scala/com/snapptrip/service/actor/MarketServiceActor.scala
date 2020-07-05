package com.snapptrip.service.actor

import akka.Done
import akka.actor.SupervisorStrategy.{Restart, Resume, Stop}
import akka.actor.{Actor, ActorInitializationException, ActorKilledException, ActorRef, ActorSystem, OneForOneStrategy, Props, SupervisorStrategy}
import akka.pattern.ask
import akka.routing.FromConfig
import akka.util.Timeout
import com.snapptrip.DI._
import com.snapptrip.api.Messages.WebEngageUserInfo
import com.snapptrip.kafka.Setting._
import com.snapptrip.kafka.{Publisher, Setting, Subscriber}
import com.snapptrip.models.User
import com.snapptrip.repos.UserRepoImpl
import com.snapptrip.service.actor.ClientActor.CheckUser
import com.snapptrip.utils.Exceptions.{ErrorCodes, ExtendedException}
import com.typesafe.scalalogging.LazyLogging
import spray.json.{JsObject, JsString}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class MarketServiceActor(
                          implicit
                          system: ActorSystem,
                          ex: ExecutionContext,
                          timeout: Timeout
                        ) extends Actor with LazyLogging {

  val publisherActor: ActorRef = Publisher(marketTopic)
  val errorPublisherActor: ActorRef = Publisher(Setting.errorTopic)
  val deleteUserResultPublisherActor: ActorRef = Publisher(Setting.deleteUserResultTopic)
  private val subscriberActorRef: ActorRef = context.actorOf(
    SubscriberActor(publisherActor, errorPublisherActor, deleteUserResultPublisherActor, clientActorRef)(system, ex, timeout), "subscriber-actor")
  private val marketSubscriber = Subscriber(marketTopic, setting = setConsumer(marketServer, consumerGroup))(key => key)((k, v) => Future {
    subscriberActorRef ! (k, v)
    Done
  })
  private val biSubscriber = Subscriber(userAttributesTopic, setting = setConsumer(biServer, consumerGroup))(key => key)((_, v) =>
    (clientActorRef ? CheckUser(v)).mapTo[Either[ExtendedException, String]].map {
      case Right(_) =>
        Done
      case Left(exception) =>
        errorPublisherActor ! (Key("bi", "check-user"), JsObject("data" -> JsString(v), "error" -> JsString(exception.message)))
        Done
    }.recover {
      case exception: Throwable =>
        errorPublisherActor ! (Key("bi", "check-user"), JsObject("data" -> JsString(v), "error" -> JsString(exception.getMessage)))
        Done
    }
  )
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

