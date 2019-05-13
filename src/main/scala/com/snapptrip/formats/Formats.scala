package com.snapptrip.formats

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import com.snapptrip.api.HealthCheckResponse
import com.snapptrip.api.Messages._
import com.snapptrip.models._
import com.snapptrip.utils.{HttpError, MetaData}
import com.sun.xml.internal.bind.v2.model.core.ID
import spray.json.{RootJsonFormat, _}

object Formats extends FormatsComponent

trait FormatsComponent extends JsonProtocol {

  // models
  implicit val userInfoFormat = jsonFormat3(UserInfo)
  implicit val businessNameFormater = jsonFormat2(BusinessName)
  implicit val businessInfoFormater = jsonFormat5(BusinessInfo)
  implicit val filterUserFormater = jsonFormat5(FilterUser)
  implicit val newUserFormater = jsonFormat7(NewUser)
  implicit val editUserFormater = jsonFormat7(EditUser)
  implicit val userFormater = jsonFormat12(User)
  implicit val userLoginInfoFormat = jsonFormat2(UserLoginInfo)
  implicit val userLoginRequestFormat = jsonFormat3(UserLoginRequest)
  implicit val healthCheckResponseFormatter = jsonFormat4(HealthCheckResponse)
  implicit val httpErrorFormat = jsonFormat3(HttpError)
  implicit val metaDataFormat = jsonFormat9(MetaData)

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

}