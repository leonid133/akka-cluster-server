package io.hydrosphere.mist


import java.lang.{ProcessBuilder => JProcessBuilder}

import akka.actor._
import akka.cluster.ClusterEvent._
import akka.cluster._
import akka.event._
import akka.actor.{Address, AddressFromURIString, Deploy, Props}
import akka.remote.RemoteScope


//import scala.sys.process.Process

//import sbt.{Process, _}
import sys.process._

import scala.concurrent.duration._

import akka.pattern.ask


case object Tick
case class Message(sender: String, message: String)

class ClusterListener extends Actor with ActorLogging {

  val cluster = Cluster(context.system)

  val clusterState = cluster.state

  def scheduler = context.system.scheduler

  val myAddress = cluster.selfAddress

  var nodeCounter = 1

  // subscribe to cluster changes, re-subscribe when restart
  override def preStart() {
    cluster.subscribe(self, InitialStateAsEvents, classOf[MemberEvent], classOf[UnreachableMember])
    scheduler.scheduleOnce(5.seconds, self, Tick)
  }

  override def postStop() {
    cluster.unsubscribe(self)
  }

  def receive = LoggingReceive {

    case Message(addr, msg) =>{
      (addr, msg) match {
        case (_, "Im Up") => {
          println (addr)
          println (msg)
          if (addr != myAddress.toString)
          cluster.system.actorSelection (addr + "/user/clusterMist") ! new Message (myAddress.toString, "Im Leader")
          cluster.system.actorSelection (addr + "/user/clusterMist") ! new Message (myAddress.toString, s"Now you node number #${nodeCounter}")
          val Adress = AddressFromURIString(addr)
          val refSparkContext = cluster.system.actorOf(Props[ContextManager].
            withDeploy(Deploy(scope = RemoteScope(AddressFromURIString(addr + "/user/clusterMist")))))

          nodeCounter += 1

          val contextFuture = refSparkContext ? new Message(myAddress.toString, "Hey. Wats up men?")
          contextFuture
            .onSuccess{
              case msg => println(msg)
            }
        }
        case _ => println(addr, msg)
      }
    }
    case Tick => {
      scheduler.scheduleOnce(5.seconds, self, Tick)
      //start New work Node
/*
      println("start node")
      val thread = new Thread {
        override def run {
          "./startworker.sh" !
        }
      }
      thread.start
*/
    }

    case MemberUp(member) => {
      println(s"[Listener] node is up: $member")

    }

    case UnreachableMember(member) =>
      println(s"[Listener] node is unreachable: $member")

    case MemberRemoved(member, prevStatus) =>
      println(s"[Listener] node is removed: $member after $prevStatus")

    case ev: MemberEvent =>
      println(s"[Listener] event: $ev")
  }
}

object ServerNode extends App {
  val system = ActorSystem("mist-system")
  system.actorOf(Props[ClusterListener], "clusterMist")
  system.awaitTermination()
}
