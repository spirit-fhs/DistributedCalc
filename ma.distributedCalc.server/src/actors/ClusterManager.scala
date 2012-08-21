package actors

import akka.actor.{ Actor, ActorRef, Props, ActorSystemImpl, Deploy, Terminated }
import akka.remote.{ RemoteScope, UnparsedSystemAddress, UnparsedTransportAddress }
import akka.dispatch.Await
import akka.routing.NoRouter
import scala.collection.immutable.HashMap
import java.util.UUID
import messages.server._
import messages.systemMessages._
import api.message._
import classes._
import api.message.Incomplete

class ClusterManager extends Actor with Serializable {
  val serverManagerRef = context.actorFor("../ServerManager")
  var nodes = HashMap[ActorRef, Node]()

  override def postStop() {
    for ((nodeRef, node) <- nodes) {
      context.stop(nodeRef)
    }
  }

  def receive() = {

    case RequestTaskFor(nodeRef: ActorRef) => {
      try {
        val managerRef = nodes(nodeRef).project.projectManagerRef
        val sr = context.actorOf(Props[SimpleRouter])
        sr ! ((RequestTaskFor(nodeRef), managerRef))
      } catch {
        case e: Exception => {
          e.printStackTrace()
        }
      }
    }

    case (task: Task, taskReceiver: ActorRef) => {
      if (nodes contains taskReceiver) {
        taskReceiver ! task
      }
    }

    case result: Result => {
      try {
        val managerRef = nodes(sender).project.projectManagerRef
        managerRef tell (result, sender)

        result match {
          case i: Incomplete => {
          }
          case _ => {
            self ! RequestTaskFor(sender)
          }
        }
      } catch {
        case e: Exception => {
          e.printStackTrace()
        }
      }
    }

    case DeployNode(client: Client, project: Project) => {

      val remoteNodeRef = deployRemoteActor(client.clientRef, project)
      println(self.path.name + " deployed node " + remoteNodeRef.path.name + " on " + client.clientRef.path.address)
      //val node = new Node(remoteNodeRef, sender, project)
      val node = new Node(remoteNodeRef, client.clientRef, project)
      nodes += (remoteNodeRef -> node)
      serverManagerRef ! NodeDeployed(node)
    }

    case RemoveNode(node: Node) => {
      try {
        context.stop(node.nodeRef)
        println(self.path.name + " removed node " + node.nodeRef.path.name)
        //nodes -= node.nodeRef
      } catch {
        case e: Exception => {
          e.printStackTrace()
        }
      }
    }

    case (SignOff(), client: Client) => {
      for ((nodeRef, node) <- client.nodes) {
        self ! RemoveNode(node)
        client.nodes -= nodeRef
      }
      sender ! (SignedOff(), client)
    }

    case Terminated(nodeRef: ActorRef) => {
      println("Node " + nodeRef.path.name + " Terminated ")
      try {
        serverManagerRef ! NodeTerminated(nodes(nodeRef))
        nodes -= nodeRef
      } catch {
        case e: Exception => {
          e.printStackTrace()
        }
      }

    }

    case "ping" => {
      sender ! "pong"
    }

    case _ => {

    }
  }

  /**
   * Returns the ActorRef of the deployed Actor
   *
   * This method will deploy an Akka Actor from given Props on a remoteSystem
   * The remoteSystem is specified by an ActorRef
   *
   * @param remoteSystemRef the ActorRef to the RemoteSystem
   * @param props the Props to spawn the Actor from
   *
   * NOTE: 	The class of the Actor to be deployed has to be known by the Remote Hosts Classloader
   * 		otherwise the RemoteSystem will throw an ClassNotFoundException
   */
  def deployRemoteActor(clientRef: ActorRef, project: Project): ActorRef = {

    val selfPath = context.self.path.toString().split("/user/")(1)

    val remoteDeployedActorName = project.name + "-" + UUID.randomUUID().toString()
    val deploymentPath = "/" + selfPath + "/" + remoteDeployedActorName

    val deployer = context.system.asInstanceOf[ActorSystemImpl].provider.deployer

    val remoteHostPort = clientRef.path.address.hostPort
    val remoteSystemName = remoteHostPort.split("@")(0)
    val remoteAddress = remoteHostPort.split("@")(1).split(":")(0)
    val remotePort = remoteHostPort.split("@")(1).split(":")(1).toInt
    val remoteProtocol = "akka"

    /*
     * taken from the akka tests
     * https://github.com/jboner/akka/blob/master/akka-remote/src/test/scala/akka/remote/RemoteDeployerSpec.scala
     */
    val deployDef = Deploy(deploymentPath,
      null,
      None,
      NoRouter,
      RemoteScope(
        UnparsedSystemAddress(
          Some(remoteSystemName),
          UnparsedTransportAddress(
            remoteProtocol,
            remoteAddress,
            remotePort))))

    deployer.deploy(deployDef)

    val remoteDeployedActor = context.actorOf(project.remoteActorProps, remoteDeployedActorName)
    context.watch(remoteDeployedActor)
  }
}