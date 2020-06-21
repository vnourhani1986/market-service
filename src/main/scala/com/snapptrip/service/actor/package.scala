package com.snapptrip.service

import akka.actor.SupervisorStrategy.{Decider, Restart, Stop}
import akka.actor.{ActorInitializationException, ActorKilledException, OneForOneStrategy, SupervisorStrategy}
import cats.data.Ior

package object actor {

  trait Message

  final val DefaultStrategy: SupervisorStrategy = {
    def defaultDecider: Decider = {
      case _: ActorInitializationException => Stop
      case _: ActorKilledException => Stop
      case _: Exception => Restart
    }

    OneForOneStrategy()(defaultDecider)
  }

  object CommandEnum extends Enumeration {
    type Enum = Value
    val Register: Enum = Value("register")
    val Login: Enum = Value("login")
  }

  type Command = Ior[CommandEnum.Enum, CommandEnum.Enum]

  final implicit val createCommand: PartialFunction[String, Command] = {
    case fa if fa == "login" => Ior.right(CommandEnum.Login)
    case fa if fa == "register" => Ior.left(CommandEnum.Register)
    case fa if fa == "check" => Ior.both(CommandEnum.Register, CommandEnum.Login)
  }

}
