package com.snapptrip.api

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import com.snapptrip.utils.Validation
import spray.json.{JsObject, JsString}

trait RouteParser extends Validation {

  def extractToken(token: String): HttpHeader => Option[String] = {
    case HttpHeader("token", value) if token == value => Some(token)
    case _ => None
  }

  def bodyParser[A, B](body: A)(f1: A => B)(f2: B => (Boolean, String))(f4: B => Route): Route = {
    val nBody = f1(body)
    val (isValid, message) = f2(nBody)
    if (isValid) {
      f4(nBody)
    } else {
      val entity = JsObject(
        "status" -> JsString("ERROR"),
        "error" -> JsString(message)
      ).toString
      val httpEntity = HttpEntity(ContentTypes.`application/json`, entity)
      complete(HttpResponse(status = StatusCodes.BadRequest).withEntity(httpEntity))
    }

  }

}
