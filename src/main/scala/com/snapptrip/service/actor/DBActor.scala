package com.snapptrip.service.actor

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import akka.dispatch.{ControlMessage, PriorityGenerator, UnboundedStablePriorityMailbox}
import com.snapptrip.repos.Repo
import com.snapptrip.utils.Exceptions.{ErrorCodes, ExtendedException}
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}

class DBActor[A, F](
                     repo: Repo[A, F]
                   )(
                     implicit ex: ExecutionContext
                   ) extends Actor {

  import DBActor._

  override def receive(): Receive = {

    case Find(f: F, ref, meta) =>
      val senderRef = sender()
      repo.find(f).map(r => senderRef ! FindResult(f, r, ref, meta))
        .recover {
          case error => Future.failed(ExtendedException(error.getMessage, ErrorCodes.DatabaseError, ref))
        }

    case Save(a: A, ref, meta) =>
      val senderRef = sender()
      repo.save(a).map(r => senderRef ! SaveResult(r, ref, meta))
        .recover {
          case error => Future.failed(ExtendedException(error.getMessage, ErrorCodes.DatabaseError, ref))
        }

    case Update(a: A, ref, meta) =>
      val senderRef = sender()
      repo.update(a).map(r => senderRef ! UpdateResult(a, r, ref, meta))
        .recoverWith {
          case error => Future.failed(ExtendedException(error.getMessage, ErrorCodes.DatabaseError, ref))
        }

  }

}

object DBActor {

  def apply[A, F](repo: Repo[A, F])(implicit ex: ExecutionContext): Props = {
    Props(new DBActor[A, F](repo))
  }

  case class Find[F, M](f: F, ref: ActorRef, meta: Option[M] = None) extends Message

  case class Save[A, M](user: A, ref: ActorRef, meta: Option[M] = None) extends Message with ControlMessage

  case class Update[A, M](user: A, ref: ActorRef, meta: Option[M] = None) extends Message with ControlMessage

  case class FindResult[A, F, M](filter: F, user: Option[A], ref: ActorRef, meta: Option[M] = None) extends Message

  case class SaveResult[A, M](a: A, ref: ActorRef, meta: Option[M] = None) extends Message

  case class UpdateResult[A, M](a: A, updated: Boolean, ref: ActorRef, meta: Option[M] = None) extends Message

  class Mailbox(
                 setting: ActorSystem.Settings,
                 config: Config
               ) extends UnboundedStablePriorityMailbox(
    PriorityGenerator {
      case _: Find[_, _] => 1
      case _: Save[_, _] => 0
      case _: Update[_, _] => 0
      case _: PoisonPill => 3
      case _ => 2
    })


}

