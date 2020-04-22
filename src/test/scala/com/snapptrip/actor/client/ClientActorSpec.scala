package com.snapptrip.actor.client

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.snapptrip.actor.StopSystemAfterAll
import org.scalatest.{MustMatchers, WordSpecLike}

class ClientActorSpec extends TestKit(ActorSystem("test-system"))
  with WordSpecLike
  with ImplicitSender
  with MustMatchers
  with StopSystemAfterAll {

  "a client actor " must {
    "" in {
      fail("not implemented")
    }
  }

}
