package com.snapptrip.service.actor

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import com.snapptrip.api.Messages.WebEngageUserInfo
import com.snapptrip.service.actor.ClientActor.CheckUserResult
import com.snapptrip.service.actor.UserActor.CheckUser
import org.scalatest.{MustMatchers, WordSpecLike}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class MarketServiceActorSpec extends TestKit(ActorSystem("test-system"))
  with WordSpecLike
  with ImplicitSender
  with MustMatchers
  with StopSystemAfterAll {

  "a market service actor " must {
    "check user " in {

//      implicit val executionContext: ExecutionContextExecutor = system.dispatcher
//      implicit val timeout: Timeout = Timeout(60 seconds)
//
//      val endProb = TestProbe()
//
//      val userInfo = WebEngageUserInfo(
//        email = Some("test@pintapin.com"),
//        mobile_no = Some("9124497405")
//      )
//
//      val result = CheckUserResult(
//        Right("9124497405"),
//        endProb.ref
//      )
//
//      val actor: ActorRef = system.actorOf(MarketServiceActor())
//
//      actor ! CheckUser(userInfo, endProb.ref)
//      endProb.expectMsg(result)

    }
  }

}
