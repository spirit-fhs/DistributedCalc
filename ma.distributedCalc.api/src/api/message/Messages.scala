package api.message

/**
 * basic Message
 */
trait Message extends Serializable

/**
 * Sent by a Node on a ClientSystem as reply to a Task message
 * ServerSide Actor have to handle this Message
 */
trait Result extends Message

trait Incomplete extends Message

/**
 * Sent by a ServerSide Actor as answer to a TaskRequest message
 * Nodes on CLientSystems must reply with a Result to this
 */
trait Task extends Message

/**
 * Sent by the Client to Server to request Tasks for a Node
 * Server have to answer with a Task message
 */
case class TaskRequest() extends Message