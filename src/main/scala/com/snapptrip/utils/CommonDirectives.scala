package com.snapptrip.utils

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive0, Route}

trait CommonDirectives {
  // Route to handle preflight CORS requests (OPTIONS)
  val corsPreflightRoute: Route = options {
    complete(HttpResponse(StatusCodes.OK).withHeaders(
      `Access-Control-Allow-Origin`.*,
      `Access-Control-Allow-Credentials`(true),
      `Access-Control-Allow-Methods`(OPTIONS, POST, PUT, GET, DELETE),
      `Access-Control-Expose-Headers`("x-ptp-token"),
      `Access-Control-Allow-Headers`("Authorization", "Content-Type", "lang", "x-ptp-token")))
  }

  val timeoutResponse = HttpResponse(
    StatusCodes.ServiceUnavailable,
    entity = HttpEntity(ContentTypes.`application/json`,s"""{"result":"failed","summary":"timeout"}"""))


  //this directive adds access control headers to normal responses
  def addCorsHeaders: Directive0 = {
    respondWithHeaders(
      `Access-Control-Allow-Origin`.*,
      `Access-Control-Allow-Credentials`(true),
      `Access-Control-Allow-Methods`(OPTIONS, POST, PUT, GET, DELETE),
      `Access-Control-Expose-Headers`("x-ptp-token"),
      `Access-Control-Allow-Headers`("Authorization", "Content-Type", "lang", "x-ptp-token")
    )
  }

  // Merge all routes
  // Note: uri.toEndpointCategory uses MetricsHandler.toEndpointCategory implicit extension method
  def commonDirectives(innerRoute: Route): Route = {
    //    (responseTimeDirectives.recordResponseTime(uri.toEndpointCategory) & withMetrics(httpMetricsExports)) {
    withRequestTimeoutResponse(_ => timeoutResponse) {
      corsPreflightRoute ~
        addCorsHeaders {
          innerRoute
        }
    }
  }


}
