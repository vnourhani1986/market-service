package com.snapptrip.notification.sms

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model._
import akka.stream.scaladsl.{Sink, Source}
import com.snapptrip.services.WebEngage.logger
import com.snapptrip.utils.{NotificationConfig, WebEngageConfig}
import spray.json.{JsArray, JsObject, JsString, JsValue}

import scala.concurrent.Future

object SmsService {



  private def emailApiCall(
                            subject: String,
                            content: String,
                            email: String,
                            sender_title: String,
                            attachment: Option[List[JsValue]] = None): Future[WSResponse] = {

    val email_details = JsObject(
      "from" -> JsString("noreply@snapptrip.com"),
      "to" -> JsString(email),
      "content" -> JsString(content),
      "subject" -> JsString(subject),
      "categories" -> JsString(""),
      "sender_title" -> JsString(sender_title),
      "client" -> JsString("ptp-b2c"),
      "attachments" -> JsArray(attachment.getOrElse(List.empty))
    )


    val fut = ws
      .url(NotificationConfig.host + NotificationConfig.baseURL)
      .withRequestTimeout(15)
      .withHttpHeaders(("Content-Type", "application/json"))
      .post(email_details)


    fut
  }

  def trackUser(request: JsValue) = {
    logger.info(s"""request track users to web engage with content:$request""")
    (for {
      body <- Marshal(request).to[RequestEntity]
      request = HttpRequest(HttpMethods.POST, WebEngageConfig.usersUrl)
        .withHeaders(RawHeader("Authorization", WebEngageConfig.apiKey))
        .withEntity(body.withContentType(ContentTypes.`application/json`))
      connectionFlow = Http().outgoingConnectionHttps(WebEngageConfig.host, settings = WebEngageConfig.clientConnectionSettings)
      (status, entity) <- Source.single(request).via(connectionFlow).runWith(Sink.head).map(x => (x.status, x.entity))
    } yield {
      logger.info(s"""response track users to web engage with result:$entity with status: $status""")
      (status, entity)
    }).recover {
      case error: Throwable =>
        logger.info(s"""response track users to web engage with result:${error.getMessage} with status: ${StatusCodes.InternalServerError}""")
        (StatusCodes.InternalServerError, null)
    }
  }

}
