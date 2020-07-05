package com.snapptrip.service

import java.time.LocalDate

import akka.actor.SupervisorStrategy.{Decider, Restart, Stop}
import akka.actor.{ActorInitializationException, ActorKilledException, OneForOneStrategy, SupervisorStrategy}
import cats.data.Ior
import com.snapptrip.api.Messages.WebEngageUserInfo
import com.snapptrip.formats.Formats.{booleanFormatter, stringFormatter}
import com.snapptrip.formats.Formats._
import spray.json.{JsValue, JsonParser, JsonReader}

import scala.util.Try

package object actor {

  trait Message

  final val DefaultStrategy: SupervisorStrategy = {
    def defaultDecider: Decider = {
      case _: ActorInitializationException => Stop
      case _: ActorKilledException => Stop
      case _: Exception => Restart
    }

    OneForOneStrategy()(defaultDecider)
  }

  object CommandEnum extends Enumeration {
    type Enum = Value
    val Register: Enum = Value("register")
    val Login: Enum = Value("login")
  }

  type Command = Ior[CommandEnum.Enum, CommandEnum.Enum]

  final implicit val createCommand: PartialFunction[String, Command] = {
    case fa if fa == "login" => Ior.right(CommandEnum.Login)
    case fa if fa == "register" => Ior.left(CommandEnum.Register)
    case fa if fa == "check" => Ior.both(CommandEnum.Register, CommandEnum.Login)
  }

  final implicit val stringReader: JsonReader[String] = JsonReader.func2Reader(stringFormatter)
  final implicit val booleanReader: JsonReader[Boolean] = JsonReader.func2Reader(booleanFormatter)
  final implicit val stringOptReader: Option[JsValue] => Option[String] = _.flatMap(json => Try{json.convertTo[String]}.toOption)
  final implicit val booleanOptReader: Option[JsValue] => Option[Boolean] = _.flatMap(json => Try{json.convertTo[Boolean]}.toOption)
  final implicit val LocalDateOptReader: Option[JsValue] => Option[LocalDate] = _.flatMap(json => Try{json.convertTo[LocalDate]}.toOption)

  final implicit val biAttributesToUserInfo: String => WebEngageUserInfo = { attributes =>

    val jsonFields = JsonParser(attributes).asJsObject.fields

    WebEngageUserInfo(
      user_name = jsonFields.get("user_name"),
      name = jsonFields.get("first_name"),
      family = jsonFields.get("last_name"),
      email = jsonFields.get("email"),
      mobile_no = jsonFields.get("mobile"),
      birth_date = jsonFields.get("birth_date"),
      gender = jsonFields.get("gender"),
      provider = jsonFields.get("provider"),
      anonymous_id = jsonFields.get("anonymous_id"),
      email_opt_in = jsonFields.get("email_opt_in"),
      sms_opt_in = jsonFields.get("sms_opt_in"),
      whatsapp_opt_in = jsonFields.get("whatsapp_opt_in"),
      company = jsonFields.get("company"),
      hashed_email = jsonFields.get("hashed_email"),
      hashed_phone = jsonFields.get("hashed_phone"),
      attributes = Option(jsonFields.filterKeys(attribute =>
        attribute != "user_name" &&
          attribute != "first_name" &&
          attribute != "last_name" &&
          attribute != "email" &&
          attribute != "mobile" &&
          attribute != "birth_date" &&
          attribute != "gender" &&
          attribute != "provider" &&
          attribute != "anonymous_id" &&
          attribute != "email_opt_in" &&
          attribute != "sms_opt_in" &&
          attribute != "whatsapp_opt_in" &&
          attribute != "company" &&
          attribute != "hashed_email" &&
          attribute != "hashed_phone"
      ).toJson)
    )

  }

}
