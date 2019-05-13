package com.snapptrip.api

import java.time.{LocalDate, LocalDateTime}

import com.snapptrip.models._

object Messages {

  trait Message

  // snapptrip
  case class UserInfo(username: String, businessId: Option[Long], role: String)
  case class BusinessName(en: String, fa: String)
  case class BusinessInfo(id: Long, name: Option[BusinessName], code: Option[String],
                          phone: Option[String], email: Option[String])
  case class FilterUser(userName: Option[String], name: Option[String],
                        family: Option[String], email: Option[String], role: Option[String])

  case class NewUser(userName: String, name: Option[String],
                     family: Option[String], email: Option[String],
                     role: String, businessId: Option[Long], disabled: Boolean)

  case class EditUser(userName: String, name: Option[String],
                      family: Option[String], email: Option[String],
                      role: String, businessId: Option[Long], disabled: Boolean)

  case class UserLoginInfo(username: String, password: String)
  case class UserLoginRequest(username: String, password: String, persist: Boolean)

  case class ServerError(message: String)

}