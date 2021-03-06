package com.snapptrip.service.actor

import java.time.LocalDate

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.snapptrip.api.Messages.{WebEngageUserInfo, WebEngageUserInfoWithUserId}
import com.snapptrip.formats.Formats._
import com.snapptrip.kafka.Setting.Key
import com.snapptrip.models.User
import com.snapptrip.service.actor.ClientActor.{CheckUserResult, LoginUserResult, RegisterUserResult}
import com.snapptrip.service.actor.DBActor._
import com.snapptrip.service.actor.UserActor.{LoginUser, RegisterUser}
import org.scalatest.{MustMatchers, WordSpecLike}
import spray.json._

import scala.util.Random

class UserActorSpec extends TestKit(ActorSystem("test-system"))
  with WordSpecLike
  with ImplicitSender
  with MustMatchers
  with StopSystemAfterAll {

  "a user actor " must {
    "login user " in {

      val endProb = TestProbe()
      val clientProb = TestProbe()
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

      val userInfo = WebEngageUserInfo(
        email = Some("test@pintapin.com"),
        mobile_no = Some("9124497405")
      )

      val result = LoginUserResult(
        Right("9124497405"),
        endProb.ref
      )

      val kafkaValue = WebEngageUserInfoWithUserId(
        email = user.email,
        birthDate = Some("2020-03-10T00:00:00+0430"),
        userId = Some(user.userId),
        phone = user.mobileNo
      ).toJson

      val dbActor: ActorRef = system.actorOf(Props(new Actor {
        override def receive: Receive = {
          case Find(userInfo: WebEngageUserInfo, ref, meta, _) => sender() ! FindResult(userInfo, Some(user), ref, meta, command = createCommand("login"))
          case Save(userInfo: WebEngageUserInfo, user: User, ref, meta, _) => sender() ! SaveResult(user, ref, meta, command = createCommand("login"))
          case Update(user: User, ref, meta, _) => sender() ! UpdateResult(user, updated = true, ref, meta, command = createCommand("login"))
        }
      }),
        s"""db-actor-${Random.nextInt}""")

      val actor = system.actorOf(Props(new Actor {

        val userActor: ActorRef = context.actorOf(UserActor(dbActor, clientProb.ref, publisherProb.ref), s"user-actor-${Random.nextInt}")

        override def receive: Receive = {
          case message: LoginUserResult => endProb.ref ! message
          case message => userActor.forward(message)
        }
      }))

      actor ! LoginUser(userInfo, endProb.ref)
      publisherProb.expectMsg((Key("9124497405", "track-user"), kafkaValue))
      clientProb.expectMsg(result)

    }
    "check user " in {

      val endProb = TestProbe()
      val clientProb = TestProbe()
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

      val userInfo = WebEngageUserInfo(
        email = Some("test@pintapin.com"),
        mobile_no = Some("9124497405")
      )

      val result = CheckUserResult(
        Right("9124497405"),
        endProb.ref
      )

      val kafkaValue = WebEngageUserInfoWithUserId(
        email = user.email,
        birthDate = Some("2020-03-10T00:00:00+0430"),
        userId = Some(user.userId),
        phone = user.mobileNo,
      ).toJson

      val dbActor: ActorRef = system.actorOf(Props(new Actor {
        override def receive: Receive = {
          case Find(userInfo: WebEngageUserInfo, ref, meta, _) => sender() ! FindResult(userInfo, Some(user), ref, meta)
          case Save(userInfo: WebEngageUserInfo, user: User, ref, meta, _) => sender() ! SaveResult(user, ref, meta)
          case Update(user: User, ref, meta, _) => sender() ! UpdateResult(user, updated = true, ref, meta)
        }
      }),
        s"""db-actor-${Random.nextInt}""")

      val testActor = system.actorOf(Props(new Actor {

        val userActor: ActorRef = context.actorOf(UserActor(dbActor, clientProb.ref, publisherProb.ref))

        override def receive: Receive = {
          case message: CheckUserResult => endProb.ref ! message
          case message => userActor.forward(message)
        }
      }))

      testActor ! RegisterUser(userInfo, endProb.ref)
      publisherProb.expectMsg((Key("9124497405", "track-user"), kafkaValue))
      clientProb.expectMsg(result)

    }
  }

}
