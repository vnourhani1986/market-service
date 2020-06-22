package com.snapptrip.service

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

import com.snapptrip.api.Messages.{WebEngageUserInfo, WebEngageUserInfoWithUserId}
import com.snapptrip.models.User
import com.snapptrip.utils.WebEngageConfig
import com.snapptrip.utils.formatters.EmailFormatter
import com.typesafe.scalalogging.LazyLogging
import spray.json.{JsObject, JsString, JsValue}

import scala.util.Try

trait Converter extends LazyLogging {

  def converter(webEngageUserInfo: WebEngageUserInfo, newUserId: String): User = {
    User(
      userName = webEngageUserInfo.user_name,
      userId = newUserId,
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
      id = oldUser.flatMap(_.id),
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
      userId = Some(user.userId),
      firstName = user.name,
      lastName = user.family,
      email = user.email,
      phone = user.mobileNo,
      birthDate = birthDate,
      gender = user.gender
    )
  }

  def dateTimeFormatter[D]: PartialFunction[(D, DateTimeFormatter, Option[String]), Either[Throwable, String]] = {
    case (d: LocalDate, format, offset) =>
      Try {
        d.atStartOfDay().format(format) + offset.getOrElse("")
      }.toEither
    case (d: LocalDateTime, format, offset) =>
      Try {
        d.format(format) + offset.getOrElse("")
      }.toEither
    case (d: String, format, offset) =>
      Try {
        LocalDateTime.parse(d).format(format) + offset.getOrElse("")
      }.toEither
  }

  def modifyEvent(userId: String, event: JsValue, timeFormat: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                  timeOffset: String = WebEngageConfig.timeOffset): Either[Throwable, (String, JsObject)] = {
    Try {
      val eventTime = event.asJsObject.fields.filterKeys(_ == "eventTime").toList.flatMap { x =>
        JsObject(x._1 -> JsString(x._2.compactPrint.replace(s""""""", ""))).fields.toList
      }.map { js =>
        dateTimeFormatter(js._2.compactPrint.replace(s""""""", ""), timeFormat, Some(timeOffset)) match {
          case Right(value) => (js._1, JsString(value))
          case Left(exception: Exception) => throw exception
        }
      }
      val jsUserId = JsObject("userId" -> JsString(userId)).fields.toList
      val jsEvent = jsUserId ::: eventTime ::: event.asJsObject.fields.filterKeys(_ != "eventTime").toList

      val content = JsObject(jsEvent.toMap)
      (userId, content)
    }.toEither
  }

  def getProvider(json: JsValue): Option[String] = {
    json.asJsObject.fields.filterKeys(_ == "eventData").toList.flatMap { x =>
      x._2.asJsObject.fields.filterKeys(_ == "provider")
    }.headOption.map(_._2.compactPrint.replace(s""""""", ""))
  }

}
