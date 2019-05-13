package com.snapptrip

import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives.handleExceptions
import akka.util.Timeout
import com.snapptrip.DI._
import com.snapptrip.api._
import com.snapptrip.repos.database.DBSetup
import com.snapptrip.utils.CommonDirectives
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future

object Main extends App with RequestTimeout with LazyLogging with CommonDirectives {

  val host = config.getString("http.host") // Gets the host and a port from the configuration
  val httpPort = config.getInt("http.port")

  DBSetup.initDbs()

  val api = handleExceptions(ExtendedExceptionHandler.handle()(logger)) {
    commonDirectives {
      new RouteHandler(system, requestTimeout(config)).routs // the RestApi provides a Route
    }
  }

  // DBMigration.migrateDatabaseSchema()

  sys.addShutdownHook(system.terminate())
  sys.addShutdownHook(db.close())

  val bindingFuture: Future[ServerBinding] = Http().bindAndHandle(api, host, httpPort) //Starts the HTTP server

  bindingFuture.map { serverBinding =>
    logger.info(s"RestApi bound to ${serverBinding.localAddress} ")
  }.onComplete {
    case scala.util.Success(_) => logger.info("Started ...")
    case scala.util.Failure(ex) =>
      logger.error(s"Failed to bind to $host:$httpPort!", ex)
      system.terminate()
  }
}

trait RequestTimeout {

  import scala.concurrent.duration._

  def requestTimeout(config: Config): Timeout = {
    val t = config.getString("akka.http.server.request-timeout")
    val d = Duration(t)
    FiniteDuration(d.length, d.unit)
  }
}
