package com.snapptrip.service.actor

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import com.snapptrip.api.Messages.WebEngageUserInfo
import org.scalamock.scalatest.MockFactory
import org.scalatest.{MustMatchers, WordSpecLike}
import spray.json
import spray.json.JsonParser

import scala.concurrent.duration._

class SubscriberActorSpec extends TestKit(ActorSystem("test-system"))
  with WordSpecLike
  with ImplicitSender
  with MockFactory
  with MustMatchers
  with StopSystemAfterAll {

  private implicit val timeout: Timeout = Timeout(1.minute)

  "An attribute to user info converter" must {

    "return :" in {

      val attributes = JsonParser(s"""{"snapptrip_purchase_count":4,"snapptrip_last_purchase_date":"2020-06-24 19:22:45.096000","snapptrip_first_purchase_date":"2018-04-12 01:33:38.194051","first_name":"محمد کاظم","last_name":"عسگری","hotel_fulfilled_count":2,"hotel_first_fulfilled_date":"2018-04-12 01:33:38.194051","hotel_last_fulfilled_date":"2018-09-04 06:36:58.535476","train_issue_count":2,"train_first_issue_date":"2020-06-23 19:26:59.355000","train_last_issue_date":"2020-06-24 19:22:45.096000","flight_issue_count":null,"flight_last_issue_date":null,"flight_first_issue_date":null,"bus_issue_count":null,"bus_first_issue_date":null,"bus_last_issue_date":null,"mobile":"9127024325"}""".stripMargin)

      SubscriberActor.biAttributesToUserInfo(attributes) mustBe WebEngageUserInfo(None,Some("محمد کاظم"),Some("عسگری"),None,Some("9127024325"),None,None,None,None,None,None,None,None,None,None,Some(json.JsonParser(s"""{"train_first_issue_date":"2020-06-23 19:26:59.355000","train_issue_count":2,"flight_issue_count":null,"flight_first_issue_date":null,"bus_last_issue_date":null,"flight_last_issue_date":null,"snapptrip_first_purchase_date":"2018-04-12 01:33:38.194051","hotel_first_fulfilled_date":"2018-04-12 01:33:38.194051","hotel_fulfilled_count":2,"bus_issue_count":null,"train_last_issue_date":"2020-06-24 19:22:45.096000","snapptrip_last_purchase_date":"2020-06-24 19:22:45.096000","bus_first_issue_date":null,"hotel_last_fulfilled_date":"2018-09-04 06:36:58.535476","snapptrip_purchase_count":4}""")))

    }

  }

}