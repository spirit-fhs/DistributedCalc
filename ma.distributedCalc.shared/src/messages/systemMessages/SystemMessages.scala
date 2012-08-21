package messages.systemMessages
import akka.actor.ActorRef
import scala.collection.immutable.List

trait SystemMessage extends Serializable

case class RequestAvailableProjects() extends SystemMessage
case class AvailableProjects(projectNames:List[String]) extends SystemMessage
case class JoinProject(project:String) extends SystemMessage
case class LeaveProject(project:String) extends SystemMessage

case class RequestNodesFor(project:String,count:Int) extends SystemMessage
case class NodeFor(nodeRef:ActorRef, project:String) extends SystemMessage
case class StopNode(nodeRef:ActorRef) extends SystemMessage

case class SignOn(availableProjectFileNames: List[String]) extends SystemMessage
case class SignOff() extends SystemMessage
case class SignedOn() extends SystemMessage
case class SignedOff() extends SystemMessage

case class Force(message:Any) extends SystemMessage

case class RequestTaskFor(nodeRef:ActorRef) extends SystemMessage
