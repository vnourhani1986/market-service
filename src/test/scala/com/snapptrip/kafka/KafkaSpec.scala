package com.snapptrip.kafka

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.snapptrip.service.actor.StopSystemAfterAll
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{MustMatchers, WordSpecLike}

import scala.util.Random

class KafkaSpec extends TestKit(ActorSystem("test-system"))
  with WordSpecLike
  with ImplicitSender
  with MockFactory
  with MustMatchers
  with EmbeddedKafka
  with StopSystemAfterAll {


  "a publisher actor " must {

    "publish and subscribe" in {

      val customBrokerConfig = Map("replica.fetch.max.bytes" -> "2000000",
        "message.max.bytes" -> "2000000")

      val customProducerConfig = Map("max.request.size" -> "2000000")
      val customConsumerConfig = Map("max.partition.fetch.bytes" -> "2000000")

      implicit val customKafkaConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig(
        kafkaPort = 9092,
        customBrokerProperties = customBrokerConfig,
        customProducerProperties = customProducerConfig,
        customConsumerProperties = customConsumerConfig)

      withRunningKafka {

        val topic = s"""test-${Random.nextInt}"""

        Subscriber(topic, testActor)
        val actor = Publisher(topic)

        actor ! "hello world"
        expectMsg("hello world")

      }


    }
  }

}
