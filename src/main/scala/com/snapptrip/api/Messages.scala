package com.snapptrip.api

import java.time.LocalDate

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
                                mobile_no: Option[String] = None,
                                birth_date: Option[LocalDate] = None,
                                gender: Option[String] = None,
                                provider: Option[String] = None,
                                anonymous_id: Option[String] = None,
                                email_opt_in: Option[Boolean] = None,
                                sms_opt_in: Option[Boolean] = None,
                                whatsapp_opt_in: Option[Boolean] = None,
                                company: Option[String] = None,
                                hashed_email: Option[String] = None,
                                hashed_phone: Option[String] = None,
                                attributes: Option[JsValue] = None
                              )

  case class WebEngageUserInfoWithUserId(
                                          userId: Option[String],
                                          anonymousId: Option[String] = None,
                                          firstName: Option[String] = None,
                                          lastName: Option[String] = None,
                                          email: Option[String] = None,
                                          phone: Option[String] = None,
                                          birthDate: Option[String] = None,
                                          gender: Option[String] = None,
                                          emailOptIn: Option[Boolean] = None,
                                          smsOptIn: Option[Boolean] = None,
                                          whatsappOptIn: Option[Boolean] = None,
                                          company: Option[String] = None,
                                          hashedEmail: Option[String] = None,
                                          hashedPhone: Option[String] = None,
                                          attributes: Option[JsValue] = None
                                        )

  case class EventUserInfo(mobile_no: Option[String], email: Option[String])

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
