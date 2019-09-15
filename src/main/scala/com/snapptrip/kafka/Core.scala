package com.snapptrip.kafka

import java.util
import java.util.Properties
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import com.snapptrip.DI._
import akka.kafka.{ConsumerSettings, ProducerSettings}
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.admin._
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.slf4j.{Logger, LoggerFactory}

import scala.language.postfixOps

object Core {

  val conf: Config = ConfigFactory.load()
  val topic: String = config.getString("kafka.topic")
  //  def this(kafkaPort: Int) = this(kafkaPort, kafkaPort + 1, ActorSystem("FTMS"))

  case class Key(userId: String, keyType: String)

  def log: Logger = LoggerFactory.getLogger(getClass)

  def bootstrapServers: String = conf.getString("kafka.bootstrap.servers")

  println("the bootstrap kafka server ::::: " + bootstrapServers)

  val DefaultKey = "key"
  val InitialMsg =
    "initial msg in topic, required to create the topic before any consumer subscribes to it"

  lazy val producerDefaults: ProducerSettings[String, String] =
    ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers(bootstrapServers)
      .withProperty(ConsumerConfig.GROUP_ID_CONFIG, "group1")

  val consumerDefaults: ConsumerSettings[String, String] = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers(bootstrapServers)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    .withGroupId("group1")
//   .withWakeupTimeout(10.seconds)
//   .withMaxWakeups(10)

  private val topicCounter = new AtomicInteger()

  def createTopicName(number: Int) = s"topic-$number"

  def createGroupId(number: Int = 0) = s"group-$number-${topicCounter.incrementAndGet()}"

  def createTransactionalId(number: Int = 0) = s"transactionalId-$number-${topicCounter.incrementAndGet()}"

  val partition0 = 0


  def adminClient(): AdminClient =
    AdminClient.create(adminDefaults)

  val adminDefaults: Properties = {
    val config = new Properties()
    config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    config
  }

  /**
    * Create a topic with given partinion number and replication factor.
    *
    * This method will block and return only when the topic has been successfully created.
    */
  def createTopic(number: Int = 0, partitions: Int = 1, replication: Int = 1): String = {
    val topicName = createTopicName(number)

    val configs = new util.HashMap[String, String]()

    val createResult: CreateTopicsResult = adminClient().createTopics(
      util.Arrays.asList(new NewTopic(topicName, partitions, replication.toShort).configs(configs))
    )
    createResult.all().get(10, TimeUnit.SECONDS)
    topicName
  }

  def createTopics(topics: Int*): Seq[String] = {
    val topicNames = topics.toList.map { number =>
      createTopicName(number)
    }
    val configs = new util.HashMap[String, String]()
    val newTopics = topicNames.map { topicName =>
      new NewTopic(topicName, 1, 1.toShort).configs(configs)
    }

    import scala.collection.JavaConverters._

    val createResult = adminClient().createTopics(newTopics.asJava)
    createResult.all().get(10, TimeUnit.SECONDS)
    topicNames
  }

  def exists(topic: Int): Boolean = {
    import scala.collection.JavaConverters._
    val existing = adminClient().listTopics(new ListTopicsOptions().timeoutMs(500).listInternal(true))
    val nms = existing.names()
    nms.get().asScala.foldLeft(false)((exist, name) => name.contains(topic.toString) | exist)
  }


}
