package com.snapptrip.kafka

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.snapptrip.kafka.Setting.Key
import com.snapptrip.service.actor.StopSystemAfterAll
import org.scalamock.scalatest.MockFactory
import org.scalatest.{MustMatchers, WordSpecLike}
import spray.json._

import scala.util.Random

class KafkaSpec extends TestKit(ActorSystem("test-system"))
  with WordSpecLike
  with ImplicitSender
  with MockFactory
  with MustMatchers
  //  with EmbeddedKafka
  with StopSystemAfterAll {


  "a publisher actor " must {

    "publish and subscribe" in {

      //      val customBrokerConfig = Map("replica.fetch.max.bytes" -> "2000000",
      //        "message.max.bytes" -> "2000000")
      //
      //      val customProducerConfig = Map("max.request.size" -> "2000000")
      //      val customConsumerConfig = Map("max.partition.fetch.bytes" -> "2000000")
      //
      //      implicit val customKafkaConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig(
      //        kafkaPort = 9092,
      //        customBrokerProperties = customBrokerConfig,
      //        customProducerProperties = customProducerConfig,
      //        customConsumerProperties = customConsumerConfig)

      val key = Key("9124497405", "track-user")
      val value = s"""{}""".stripMargin.parseJson

      //      withRunningKafka {

      val topic = s"""test-${Random.nextInt}"""

      Subscriber(topic, testActor, setting = Setting.setConsumer(Setting.marketServer))
      val actor = Publisher(topic)

      actor ! (key, value)
      expectMsg((key, value))

      //      }


    }
  }

}
