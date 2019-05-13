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
import com.snapptrip.repos.{BusinessRepoImpl, UsersRepoImpl}
import com.snapptrip.services.WebEngage
import com.typesafe.scalalogging.LazyLogging
import spray.json.{JsNumber, JsObject, JsString, JsValue}

import scala.concurrent.ExecutionContextExecutor

class RouteHandler(system: ActorSystem, timeout: Timeout) extends LazyLogging {

  implicit val requestTimeout: Timeout = timeout

  implicit def executionContext: ExecutionContextExecutor = system.dispatcher

  def routs: Route =

    HealthCheckHandler.route ~ AuthHandler.routes ~ webEngageApi ~
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
      } ~
        get {
          logger.info(
            s"""get users request""")
          onSuccess(UsersRepoImpl.get) {
            users =>
              logger.info(s"""get users response users size : ${users.size} by user : $userInfo""")
              complete(users)
          }
        } ~
        path("add-user") {
          post {
            entity(as[NewUser]) { user =>
              logger.info(
                s"""add new user request by filter content: $user""")
              onSuccess(UsersRepoImpl.save(user)) {
                id =>
                  logger.info(s"""add new user response by id : $id by user : $userInfo""")
                  complete(id.toString)
              }
            }
          }
        } ~
        path("edit-user") {
          put {
            entity(as[EditUser]) { user =>
              logger.info(
                s"""edit new user request by filter content: $user""")
              onSuccess(UsersRepoImpl.update(user)) {
                saveResult =>
                  logger.info(s"""edit new user response by result : $saveResult by user : $userInfo""")
                  complete(saveResult.toString)
              }
            }
          }
        }
    }
  }

  def webEngageApi: Route = {

    pathPrefix("api" / "v1" / "webengage") {
      path("users") {
        post {
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
      } ~
      path("events") {
        post {
          entity(as[JsValue]) { body =>
            logger.info(s"""post events request by body $body""")
            onSuccess(WebEngage.trackEvent(body)) {
              case (status, entity) if status == StatusCodes.Created =>
                logger.info(s"""post events response by result: $entity and status: $status""")
                complete(HttpResponse(status = status).withEntity(entity))
              case (status, _) if status == StatusCodes.InternalServerError =>
                logger.info(s"""post events response by result: server error and status: $status""")
                complete(HttpResponse(status = status))
              case (status, entity) =>
                logger.info(s"""post events response by result: $entity and status: $status""")
                complete(HttpResponse(status = status).withEntity(entity))
            }
          }
        }
      }
    }
  }


}

