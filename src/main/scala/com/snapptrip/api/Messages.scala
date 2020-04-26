package com.snapptrip.api

import java.time.LocalDate

import com.snapptrip.utils.formatters.MobileNoFormatter._
import spray.json.JsValue

object Messages {

  // webengage notification provider -> (SSP, SEP)
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
    format(mobile_no)
  }

  case class WebEngageUserAttributes(Age: String)

  case class WebEngageUserInfoWithUserId(
                                          userId: String,
                                          firstName: Option[String] = None,
                                          lastName: Option[String] = None,
                                          email: Option[String] = None,
                                          var phone: Option[String] = None,
                                          birthDate: Option[String] = None,
                                          gender: Option[String] = None
                                        ) {
    phone = format(phone)
  }

  case class EventUserInfo(var mobile_no: Option[String], email: Option[String]) {
    format(mobile_no)
  }

  case class WebEngageEvent(user: EventUserInfo, event: JsValue)

  case class SubjectIdentities(
                                identity_type: String,
                                identity_value: String
                              )

  case class OpengdprRequests(
                               subject_request_id: String,
                               subject_request_type: String,
                               subject_identities: List[SubjectIdentities]
                             )


}
