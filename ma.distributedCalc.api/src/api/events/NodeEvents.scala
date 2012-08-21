package api.events
import akka.actor.ActorRef

trait NodeEvent

case class NodeStarted(nodeRef:ActorRef) extends NodeEvent
case class NodeStopped(nodeRef:ActorRef) extends NodeEvent

case class TaskStarted(nodeRef:ActorRef, taskMsg:Any) extends NodeEvent
case class TaskFinished(nodeRef:ActorRef, taskMsg:Any) extends NodeEvent