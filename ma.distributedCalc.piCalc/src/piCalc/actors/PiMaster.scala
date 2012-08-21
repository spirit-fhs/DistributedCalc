package piCalc.actors

import akka.actor.{ Actor, ActorRef, Props }
import akka.routing.RoundRobinRouter
import piCalc.messages._
import api.events._
import api.message._

class PiMaster() extends Actor with Serializable {

  var calculateSender: ActorRef = _
  var startTime: Long = _
  var nrOfMessages: Int = _
  var nrOfResults: Int = 0
  var nrOfElements:Int = _
  var pi: Double = _
  var s: Int = 0
  var task:Task = _

  var piWorkers: ActorRef = _
  
  override def preStart() {
    context.system.eventStream.publish(NodeStarted(self))
  }
  
  override def postStop() {
    calculateSender ! IncompletePiResult(nrOfMessages-nrOfResults,nrOfElements, s, pi, calcTime)
    context.system.eventStream.publish(NodeStopped(self))
  }
  
  def receive = waiting

  def calcTime:Long = {
    System.currentTimeMillis() - startTime
  }
  
  def waiting:Receive = {
    case t:IncompleteCalculate => {
      context.become(calculating)
      task = t
      context.system.eventStream.publish(TaskStarted(self, task))
      startTime = System.currentTimeMillis()
      nrOfElements = t.nrOfElements
      nrOfMessages = t.nrOfMessages
      nrOfResults = 0
      pi = t.partPi
      s=t.partStart
      
      calculateSender = sender
      
      piWorkers = context.actorOf(Props[PiWorker].withRouter(RoundRobinRouter(t.nrOfWorkers)))
      
      for (i <- 0 until t.nrOfWorkers) {
        piWorkers ! Work(s * nrOfElements, nrOfElements)
        s += 1
      }
    }
    case t:Calculate => {
      context.become(calculating)
      task = t
      context.system.eventStream.publish(TaskStarted(self, task))
      startTime = System.currentTimeMillis()
      nrOfElements = t.nrOfElements
      nrOfMessages = t.nrOfMessages
      nrOfResults = 0
      pi = 0
      s=0
      
      calculateSender = sender
      
      piWorkers = context.actorOf(Props[PiWorker].withRouter(RoundRobinRouter(t.nrOfWorkers)))
      
      for (i <- 0 until t.nrOfWorkers) {
        piWorkers ! Work(s * nrOfElements, nrOfElements)
        s += 1
      }
    }
    
    case msg => {
      sender ! ("invalid message",msg)
    }
  }
  
  def calculating:Receive = {
    case PiResult(value, _) => {
      nrOfResults += 1
      pi += value
      
      if (nrOfResults != nrOfMessages) {
        piWorkers ! Work(s * nrOfElements, nrOfElements)
        s += 1
      } else {
        context.stop(piWorkers)
        context.unbecome()
        calculateSender ! PiResult(pi, calcTime)
        context.system.eventStream.publish(TaskFinished(self, task))
      }
    }
    
    case msg => {
      sender ! ("invalid message", msg)
    }
  }
}