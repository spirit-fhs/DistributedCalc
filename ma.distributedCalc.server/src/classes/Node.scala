package classes

import akka.actor.{Actor, ActorRef, Props}
import scala.collection.immutable.HashMap

case class Node(val nodeRef: ActorRef, val clientRef:ActorRef, val project:Project)