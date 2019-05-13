package com.snapptrip.api

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.BasicDirectives.extractExecutionContext
import akka.http.scaladsl.server.directives.FutureDirectives.onComplete
import akka.http.scaladsl.server.directives.RouteDirectives.reject
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Sink, Source}
import com.snapptrip.DI._
import com.snapptrip.api.Messages.{UserInfo, UserLoginInfo, UserLoginRequest}
import com.snapptrip.auth.SnapptripAuthConfig
import com.snapptrip.formats.Formats._
import com.snapptrip.repos.{BusinessRepoImpl, UsersRepoImpl}
import com.snapptrip.utils._
import com.typesafe.scalalogging.LazyLogging
import spray.json.{JsBoolean, JsObject, JsString, JsonParser, enrichAny}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object AuthHandler extends LazyLogging {

  private lazy val instance: AuthHandler = new AuthHandler

  def routes: Route = {
    pathPrefix("v1" / "auth") {
      path("login") {
        post {
          entity(as[UserLoginInfo]) { userInfo =>
            instance.login(userInfo)
          }
        }
      } ~
        path("user" / "info") {
          get {
            optionalHeaderValueByName("x-ptp-token") {
              case Some(value) => instance.getUserInfo(value)
              case None        => complete(StatusCodes.Unauthorized)
            }

          }
        }
    }
  }

  def authenticated(innerRoute: (UserInfo, Option[String]) => Route) = {
    optionalHeaderValueByName("x-ptp-token") {
      case Some(token) =>
        logger.info(s"""authHandler.authenticated, token:$token""")
        authorizeAsyncWithResult(instance.authentication(token)) { tu =>
          innerRoute(tu._1, tu._2)
        }
      case None        =>
        logger.error("authenticated rejected")
        reject(AuthorizationFailedRejection)
    }
  }

  def authorizeAsyncWithResult[T](f: Future[T]): Directive1[T] =
    extractExecutionContext.flatMap { implicit ec ⇒
      onComplete(f).flatMap {
        case Success(t) ⇒ provide(t)
        case _          ⇒ reject(AuthorizationFailedRejection)
      }
    }


}

class AuthHandler extends LazyLogging {

  private def loginUserToSnapptripAuth(username: String, password: String, persist: Boolean): Future[(String, StatusCode)] = {

    for {
      requestEntity <- Marshal(toLoginFormat(UserLoginRequest(username, password, persist))).to[RequestEntity]
      request = HttpRequest(HttpMethods.POST, SnapptripAuthConfig.loginUrl).withEntity(requestEntity.withContentType(ContentTypes.`application/json`))
      connectionFlow = Http().outgoingConnection(SnapptripAuthConfig.apiBaseUrl, SnapptripAuthConfig.port)
      response <- Source.single(request).via(connectionFlow).runWith(Sink.head)
      entity <- Unmarshal(response.entity).to[String]
      status = response.status
    } yield {
      (entity, status)
    }

  }

  private def toLoginFormat(request: UserLoginRequest): String = {

    val fJson = JsObject(
      "username" -> JsString(request.username),
      "password" -> JsString(request.password),
      "persist" -> JsBoolean(request.persist)
    )

    fJson.compactPrint

  }

  private def authUserToSnapptripAuth(token: String): Future[(String, StatusCode)] = {
    val request = HttpRequest(HttpMethods.GET, SnapptripAuthConfig.authUrl)
      .withHeaders(RawHeader("x-auth-id", token))
    val connectionFlow = Http().outgoingConnection(SnapptripAuthConfig.apiBaseUrl, SnapptripAuthConfig.port)
    for {
      response <- Source.single(request).via(connectionFlow).runWith(Sink.head)
      entity <- Unmarshal(response.entity).to[String]
      status = response.status
    } yield {
      (entity, status)
    }

  }


