package com.snapptrip.kafka

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.kafka.{CommitterSettings, ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.{Keep, Sink}
import com.snapptrip.DI._

import scala.collection.immutable
import scala.concurrent.Future

class Subscriber(
                  topic: String,
                  onCompleteMessage: String,
                  maxBatch: Int,
                  committerSetting: CommitterSettings,
                  setting: ConsumerSettings[String, String]
                )(
                  keyRecover: String => String
                )(
                  f: (String, String) => Future[Done]
                ) {

  val control: DrainingControl[immutable.Seq[Done]] =
    Consumer
      .committableSource(setting, Subscriptions.topics(topic))
      .mapAsync(10) { msg =>
        f(keyRecover(msg.record.key), msg.record.value).map { _ =>
          msg.committableOffset
        }
      }
      .via(Committer.flow(committerSetting.withMaxBatch(1)))
      .toMat(Sink.seq)(Keep.both)
      .mapMaterializedValue(DrainingControl.apply)
      .run()

}

object Subscriber {

  def apply(
             topic: String,
             setting: ConsumerSettings[String, String]
           )(
             keyRecover: String => String
           )(
             f: (String, String) => Future[Done]
           )(
             implicit system: ActorSystem
           ): Subscriber = new Subscriber(topic, "complete", 1, CommitterSettings(system), setting)(keyRecover)(f)


}