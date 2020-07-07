package com.snapptrip.service.actor

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import com.snapptrip.api.Messages.WebEngageUserInfo
import com.snapptrip.service.actor.ClientActor.RegisterUserResult
import com.snapptrip.service.actor.UserActor.RegisterUser
import org.scalatest.{MustMatchers, WordSpecLike}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class MarketServiceActorSpec extends TestKit(ActorSystem("test-system"))
  with WordSpecLike
  with ImplicitSender
  with MustMatchers
  with StopSystemAfterAll {

  "a market service actor " must {
    "register user " in {

      implicit val executionContext: ExecutionContextExecutor = system.dispatcher
      implicit val timeout: Timeout = Timeout(60 seconds)

      val endProb = TestProbe()

      val userInfo = WebEngageUserInfo(
        email = Some("test@pintapin.com"),
        mobile_no = Some("9124497405")
      )

      val result = RegisterUserResult(
        Right("9124497405"),
        endProb.ref
      )

      val actor: ActorRef = system.actorOf(MarketServiceActor())

      actor ! RegisterUser(userInfo, endProb.ref)
      endProb.expectMsg(result)

    }
  }

}
