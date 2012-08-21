package actors

import scala.collection.immutable.HashMap
import scala.util.Random
import akka.actor.{ Actor, ActorRef, Props, PoisonPill }
import akka.util.Timeout
import akka.dispatch.Await
import actors.projectManager.PiManager
import actors.projectManager.MathOpManager
import piCalc.actors.PiMaster
import mathOp.AdvancedCalculatorActor
import messages.server._
import messages.systemMessages._
import messages.fileHandlerMessages._
import classes._

class ServerManager(projectsPath: String) extends Actor with Serializable {

  var serverProjects: HashMap[String, Project] = setupProjectsList
  var clients = HashMap[ActorRef, Client]()

  val clusterManager: ActorRef = context.actorFor("../ClusterManager")

  def receive() = {
    case msg: RequestTaskFor => {
      clusterManager ! msg
    }

    case RequestNodesFor(pName: String, count: Int) => {

      try {
        for (i <- 0 until count) {
          clusterManager ! DeployNode(clients(sender), serverProjects(pName))
        }
      } catch {
        case e: Exception => {
          e.printStackTrace()
        }
      }

    }

    case NodeDeployed(node) => {
      try {
        clients(node.clientRef).nodes += (node.nodeRef -> node)
      } catch {
        case e: Exception => {
          e.printStackTrace()
        }
      }
    }

    case StopNode(nodeRef: ActorRef) => {
      try {
        val node = clients(sender).nodes(nodeRef)
        clusterManager ! RemoveNode(node)
        clients(sender).nodes -= node.nodeRef
      } catch {
        case e: Exception => {
          e.printStackTrace()
        }
      }
    }

    case NodeTerminated(node: Node) => {
      try {
        clients(node.clientRef).nodes -= node.nodeRef
      } catch {
        case e: Exception => {
          e.printStackTrace()
        }
      }
      /*
      if(clients(node.clientRef).nodes contains node.nodeRef){
        clients(node.clientRef).nodes -= node.nodeRef
      }
      */
    }

    case JoinProject(pName: String) => {
      try {
        val project = serverProjects(pName)
        val fh = context.actorOf(Props(new FileHandler(projectsPath)))
        fh ! SendFileTo(project.name, project.projectFilePath, sender)
        fh ! PoisonPill
        //context.stop(fh)
      } catch {
        case e: Exception => {

        }
      }
    }

    case LeaveProject(pName: String) => {
      try {
        val client = clients(sender)

        for ((nodeRef, node) <- client.nodes) {

          if (node.project.name.equalsIgnoreCase(pName)) {
            clusterManager ! RemoveNode(node)
            client.nodes -= nodeRef
          }
        }
      } catch {
        case e: Exception => {
          e.printStackTrace()
        }
      }
    }

    case ProjectFileLoaded(project: String, _) => {
      try {
        clients(sender).projects ::= project
        for ((pName, project) <- serverProjects) {
          if (project.forceDistibute) {
            sender ! Force(RequestNodesFor(pName, project.nodeCount))
          }
        }
      } catch {
        case e: Exception => {
          e.printStackTrace()
        }
      }
    }

    case SignOn(availableProjects) => {

      val client = new Client(sender)

      for (p <- availableProjects) {
        client.projects ::= p
      }

      clients += (sender -> client)
      sender ! SignedOn()

      for ((pName, project) <- serverProjects) {
        if (project.forceDistibute) {
          sender ! Force(JoinProject(pName))
        }
      }
    }

    case SignOff() => {
      try {
        clusterManager ! (SignOff(), clients(sender))
      } catch {
        case e: Exception => {
          sender ! SignedOff()
          e.printStackTrace()
        }
      }
    }

    case (SignedOff(), client: Client) => {
      try {
        clients -= client.clientRef
        client.clientRef ! SignedOff()
      } catch {
        case e: Exception => {
          e.printStackTrace()
        }
      }
    }

    case RequestAvailableProjects() => {
      var list = List[String]()
      for ((name, project) <- serverProjects) {
        list ::= name
      }
      sender ! AvailableProjects(list)
    }

    case "ping" => {
      sender ! "pong"
    }

    case "pong" => {
      println(self.path.name + ": pong")
    }

    case ("invalid Message", msg) => {
      println(sender.path.name + " couldn't understand MSG(" + msg + ")")
    }

    case msg => {
      sender ! ("invalid Message", msg)
    }

  }

  def setupProjectsList: HashMap[String, Project] = {

    val piManager: ActorRef = context.actorOf(Props[PiManager], "PiManager")
    val mathOpManager: ActorRef = context.actorOf(Props[MathOpManager], "MathOpManager")

    val fh = context.actorOf(Props(new FileHandler(projectsPath)))
    implicit val t: Timeout = Timeout(5000)

    val piCalcFilePath: String = Await.result(fh ? GetPathByClass(classOf[PiMaster]), t.duration).asInstanceOf[String]
    val mathOpFilePath: String = Await.result(fh ? GetPathByClass(classOf[AdvancedCalculatorActor]), t.duration).asInstanceOf[String]

    context.stop(fh)

    val piCalcP = new Project("PiCalc", piManager, piCalcFilePath, Props[PiMaster])
    piCalcP.forceDistibute = true

    val mathOpP = new Project("MathOp", mathOpManager, mathOpFilePath, Props[AdvancedCalculatorActor])

    HashMap[String, Project](
      "PiCalc" -> piCalcP,
      "MathOp" -> mathOpP)
  }
}