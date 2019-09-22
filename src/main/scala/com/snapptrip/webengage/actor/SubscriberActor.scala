package com.snapptrip.webengage.actor

import akka.actor.{Actor, ActorRef, ActorSystem, Cancellable, Props, _}
import akka.util.Timeout
import com.snapptrip.DI.{ec, system}
import com.snapptrip.api.Messages.WebEngageUserInfoWithUserId
import com.snapptrip.formats.Formats._
import com.snapptrip.kafka.Core.Key
import com.snapptrip.kafka.Subscriber
import com.snapptrip.webengage.actor.SubscriberActor.{NewRequest, Start}
import com.snapptrip.webengage.actor.WebEngageActor.{SendEventInfo, SendUserInfo}
import com.typesafe.scalalogging.LazyLogging
import spray.json.JsonParser

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, _}
import scala.util.Random

class SubscriberActor(
                       implicit
                       system: ActorSystem,
                       ec: ExecutionContext,
                       timeout: Timeout
                     ) extends Actor with LazyLogging {


  // start subscriber
  Subscriber

  //
  val retryStep = 1
  val retryMax = 5

  self ! Start()

  override def receive(): Receive = {

    case Start() =>
      logger.info(s"""start subscriber actor""")
    //      retry(Start(), (1 * retryStep).second)

    case NewRequest(key, value) =>

      val userId = JsonParser(key).convertTo[Key].userId
      val keyType = JsonParser(key).convertTo[Key].keyType
      if (keyType == "track-user") {
        val user = JsonParser(value).convertTo[WebEngageUserInfoWithUserId]
        if (context.child(s"webengage-actor-$userId").isDefined) {
          context.child(s"webengage-actor-$userId").foreach(_ ! SendUserInfo(user, 1))
        } else {
          context.actorOf(Props(new WebEngageActor), s"webengage-actor-$userId") ! SendUserInfo(user, 1)
        }
      } else if (keyType == "track-event") {
        val event = JsonParser(value)
        if (context.child(s"webengage-actor-$userId").isDefined) {
          context.child(s"webengage-actor-$userId").foreach(_ ! SendEventInfo(userId, event, 1))
        } else {
          context.actorOf(Props(new WebEngageActor), s"webengage-actor-$userId") ! SendEventInfo(userId, event, 1)
        }
      }

    case _ =>
      logger.info(s"""start subscriber actor""")

  }

  def retry(start: Start, time: FiniteDuration): Cancellable = {
    context.system.scheduler.scheduleOnce(time, self, start)
  }

}

object SubscriberActor {

  private implicit val timeout: Timeout = Timeout(3.minute)
  val subscriberActor: ActorRef = system.actorOf(Props(new SubscriberActor), s"subscriber-Actor-${Random.nextInt}")

  case class Start()

  case class NewRequest(key: String, value: String)

}
