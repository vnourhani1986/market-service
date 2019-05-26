package com.snapptrip.services

import java.util.UUID

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.scaladsl.{Sink, Source}
import com.snapptrip.DI.{ec, materializer, system}
import com.snapptrip.api.Messages.WebEngageUserInfo
import com.snapptrip.models.WebEngageUser
import com.snapptrip.repos.WebEngageUserRepoImpl
import com.snapptrip.utils.WebEngageConfig
import com.typesafe.scalalogging.LazyLogging
import spray.json.JsValue

import scala.concurrent.Future

object WebEngage extends LazyLogging {

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

  def trackEvent(request: JsValue) = {
    logger.info(s"""request track events to web engage with content:$request""")
    (for {
      body <- Marshal(request).to[RequestEntity]
      request = HttpRequest(HttpMethods.POST, WebEngageConfig.eventsUrl)
        .withHeaders(RawHeader("Authorization", WebEngageConfig.apiKey))
        .withEntity(body.withContentType(ContentTypes.`application/json`))
      connectionFlow = Http().outgoingConnectionHttps(WebEngageConfig.host, settings = WebEngageConfig.clientConnectionSettings)
      (status, entity) <- Source.single(request).via(connectionFlow).runWith(Sink.head).map(x => (x.status, x.entity))
    } yield {
      logger.info(s"""response track events to web engage with result:$entity with status: $status""")
      (status, entity)
    }).recover {
      case error: Throwable =>
        logger.info(s"""response track events to web engage with result:${error.getMessage} with status: ${StatusCodes.InternalServerError}""")
        (StatusCodes.InternalServerError, null)
    }
  }

  def userCheck(request: WebEngageUserInfo): Future[(String, Boolean)] = {
    (for {
      userIdOpt <- WebEngageUserRepoImpl.findByFilter(request)
      userId <- if (userIdOpt.isDefined) {
        val webEngageUser = WebEngageUser(
          userName = request.user_name,
          userId = userIdOpt.get,
          name = request.name,
          family = request.family,
          email = request.email,
          mobileNo = request.mobile_no,
          birthDate = request.birth_date,
          gender = request.gender
        )
        WebEngageUserRepoImpl.update(webEngageUser).map(_ => userIdOpt.get)
      } else {
        val webEngageUser = WebEngageUser(
          userName = request.user_name,
          userId = UUID.randomUUID().toString,
          name = request.name,
          family = request.family,
          email = request.email,
          mobileNo = request.mobile_no,
          birthDate = request.birth_date,
          gender = request.gender
        )
        WebEngageUserRepoImpl.save(webEngageUser)
      }

    } yield {
      (userId, true)
    }).recover {
      case error: Throwable =>
        println(error.getMessage)
        (error.getMessage, false)
    }
  }

}
