package com.snapptrip.webengage.actor

import akka.actor.{Actor, ActorRef, ActorSystem, Props, _}
import akka.util.Timeout
import com.snapptrip.DI.{ec, system}
import com.snapptrip.api.Messages.WebEngageUserInfoWithUserId
import com.snapptrip.formats.Formats._
import com.snapptrip.kafka.Setting.Key
import com.snapptrip.webengage.actor.WebEngageActor.{SendEventInfo, SendUserInfo}
import com.typesafe.scalalogging.LazyLogging
import spray.json.JsonParser

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Random

class SubscriberActor(
                       publisherActor: ActorRef
                     )(
                       implicit
                       system: ActorSystem,
                       ec: ExecutionContext,
                       timeout: Timeout
                     ) extends Actor with LazyLogging {

  override def receive(): Receive = {

    case (key: String, value: String) =>

      val userId = JsonParser(key).convertTo[Key].userId
      val keyType = JsonParser(key).convertTo[Key].keyType
      if (keyType == "track-user") {
        val user = JsonParser(value).convertTo[WebEngageUserInfoWithUserId]
        if (context.child(s"webengage-actor-$userId").isDefined) {
          context.child(s"webengage-actor-$userId").foreach(_ ! SendUserInfo(user, 1))
        } else {
          context.actorOf(WebEngageActor(publisherActor).withMailbox("mailbox.webengage-actor"), s"webengage-actor-$userId") ! SendUserInfo(user, 1)
        }
      } else if (keyType == "track-event") {
        val event = JsonParser(value)
        if (context.child(s"webengage-actor-$userId").isDefined) {
          context.child(s"webengage-actor-$userId").foreach(_ ! SendEventInfo(userId, event, 1))
        } else {
          context.actorOf(WebEngageActor(publisherActor).withMailbox("mailbox.webengage-actor"), s"webengage-actor-$userId") ! SendEventInfo(userId, event, 1)
        }
      }

  }

}

object SubscriberActor {

  def apply(
             publisherActor: ActorRef
           )(
             implicit
             system: ActorSystem,
             ec: ExecutionContext,
             timeout: Timeout
           ): Props = Props(new SubscriberActor(publisherActor))

  private implicit val timeout: Timeout = Timeout(30.second)
  val subscriberActor: ActorRef = system.actorOf(Props(new SubscriberActor(null)), s"subscriber-Actor-${Random.nextInt}")

  val retryStep = 1
  val retryMax = 5

  case class NewRequest(key: String, value: String)

}
