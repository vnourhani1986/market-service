package com.snapptrip

import java.time.ZoneId

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.slick.scaladsl.SlickSession
import akka.util.Timeout
import com.snapptrip.utils.PostgresProfiler.api._
import com.typesafe.config.{Config, ConfigFactory}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContextExecutor

object DI {
  implicit val config: Config = ConfigFactory.load()
  implicit val system: ActorSystem = ActorSystem("snapptrip")
  implicit val ec: ExecutionContextExecutor = system.dispatcher //bindAndHandle requires an implicit ExecutionContext
  implicit val timeout: Timeout = Timeout(30 seconds)
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val timezone: ZoneId = ZoneId.of(config.getString("timezone"))
  implicit val db: Database = com.snapptrip.repos.database.DBManager.db
  implicit val dbSession: SlickSession = SlickSession.forConfig("slick-postgres")
}