package com.snapptrip.kafka

import com.snapptrip.DI._
import com.snapptrip.kafka.Core._
import akka.Done
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.kafka.{CommitterSettings, ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.{Keep, Sink}

import scala.collection.immutable
import scala.concurrent.Future

class Subscriber {

  private lazy val committerDefaultsInstance = CommitterSettings(system)

  def committerDefaults: CommitterSettings = committerDefaultsInstance

  val consumerSettings: ConsumerSettings[String, String] = Core.consumerDefaults
  val control: DrainingControl[immutable.Seq[Done]] =
    Consumer
      .committableSource(consumerSettings, Subscriptions.topics(topic))
      .mapAsync(10) { msg =>
        get(msg.record.key, msg.record.value).map { _ =>
          msg.committableOffset
        }
      }
      .via(Committer.flow(committerDefaults.withMaxBatch(1)))
      .toMat(Sink.seq)(Keep.both)
      .mapMaterializedValue(DrainingControl.apply)
      .run()


  def get(key: String, value: String): Future[Done] = {
    println(key, value)
    Future.successful(Done)
  }

}
