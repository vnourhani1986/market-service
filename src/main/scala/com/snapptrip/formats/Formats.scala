package com.snapptrip.formats

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import com.snapptrip.api.HealthCheckHandler.HealthCheckResponse
import com.snapptrip.api.Messages._
import com.snapptrip.kafka.Setting.{DeleteCancelKey, Key}
import com.snapptrip.models._
import com.snapptrip.utils.Exceptions.HttpError
import com.snapptrip.utils.MetaData
import spray.json.{RootJsonFormat, _}

object Formats extends FormatsComponent

trait FormatsComponent extends JsonProtocol {

  // models
  implicit val userFormater = jsonFormat15(User)
  implicit val userDBFromatFormater = jsonFormat15(UserDBFormat)
  implicit val healthCheckResponseFormatter = jsonFormat4(HealthCheckResponse)
  implicit val httpErrorFormat = jsonFormat3(HttpError)
  implicit val metaDataFormat = jsonFormat9(MetaData)
  implicit val smsDataFormat = jsonFormat3(SMSData)
  implicit val wMetaDataFormat = jsonFormat3(WMetaData)
  implicit val webEngageSMSBodyFormat = jsonFormat3(WebEngageSMSBody)
  implicit val nameEmailFormat = jsonFormat2(NameEmail)
  implicit val nameUrlFormat = jsonFormat2(NameUrl)
  implicit val recipientsFormat = jsonFormat3(Recipients)
  implicit val emailFormat = jsonFormat8(Email)
  implicit val webEngageEmailBodyFormat = jsonFormat3(WebEngageEmailBody)
  implicit val webEngageUserInfoFormat = jsonFormat16(WebEngageUserInfo)
  implicit val webEngageUserWithUserIdInfoFormat = jsonFormat15(WebEngageUserInfoWithUserId)
  implicit val eventUserInfoFormat = jsonFormat2(EventUserInfo)
  implicit val webEngageEventFormat = jsonFormat2(WebEngageEvent)
  implicit val keyFormat = jsonFormat2(Key)
  implicit val deleteCancelKeyFormat = jsonFormat2(DeleteCancelKey)
  implicit val webEngageSubjectIdentitiesFormat = jsonFormat2(SubjectIdentities)
  implicit val webEngageOpengdprRequeststFormat = jsonFormat3(OpengdprRequests)

}


trait JsonProtocol extends DefaultJsonProtocol {

  val shortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  val shortDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  implicit val localDateTimeFormat: RootJsonFormat[LocalDateTime] = new RootJsonFormat[LocalDateTime] {
    def write(x: LocalDateTime) = JsString(x.format(shortDateTimeFormatter))

    def read(value: JsValue) = value match {
      case JsString(x) if x.contains("T") => LocalDateTime.parse(x, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
      case JsString(s) => LocalDateTime.parse(s, shortDateTimeFormatter)
      case x => throw new RuntimeException(s"Unexpected type ${x.getClass.getName} when trying to parse LocalDateTime")
    }
  }

  implicit val localDateFormat: RootJsonFormat[LocalDate] = new RootJsonFormat[LocalDate] {
    private val iso_date = DateTimeFormatter.ISO_DATE

    def write(x: LocalDate) = JsString(iso_date.format(x))

    def read(value: JsValue) = value match {
      case JsString(x) => LocalDate.parse(x, iso_date)
      case x => throw new RuntimeException(s"Unexpected type ${x.getClass.getName} when trying to parse LocalDateTime")
    }
  }

  implicit val uuidJsonFormat: RootJsonFormat[UUID] = new RootJsonFormat[UUID] {
    def write(uuid: UUID) = {
      uuid.toString.toJson
    }

    def read(value: JsValue) = {
      UUID.fromString(value.prettyPrint)
    }

  }

  implicit def enumFormat[T <: Enumeration](implicit enu: T): RootJsonFormat[T#Value] =
    new RootJsonFormat[T#Value] {
      def write(obj: T#Value): JsValue = JsString(obj.toString)

      def read(json: JsValue): T#Value = {
        json match {
          case JsString(txt) => enu.withName(txt)
          case somethingElse => throw DeserializationException(s"Expected a value from enum $enu instead of $somethingElse")
        }
      }
    }

  final implicit val stringFormatter: JsValue => String = {
    case JsString(s) => s
    case x => throw new RuntimeException(s"Unexpected type ${x.getClass.getName} when trying to parse attributes")
  }

  final implicit val booleanFormatter: JsValue => Boolean = {
    case JsBoolean(b) => b
    case x => throw new RuntimeException(s"Unexpected type ${x.getClass.getName} when trying to parse attributes")
  }

}