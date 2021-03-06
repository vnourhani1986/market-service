package com.snapptrip.service.actor

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import com.snapptrip.api.Messages.WebEngageUserInfoWithUserId
import com.snapptrip.service.actor.WebEngageActor.SendUserInfo
import org.scalamock.scalatest.MockFactory
import org.scalatest.{MustMatchers, WordSpecLike}
import spray.json.{JsObject, JsString}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.concurrent.duration._
import scala.util.Random

class WebEngageActorSpec extends TestKit(ActorSystem("test-system"))
  with WordSpecLike
  with ImplicitSender
  with MockFactory
  with MustMatchers
  with StopSystemAfterAll {

  private implicit val timeout: Timeout = Timeout(1.minute)

  "An webengage actor" must {

    "send back messages :" in {

      implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global

      val actor = system.actorOf(WebEngageActor(testActor, testActor, testActor), s"webengage-actor-${Random.nextInt}")

      val user = WebEngageUserInfoWithUserId(
        userId = Some("c3df9e0a-9695-4b12-9e4f-b74b9d15438d"),
        firstName = Some("test"),
        lastName = Some("test"),
        email = Some("test@pintapin.com"),
        phone = Some("09124497405"),
        birthDate = Some("2018-09-15T18:29:00-0800"),
        gender = Some("male")
      )

//      actor ! SendUserInfo(user, 1)
//      expectMsg((200, JsObject("status" -> JsString("success"))))

    }

  }

}