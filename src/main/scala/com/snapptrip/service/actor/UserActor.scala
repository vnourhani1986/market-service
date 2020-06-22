package com.snapptrip.service.actor

import java.time.format.DateTimeFormatter
import java.util.UUID

import akka.actor.SupervisorStrategy.Resume
import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props, SupervisorStrategy}
import akka.util.Timeout
import com.snapptrip.api.Messages.WebEngageUserInfo
import com.snapptrip.formats.Formats._
import com.snapptrip.kafka.Setting.Key
import com.snapptrip.models.User
import com.snapptrip.service.Converter
import com.snapptrip.service.actor.ClientActor.{CheckUserResult, LoginUserResult, RegisterUserResult}
import com.snapptrip.service.actor.DBActor.{Find, Save, Update}
import com.snapptrip.utils.Exceptions.{ErrorCodes, ExtendedException}
import com.snapptrip.utils.WebEngageConfig
import com.typesafe.scalalogging.LazyLogging
import spray.json._

import scala.concurrent.duration._

class UserActor(
                 dbRouter: => ActorRef,
                 clientActor: ActorRef,
                 publisherActor: => ActorRef
               )(
                 implicit timeout: Timeout
               ) extends Actor with Converter with LazyLogging {

  type E <: Throwable

  import UserActor._

  override def preStart(): Unit = {
    super.preStart()
    context.watch(dbRouter)
  }

  override def supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy(10, 60 seconds, loggingEnabled = true) {
      case _: E => Resume
    }

  override def receive(): Receive = {

    case RegisterUser(user, ref) =>
      self ! FindUser(user, ref, "register")

    case LoginUser(user, ref) =>
      self ! FindUser(user, ref, "login")

    case CheckUser(user, ref) =>
      self ! FindUser(user, ref, "check")

    case FindUser(user, ref, command) =>
      dbRouter ! Find(user, ref, command = command)

    case DBActor.FindResult(newUser: WebEngageUserInfo, oldUserOpt: Option[User], ref, _, fail, command) =>
      if (fail) {
        command match {
          case c if c.isLeft => clientActor ! RegisterUserResult(Left(ExtendedException("can not query data in database",
            ErrorCodes.DatabaseQueryError)), ref)
          case c if c.isRight => clientActor ! LoginUserResult(Left(ExtendedException("can not query data in database",
            ErrorCodes.DatabaseQueryError)), ref)
          case c if c.isBoth => clientActor ! CheckUserResult(Left(ExtendedException("can not query data in database",
            ErrorCodes.DatabaseQueryError)), ref)
        }
      } else {
        (oldUserOpt, command) match {
          case (Some(_), c) if c.isLeft =>
            clientActor ! RegisterUserResult(Left(ExtendedException("user already exist", ErrorCodes.UserAlreadyExistError)), ref)
          case (userOpt: Some[User], _) =>
            val user = converter(newUser, userOpt)
            dbRouter ! Update(user, ref)
          case (None, c) if c.isRight =>
            clientActor ! LoginUserResult(Left(ExtendedException("user does not exist", ErrorCodes.UserIsNotExistError)), ref)
          case (None, _) =>
            val newUserId = UUID.randomUUID().toString
            val user = converter(newUser, newUserId)
            dbRouter ! Save(newUser, user, ref, command = command)
        }
      }

    case DBActor.UpdateResult(user: User, updated, ref, _, command) =>
      val result = if (updated) {
        val birthDate = user.birthDate.map(b => dateTimeFormatter(
          b, DateTimeFormatter.ISO_LOCAL_DATE_TIME, Some(WebEngageConfig.timeOffset)) match {
          case Right(value) => value
          case Left(exception) => throw ExtendedException(exception.getMessage, ErrorCodes.TimeFormatError, ref)
        })
        val wUser = converter(user, birthDate)
        self ! SendToKafka(Key(wUser.userId.get, "track-user"), wUser.toJson)
        Right(wUser.userId.get)
      } else {
        Left(ExtendedException("can not update user data in database", ErrorCodes.DatabaseQueryError))
      }
      command match {
        case c if c.isRight => clientActor ! LoginUserResult(result, ref)
        case c if c.isBoth => clientActor ! CheckUserResult(result, ref)
      }


    case DBActor.SaveResult(user: User, ref, _, fail, command) =>
      if (fail) {
        command match {
          case c if c.isLeft => clientActor ! RegisterUserResult(Left(ExtendedException("can not save user data in database",
            ErrorCodes.DatabaseQueryError)), ref)
          case c if c.isBoth => clientActor ! CheckUserResult(Left(ExtendedException("can not save user data in database",
            ErrorCodes.DatabaseQueryError)), ref)
        }
      } else {
        val birthDate = user.birthDate.map(b => dateTimeFormatter(
          b, DateTimeFormatter.ISO_LOCAL_DATE_TIME, Some(WebEngageConfig.timeOffset)) match {
          case Right(value) => value
          case Left(exception) => throw ExtendedException(exception.getMessage, ErrorCodes.TimeFormatError, ref)
        })
        val wUser = converter(user, birthDate)
        self ! SendToKafka(Key(wUser.userId.get, "track-user"), wUser.toJson)
        command match {
          case c if c.isLeft => clientActor ! RegisterUserResult(Right(wUser.userId.get), ref)
          case c if c.isBoth => clientActor ! CheckUserResult(Right(wUser.userId.get), ref)
        }
      }

    case SendToKafka(key, value) =>
      publisherActor ! (key, value)

  }

}

object UserActor {

  import com.snapptrip.DI.timeout

  def apply(dbActor: => ActorRef, clientActor: => ActorRef, kafkaActor: => ActorRef): Props = {
    Props(new UserActor(dbActor, clientActor, kafkaActor))
  }

  case class RegisterUser(userInfo: WebEngageUserInfo, ref: ActorRef) extends Message

  case class LoginUser(userInfo: WebEngageUserInfo, ref: ActorRef) extends Message

  case class CheckUser(userInfo: WebEngageUserInfo, ref: ActorRef) extends Message

  case class FindUser(user: WebEngageUserInfo, ref: ActorRef, implicit val command: Command) extends Message

  case class SendToKafka(key: Key, value: JsValue) extends Message

}

