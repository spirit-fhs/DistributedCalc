package mathOp

import akka.actor.Actor
import api.message._
import api.events._

trait MathOp

case class Add(nbr1: Int, nbr2: Int) extends Task with MathOp

case class Subtract(nbr1: Int, nbr2: Int) extends Task with MathOp

case class Multiply(nbr1: Int, nbr2: Int) extends Task with MathOp

case class Divide(nbr1: Double, nbr2: Int) extends Task with MathOp

trait MathResult

case class AddResult(nbr: Int, nbr2: Int, result: Int) extends Result with MathResult

case class SubtractResult(nbr1: Int, nbr2: Int, result: Int) extends Result with MathResult

case class MultiplicationResult(nbr1: Int, nbr2: Int, result: Int) extends Result with MathResult

case class DivisionResult(nbr1: Double, nbr2: Int, result: Double) extends Result with MathResult

class AdvancedCalculatorActor extends Actor with Serializable {
  override def preStart(){
    context.system.eventStream.publish(NodeStarted(self))
  }
  override def postStop(){
	context.system.eventStream.publish(NodeStopped(self))  
  }
  def receive = {
    case t:Multiply => {
      context.system.eventStream.publish(TaskStarted(self, t))
      sender ! MultiplicationResult(t.nbr1, t.nbr2, t.nbr1 * t.nbr2)
      context.system.eventStream.publish(TaskFinished(self, t))
    }

    case t:Divide => {
      context.system.eventStream.publish(TaskStarted(self, t))
      sender ! DivisionResult(t.nbr1, t.nbr2, t.nbr1 / t.nbr2)
      context.system.eventStream.publish(TaskFinished(self, t))
    }
  }
}