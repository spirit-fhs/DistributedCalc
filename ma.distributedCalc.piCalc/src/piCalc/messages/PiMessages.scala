package piCalc.messages

import akka.actor.ActorRef
import api.message._

trait PiMessage

case class Calculate(
  nrOfWorkers: Int,
  nrOfMessages: Int,
  nrOfElements: Int) extends PiMessage with Task
  
case class PiResult(value: Double, duration: Long) extends PiMessage with Result

case class Work(start: Int, nrOfElements: Int) extends PiMessage


case class IncompleteCalculate(
    nrOfWorkers: Int,
    nrOfMessages: Int,
    nrOfElements: Int,
    partStart: Int,
    partPi: Double,
    partTime: Long) extends PiMessage with Task with Incomplete
    
case class IncompletePiResult(
		nrOfMessages: Int,
		nrOfElements: Int,
		partStart: Int,
		partPi: Double,
		partTime: Long) extends PiMessage with Result with Incomplete