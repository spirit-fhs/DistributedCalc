package messages.server

import classes._

case class DeployNode(client:Client,project:Project)

case class NodeDeployed(node:Node)

case class NodeTerminated(node:Node)

case class RemoveNode(node:Node)
