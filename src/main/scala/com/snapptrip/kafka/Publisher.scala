package com.snapptrip.kafka

import akka.actor.ActorRef
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Keep, Source}
import com.snapptrip.DI._
import com.snapptrip.formats.Formats._
import com.snapptrip.kafka.Setting._
import org.apache.kafka.clients.producer.ProducerRecord
import spray.json.{JsValue, _}

class Publisher(
                 bufferSize: Int,
                 overflowStrategy: OverflowStrategy,
                 topic: String,
                 setting: ProducerSettings[String, String]
               ) {

  def actorRef: ActorRef = Source
    .actorRef[(Key, JsValue)](bufferSize, overflowStrategy)
    .map {
      case (key, value) => new ProducerRecord[String, String](topic, key.toJson.compactPrint, value.toString)
    }.toMat(Producer.plainSink(setting))(Keep.left)
    .run()

}

object Publisher {

  def apply(topic: String): ActorRef = new Publisher(
    bufferSize,
    OverflowStrategy.dropHead,
    topic,
    producerDefaults).actorRef

  val bufferSize = 0

}