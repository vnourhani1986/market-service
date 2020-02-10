package com.snapptrip.notification.sms

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.stream.scaladsl.{Sink, Source}
import com.snapptrip.DI.{ec, materializer, system}
import com.snapptrip.utils.NotificationConfig
import com.typesafe.scalalogging.LazyLogging
import spray.json
import spray.json.{JsObject, JsString, JsValue}

import scala.concurrent.Future

object SmsService extends LazyLogging {

  def sendSMS(mobilesNo: List[String], content: String): Future[StatusCode] = {

    println(content, "sms content")
    val smsDetails = JsObject(
      "to" -> json.JsArray(mobilesNo.map(JsString(_)).toVector),
      "content" -> JsString(content),
      "client" -> JsString(NotificationConfig.client)
    )

    send(smsDetails).map(_._1)

  }

  private def send(request: JsValue) = {
    logger.info(s"""request sms notification with content:$request""")
    (for {
      body <- Marshal(request).to[RequestEntity]
      request = HttpRequest(HttpMethods.POST, NotificationConfig.smsUrl)
        .withEntity(body.withContentType(ContentTypes.`application/json`))
      connectionFlow = Http().outgoingConnection(NotificationConfig.host, NotificationConfig.port, settings = NotificationConfig.clientConnectionSettings)
      (status, entity) <- Source.single(request).via(connectionFlow).runWith(Sink.head).map(x => (x.status, x.entity))
    } yield {
      logger.info(s"""response sms notification with result:${entity.toString} with status: $status""")
      (status, entity)
    }).recover {
      case error: Throwable =>
        logger.info(s"""response sms notification with result:${error.getMessage} with status: ${StatusCodes.InternalServerError}""")
        (StatusCodes.InternalServerError, null)
    }
  }

}
