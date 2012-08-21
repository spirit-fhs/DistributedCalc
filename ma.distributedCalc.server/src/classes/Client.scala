package classes

import akka.actor.ActorRef
import scala.collection.immutable.HashMap

class Client(val clientRef:ActorRef) {
  
  var projects:List[String] = List[String]()
  var nodes:HashMap[ActorRef,Node] = HashMap[ActorRef,Node]()
}