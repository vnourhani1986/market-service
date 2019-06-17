package com.snapptrip.api

import akka.actor._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.RouteDirectives.reject
import akka.util.Timeout
import com.snapptrip.api.Messages._
import com.snapptrip.auth.UserRole
import com.snapptrip.formats.Formats._
import com.snapptrip.notification.email.EmailService
import com.snapptrip.notification.sms.SmsService
import com.snapptrip.repos.BusinessRepoImpl
import com.snapptrip.services.WebEngage
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import spray.json.{JsNumber, JsObject, JsString, JsValue}
import com.snapptrip.DI._

import scala.concurrent.ExecutionContextExecutor

class RouteHandler(system: ActorSystem, timeout: Timeout) extends LazyLogging {

  implicit val requestTimeout: Timeout = timeout

  implicit def executionContext: ExecutionContextExecutor = system.dispatcher

  val token: String = config.getString("web-engage.token")

  def routs: Route =

    HealthCheckHandler.route ~ AuthHandler.routes ~ webEngageApi(token) ~
      AuthHandler.authenticated { (userInfo, channel) =>
        userValidator(userInfo => userInfo.role == UserRole.ADMIN, userInfo) { userInfo =>
          users(userInfo)
        } //~
        //          userValidator(userInfo => userInfo.role == UserRole.SUPER_SUPPORT, userInfo) { userInfo =>
        //
        //          } ~
        //          userValidator(userInfo => userInfo.role == UserRole.SUPPORT, userInfo) { userInfo =>
        //
        //          } ~
        //          userValidator(userInfo => userInfo.role == UserRole.FINANCE, userInfo) { userInfo =>
        //
        //          } ~
        //          userValidator(userInfo => userInfo.role == UserRole.BUSINESS, userInfo) { userInfo =>
        //
        //          }

      }

  def userValidator[T](validator: UserInfo => Boolean, userInfo: UserInfo)(route: UserInfo => Route): Route = {
    if (validator(userInfo)) {
      route(userInfo)
    } else {
      reject(AuthorizationFailedRejection)
    }
  }

  def users(userInfo: UserInfo): Route = {

    pathPrefix("v1" / "users") {
      path("business-info") {
        get {
          logger.info(
            s"""get business info request""")
          onSuccess(BusinessRepoImpl.get) {
            businessInfo =>
              logger.info(s"""get business response by result size: ${businessInfo.size} by user : $userInfo""")
              complete(businessInfo)
          }
        }
      }
      //      ~
      //        get {
      //          logger.info(
      //            s"""get users request""")
      //          onSuccess(UsersRepoImpl.get) {
      //            users =>
      //              logger.info(s"""get users response users size : ${users.size} by user : $userInfo""")
      //              complete(users)
      //          }
      //        } ~
      //        path("add-user") {
      //          post {
      //            entity(as[WebEngageUser]) { user =>
      //              logger.info(
      //                s"""add new user request by filter content: $user""")
      //              onSuccess(UsersRepoImpl.save(user)) {
      //                id =>
      //                  logger.info(s"""add new user response by id : $id by user : $userInfo""")
      //                  complete(id.toString)
      //              }
      //            }
      //          }
      //        } ~
      //        path("edit-user") {
      //          put {
      //            entity(as[WebEngageUser]) { user =>
      //              logger.info(
      //                s"""edit new user request by filter content: $user""")
      //              onSuccess(UsersRepoImpl.update(user)) {
      //                saveResult =>
      //                  logger.info(s"""edit new user response by result : $saveResult by user : $userInfo""")
      //                  complete(saveResult.toString)
      //              }
      //            }
      //          }
      //        }
    }
  }

