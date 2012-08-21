package actors
import akka.actor.Actor
import akka.actor.ActorRef
import api.message._
import messages.systemMessages.RequestTaskFor

class SimpleRouter extends Actor {
  
  var taskReceiver: ActorRef = null

  def receive = {
    case (RequestTaskFor(nodeRef: ActorRef), managerRef: ActorRef) => {
      taskReceiver = nodeRef

      managerRef ! TaskRequest
    }
    case response: Task => {
      context.parent ! ((response, taskReceiver))
      context.stop(self)
    }
  }

}