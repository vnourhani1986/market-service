package com.snapptrip.webengage

import akka.actor.Props
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import com.snapptrip.DI.{ec, materializer, system}
import com.snapptrip.api.Messages.WebEngageUserInfoWithUserId
import com.snapptrip.webengage.actor.WebEngageActor
import com.snapptrip.webengage.actor.WebEngageActor.SendUserInfo
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import spray.json.{JsObject, JsString}

import scala.concurrent.duration._

class WebEngageActorTest extends TestKit(system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  private implicit val timeout: Timeout = Timeout(1.minute)

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "An webengage actor" must {

    "send back messages :" in {

      val actor = system.actorOf(Props(new WebEngageActor(null)), "fghjkl")

      val user = WebEngageUserInfoWithUserId(
        userId = "c3df9e0a-9695-4b12-9e4f-b74b9d15438d",
        firstName = Some("test"),
        lastName = Some("test"),
        email = Some("test@pintapin.com"),
        phone = Some("09124497405"),
        birthDate = Some("2018-09-15T18:29:00-0800"),
        gender = Some("male")
      )

      actor ! SendUserInfo(user, 1)
      Thread.sleep(3000)
      expectMsg((200, JsObject("status" -> JsString("success"))))

    }

  }

}