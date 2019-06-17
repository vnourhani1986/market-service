package com.snapptrip.services

import java.time.{LocalDateTime, LocalTime}
import java.util.UUID

import akka.actor.ActorRef
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.pattern.ask
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import com.snapptrip.DI.{ec, materializer, system}
import com.snapptrip.api.Messages.{WebEngageEvent, WebEngageUserInfo, WebEngageUserInfoWithUserId}
import com.snapptrip.models.User
import com.snapptrip.repos.WebEngageUserRepoImpl
import com.snapptrip.utils.WebEngageConfig
import com.snapptrip.webengage.{SendEventInfo, SendUserInfo, WebengageService}
import com.typesafe.scalalogging.LazyLogging
import spray.json.{JsObject, JsString, JsValue}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try

object WebEngage extends LazyLogging {

  private implicit val timeout: Timeout = Timeout(1.minute)
  val actor: ActorRef = (new WebengageService).actor

  def trackUser(request: JsValue): Future[(StatusCode, ResponseEntity)] = {
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

  def trackEventWithUserId(request: JsValue): Future[(StatusCode, ResponseEntity)] = {
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

  def trackEventWithoutUserId(request: WebEngageEvent): Future[(Boolean, JsObject)] = {
    val user = request.user
    val event = request.event
    (for {
      _ <- if (user.email.isEmpty && user.mobile_no.isEmpty) {
        Future.failed(new Exception("must define one of email or mobile number"))
      } else {
        Future.successful("")
      }
      user <- WebEngageUserRepoImpl.findByFilter(user.mobile_no, user.email)
      userId = user.map(_.userId)
      newRequest <- if (userId.isDefined) {
        val lContent = JsObject("userId" -> JsString(userId.get)).fields.toList ::: event.asJsObject.fields.filterKeys(x => x != "email" || x != "mobile_no").toList
        val jContent = JsObject(lContent.toMap)
        Future.successful(jContent)
      } else {
        Future.failed(new Exception("the user is not exists"))
      }
      _ <- actor ? SendEventInfo(newRequest, 1)
    } yield {
      (true, JsObject("status" -> JsString("success")))
    }).recover {
      case error: Throwable =>
        (false, JsObject("status" -> JsString("failed")))
    }
  }

  def userCheck(request: WebEngageUserInfo): Future[(WebEngageUserInfoWithUserId, Int)] = {
    (for {
      oldUser <- WebEngageUserRepoImpl.findByFilter(request)
      user <- if (oldUser.isDefined) {
        val webEngageUser = converter(request, oldUser)
        WebEngageUserRepoImpl.update(webEngageUser).map {
          case true =>
            val birthDate = Try {
              webEngageUser.birthDate.map(x => LocalDateTime.of(x, LocalTime.of(1, 1, 1, 1)).toString.replace(".", "-").concat("0"))
            }.toOption.flatten
            Right((converter(webEngageUser, birthDate), 200))
          case false =>
            Left(new Exception("can not update user data in database"))
        }
      } else {
        val webEngageUser = converter(request)
        WebEngageUserRepoImpl.save(webEngageUser).map { user =>
          val birthDate = Try {
            user.birthDate.map(x => LocalDateTime.of(x, LocalTime.of(1, 1, 1, 1)).toString.replace(".", "-").concat("0"))
          }.toOption.flatten
          Right((converter(webEngageUser, birthDate), 201))
        }
      }
      _ <- user match {
        case Right(u) => actor ? SendUserInfo(u._1, 1)
        case Left(e) => Future.failed(e)
      }
    } yield {
      (user.right.get._1, user.right.get._2)
    }).recover {
      case error: Throwable =>
        (WebEngageUserInfoWithUserId(userId = "-1"), 500)
    }
  }

  def converter(webEngageUserInfo: WebEngageUserInfo): User = {
    User(
      userName = webEngageUserInfo.user_name,
      userId = UUID.randomUUID().toString,
      name = webEngageUserInfo.name,
      family = webEngageUserInfo.family,
      email = webEngageUserInfo.email,
      mobileNo = webEngageUserInfo.mobile_no,
      birthDate = webEngageUserInfo.birth_date,
      gender = webEngageUserInfo.gender,
      provider = webEngageUserInfo.provider
    )
  }

  def converter(webEngageUserInfo: WebEngageUserInfo, oldUser: Option[User]): User = {
    User(
      userName = webEngageUserInfo.user_name.orElse(oldUser.get.userName),
      userId = oldUser.get.userId,
      name = webEngageUserInfo.name.orElse(oldUser.get.name),
      family = webEngageUserInfo.family.orElse(oldUser.get.family),
      email = webEngageUserInfo.email.orElse(oldUser.get.email),
      mobileNo = webEngageUserInfo.mobile_no.orElse(oldUser.get.mobileNo),
      birthDate = webEngageUserInfo.birth_date.orElse(oldUser.get.birthDate),
      gender = webEngageUserInfo.gender.orElse(oldUser.get.gender),
      provider = webEngageUserInfo.provider.orElse(oldUser.get.provider)
    )
  }

  def converter(user: User, birthDate: Option[String]): WebEngageUserInfoWithUserId = {
    WebEngageUserInfoWithUserId(
      userId = user.userId,
//      user_name = user.userName,
      firstName = user.name,
      lastName = user.family,
      email = user.email,
      phone = user.mobileNo,
      birthDate = birthDate,
      gender = user.gender,
//      provider = user.provider
    )
  }

}
