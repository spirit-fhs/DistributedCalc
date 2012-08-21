package actors.projectManager

import api.message._
import mathOp._
import scala.util.Random
import akka.actor.Actor

class MathOpManager extends Actor {
  def receive() = {
    
    case TaskRequest => {
      for (i <- 0 until 10) {
        sender ! provideTask
      }
    }
    case result: MathResult => {
      processResult(result)
    }

    case ("invalid Message", msg) => {
      println(sender + " couldn't understand MSG(" + msg + ")")
    }
    case msg => {
      sender ! ("invalid Message", msg)
    }
  }

  def provideTask: Task = {
    if (Random.nextInt(100) % 2 == 0) {
      Multiply(Random.nextInt(20), Random.nextInt(20))
    } else {
      Divide(Random.nextInt(10000), (Random.nextInt(99) + 1))
    }
  }

  def processResult(result: MathResult) {
    result match {
      case MultiplicationResult(n1, n2, r) =>
        println("Mul result: %d * %d = %d".format(n1, n2, r))
      case DivisionResult(n1, n2, r) =>
        println("Div result: %.0f / %d = %.2f".format(n1, n2, r))
    }
  }
  
}