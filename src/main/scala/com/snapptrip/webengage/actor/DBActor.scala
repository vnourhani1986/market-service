package com.snapptrip.webengage.actor

import akka.actor.{Actor, ActorRef, Props}
import com.snapptrip.repos.Repo

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

class DBActor[A, F](
                     repo: Repo[A, F]
                   )(
                     implicit ex: ExecutionContext
                   ) extends Actor {

  import DBActor._

  override def receive(): Receive = {

    case Find(f: F, ref) =>
      val result = repo.find(f)
        .map(r => ref ! r)
      Await.result(result, Duration.Inf)

    case Save(a: A, ref) =>
      val result = repo.save(a)
        .map(r => ref ! r)
      Await.result(result, Duration.Inf)

    case Update(a: A, ref) =>
      val result = repo.update(a)
        .map(r => ref ! r)
      Await.result(result, Duration.Inf)

  }

}

object DBActor {

  def apply[A, F](repo: Repo[A, F])(implicit ex: ExecutionContext): Props = {
    Props(new DBActor[A, F](repo))
  }

  case class Find[F](f: F, ref: ActorRef) extends Message

  case class Save[A](user: A, ref: ActorRef) extends Message

  case class Update[A](user: A, ref: ActorRef) extends Message

}

