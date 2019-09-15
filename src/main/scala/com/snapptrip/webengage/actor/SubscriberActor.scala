package com.snapptrip.webengage.actor

import akka.actor.{Actor, ActorRef, ActorSystem, Cancellable, PoisonPill, Props}
import akka.util.Timeout
import akka.actor._
import com.snapptrip.DI.{ec, system}
import com.snapptrip.kafka.Subscriber
import com.snapptrip.webengage.actor.SubscriberActor.Start
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, _}
import scala.util.Random

class SubscriberActor(
                       implicit
                       system: ActorSystem,
                       ec: ExecutionContext,
                       timeout: Timeout
                     ) extends Actor with LazyLogging {

  val retryStep = 1
  val retryMax = 5
  self ! Start()

  override def receive(): Receive = {

    case Start() =>
      logger.info(s"""start subscriber actor""")

      val userId = "234567890dftghjkl;'dfghjkl;"
      Subscriber.get(userId, "")
      context.actorOf(Props(new WebEngageActor), s"webengage-actor-$userId")
      context.child(s"webengage-actor-$userId").foreach(_ ! PoisonPill)

      retry(Start(), (1 * retryStep).second)

    case _ =>
      logger.info(s"""start subscriber actor""")

  }

  def retry(start: Start, time: FiniteDuration): Cancellable = {
    context.system.scheduler.scheduleOnce(time, self, start)
  }

}

object SubscriberActor {

  private implicit val timeout: Timeout = Timeout(1.minute)
  val subscriberActor: ActorRef = system.actorOf(Props(new SubscriberActor), s"subscriber-Actor-${Random.nextInt}")

  case class Start()

}
