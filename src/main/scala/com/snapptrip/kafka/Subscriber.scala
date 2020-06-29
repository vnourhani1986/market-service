package com.snapptrip.kafka

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.kafka.{CommitterSettings, ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.{Keep, Sink}
import com.snapptrip.DI._

import scala.collection.immutable
import scala.concurrent.Future

class Subscriber(
                  topic: String,
                  actorRef: ActorRef,
                  onCompleteMessage: String,
                  maxBatch: Int,
                  committerSetting: CommitterSettings,
                  setting: ConsumerSettings[String, String]
                ) {


  val control: DrainingControl[immutable.Seq[Done]] =
    Consumer
      .committableSource(setting, Subscriptions.topics(topic))
      .mapAsync(10) { msg =>
        get(msg.record.key, msg.record.value).map { _ =>
          msg.committableOffset
        }
      }
      .via(Committer.flow(committerSetting.withMaxBatch(1)))
      .toMat(Sink.seq)(Keep.both)
      .mapMaterializedValue(DrainingControl.apply)
      .run()

  def get(key: String, value: String): Future[Done] = {
    Future.successful {
      actorRef ! (key, value)
      Done
    }
  }

}

object Subscriber {

  def apply(
             topic: String,
             actorRef: ActorRef,
             setting: ConsumerSettings[String, String]
           )(
             implicit system: ActorSystem
           ): Subscriber = new Subscriber(topic, actorRef, "complete", 1, CommitterSettings(system), setting)


}