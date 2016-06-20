package io.hydrosphere.mist

import akka.actor.Actor

/** Manages context repository */
class ContextManager extends Actor {
  override def receive: Receive = {
    case _ => println("************************")
    case Message(addr, msg) => println(addr, msg)
  }
}
