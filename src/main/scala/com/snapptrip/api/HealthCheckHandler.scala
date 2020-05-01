package com.snapptrip.api

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.snapptrip.DI._
import com.snapptrip.api.HealthCheckHandler.HealthCheckResponse
import com.snapptrip.formats.Formats._
import com.snapptrip.utils.PostgresProfiler.api._
import spray.json._

import scala.concurrent.Future

/**
  * Handle requests to monitor healthcheck
  */

class HealthCheckHandler {

  private def handle(ctx: RequestContext): Future[RouteResult] = {
    val remoteAddress = ctx.request.headers.find(h => "Remote-Address".equalsIgnoreCase(h.name)).getOrElse(new RawHeader("Remote-Address", "127.0.0.1")).value()

    db.run(sql"""SELECT 1""".as[Boolean]) flatMap { _ =>
      val entity = HealthCheckResponse("success", "This is from a healthy market service app :D", remoteAddress, "successfully ran a `select 1` query")
      val httpEntity = HttpEntity(ContentTypes.`application/json`, entity.toJson.compactPrint)
      ctx.complete(HttpResponse(status = StatusCodes.Created).withEntity(httpEntity))
    } recoverWith {
      case error: Throwable => ctx.fail(error)
    }
  }

  // Route to this handler
  def route: Route =
    (get & pathPrefix("health")) { ctx =>
      handle(ctx)
    }
}

object HealthCheckHandler {

  def apply(): HealthCheckHandler = new HealthCheckHandler()

  case class HealthCheckResponse(result: String, summery: String, remoteAddress: String, db: String)

}