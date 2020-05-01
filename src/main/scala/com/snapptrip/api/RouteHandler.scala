package com.snapptrip.api

import java.time.format.DateTimeFormatter

import akka.actor._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{HttpResponse, StatusCodes, _}
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.util.Timeout
import com.snapptrip.DI.config
import com.snapptrip.api.Messages._
import com.snapptrip.formats.Formats._
import com.snapptrip.notification.email.EmailService
import com.snapptrip.notification.sms.SmsService
import com.snapptrip.service.Converter
import com.snapptrip.service.actor.ClientActor.{CheckUser, TrackEvent}
import com.snapptrip.service.api.WebEngageApi
import com.snapptrip.utils.Exceptions.ExtendedException
import com.snapptrip.utils.formatters.{EmailFormatter, MobileNoFormatter}
import com.typesafe.scalalogging.LazyLogging
import spray.json.{JsNumber, JsObject, JsString, JsValue}

import scala.concurrent.ExecutionContext

class RouteHandler(
                    token: String,
                    marketServiceActor: ActorRef
                  )(implicit
                    system: ActorSystem,
                    timeout: Timeout,
                    ec: ExecutionContext
                  ) extends RouteParser
  with LazyLogging {

  import RouteHandler._

  def routs: Route = HealthCheckHandler().route ~ ping ~ webEngageApi(token)

  def ping: Route = path("api" / "v1" / "webengage" / "user" / "check") {
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
                  val httpEntity = HttpEntity(ContentTypes.`application/json`, entity.compactPrint)
                  complete(HttpResponse(status = status).withEntity(httpEntity))
                case (status, _) if status == StatusCodes.InternalServerError =>
                  logger.info(s"""post user: $body response by result: server error and status: $status""")
                  complete(HttpResponse(status = status))
                case (status, entity) =>
                  logger.info(s"""post user: $body response by result: $entity and status: $status""")
                  val httpEntity = HttpEntity(ContentTypes.`application/json`, entity.compactPrint)
                  complete(HttpResponse(status = status).withEntity(httpEntity))
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
                    ).compactPrint
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
                  entity(as[WebEngageUserInfo]) { body =>
                    bodyParser(body)(validateBody)(formatBody) { userInfo =>
                      onSuccess(marketServiceActor.ask(CheckUser(userInfo)).mapTo[Either[ExtendedException, String]]) {
                        case Right(userId) =>
                          val entity = JsObject(
                            "status" -> JsString("SUCCESS"),
                            "user_id" -> JsString(userId)
                          ).compactPrint
                          val httpEntity = HttpEntity(ContentTypes.`application/json`, entity)
                          complete(HttpResponse(status = StatusCodes.Created).withEntity(httpEntity))
                        case Left(ex) =>
                          val entity = JsObject(
                            "status" -> JsString("ERROR"),
                            "error" -> JsString(ex.message)
                          ).compactPrint
                          val httpEntity = HttpEntity(ContentTypes.`application/json`, entity)
                          complete(HttpResponse(status = StatusCodes.InternalServerError).withEntity(httpEntity))
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
              entity(as[WebEngageUserInfo]) { body =>
                bodyParser(body)(validateBody)(formatBody) { userInfo =>
                  onSuccess(marketServiceActor.ask(CheckUser(userInfo)).mapTo[Either[ExtendedException, String]]) {
                    case Right(userId) =>
                      val entity = JsObject(
                        "status" -> JsString("SUCCESS"),
                        "user_id" -> JsString(userId)
                      ).compactPrint
                      val httpEntity = HttpEntity(ContentTypes.`application/json`, entity)
                      complete(HttpResponse(status = StatusCodes.Created).withEntity(httpEntity))
                    case Left(ex) =>
                      val entity = JsObject(
                        "status" -> JsString("ERROR"),
                        "error" -> JsString(ex.message)
                      ).compactPrint
                      val httpEntity = HttpEntity(ContentTypes.`application/json`, entity)
                      complete(HttpResponse(status = StatusCodes.InternalServerError).withEntity(httpEntity))
                  }
                }
              }
            }
          }
        } ~
        path("events") {
          post {
            headerValue(extractToken(token)) { _ =>
              entity(as[WebEngageEvent]) { body =>
                bodyParser(body)(validateBody)(formatBody) { event =>
                  onSuccess(marketServiceActor.ask(TrackEvent(event)).mapTo[String]) { _ =>
                    val entity = JsObject(
                      "status" -> JsString("SUCCESS")
                    ).compactPrint
                    val httpEntity = HttpEntity(ContentTypes.`application/json`, entity)
                    complete(HttpResponse(status = StatusCodes.Created).withEntity(httpEntity))
                  }
                }
              }
            }
          }
        }
    }
  }

}

object RouteHandler extends CORSHandler with Converter {

  def apply(
             token: String = token,
             marketServiceActor: ActorRef
           )(implicit
             system: ActorSystem,
             timeout: Timeout,
             ec: ExecutionContext
           ): RouteHandler = new RouteHandler(token, marketServiceActor)

  val token: String = config.getString("web-engage.token")

  private val cors = new CORSHandler {}

  def formatBody(body: WebEngageEvent): WebEngageEvent = {
    body.copy(
      user = body.user.copy(
        mobile_no = MobileNoFormatter.format(body.user.mobile_no),
        email = EmailFormatter.format(body.user.email)
      )
    )
  }

  def formatBody(body: WebEngageUserInfo): WebEngageUserInfo = {
    body.copy(
      mobile_no = MobileNoFormatter.format(body.mobile_no),
      email = EmailFormatter.format(body.email)
    )
  }

  def validateBody[A](body: A): (Boolean, String) = {

    val (mobileNo, email, birthDate) = body match {
      case b: WebEngageEvent => (b.user.mobile_no, b.user.email, None)
      case b: WebEngageUserInfo => (b.mobile_no, b.email, b.birth_date)
    }

    val birthDateIsValid = birthDate.forall { b =>
      dateTimeFormatter(b, DateTimeFormatter.ISO_LOCAL_DATE_TIME, None) match {
        case Right(_) => true
        case Left(_) => false
      }
    }

    val isValidMobile: Boolean = mobileNo match {
      case Some(_) => MobileNoFormatter.format(mobileNo).isDefined
      case None => true
    }

    val isValidEmail: Boolean = email match {
      case Some(_) => EmailFormatter.format(email).isDefined
      case None => true
    }

    if (!birthDateIsValid) {
      (false, "birth date is not valid")
    } else if (email.isEmpty && mobileNo.isEmpty) {
      (false, "one of the fields of mobile or email need to be defined")
    }
    else if (email.nonEmpty && !isValidEmail && mobileNo.nonEmpty && !isValidMobile) {
      (false, "invalid Email and mobile number")
    }
    else if (email.nonEmpty && !isValidEmail) {
      (false, "invalid Email")
    }
    else if (mobileNo.nonEmpty && !isValidMobile) {
      (false, "invalid mobile number")
    }
    else (true, "")
  }

}