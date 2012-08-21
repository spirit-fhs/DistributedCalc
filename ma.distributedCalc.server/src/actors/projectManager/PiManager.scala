package actors.projectManager

import piCalc.messages._
import api.message._
import akka.actor.Actor

class PiManager extends Actor {
  val nrOfWorkers: Int = 4
  val nrOfMessages: Int = 100000
  val nrOfElements = 10000

  var incompleteTasks = Set[IncompleteCalculate]()

  def receive = {
    case TaskRequest => {
      var task: Task = null
      try {
        task = incompleteTasks.head
        incompleteTasks = incompleteTasks.tail
      } catch {
        case e: Exception => {
          task = Calculate(nrOfWorkers, nrOfMessages, nrOfElements)
        }
      }

      println(self.path.name + " sending " + task + " to " + sender)
      sender ! task
    }
    case res: Result => {
      println(self.path.name + " received " + res + " from " + sender.path.name)

      res match {
        case inc: IncompletePiResult => {
          incompleteTasks += IncompleteCalculate(
            nrOfWorkers,
            inc.nrOfMessages,
            inc.nrOfElements,
            inc.partStart,
            inc.partPi,
            inc.partTime)
        }

        case pi: PiResult => {
          processPiResult(pi)
        }
      }
    }
  }

  def processPiResult(result: PiResult) {
    println("\n" + """
          +-------------------------------------------------------------------------+ 
          | """ + self.path.name + """ received Result from """ + sender.path.name + """
          | Pi estimate: """ + result.value + """
          | Calculation Time: """ + result.duration + """millis
          +-------------------------------------------------------------------------+ """ + "\n")
  }
}