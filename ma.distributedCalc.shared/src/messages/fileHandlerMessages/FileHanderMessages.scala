package messages.fileHandlerMessages
import java.io.File
import scala.collection.immutable.HashMap
import akka.actor.ActorRef

trait FileHandlerMessage extends Serializable

case class GetAvailableProjectFiles() extends FileHandlerMessage

case class ProjectFile(name:String,bytes:Array[Byte]) extends FileHandlerMessage
case class ProjectFileLoaded(projectName:String, filePath:String) extends FileHandlerMessage

case class LoadFileAtPath(path:String) extends FileHandlerMessage

case class GetPathByClass(clazz:Class[_]) extends FileHandlerMessage
case class SendFileTo(pName:String, path:String,ref:ActorRef) extends FileHandlerMessage
