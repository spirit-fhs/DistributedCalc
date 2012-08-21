package piCalc.actors
import akka.actor.Actor
import piCalc.messages.Work
import piCalc.messages.PiResult

class PiWorker extends Actor with Serializable {
  var startTime: Long = _

  override def preStart() {
    startTime = System.currentTimeMillis()
  }

  def receive = {
    case Work(start, nrOfElements) => {
      sender ! PiResult(calculatePi(start, nrOfElements), uptime) //perform the work
    }
  }

  def calculatePi(start: Int, nrOfElements: Int): Double = {
    var acc = 0.0
    for (i <- start until (start + nrOfElements)) {
      acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1)
    }
    acc
  }
  
  def uptime:Long = {
    System.currentTimeMillis() - startTime
  }
}