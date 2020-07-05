package com.snapptrip.service.actor

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.snapptrip.repos.Repo
import com.snapptrip.service.actor.DBActor._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{MustMatchers, WordSpecLike}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.Random

class DBActorSpec extends TestKit(ActorSystem("test-system"))
  with WordSpecLike
  with ImplicitSender
  with MockFactory
  with MustMatchers
  with StopSystemAfterAll {

  implicit val ex: ExecutionContextExecutor = ExecutionContext.global


  "a database actor " must {

    "find by filter" in {

      case class A()
      case class F()

      val repo = stub[Repo[A, F]]
      (repo.find(_: String)).when(*).returns(Future.successful(Some(A())))

      val actor = system.actorOf(DBActor(repo), s"""db-actor-${Random.nextInt}""")

      actor ! Find(F(), testActor)
      expectMsg(FindResult(F(), Some(A()), testActor))

    }
    "save" in {

      case class A()
      case class F()

      val repo = stub[Repo[A, F]]
      (repo.save _).when(*).returns(Future.successful(A()))

      val actor = system.actorOf(DBActor(repo), s"""db-actor-${Random.nextInt}""")

      actor ! Save(F(), A(), testActor)
      expectMsg(SaveResult(A(), testActor))

    }
    "update" in {

      case class A()
      case class F()

      val repo = stub[Repo[A, F]]
      (repo.update _).when(*).returns(Future.successful(true))

      val actor = system.actorOf(DBActor(repo), s"""db-actor-${Random.nextInt}""")

      actor ! Update(A(), testActor)
      expectMsg(UpdateResult(A(), updated = true, testActor))

    }
    "save and update messages have higher priority than find" in {

      case class A()
      case class F()

      val repo = stub[Repo[A, F]]
      (repo.find(_: String)).when(*).returns(Future.successful(Some(A())))
      (repo.save _).when(*).returns(Future.successful(A()))
      (repo.update _).when(*).returns(Future.successful(true))

      val actor = system.actorOf(DBActor(repo).withDispatcher("mailbox.db-actor"), s"""db-actor""")

      actor ! Find(F(), testActor)
      actor ! Save(F(), A(), testActor)
      actor ! Find(F(), testActor)
      actor ! Find(F(), testActor)
      actor ! Update(A(), testActor)
      actor ! Find(F(), testActor)
      actor ! Update(A(), testActor)
      actor ! Update(A(), testActor)


      expectMsg(SaveResult(A(), testActor))
      expectMsg(UpdateResult(A(), updated = true, testActor))
      expectMsg(UpdateResult(A(), updated = true, testActor))
      expectMsg(UpdateResult(A(), updated = true, testActor))
      expectMsg(FindResult(F(), Some(A()), testActor))
      expectMsg(FindResult(F(), Some(A()), testActor))
      expectMsg(FindResult(F(), Some(A()), testActor))
      expectMsg(FindResult(F(), Some(A()), testActor))

    }
  }

}
