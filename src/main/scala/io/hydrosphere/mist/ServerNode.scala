package io.hydrosphere.mist

import akka.actor._
import akka.cluster.ClusterEvent._
import akka.cluster._
import akka.event._

//import sbt.{Process, _}

import java.lang.{ProcessBuilder => JProcessBuilder}
import sys.process._

import scala.concurrent.duration._

import akka.actor.{Address, AddressFromURIString, Deploy, Props}
import akka.remote.RemoteScope
import akka.pattern.{ask, AskTimeoutException}

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToResponseMarshallable

import spray.json._
import org.json4s.DefaultFormats
import org.json4s.native.Json

case object Tick
case class Message(sender: String, message: String)

private[mist] trait JsonFormatSupport extends DefaultJsonProtocol{
  /** We must implement json parse/serializer for [[Any]] type */
  implicit object AnyJsonFormat extends JsonFormat[Any] {
    def write(x: Any) = x match {
      case number: Int => JsNumber(number)
      case string: String => JsString(string)
      case sequence: Seq[_] => seqFormat[Any].write(sequence)
      case map: Map[String, _] => mapFormat[String, Any] write map
      case boolean: Boolean if boolean => JsTrue
      case boolean: Boolean if !boolean => JsFalse
      case unknown => serializationError("Do not understand object of type " + unknown.getClass.getName)
    }
    def read(value: JsValue) = value match {
      case JsNumber(number) => number.toBigInt()
      case JsString(string) => string
      case array: JsArray => listFormat[Any].read(value)
      case jsObject: JsObject => mapFormat[String, Any].read(value)
      case JsTrue => true
      case JsFalse => false
      case unknown => deserializationError("Do not understand how to deserialize " + unknown)
    }
  }

  // JSON to JobConfiguration mapper (6 fields)
  implicit val jobCreatingRequestFormat = jsonFormat6(JobConfiguration)
}

class ClusterListener extends Actor with ActorLogging with JsonFormatSupport{

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
          nodeCounter += 1

          // actor which is used for running jobs according to request

          lazy val jobRequestActor:ActorRef = cluster.system.actorOf(Props[JobRunner].
            withDeploy(Deploy(scope = RemoteScope(AddressFromURIString(addr + "/user/clusterMist")))), name = "SyncJobRunner")

          val stringMessage = "{\"jarPath\":\"/home/lblokhin/cluster/worker-node/mistjob_2.10-1.0.jar\", \"className\":\"SimpleContext$\",\"parameters\":{\"digits\":[1,2,3,4,5,6,7,8,9,0]}, \"external_id\":\"12345678\",\"name\":\"foo\"}"
          val json = stringMessage.parseJson
          val jobCreatingRequest = json.convertTo[JobConfiguration]
          println(jobCreatingRequest.parameters)
            val future = jobRequestActor.ask(jobCreatingRequest)(timeout = 1.day)

            future
              .onSuccess {
              case result: Either[Map[String, Any], String] =>
                val jobResult: JobResult = result match {
                  case Left(jobResults: Map[String, Any]) =>
                    JobResult(success = true, payload = jobResults, request = jobCreatingRequest, errors = List.empty)
                  case Right(error: String) =>
                    JobResult(success = false, payload = Map.empty[String, Any], request = jobCreatingRequest, errors = List(error))
                }
                println(Json(DefaultFormats).write(jobResult))
            }


          /*
          val refSparkContext = cluster.system.actorOf(Props[ContextManager].
            withDeploy(Deploy(scope = RemoteScope(AddressFromURIString(addr + "/user/clusterMist")))))

          val contextFuture = refSparkContext ? new Message(myAddress.toString, "Hey. Wats up men?")
          contextFuture
            .onSuccess{
              case msg => println(msg)
            }
          */
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
