package com.snapptrip.kafka

import akka.Done
import akka.actor.ActorRef
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.kafka.{CommitterSettings, ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.{Keep, Sink}
import com.snapptrip.DI._
import com.snapptrip.kafka.Setting._
import com.snapptrip.service.actor.SubscriberActor
import com.snapptrip.service.actor.SubscriberActor.NewRequest

import scala.concurrent.Future

class Subscriber(
                  topic: String,
                  actorRef: ActorRef,
                  onCompleteMessage: String,
                  maxBatch: Int,
                  committerSetting: CommitterSettings,
                  setting: ConsumerSettings[String, String]
                ) {

  def consumer: Consumer.Control = Consumer
    .committableSource(setting, Subscriptions.topics(topic))
    .map { msg =>
      msg.committableOffset
    }
    .via(Committer.flow(committerSetting.withMaxBatch(maxBatch)))
    .toMat(Sink.actorRef(actorRef, onCompleteMessage))(Keep.left)
    .run()

}

object Subscriber {

  def apply(
             topic: String,
             actorRef: ActorRef
           ): Subscriber = new Subscriber(topic, actorRef, "", 1, committerDefaultsInstance, consumerDefaults)

  private lazy val committerDefaultsInstance: CommitterSettings = CommitterSettings(system)

  def committerDefaults: CommitterSettings = committerDefaultsInstance

  val control = Consumer
    .committableSource(consumerDefaults, Subscriptions.topics(topic))
    .map { msg =>
      msg.committableOffset
    }
    .via(Committer.flow(committerDefaults.withMaxBatch(1)))
    .toMat(Sink.actorRef(SubscriberActor.subscriberActor, ""))(Keep.both)
    .run()

  def get(key: String, value: String): Future[Done] = {
    SubscriberActor.subscriberActor ! NewRequest(key, value)
    Future.successful(Done)
  }

}
