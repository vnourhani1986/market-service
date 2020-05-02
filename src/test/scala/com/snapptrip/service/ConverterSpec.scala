package com.snapptrip.service

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import org.scalatest.{FlatSpec, Matchers}
import spray.json._

class ConverterSpec extends FlatSpec with Matchers with Converter {

  "event modify " should "add user id to event and change birth date" in {

    val userId = "9124497405"

    val event =
      s"""{
            "eventName":"Fulfilled",
            "eventTime":"2020-03-10T12:18:32",
            "eventData":{
              "familyName":"test",
              "provider": "hotel"
            }
          }""".stripMargin.parseJson

    val modifiedEvent =
      s"""{
            "userId": "9124497405",
            "eventTime":"2020-03-10T12:18:32+0430",
            "eventName":"Fulfilled",
            "eventData":{
             "familyName":"test",
             "provider": "hotel"
            }
          }""".stripMargin.parseJson

    modifyEvent(userId, event) shouldBe Right(userId, modifiedEvent)

  }
  "format date" should "format local date to new format" in {

    val date = LocalDate.of(2020, 1, 1)
    dateTimeFormatter(date, DateTimeFormatter.ISO_LOCAL_DATE_TIME, Some("+0430")) shouldBe Right("2020-01-01T00:00:00+0430")

  }
  "get provider" should "find provider from json value" in {

    val event =
      s"""{
            "eventName":"Fulfilled",
            "eventTime":"2020-03-10T12:18:32",
            "eventData":{
              "familyName":"test",
              "provider": "hotel"
            }
          }""".stripMargin.parseJson

    getProvider(event) shouldBe Some("hotel")

  }

}
