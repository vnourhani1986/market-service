package com.snapptrip.redis

import java.util.concurrent.TimeUnit

import com.snapptrip.DI._
import com.typesafe.scalalogging.LazyLogging

object RedisConfig extends LazyLogging {

  private val redis_ip = config.getString("redis.ip")
  private val redis_db_number = config.getInt("redis.db")
  private val redis_connection_timeout = config.getDuration("redis.connectionTimeout", TimeUnit.SECONDS)

  //  private implicit val system: ActorSystem = ActorSystem("RedisActorSystem")
  //  lazy val redis = RedisClient(
  //    host = redis_ip,
  //    db = Some(redis_db_number),
  //    connectTimeout = Some(FiniteDuration(redis_connection_timeout, TimeUnit.SECONDS))
  //  )
  //
  //  lazy val adminNameSpace = "flight:admin"
  //
  //  def healthCheck(): Unit = {
  //
  //    val futurePong = redis.ping()
  //    println("Ping sent!")
  //    futurePong.map(pong => {
  //      println(s"Redis replied with a $pong")
  //    })
  //
  //    futurePong.onComplete {
  //      case Success(value)     =>
  //        logger.info(s"redis health check succeed : $value")
  //      case Failure(exception) =>
  //        logger.error("redis health check failed", exception)
  //    }
  //  }


}