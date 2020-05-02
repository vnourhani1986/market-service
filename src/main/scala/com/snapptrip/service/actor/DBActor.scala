package com.snapptrip.service.actor

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import akka.dispatch.{ControlMessage, PriorityGenerator, UnboundedStablePriorityMailbox}
import com.snapptrip.repos.Repo
import com.snapptrip.utils.Exceptions.{ErrorCodes, ExtendedException}
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}

class DBActor[A, F](
                     repo: Repo[A, F]
                   )(
                     implicit ex: ExecutionContext
                   ) extends Actor
  with LazyLogging {

  import DBActor._

  override def receive(): Receive = {

    case Find(f: F, ref, meta) =>
      val senderRef = sender()
      repo.find(f).map(r => senderRef ! FindResult(f, r, ref, meta))
        .recover {
          case error =>
            logger.error("find user" + error.getMessage)
            Future.failed(ExtendedException(error.getMessage, ErrorCodes.DatabaseError, ref))
        }

    case Save(f: F, a: A, ref, meta) =>
      val senderRef = sender()
      repo.save(a).map(r => senderRef ! SaveResult(r, ref, meta))
        .recoverWith {
          case error =>
            logger.error("save user" + error.getMessage)
            repo.find(f).map{
              case Some(r) => senderRef ! SaveResult(r, ref, meta)
              case None => senderRef ! SaveResult(a, ref, meta, fail = true)
            }
            Future.failed(ExtendedException(error.getMessage, ErrorCodes.DatabaseError, ref))
        }

    case Update(a: A, ref, meta) =>
      val senderRef = sender()
      repo.update(a).map(r => senderRef ! UpdateResult(a, r, ref, meta))
        .recoverWith {
          case error =>
            logger.error("update user" + error.getMessage)
            Future.failed(ExtendedException(error.getMessage, ErrorCodes.DatabaseError, ref))
        }

  }

}

object DBActor {

  def apply[A, F](repo: Repo[A, F])(implicit ex: ExecutionContext): Props = {
    Props(new DBActor[A, F](repo))
  }

  case class Find[F, M](f: F, ref: ActorRef, meta: Option[M] = None) extends Message

  case class Save[F, A, M](f: F, user: A, ref: ActorRef, meta: Option[M] = None) extends Message

  case class Update[A, M](user: A, ref: ActorRef, meta: Option[M] = None) extends Message

  case class FindResult[A, F, M](filter: F, user: Option[A], ref: ActorRef, meta: Option[M] = None) extends Message

  case class SaveResult[A, M](a: A, ref: ActorRef, meta: Option[M] = None, fail: Boolean = false) extends Message

  case class UpdateResult[A, M](a: A, updated: Boolean, ref: ActorRef, meta: Option[M] = None) extends Message

  class Mailbox(
                 setting: ActorSystem.Settings,
                 config: Config
               ) extends UnboundedStablePriorityMailbox(
    PriorityGenerator {
      case _: Find[_, _] => 1
      case _: Save[_, _, _] => 0
      case _: Update[_, _] => 0
      case _: PoisonPill => 3
      case _ => 2
    })


}

