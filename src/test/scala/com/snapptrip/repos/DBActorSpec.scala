package com.snapptrip.repos

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.snapptrip.actor.StopSystemAfterAll
import com.snapptrip.models.User
import com.snapptrip.webengage.actor.DBActor
import com.snapptrip.webengage.actor.DBActor._
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{MustMatchers, WordSpecLike}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

class DBActorSpec extends TestKit(ActorSystem("test-system"))
  with WordSpecLike
  with ImplicitSender
  with AsyncMockFactory
  with MustMatchers
  with StopSystemAfterAll {

  implicit val ex: ExecutionContextExecutor = ExecutionContext.global


  "a database actor " must {

    "find by filter" in {

      case class A()
      case class F()

      val repo = stub[Repo[A, F]]
      (repo.find _).when(*).returns(Future.successful(Some(A())))

      val actor = system.actorOf(DBActor(repo), "db-actor")

      actor ! Find(F(), testActor)
      expectMsg(FindResult(Some(A()), null, testActor))

    }
    "save" in {

      case class A()
      case class F()

      val repo = stub[Repo[A, F]]
      (repo.save _).when(*).returns(Future.successful(A()))

      val actor = system.actorOf(DBActor(repo), "db-actor")

      actor ! Save(A(), testActor)
      expectMsg(SaveResult(A(), testActor))

    }
    "update" in {

      case class A()
      case class F()

      val repo = stub[Repo[A, F]]
      (repo.update _).when(*).returns(Future.successful(true))

      val actor = system.actorOf(DBActor(repo), "db-actor")

      actor ! Update(A(), testActor)
      expectMsg(UpdateResult(null, updated = true, testActor))

    }
  }

}
