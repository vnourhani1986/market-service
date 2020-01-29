package com.snapptrip.api

import akka.actor._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.{HttpResponse, StatusCodes, _}
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.http.scaladsl.server.directives.RouteDirectives.{complete, reject}
import akka.http.scaladsl.server.{Directive0, Route, _}
import akka.pattern.ask
import akka.util.Timeout
import com.snapptrip.DI._
import com.snapptrip.api.Messages._
import com.snapptrip.formats.Formats._
import com.snapptrip.notification.email.EmailService
import com.snapptrip.notification.sms.SmsService
import com.snapptrip.repos.BusinessRepoImpl
import com.snapptrip.utils.EmailAddress
import com.snapptrip.utils.formatters.EmailFormatter
import com.snapptrip.utils.formatters.MobileNoFormatter._
import com.snapptrip.webengage.actor.ClientActor
import com.snapptrip.webengage.actor.ClientActor.{CheckUser, TrackEvent}
import com.snapptrip.webengage.api.WebEngageApi
import com.typesafe.scalalogging.LazyLogging
import spray.json.{JsNumber, JsObject, JsString, JsValue}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class RouteHandler(system: ActorSystem, timeout: Timeout) extends LazyLogging {

  implicit val requestTimeout: Timeout = timeout

  implicit def executionContext: ExecutionContextExecutor = system.dispatcher

  val token: String = config.getString("web-engage.token")

  def routs: Route =

    HealthCheckHandler.route ~ ping ~ AuthHandler.routes ~ webEngageApi(token)

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
    }
  }

  private val cors = new CORSHandler {}

  def ping: Route = path("api" / "v1" / "webengage" / "user" / "check") {
    //The OPTIONS endpoint - doesn't need to do anything except send an OK
    options {
      cors.corsHandler(complete(StatusCodes.OK))
    }
  }

  def webEngageApi(token: String): Route = {

    pathPrefix("api" / "v1" / "webengage") {
      path("users") {
        post {
          headerValue(extractToken(token)) { _ =>
            entity(as[JsValue]) { body =>
              onSuccess(WebEngageApi.trackUser(body)) {
                case (status, entity) if status == StatusCodes.Created =>
                  complete(HttpResponse(status = status).withEntity(entity))
                case (status, _) if status == StatusCodes.InternalServerError =>
                  logger.info(s"""post user: $body response by result: server error and status: $status""")
                  complete(HttpResponse(status = status))
                case (status, entity) =>
                  logger.info(s"""post user: $body response by result: $entity and status: $status""")
                  complete(HttpResponse(status = status).withEntity(entity))
              }
            }
          }
        }
      } ~
        path("events") {
          post {
            headerValue(extractToken(token)) { _ =>
              entity(as[WebEngageEvent]) { body1 =>
                val body = body1.copy(user = body1.user.copy(mobile_no = format(body1.user.mobile_no), email = format(body1.user.email)))

                val mobile = body1.user.mobile_no.getOrElse("")
                val email = body1.user.email.getOrElse("")
                val isValidMobile = isNumber(mobile)
                val isValidEmail = EmailAddress.isValid(email)

                val msg = if (email.isEmpty && mobile.isEmpty) {
                  "one of the fields of mobile or email need to be defined"
                }
                else if (email.nonEmpty && !isValidEmail && mobile.nonEmpty && !isValidMobile) {
                  "invalid Email and mobile number"
                }
                else if (email.nonEmpty && !isValidEmail) {
                  "invalid Email"
                }
                else if (mobile.nonEmpty && !isValidMobile) {
                  "invalid mobile number"
                }
                else ""


                if (msg.nonEmpty) {
                  logger.info(s"""post check user response by result: server error and status: ${StatusCodes.BadRequest.intValue}""")
                  val entity = JsObject(
                    "status" -> JsString("ERROR"),
                    "error" -> JsString(msg)
                  ).toString
                  val httpEntity = HttpEntity(ContentTypes.`application/json`, entity)
                  complete(HttpResponse(status = StatusCodes.BadRequest).withEntity(httpEntity))
                } else {
                  onSuccess(ClientActor.clientActor.ask(TrackEvent(body)).mapTo[(Boolean, JsObject)]) {
                    case (status, entity) if status =>
                      val httpEntity = HttpEntity(ContentTypes.`application/json`, entity.compactPrint)
                      complete(HttpResponse(status = StatusCodes.Created).withEntity(httpEntity))
                    case (status, entity) if !status =>
                      logger.info(s"""post event : $body response by result: server error and status: $status""")
                      val httpEntity = HttpEntity(ContentTypes.`application/json`, entity.compactPrint)
                      complete(HttpResponse(status = StatusCodes.InternalServerError).withEntity(httpEntity))
                  }
                }
              }
            }
          }
        } ~
        path("sms") {
          post {
            headerValue(extractToken(token)) { _ =>
              entity(as[WebEngageSMSBody]) { body =>
                onSuccess(SmsService.sendSMS(List(body.smsData.toNumber), body.smsData.body)) {
                  case status if status == StatusCodes.OK =>
                    val entity = JsObject(
                      "status" -> JsString("sms_accepted")
                    ).toString
                    val httpEntity = HttpEntity(ContentTypes.`application/json`, entity)
                    complete(HttpResponse(status = status).withEntity(httpEntity))
                  case status if status == StatusCodes.InternalServerError =>
                    logger.info(s"""post email : $body response by result: server error and status: $status""")
                    val entity = JsObject(
                      "status" -> JsString("sms_rejected"),
                      "statusCode" -> JsNumber(9988), // unknown error
                      "message" -> JsString("server_error")
                    ).toString
                    val httpEntity = HttpEntity(ContentTypes.`application/json`, entity)
                    complete(HttpResponse(status = StatusCodes.OK).withEntity(httpEntity))
                  case status =>
                    logger.info(s"""post email : $body response by status: $status""")
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
                onSuccess(EmailService.sendEmail(body.email.subject,
                  body.email.text,
                  body.email.recipients.to.head.email,
                  body.email.fromName,
                  None)) {
                  case status if status == StatusCodes.OK =>
                    val entity = JsObject(
                      "status" -> JsString("SUCCESS"),
                      "statusCode" -> JsNumber(1000),
                      "message" -> JsString("NA")
                    ).toString
                    val httpEntity = HttpEntity(ContentTypes.`application/json`, entity)
                    complete(HttpResponse(status = status).withEntity(httpEntity))
                  case status if status == StatusCodes.InternalServerError =>
                    logger.info(s"""post email : $body response by result: server error and status: $status""")
                    val entity = JsObject(
                      "status" -> JsString("ERROR"),
                      "statusCode" -> JsNumber(9999), // unknown error
                      "message" -> JsString("server_error")
                    ).toString
                    val httpEntity = HttpEntity(ContentTypes.`application/json`, entity)
                    complete(HttpResponse(status = StatusCodes.OK).withEntity(httpEntity))
                  case status =>
                    logger.info(s"""post email : $body response by status: $status""")
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
          //Necessary to let the browser make OPTIONS requests as it likes to do
          options {
            cors.corsHandler(complete(StatusCodes.OK))
          } ~
            post {
              cors.corsHandler {
                headerValue(extractToken(token)) { _ =>
                  entity(as[WebEngageUserInfo]) { body1 =>
                    val body = body1.copy(mobile_no = format(body1.mobile_no), email = EmailFormatter.format(body1.email))

                    val mobile = body1.mobile_no.getOrElse("")
                    val email = body1.email.getOrElse("")
                    val isValidMobile = isNumber(mobile)
                    val isValidEmail = EmailAddress.isValid(email)

                    println(s"mobile=$mobile,valid=$isValidMobile")
                    val msg = if (email.isEmpty && mobile.isEmpty) {
                      "one of the fields of mobile or email need to be defined"
                    }
                    else if (email.nonEmpty && !isValidEmail && mobile.nonEmpty && !isValidMobile) {
                      "invalid Email and mobile number"
                    }
                    else if (email.nonEmpty && !isValidEmail) {
                      "invalid Email"
                    }
                    else if (mobile.nonEmpty && !isValidMobile) {
                      "invalid mobile number"
                    }
                    else ""

                    if (msg.nonEmpty) {
                      logger.info(s"""post check user : $body response by result: server error and status: ${StatusCodes.BadRequest.intValue}""")
                      val entity = JsObject(
                        "status" -> JsString("ERROR"),
                        "error" -> JsString(msg)
                      ).toString
                      val httpEntity = HttpEntity(ContentTypes.`application/json`, entity)
                      complete(HttpResponse(status = StatusCodes.BadRequest).withEntity(httpEntity))
                    } else {
                      onSuccess(ClientActor.clientActor.ask(CheckUser(body)).mapTo[(WebEngageUserInfoWithUserId, Int)]) {
                        case (user, status) if status == StatusCodes.OK.intValue || status == StatusCodes.Created.intValue =>
                          val entity = JsObject(
                            "status" -> JsString("SUCCESS"),
                            "user_id" -> JsString(user.userId)
                          ).toString
                          val httpEntity = HttpEntity(ContentTypes.`application/json`, entity)
                          complete(HttpResponse(status = status).withEntity(httpEntity))
                        case (user, status) =>
                          logger.info(s"""post check user : $body response by result: server error and status: ${user.userId}""")
                          val entity = JsObject(
                            "status" -> JsString("ERROR"),
                            "error" -> JsString(user.userId)
                          ).toString
                          val httpEntity = HttpEntity(ContentTypes.`application/json`, entity)
                          complete(HttpResponse(status = status).withEntity(httpEntity))
                      }
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

                val body = body1.copy(mobile_no = format(body1.mobile_no), email = EmailFormatter.format(body1.email))

                val mobile = body1.mobile_no.getOrElse("")
                val email = body1.email.getOrElse("")
                val isValidMobile = isNumber(mobile)
                val isValidEmail = EmailAddress.isValid(email)

                val msg = if (email.isEmpty && mobile.isEmpty) {
                  "one of the fields of mobile or email need to be defined"
                }
                else if (email.nonEmpty && !isValidEmail && mobile.nonEmpty && !isValidMobile) {
                  "invalid Email and mobile number"
                }
                else if (email.nonEmpty && !isValidEmail) {
                  "invalid Email"
                }
                else if (mobile.nonEmpty && !isValidMobile) {
                  "invalid mobile number"
                }
                else ""


                if (msg.nonEmpty) {
                  logger.info(s"""post register user : $body response by result: server error and status: ${StatusCodes.BadRequest.intValue}""")
                  val entity = JsObject(
                    "status" -> JsString("ERROR"),
                    "error" -> JsString(msg),
                    "user_id" -> JsString("-1")
                  ).toString
                  val httpEntity = HttpEntity(ContentTypes.`application/json`, entity)
                  complete(HttpResponse(status = StatusCodes.BadRequest).withEntity(httpEntity))
                } else {
                  onSuccess(ClientActor.clientActor.ask(CheckUser(body)).mapTo[(WebEngageUserInfoWithUserId, Int)]) {
                    case (_, status) if status == StatusCodes.OK.intValue || status == StatusCodes.Created.intValue =>
                      val entity = JsObject(
                        "status" -> JsString("SUCCESS")
                      ).toString
                      val httpEntity = HttpEntity(ContentTypes.`application/json`, entity)
                      complete(HttpResponse(status = status).withEntity(httpEntity))
                    case (_, status) =>
                      logger.info(s"""post register user : $body response by result: server error and status: ${StatusCodes.InternalServerError.intValue}""")
                      val entity = JsObject(
                        "status" -> JsString("ERROR")
                      ).toString
                      val httpEntity = HttpEntity(ContentTypes.`application/json`, entity)
                      complete(HttpResponse(status = status).withEntity(httpEntity))
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

trait CORSHandler {

  private val corsResponseHeaders = List(
    `Access-Control-Allow-Origin`.*,
    `Access-Control-Allow-Credentials`(true),
    `Access-Control-Allow-Headers`("*"),
    `Access-Control-Max-Age`(1.day.toMillis) //Tell browser to cache OPTIONS requests
  )

  //this directive adds access control headers to normal responses
  private def addAccessControlHeaders: Directive0 = {
    respondWithHeaders(corsResponseHeaders)
  }

  //this handles preflight OPTIONS requests.
  private def preflightRequestHandler: Route = options {
    complete(HttpResponse(StatusCodes.OK).
      withHeaders(`Access-Control-Allow-Methods`(OPTIONS, POST, PUT, GET, DELETE)))
  }

  // Wrap the Route with this method to enable adding of CORS headers
  def corsHandler(r: Route): Route = addAccessControlHeaders {
    preflightRequestHandler ~ r
  }

  // Helper method to add CORS headers to HttpResponse
  // preventing duplication of CORS headers across code
  def addCORSHeaders(response: HttpResponse): HttpResponse =
    response.withHeaders(corsResponseHeaders)
}