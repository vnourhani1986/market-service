package com.snapptrip.kafka

import akka.kafka.{ConsumerSettings, ProducerSettings}
import com.snapptrip.DI._
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}

import scala.language.postfixOps

object Setting {

  val conf: Config = ConfigFactory.load()
  val topic: String = config.getString("kafka.topic")
  val errorTopic: String = config.getString("kafka.error-topic")

  case class Key(userId: String, keyType: String)

  def bootstrapServers: String = conf.getString("kafka.bootstrap.servers")

  println("the bootstrap kafka server ::::: " + bootstrapServers)

  lazy val producerDefaults: ProducerSettings[String, String] =
    ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers(bootstrapServers)

  val consumerDefaults: ConsumerSettings[String, String] = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers(bootstrapServers)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    .withGroupId("group1")

}
