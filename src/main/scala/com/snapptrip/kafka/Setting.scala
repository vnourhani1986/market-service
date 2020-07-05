package com.snapptrip.kafka

import akka.kafka.{ConsumerSettings, ProducerSettings}
import com.snapptrip.DI._
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}

import scala.language.postfixOps

object Setting {

  val conf: Config = ConfigFactory.load()
  val marketTopic: String = config.getString("kafka.topic")
  val errorTopic: String = config.getString("kafka.error-topic")
  val deleteUserResultTopic: String = config.getString("kafka.delete-user-result-topic")
  val userAttributesTopic: String = config.getString("kafka.user-attributes-topic")
  val consumerGroup: String = config.getString("kafka.consumer-group")

  case class Key(userId: String, keyType: String)
  case class DeleteCancelKey(requestId: String, keyType: String)

  def marketServer: String = conf.getString("kafka.bootstrap.market-server")
  def biServer: String = conf.getString("kafka.bootstrap.bi-server")

  println("the bootstrap kafka market server ::::: " + marketServer)
  println("the bootstrap kafka bi server ::::: " + biServer)

  def setProducer(kafkaServers: String): ProducerSettings[String, String] =
    ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers(kafkaServers)

  def setConsumer(kafkaServers: String, groupId: String = consumerGroup): ConsumerSettings[String, String] = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers(kafkaServers)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    .withGroupId(groupId)

}
