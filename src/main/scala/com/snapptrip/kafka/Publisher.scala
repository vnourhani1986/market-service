package com.snapptrip.kafka

import akka.Done
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.Source
import com.snapptrip.DI._
import com.snapptrip.kafka.Core._
import org.apache.kafka.clients.producer.ProducerRecord
import spray.json.JsValue

import scala.concurrent.Future

object Publisher {

  val producerSettings: ProducerSettings[String, String] = producerDefaults

  def publish(key: (String, String), data: List[JsValue]): Future[Done] = {

    Source
      .fromIterator(() => data.iterator)
      .map { hotel =>
        hotel.toString
      }
      .map(value => {
        new ProducerRecord[String, String](topic, key.toString, value)
      })
      .runWith(Producer.plainSink(producerSettings))

  }

}