  def webEngageApi(token: String): Route = {

    pathPrefix("api" / "v1" / "webengage") {
      path("users") {
        post {
          headerValue(extractToken(token)) { _ =>
            entity(as[JsValue]) { body =>
              logger.info(s"""post users request by body $body""")
              onSuccess(WebEngage.trackUser(body)) {
                case (status, entity) if status == StatusCodes.Created =>
                  logger.info(s"""post users response by result: $entity and status: $status""")
                  complete(HttpResponse(status = status).withEntity(entity))
                case (status, _) if status == StatusCodes.InternalServerError =>
                  logger.info(s"""post users response by result: server error and status: $status""")
                  complete(HttpResponse(status = status))
                case (status, entity) =>
                  logger.info(s"""post users response by result: $entity and status: $status""")
                  complete(HttpResponse(status = status).withEntity(entity))
              }
            }
          }
        }
      } ~
        path("events") {
          post {
            headerValue(extractToken(token)) { _ =>
              entity(as[WebEngageEvent]) { body =>
                logger.info(s"""post events request by body $body""")
                onSuccess(WebEngage.trackEventWithoutUserId(body)) {
                  case (status, entity) if status =>
                    logger.info(s"""post events response by result: $entity and status: $status""")
                    val httpEntity = HttpEntity(ContentTypes.`application/json`, entity.compactPrint)
                    complete(HttpResponse(status = StatusCodes.Created).withEntity(httpEntity))
                  case (status, entity) if !status =>
                    logger.info(s"""post events response by result: server error and status: $status""")
                    val httpEntity = HttpEntity(ContentTypes.`application/json`, entity.compactPrint)
                    complete(HttpResponse(status = StatusCodes.InternalServerError).withEntity(httpEntity))
                }
              }
            }
          }
        } ~
        path("sms") {
          post {
            println("sms to me")
            headerValue(extractToken(token)) { ctx =>
              println("sms to me")
              println(ctx)
              entity(as[WebEngageSMSBody]) { body =>
                logger.info(s"""post sms request by body $body""")
                onSuccess(SmsService.sendSMS(List(body.smsData.toNumber), body.smsData.body)) {
                  case status if status == StatusCodes.OK =>
                    logger.info(s"""post sms response by status: $status""")
                    val entity = JsObject(
                      "status" -> JsString("sms_accepted")
                    ).toString
                    val httpEntity = HttpEntity(ContentTypes.`application/json`, entity)
                    complete(HttpResponse(status = status).withEntity(httpEntity))
                  case status if status == StatusCodes.InternalServerError =>
                    logger.info(s"""post email response by result: server error and status: $status""")
                    val entity = JsObject(
                      "status" -> JsString("sms_rejected"),
                      "statusCode" -> JsNumber(9988), // unknown error
                      "message" -> JsString("server_error")
                    ).toString
                    val httpEntity = HttpEntity(ContentTypes.`application/json`, entity)
                    complete(HttpResponse(status = StatusCodes.OK).withEntity(httpEntity))
                  case status =>
                    logger.info(s"""post email response by status: $status""")
                    val entity = JsObject(
                      "status" -> JsString("sms_rejected"),
                      "statusCode" -> JsNumber(9988), // unknown error
                      "message" -> JsString("server_error")
                    ).toString
                    complete(HttpResponse(status = status).withEntity(entity))
                }
              }
            }
          }
        } ~
        path("email") {
          post {
            headerValue(extractToken(token)) { _ =>
              entity(as[WebEngageEmailBody]) { body =>
                logger.info(s"""post email request by body $body""")
                onSuccess(EmailService.sendEmail(body.email.subject,
                  body.email.text,
                  body.email.recipients.to.head.email,
                  body.email.fromName,
                  None)) {
                  case status if status == StatusCodes.OK =>
                    logger.info(s"""post email response by status: $status""")
                    val entity = JsObject(
                      "status" -> JsString("SUCCESS"),
                      "statusCode" -> JsNumber(1000),
                      "message" -> JsString("NA")
                    ).toString
                    val httpEntity = HttpEntity(ContentTypes.`application/json`, entity)
                    complete(HttpResponse(status = status).withEntity(httpEntity))
                  case status if status == StatusCodes.InternalServerError =>
                    logger.info(s"""post email response by result: server error and status: $status""")
                    val entity = JsObject(
                      "status" -> JsString("ERROR"),
                      "statusCode" -> JsNumber(9999), // unknown error
                      "message" -> JsString("server_error")
                    ).toString
                    val httpEntity = HttpEntity(ContentTypes.`application/json`, entity)
                    complete(HttpResponse(status = StatusCodes.OK).withEntity(httpEntity))
                  case status =>
                    logger.info(s"""post email response by status: $status""")
                    val entity = JsObject(
                      "status" -> JsString("ERROR"),
                      "statusCode" -> JsNumber(9999), // unknown error
                      "message" -> JsString("server_error")
                    ).toString
                    complete(HttpResponse(status = status).withEntity(entity))
                }
              }
            }
          }
        } ~
        path("user" / "check") {
          post {
            headerValue(extractToken(token)) { _ =>
              entity(as[WebEngageUserInfo]) { body1 =>
                val body = WebEngageUserInfo(
                  user_name = body1.user_name,
                  name = body1.name,
                  family = body1.family,
                  email = body1.email,
                  mobile_no = body1.mobile_no.map(format),
                  birth_date = body1.birth_date,
                  gender = body1.gender,
                  provider = body1.provider
                )
                logger.info(s"""post check user request by body $body""")
                if (body.email.isEmpty && body.mobile_no.isEmpty) {
                  logger.info(s"""post check user response by result: server error and status: 400""")
                  val entity = JsObject(
                    "status" -> JsString("ERROR"),
                    "user_id" -> JsString("-1")
                  ).toString
                  val httpEntity = HttpEntity(ContentTypes.`application/json`, entity)
                  complete(HttpResponse(status = StatusCodes.BadRequest).withEntity(httpEntity))
                } else {
                  onSuccess(WebEngage.userCheck(body)) {
                    case (user, status) if status =>
                      logger.info(s"""post check user response by status: 200""")
                      val entity = JsObject(
                        "status" -> JsString("SUCCESS"),
                        "user_id" -> JsString(user.userId),
                      ).toString
                      val httpEntity = HttpEntity(ContentTypes.`application/json`, entity)
                      complete(HttpResponse(status = StatusCodes.OK).withEntity(httpEntity))
                    case (_, _) =>
                      logger.info(s"""post check user response by result: server error and status: 500""")
                      val entity = JsObject(
                        "status" -> JsString("ERROR"),
                        "user_id" -> JsString("-1")
                      ).toString
                      val httpEntity = HttpEntity(ContentTypes.`application/json`, entity)
                      complete(HttpResponse(status = StatusCodes.InternalServerError).withEntity(httpEntity))
                  }
                }
              }
            }
          }
        } ~
        path("user" / "register") {
          headerValue(extractToken(token)) { _ =>
            post {
              entity(as[WebEngageUserInfo]) { body1 =>
                val body = WebEngageUserInfo(
                  user_name = body1.user_name,
                  name = body1.name,
                  family = body1.family,
                  email = body1.email,
                  mobile_no = body1.mobile_no.map(format),
                  birth_date = body1.birth_date,
                  gender = body1.gender,
                  provider = body1.provider
                )
                body.mobile_no.map(format)
                logger.info(s"""post check user request by body $body""")
                if (body.email.isEmpty && body.mobile_no.isEmpty) {
                  logger.info(s"""post check user response by result: server error and status: 400""")
                  val entity = JsObject(
                    "status" -> JsString("ERROR"),
                    "user_id" -> JsString("-1")
                  ).toString
                  val httpEntity = HttpEntity(ContentTypes.`application/json`, entity)
                  complete(HttpResponse(status = StatusCodes.BadRequest).withEntity(httpEntity))
                } else {
                  onSuccess(WebEngage.userCheck(body)) {
                    case (_, status) if status =>
                      logger.info(s"""post check user response by status: 200""")
                      val entity = JsObject(
                        "status" -> JsString("SUCCESS")
                      ).toString
                      val httpEntity = HttpEntity(ContentTypes.`application/json`, entity)
                      complete(HttpResponse(status = StatusCodes.OK).withEntity(httpEntity))
                    case (_, _) =>
                      logger.info(s"""post check user response by result: server error and status: 500""")
                      val entity = JsObject(
                        "status" -> JsString("ERROR")
                      ).toString
                      val httpEntity = HttpEntity(ContentTypes.`application/json`, entity)
                      complete(HttpResponse(status = StatusCodes.InternalServerError).withEntity(httpEntity))
                  }
                }
              }
            }
          }
        }

    }
  }

  def extractToken(token: String): HttpHeader => Option[String] = {
    case HttpHeader("token", value) if token == value => Some(token)
    case _ => None
  }

}
