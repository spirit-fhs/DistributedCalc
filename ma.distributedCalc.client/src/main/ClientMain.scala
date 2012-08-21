package main

import com.typesafe.config.{ Config, ConfigFactory }
import akka.actor.{ ActorSystem, Actor, ActorRef, Props, PoisonPill }
import akka.dispatch.Await
import akka.event.Logging
import akka.util.Timeout
import java.io.File
import java.net.InetAddress
import actors.ClientManager
import actors.FileHandler
import messages.fileHandlerMessages.GetPathByClass
import api.events._

object ClientMain {

  val hostName = InetAddress.getLocalHost().getHostName()
  var clientRef: ActorRef = _
  val t = Timeout(10000)

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("ClientSystem_" + hostName, getConfig)
    println(system.name + " running ...")
    val serverRef = system.actorFor(serverPathString)

    if (chkConnection(system, serverPathString)) {
      val path = workingDirectory + "ProjectsFiles/"
      clientRef = system.actorOf(Props(new ClientManager(serverPathString, path)), "ClientManager")
      CLI_main
    } else {
      println("Can't connect to Server")
      system.shutdown()
      System.exit(0)
    }
  }

  def chkConnection(system: ActorSystem, serverPath: String): Boolean = {
    for (i <- 0 to 3) {
      try {
        val serverRef = system.actorFor(serverPath)
        val ping = Await.result(serverRef ? ("ping", t), t.duration).asInstanceOf[String]
        ping match {
          case "pong" => {
            return true
          }
        }
      } catch {
        case e: Exception => {
        }
      }
    }
    false
  }

  def getConfig: Config = {
    //config = ConfigFactory.load().getConfig("ClientConfig")
    //config = ConfigFactory.parseFile(new File("ClientConfig.conf")).getConfig("ClientConfig")
    val confString = """
          akka {
            stdout-loglevel = INFO
		    actor {
			  timeout = 60
			  provider="akka.remote.RemoteActorRefProvider"
		    }
		    remote {
			  server {
		        port=2565
			  }
		    }
          }"""
    ConfigFactory.parseString(confString).withFallback(ConfigFactory.load())
  }

  def CLI_main {
    println("CLI - Main")
    println("commands: \n(1) ServerProjects\n(2) LoacalProjects\n(0) Exit")
    val cmd = readLine().toLowerCase()
    cmd match {
      case "0" | "exit" => {

        clientRef ! "signOff"
      }
      case "1" | "serverprojects" => {
        CLI_serverProjects
      }
      case "2" | "localprojects" => {
        CLI_localProjects
      }
    }
  }
  def CLI_serverProjects {
    println("ServerProjects")
    println("(0) Back to Main Menu")
    val projectsList = getServerProjectList
    for (p <- projectsList) {
      val index = projectsList.indexOf(p) + 1
      println("(" + index + ") " + p)
    }

    println("choose a project by its index: ")
    val index = readInt()
    if (index == 0) {
      CLI_main
    } else {
      val pName: String = projectsList(index - 1)
      clientRef ! ("joinProject", pName)

      println("\nhow many tasks do you want to compute paralel ?")
      val count: Int = readInt()
      clientRef ! ("requestNodes", pName, count)
      CLI_serverProjects
    }

  }

  def CLI_localProjects {
    println("LocalProjects")
    println("(0) back to main menu")
    val projectsList = getLocalProjectList
    if (!projectsList.isEmpty) {

      for (p <- projectsList) {
        val index = projectsList.indexOf(p) + 1
        println("(" + index + ") " + p)
      }

      println("choose a project by its index: ")
      var index = readInt()
      index match {
        case 0 => CLI_main
        case _ => {
          val pName: String = projectsList(index - 1)
          CLI_manageProject(pName)
        }
      }
    } else {
      println("No local projects available")
    }

  }

  def CLI_manageProject(pName: String) {
    println("Project: " + pName)
    var localNodes = getLocalNodesByProject(pName)
    println("(0) Back to Main Menu")
    println("(1) start calculating")
    println("(2) stop calculating")
    println("(3) list active nodes")
    println("(4) stop active nodes")
    
    

    readInt() match {
      case 1 => {
        print("\nhow many tasks do you want to compute paralel ?")
        val count: Int = readInt()
        clientRef ! ("requestNodes", pName, count)
        CLI_manageProject(pName)
      }
      case 2 => {
        for(node <- localNodes){
          clientRef ! ("stopNode", node)
        }
        CLI_manageProject(pName)
      }
      case 3 => {
        printActiveNodes(localNodes, pName)
        CLI_manageProject(pName)
      }
      case 4 => {
        val localNodes = getLocalNodesByProject(pName)
        println("(0) Back")
        printActiveNodes(localNodes, pName)
        val nodeIndex = readInt()

        val node = localNodes(nodeIndex - 1)
        clientRef ! ("stopNode", node)
        CLI_manageProject(pName)
      }
      case 0 => {
        CLI_main
      }
    }
  }
  def printActiveNodes(localNodes: List[ActorRef], pName: String) {
    if (!localNodes.isEmpty) {
      for (nodeRef <- localNodes) {
        println("(" + (localNodes.indexOf(nodeRef) + 1) + ") " + nodeRef.path.name)
      }
    } else {
      println("No local Nodes available")
    }
  }

  def getServerProjectList: List[String] = {
    try {
      Await.result(clientRef ? ("getServerProjectList", t), t.duration).asInstanceOf[List[String]]
    } catch {
      case e: Exception => {
        List[String]()
      }
    }
  }

  def getLocalProjectList: List[String] = {
    try {
      Await.result(clientRef ? ("getLocalProjectList", t), t.duration).asInstanceOf[List[String]]
    } catch {
      case e: Exception => {
        println("timeout")
        List[String]()
      }
    }
  }

  def getLocalNodesByProject(pName: String) = {
    try {
      Await.result(clientRef ? (("getNodesByProject", pName), t), t.duration).asInstanceOf[List[ActorRef]]
    } catch {
      case e: Exception => {
        println("timeout")
        List[ActorRef]()
      }
    }

  }

  def workingDirectory: String = {
    val clazz: Class[_] = this.getClass()
    val pd = clazz.getProtectionDomain()
    val cs = pd.getCodeSource()
    val loc = cs.getLocation()
    var path = loc.getPath()
    if (path.endsWith(".jar")) {
      path = path.substring(0, path.lastIndexOf("/") + 1)
    }
    path
  }

  def serverPathString: String = {
    val protocol: String = "akka"
    val serverSystemName: String = "ServerSystem"
    val serverAdress: String = "192.168.0.3"
    val serverPort: String = "2555"
    val serverActorName: String = "ServerManager"

    protocol + "://" + serverSystemName + "@" + serverAdress + ":" + serverPort + "/user/" + serverActorName
  }
}