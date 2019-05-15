package com.snapptrip.notification.email

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.stream.scaladsl.{Sink, Source}
import com.snapptrip.DI.{ec, materializer, system}
import com.snapptrip.utils.NotificationConfig
import com.typesafe.scalalogging.LazyLogging
import spray.json.{JsArray, JsObject, JsString, JsValue}

import scala.concurrent.Future

object EmailService extends LazyLogging {

  def sendEmail(subject: String,
              content: String,
              email: String,
              senderTitle: String,
              attachment: Option[List[JsValue]] = None): Future[StatusCode] = {


    val emailDetails = JsObject(
      "from" -> JsString("noreply@snapptrip.com"),
      "to" -> JsString(email),
      "content" -> JsString(content),
      "subject" -> JsString(subject),
      "categories" -> JsString(""),
      "sender_title" -> JsString(senderTitle),
      "client" -> JsString("ptp-b2c"),
      "attachments" -> JsArray(attachment.getOrElse(Vector.empty).toVector)
    )

    send(emailDetails).map(_._1)

  }

  private def send(request: JsValue) = {
    logger.info(s"""request email notification with content:$request""")
    (for {
      body <- Marshal(request).to[RequestEntity]
      request = HttpRequest(HttpMethods.POST, NotificationConfig.emailUrl)
        .withEntity(body.withContentType(ContentTypes.`application/json`))
      connectionFlow = Http().outgoingConnectionHttps(NotificationConfig.host, settings = NotificationConfig.clientConnectionSettings)
      (status, entity) <- Source.single(request).via(connectionFlow).runWith(Sink.head).map(x => (x.status, x.entity))
    } yield {
      logger.info(s"""response email notification with result:$entity with status: $status""")
      (status, entity)
    }).recover {
      case error: Throwable =>
        logger.info(s"""response email notification with result:${error.getMessage} with status: ${StatusCodes.InternalServerError}""")
        (StatusCodes.InternalServerError, null)
    }
  }

}
