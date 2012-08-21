package classes

import akka.actor.Props
import akka.actor.ActorRef

class Project(val name:String,val projectManagerRef:ActorRef, val projectFilePath:String, val remoteActorProps:Props){
  var forceDistibute:Boolean = false
  var nodeCount:Int = 2 //default number of nodes per client
}