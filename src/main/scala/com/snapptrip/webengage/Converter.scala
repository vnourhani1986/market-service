package com.snapptrip.webengage

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

import com.snapptrip.api.Messages.{WebEngageUserInfo, WebEngageUserInfoWithUserId}
import com.snapptrip.models.User
import com.snapptrip.utils.WebEngageConfig
import com.snapptrip.utils.formatters.EmailFormatter

import scala.util.Try

trait Converter {

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

  def dateTimeFormatter(date: LocalDate): Option[String] = {
    Try {
      date.atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss" + WebEngageConfig.timeOffset))
    }.toOption
  }

}
