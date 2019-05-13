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

  // web engage notification provider -> (SSP, SEP)
  case class SMSData(toNumber: String, fromNumber: String, body: String)
  case class WMetaData(campaignType: String, timestamp: String, messageId: String)
  case class WebEngageSMSBody(version: String, smsData: SMSData, metadata: WMetaData)
  case class NameEmail(name: String, email: String)
  case class NameUrl(name: String, url: String)
  case class Recipients(to: List[NameEmail], cc: List[String], bcc: List[String])
  case class Email(from: String, fromName: String, replyTo: List[String], subject: String, text: String, html: String,
                   recipients: Recipients, attachments: List[NameUrl])
  case class WebEngageEmailBody(email: Email, metadata: WMetaData, version: String)

}