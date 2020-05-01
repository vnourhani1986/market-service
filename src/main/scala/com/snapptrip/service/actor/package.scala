package com.snapptrip.service

import akka.actor.{ActorInitializationException, ActorKilledException, OneForOneStrategy, SupervisorStrategy}
import akka.actor.SupervisorStrategy.{Decider, Restart, Stop}

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

}
