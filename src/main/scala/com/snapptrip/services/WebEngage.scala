package com.snapptrip.services

import java.util.UUID

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.pattern.ask
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import com.snapptrip.DI.{ec, materializer, system}
import com.snapptrip.api.Messages.{WebEngageUserInfo, WebEngageUserInfoWithUserId}
import com.snapptrip.models.WebEngageUser
import com.snapptrip.repos.WebEngageUserRepoImpl
import com.snapptrip.utils.WebEngageConfig
import com.snapptrip.webengage.{SendUserInfo, WebengageService}
import com.typesafe.scalalogging.LazyLogging
import spray.json.JsValue

import scala.concurrent.Future
import scala.concurrent.duration._

object WebEngage extends LazyLogging {

  private implicit val timeout: Timeout = Timeout(1.minutes)

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

  def userCheck(request: WebEngageUserInfo): Future[(WebEngageUserInfoWithUserId, Boolean)] = {
    (for {
      oldUser <- WebEngageUserRepoImpl.findByFilter(request)
      user <- if (oldUser.isDefined) {
        val webEngageUser = WebEngageUser(
          userName = request.user_name.orElse(oldUser.get.userName),
          userId = oldUser.get.userId,
          name = request.name.orElse(oldUser.get.name),
          family = request.family.orElse(oldUser.get.family),
          email = request.email.orElse(oldUser.get.email),
          mobileNo = request.mobile_no.orElse(oldUser.get.mobileNo),
          birthDate = request.birth_date.orElse(oldUser.get.birthDate),
          gender = request.gender.orElse(oldUser.get.gender),
          provider = request.gender.orElse(oldUser.get.provider)
        )
        WebEngageUserRepoImpl.update(webEngageUser).map {
          case true =>
            Right(WebEngageUserInfoWithUserId(
              userId = webEngageUser.userId,
//              user_name = webEngageUser.userName,
              firstName = webEngageUser.name,
              lastName = webEngageUser.family,
              email = webEngageUser.email,
              phone = webEngageUser.mobileNo,
//              birthDate = webEngageUser.birthDate,
              gender = webEngageUser.gender,
//              provider = webEngageUser.provider
            ))
          case false =>
            Left(new Exception("can not update user data in database"))
        }
      } else {
        val webEngageUser = WebEngageUser(
          userName = request.user_name,
          userId = UUID.randomUUID().toString,
          name = request.name,
          family = request.family,
          email = request.email,
          mobileNo = request.mobile_no,
          birthDate = request.birth_date,
          gender = request.gender,
          provider = request.provider
        )
        WebEngageUserRepoImpl.save(webEngageUser).map(user =>
          Right(WebEngageUserInfoWithUserId(
            userId = user.userId,
//            user_name = user.userName,
            firstName = user.name,
            lastName = user.family,
            email = user.email,
            phone = user.mobileNo,
//            birth_date = user.birthDate,
            gender = user.gender,
//            provider = user.provider
          ))
        )
      }
      _ <- user match {
        case Right(u) => (new WebengageService).actor ? SendUserInfo(u)
        case Left(e) => Future.failed(e)
      }
    } yield {
      (user.right.get, true)
    }).recover {
      case error: Throwable =>
        println(error.getMessage)
        (WebEngageUserInfoWithUserId(userId = "-1"), false)
    }
  }

}