  private def login(userInfo: UserLoginInfo) = {

    val genTokenFut = for {

      (entity, status) <- loginUserToSnapptripAuth(userInfo.username, userInfo.password, false)

      _ <- if (status == OK) Future.successful(status)
      else Future.failed(new SnapptripAuthGetUserException("user is not valid", Some(ErrorCodes.USER_IS_NOT_VALID), StatusCodes.BadRequest))

      token = JsonParser(entity).asJsObject.getFields("user")
        .head.asJsObject.getFields("token").headOption.map(_.convertTo[String])

    } yield token

    onComplete(genTokenFut) {
      case Success(token) =>
        respondWithHeader(RawHeader("x-ptp-token", token.getOrElse(""))) {
          complete(OK)
        }
      case Failure(err)   =>
        //failWith(HttpError(StatusCodes.Unauthorized -> err))
        complete(StatusCodes.Unauthorized)
    }

  }

  def authentication(token: String): Future[(UserInfo, Option[String])] = {

    val genTokenFut = for {

      (entity, status) <- authUserToSnapptripAuth(token)
      _ <- if (status == OK) Future.successful(status)
      else Future.failed(new SnapptripAuthGetUserException("user is not valid", Some(ErrorCodes.USER_IS_NOT_VALID), StatusCodes.BadRequest))

      username = JsonParser(entity).asJsObject.getFields("username")
        .headOption.map(_.convertTo[String])

      _ <- if (username.isDefined) Future.successful(status)
      else Future.failed(new SnapptripAuthGetUserException("username field is not exist", Some(ErrorCodes.USER_IS_NOT_VALID), StatusCodes.BadRequest))

      userInfo <- UsersRepoImpl.findOptionalByUserName(username.get)

      _ <- if (userInfo.isDefined) Future.successful(true)
      else Future.failed(new SnapptripAuthGetUserException("username field is not exist", Some(ErrorCodes.USER_IS_NOT_VALID), StatusCodes.BadRequest))

      disabled <- UsersRepoImpl.isDisabled(userInfo.get.userName)

      _ <- if (!disabled) Future.successful(true)
      else Future.failed(new SnapptripAuthGetUserException("the username is disabled", Some(ErrorCodes.USER_IS_DISABLED), StatusCodes.Unauthorized))

      channel <- if (userInfo.get.businessId.isDefined)
        BusinessRepoImpl.findById(userInfo.get.businessId.get).map(_.map(business => s"""b2b_${business.code}"""))
      else Future.successful(None)

    } yield {

      (UserInfo(userInfo.get.userName, userInfo.get.businessId, userInfo.get.role.toString), channel)
    }

    genTokenFut

  }

  private def getUserInfo(token: String) = {

    val genTokenFut = for {

      (entity, status) <- authUserToSnapptripAuth(token)

      _ <- if (status == OK) Future.successful(status)
      else Future.failed(new SnapptripAuthGetUserException("user is not valid", Some(ErrorCodes.USER_IS_NOT_VALID), StatusCodes.BadRequest))

      username = JsonParser(entity).asJsObject.getFields("username")
        .headOption.map(_.convertTo[String])

      _ = logger.info(s"username:${username.getOrElse("")}")

      _ <- if (username.isDefined) Future.successful(status)
      else Future.failed(new SnapptripAuthGetUserException("username field is not exist", Some(ErrorCodes.USER_IS_NOT_VALID), StatusCodes.BadRequest))

      userInfo <- UsersRepoImpl.findOptionalByUserName(username.get)

      _ <- if (userInfo.isDefined) Future.successful(true)
      else Future.failed(new SnapptripAuthGetUserException("username in not found", Some(ErrorCodes.USER_IS_NOT_VALID), StatusCodes.BadRequest))

      _ = logger.info(s"userInfo:$userInfo")

    } yield UserInfo(userInfo.get.userName, userInfo.get.businessId, userInfo.get.role.toString)

    onComplete(genTokenFut) {
      case Success(userInfo) =>
        respondWithHeader(RawHeader("x-ptp-token", token)) {
          complete(OK, userInfo.toJson)
        }
      case Failure(err)      =>
        //failWith(HttpError(StatusCodes.Unauthorized -> err))
        complete(StatusCodes.Unauthorized)
    }

  }

}