package com.snapptrip.controlmessage

import akka.actor.{Actor, PoisonPill, Props}
import akka.dispatch.ControlMessage
import akka.event.{Logging, LoggingAdapter}
import com.snapptrip.DI._
import com.snapptrip.controlmessage.Logger.MyControlMessage

class Logger extends Actor {
  val log: LoggingAdapter = Logging(context.system, this)

  self ! 'foo
  self ! 'bar
  self ! PoisonPill
  self ! MyControlMessage()
  self ! PoisonPill

  def receive: PartialFunction[Any, Unit] = {
    case x => log.info(x.toString)
  }
}

object Logger {

  case class MyControlMessage() extends ControlMessage

  system.actorOf(Props(new Logger()).withDispatcher("control-aware-dispatcher"), "logger")

}