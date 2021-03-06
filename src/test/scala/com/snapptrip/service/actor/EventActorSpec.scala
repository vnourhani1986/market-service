package com.snapptrip.service.actor

import java.time.LocalDate

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.snapptrip.api.Messages.{EventUserInfo, WebEngageEvent, WebEngageUserInfo}
import com.snapptrip.kafka.Setting.Key
import com.snapptrip.models.User
import com.snapptrip.service.actor.DBActor._
import com.snapptrip.service.actor.EventActor.TrackEvent
import org.scalatest.{MustMatchers, WordSpecLike}
import spray.json._

import scala.util.Random

class EventActorSpec extends TestKit(ActorSystem("test-system"))
  with WordSpecLike
  with ImplicitSender
  with MustMatchers
  with StopSystemAfterAll {

  "a event actor " must {
    "track event user " in {

      val endProb = TestProbe()
      val publisherProb = TestProbe()

      val user = User(
        id = Some(1L),
        userName = Some("test-user"),
        userId = "9124497405",
        name = Some("test"),
        family = Some("test"),
        email = Some("test@pintapin.com"),
        originEmail = List("test@snapptrip.com"),
        mobileNo = Some("9124497405"),
        birthDate = Some(LocalDate.of(2020, 3, 10)),
        gender = Some("male"),
        provider = Some("hotel")
      )

      val eventBody =
        s"""{
              "eventName":"Fulfilled",
              "eventTime":"2020-03-10T12:18:32",
              "eventData":{
                "familyName":"test",
                "provider": "hotel"
              }
          }""".stripMargin.parseJson

      val event = WebEngageEvent(
        EventUserInfo(
          mobile_no = Some("9124497405"),
          email = Some("test@pintapin.com")
        ),
        eventBody
      )

      val kafkaUserValue =
        s"""{
           |"email":"test@pintapin.com",
           |"lastName":"test",
           |"firstName":"test",
           |"birthDate":"2020-03-10T00:00:00+0430",
           |"userId":"9124497405",
           |"phone":"9124497405",
           |"gender":"male"
           |}""".stripMargin.parseJson

      val kafkaEventValue =
        s"""{
	        "userId": "9124497405",
	        		"eventName": "Fulfilled",
	        		"eventTime": "2020-03-10T12:18:32+0430",
	        		"eventData":{
                "familyName":"test",
                "provider": "hotel"
              }

        }""".stripMargin.parseJson

      val dbActor: ActorRef = system.actorOf(Props(new Actor {
        override def receive: Receive = {
          case Find(userInfo: WebEngageUserInfo, ref, meta, _) => sender() ! FindResult(userInfo, Some(user), ref, meta)
          case Save(userInfo: WebEngageUserInfo, user: User, ref, meta, _) => sender() ! SaveResult(user, ref, meta)
          case Update(user: User, ref, meta, _) => sender() ! UpdateResult(user, updated = true, ref, meta)
        }
      }),
        s"""db-actor-${Random.nextInt}""")

      val testActor = system.actorOf(Props(new Actor {

        val userActor: ActorRef = context.actorOf(EventActor(dbActor, publisherProb.ref))

        override def receive: Receive = {
          case message => userActor.forward(message)
        }
      }))

      testActor ! TrackEvent(event, endProb.ref)
      publisherProb.expectMsg((Key("9124497405", "track-user"), kafkaUserValue))
      publisherProb.expectMsg((Key("9124497405", "track-event"), kafkaEventValue))

    }

  }

}
