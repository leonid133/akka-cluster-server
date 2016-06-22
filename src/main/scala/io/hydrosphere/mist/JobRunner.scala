package io.hydrosphere.mist

import java.util.concurrent.Executors._

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

private[mist] class JobRunner extends Actor {

  override def receive: Receive = {

    case configuration: JobConfiguration =>
      val originalSender = sender

  }
}
