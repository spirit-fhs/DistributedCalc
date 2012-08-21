package actors

import akka.actor.{ Actor, ActorRef, Props, PoisonPill, AskTimeoutException }
import akka.util.Timeout
import akka.dispatch.Await
import scala.collection.immutable.HashMap
import messages.systemMessages._
import messages.fileHandlerMessages._
import api.events._
import api.message.Task

class ClientManager(serverPath: String, projectsPath: String) extends Actor with Serializable {

  context.system.eventStream.subscribe(self, classOf[NodeEvent])

  implicit val timeout = Timeout(10000)

  val serverRef: ActorRef = context.actorFor(serverPath)

  var localProjectFiles: HashMap[String, String] = loadLocalProjectFiles

  var localNodes: HashMap[ActorRef, String] = HashMap[ActorRef, String]()

  def projectNamesList: List[String] = {
    var list: List[String] = List[String]()
    for (p <- localProjectFiles) {
      list ::= p._1
    }
    list
  }

  override def preStart() {
    println("\n" + self.path.name + " running ...")
    serverRef ! SignOn(projectNamesList)

  }

  override def postStop() {
    println("\n" + self.path.name + " ... stopped")
    serverRef ! SignOff()
  }

  def receive() = {
    case TaskFinished(nodeRef: ActorRef, t: Task) => {
      println("\n" + "task " + t + " finished at " + nodeRef.path.name)
    }
    case TaskStarted(nodeRef: ActorRef, t: Task) => {
      println("\n" + "task " + t + " started at " + nodeRef.path.name)
    }

    case NodeStarted(nodeRef: ActorRef) => {
      val pName = nodeRef.path.name.split('-')(0)
      println("\n" + "local node for Project " + pName + " started: " + nodeRef.path.name)
      localNodes += (nodeRef -> pName)
      serverRef ! RequestTaskFor(nodeRef)
    }
    case NodeStopped(nodeRef: ActorRef) => {
      println("\n" + "local node stopped: " + nodeRef.path.name)
      try {
        localNodes -= nodeRef
      } catch {
        case e: Exception => {
          e.printStackTrace()
        }
      }
    }

    case ProjectFileLoaded(name: String, path: String) => {
      context.stop(sender)
      println("ClientManager: ProjectFileLoaded: " + name)

      localProjectFiles += (name -> path)
      serverRef ! ProjectFileLoaded(name, "")
    }

    case ProjectFile(pName: String, bytes: Array[Byte]) => {

      val fh: ActorRef = context.actorOf(Props(new FileHandler(projectsPath)))
      fh ! ProjectFile(pName: String, bytes: Array[Byte])
    }

    case SignedOn() => {
      println(self.path.name + ": Signed on @ " + serverRef.path.name)
    }
    case SignedOff() => {
      println(self.path.name + ": Signed off @ " + serverRef.path.name)
      //context.stop(self)
      context.system.shutdown()
      System.exit(0)
    }

    case Force(msg: Any) => {
      sender ! msg
    }
    case ("getNodesByProject", pName: String) => {
      var nodes = List[ActorRef]()
      for ((nodeRef, project) <- localNodes) {
        if (project.equalsIgnoreCase(pName)) {
          nodes ::= nodeRef
        }
      }
      sender ! nodes
    }
    case ("stopNode", nodeRef: ActorRef) => {
      //serverRef ! StopNode(nodeRef)
      context.system.stop(nodeRef)
    }
    case ("joinProject", pName: String) => {
      serverRef ! JoinProject(pName)
    }
    case ("leaveProject", pName: String) => {
      serverRef ! LeaveProject(pName)
    }
    case ("requestNodes", pName: String, count: Integer) => {
      serverRef ! RequestNodesFor(pName, count)
    }
    case "getServerProjectList" => {
      try {
        val serverProjects: List[String] = Await.result(serverRef ? (RequestAvailableProjects()), timeout.duration).asInstanceOf[AvailableProjects].projectNames
        sender ! serverProjects
      } catch {
        case e:AskTimeoutException => {
          e.printStackTrace()
          sender ! List[String]()
        }
      }

    }
    case "getLocalProjectList" => {
      var list = List[String]()
      for ((pName, _) <- localProjectFiles) {
        list ::= pName
      }
      sender ! list
    }
    case "signOff" => {
      serverRef ! SignOff()
    }
    case ("invalid Message", msg) => {
      println(sender.path.name + " couldn't understand MSG(" + msg + ")")
    }

    case msg => {
      sender ! ("invalid Message", msg)
    }
  }

  def loadLocalProjectFiles: HashMap[String, String] = {
    var map: HashMap[String, String] = HashMap[String, String]()

    val fh: ActorRef = context.actorOf(Props(new FileHandler(projectsPath)))
    try {
      val projectFiles: HashMap[String, String] = Await.result(fh ? GetAvailableProjectFiles(), timeout.duration).asInstanceOf[HashMap[String, String]]

      for ((pName, pPath) <- projectFiles) {
        val pFile: ProjectFileLoaded = Await.result(fh ? LoadFileAtPath(pPath), timeout.duration).asInstanceOf[ProjectFileLoaded]
        map += (pFile.projectName -> pFile.filePath)
      }
      map
    } catch {
      case e: AskTimeoutException => {
        map
      }
    } finally {
      context.stop(fh)
    }
  }

}