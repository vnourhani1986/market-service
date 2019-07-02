package com.snapptrip.api

import java.time.LocalDate

import spray.json.JsValue

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

  case class WebEngageUserInfo(
                                user_name: Option[String] = None,
                                name: Option[String] = None,
                                family: Option[String] = None,
                                email: Option[String] = None,
                                var mobile_no: Option[String] = None,
                                birth_date: Option[LocalDate] = None,
                                gender: Option[String] = None,
                                provider: Option[String] = None
                              ) {
    mobile_no.map(format)
  }

  case class WebEngageUserAttributes(Age: String)

  case class WebEngageUserInfoWithUserId(
                                          userId: String,
//                                          user_name: Option[String] = None,
                                          firstName: Option[String] = None,
                                          lastName: Option[String] = None,
                                          email: Option[String] = None,
                                          var phone: Option[String] = None,
                                          birthDate: Option[String] = None,
                                          gender: Option[String] = None,
//                                          provider: Option[String] = None,

                                        ) {
    phone = phone.map(format)
  }

  case class EventUserInfo(var mobile_no: Option[String], email: Option[String]) {
    mobile_no.map(format)
  }

  case class WebEngageEvent(user: EventUserInfo, event: JsValue)

  val formats = List(("""[0][9][0-9][0-9]{8,8}""", 1), ("""[0][0][9][8][9][0-9][0-9]{8,8}""", 4), ("""[+][9][8][9][0-9][0-9]{8,8}""", 3))

  def format(mobileNo: String) = {
    formats.map(x => (mobileNo.trim.matches(x._1), x)).find(_._1).map(_._2._2).map(mobileNo.trim.substring).getOrElse(mobileNo.trim)
  }

}
