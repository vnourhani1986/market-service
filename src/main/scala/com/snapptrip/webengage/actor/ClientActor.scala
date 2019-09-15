package com.snapptrip.webengage.actor

import java.time.format.DateTimeFormatter
import java.util.UUID

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.pipe
import akka.routing.{DefaultResizer, RoundRobinPool}
import akka.util.Timeout
import com.snapptrip.DI._
import com.snapptrip.api.Messages.{WebEngageEvent, WebEngageUserInfo, WebEngageUserInfoWithUserId}
import com.snapptrip.formats.Formats._
import com.snapptrip.kafka.Core.Key
import com.snapptrip.kafka.Publisher
import com.snapptrip.models.User
import com.snapptrip.repos.WebEngageUserRepoImpl
import com.snapptrip.utils.WebEngageConfig
import com.snapptrip.utils.formatters.EmailFormatter
import com.snapptrip.webengage.actor.ClientActor.{CheckUser, RegisterUser, Start, TrackEvent, _}
import com.typesafe.scalalogging.LazyLogging
import spray.json._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Random, Try}

class ClientActor(
                   implicit
                   system: ActorSystem,
                   ec: ExecutionContext,
                   timeout: Timeout
                 ) extends Actor with LazyLogging {

  val retryStep = 10
  val retryMax = 5
  self ! Start()

  override def receive(): Receive = {

    case Start() =>
      logger.info(s"""client actor start""")

    case RegisterUser(user) =>

      val ref = sender()
      userCheck(user).pipeTo(ref)

    case CheckUser(user) =>

      val ref = sender()
      userCheck(user).pipeTo(ref)

    case TrackEvent(event) =>

      val ref = sender()
      trackEventWithoutUserId(event).pipeTo(ref)

    case _ =>
      logger.info(s"""welcome to client actor""")

  }

  def userCheck(request: WebEngageUserInfo): Future[(WebEngageUserInfoWithUserId, Int)] = {

    (for {
      oldUser <- WebEngageUserRepoImpl.findByFilter(request)
      user <- if (oldUser.isDefined) {
        val webEngageUser = converter(request, oldUser)
        WebEngageUserRepoImpl.update(webEngageUser).map {
          case true =>
            val birthDate = Try {
              webEngageUser.birthDate.map(_.atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss" + WebEngageConfig.timeOffset)))
            }.toOption.flatten
            Right((converter(webEngageUser, birthDate), 200))
          case false =>
            Left(new Exception("can not update user data in database"))
        }
      } else {
        val webEngageUser = converter(request)
        WebEngageUserRepoImpl.save(webEngageUser).map { user =>
          val birthDate = Try {
            user.birthDate.map(_.atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss" + WebEngageConfig.timeOffset)))
          }.toOption.flatten
          Right((converter(webEngageUser, birthDate), 201))
        }
      }
      _ <- user match {
        case Right(u) => Publisher.publish(Key(u._1.userId, "track-user"), List(u._1.toJson))
        case Left(e) => Future.failed(e)
      }
    } yield {
      user.right.get
    }).recover {
      case error: Throwable =>
        (WebEngageUserInfoWithUserId(userId = error.getMessage), 500)
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
      oldUser <- WebEngageUserRepoImpl.findByFilter(user.mobile_no, user.email)
      userIdOpt = oldUser.map(_.userId)
      (userId, newRequest) <- if (userIdOpt.isDefined) {
        val lContent = JsObject("userId" -> JsString(userIdOpt.get)).fields.toList :::
          event.asJsObject.fields.filterKeys(_ == "eventTime").toList.flatMap(x => JsObject(x._1 -> JsString(x._2.compactPrint.replace(s""""""", "").concat(WebEngageConfig.timeOffset))).fields.toList) :::
          event.asJsObject.fields.filterKeys(x => x != "email" && x != "mobile_no" && x != "eventTime").toList
        val jContent = JsObject(lContent.toMap)
        Future.successful((userIdOpt.get, jContent))
      } else {
        val provider = event.asJsObject.fields.filterKeys(_ == "eventData").headOption.flatMap(_._2.asJsObject.fields.filterKeys(_ == "provider").headOption).map(_._2.compactPrint.replace(s""""""", ""))
        logger.info("provider", provider)
        userCheck(WebEngageUserInfo(mobile_no = user.mobile_no, email = user.email, provider = provider)).map { response =>
          val (body, _) = response
          val lContent = JsObject("userId" -> JsString(body.userId)).fields.toList :::
            event.asJsObject.fields.filterKeys(_ == "eventTime").toList.flatMap(x => JsObject(x._1 -> JsString(x._2.compactPrint.replace(s""""""", "").concat(WebEngageConfig.timeOffset))).fields.toList) :::
            event.asJsObject.fields.filterKeys(x => x != "email" && x != "mobile_no" && x != "eventTime").toList
          val jContent = JsObject(lContent.toMap)
          (userIdOpt.get, jContent)
        }
      }
      _ <- Publisher.publish(Key(userId, "track-event"), List(newRequest))
    } yield {
      (true, JsObject("status" -> JsString("success")))
    }).recover {
      case error: Throwable =>
        println(error.getMessage)
        (false, JsObject("status" -> JsString("failed")))
    }

  }

}

object ClientActor {

  private implicit val timeout: Timeout = Timeout(1.minute)
  val resizer = DefaultResizer(lowerBound = 2, upperBound = 25)
  val clientActor: ActorRef = system.actorOf(RoundRobinPool(10, Some(resizer)).props(Props(new ClientActor)), s"client-Actor-${Random.nextInt}")

  case class Start()

  case class RegisterUser(userInfo: WebEngageUserInfo)

  case class CheckUser(userInfo: WebEngageUserInfo)

  case class TrackEvent(event: WebEngageEvent)

  def converter(webEngageUserInfo: WebEngageUserInfo): User = {
    User(
      userName = webEngageUserInfo.user_name,
      userId = UUID.randomUUID().toString,
      name = webEngageUserInfo.name,
      family = webEngageUserInfo.family,
      email = EmailFormatter.format(webEngageUserInfo.email),
      originEmail = if (webEngageUserInfo.email.isDefined) List(webEngageUserInfo.email.get) else Nil,
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
      email = EmailFormatter.format(webEngageUserInfo.email).orElse(oldUser.get.email),
      originEmail = if (webEngageUserInfo.email.isDefined) oldUser.get.originEmail.filter(_ != webEngageUserInfo.email.get) ++ List(webEngageUserInfo.email.get) else oldUser.get.originEmail,
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
