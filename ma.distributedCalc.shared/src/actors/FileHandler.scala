package actors

import akka.actor.Actor
import java.net.URLClassLoader
import java.net.URI
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.net.URL
import java.io.FileInputStream
import java.io.ByteArrayOutputStream
import java.net.MalformedURLException
import java.net.URISyntaxException
import scala.collection.immutable.HashMap
import java.util.jar.JarFile
import akka.actor.ActorRef
import messages.fileHandlerMessages._

class FileHandler(projectsPath: String) extends Actor {

  val classLoader: URLClassLoader = ClassLoader.getSystemClassLoader().asInstanceOf[URLClassLoader]

  override def preStart {
    new File(projectsPath).mkdir()
  }

  def receive() = {

    case GetAvailableProjectFiles() => {
      sender ! getAvailableProjectFiles
    }

    case LoadFileAtPath(path: String) => {
      val file = getFileByPath(path)

      if (addFileToClassloader(file)) {
        sender ! ProjectFileLoaded(file.getName().split('.')(0), file.getPath())
      }
      /*
      val newFile = writeBytesToFile(file.getName(), readBytesFromFile(file))
    		  
      if (addFileToClassloader(newFile)) {
    	  sender ! ProjectFileLoaded(newFile.getName().split('.')(0), newFile.getPath())
      }
      */
    }

    case ProjectFile(name: String, bytes: Array[Byte]) => {
      val file = writeBytesToFile(name + ".jar", bytes)

      if (addFileToClassloader(file)) {
        sender ! ProjectFileLoaded(name, file.getPath())
      }
    }

    case GetPathByClass(clazz: Class[_]) => {
      sender ! getFilePathByClass(clazz)
    }

    case SendFileTo(pName: String, path: String, ref: ActorRef) => {

      val file = getFileByPath(path)
      val bytes = readBytesFromFile(file)
      ref forward ProjectFile(pName, bytes)
    }
  }

  def getFilePathByClass(clazz: Class[_]): String = {

    //println(self.path.name + ": clazz.getname => " + clazz.getName())

    val resourceName = clazz.getName().replace(""".""", "/") + ".class"
    //println(self.path.name + ": resourceName => " + resourceName)
    val fr = classLoader.findResource(resourceName)
    //println("fr: " + fr)
    val frp = classLoader.findResource(resourceName).getPath()
    //println("fr.p: " + frp)
    val frps = classLoader.findResource(resourceName).getPath().split('!')(0)
    //println("fr.p.s: " + frps)
    val resourcePath = classLoader.getResource(resourceName).getPath().split("!")(0)
    //println(self.path.name + ": resourcePath => " + resourcePath)

    val path = (new URI(resourcePath)).getPath() //resource file path with decoded whitespaces " " instead of "%20"
    //println(self.path.name + ": path => " + path)

    path
  }

  def getFileByPath(path: String): File = {
    new File(path)
  }

  def addFileToClassloader(file: File): Boolean = {
    try {
      //add the whole File to the ClassLoader
      val method = classOf[URLClassLoader].getDeclaredMethod("addURL", classOf[URL])
      method.setAccessible(true)
      method.invoke(classLoader, file.toURI().toURL())

      true
    } catch {
      case e: Exception => {
        false
      }
    }
  }

  def writeBytesToFile(name: String, bytes: Array[Byte]): File = {
    val file = new File(projectsPath + File.separator + name) //create the file

    var fos = new FileOutputStream(file)

    (new PrintStream(fos)).write(bytes) //write the bytes to the file

    fos.close() //close the outputsteam

    file
  }

  def readBytesFromFile(file: File): Array[Byte] = {
    val fis = new FileInputStream(file)
    val availableBytes = fis.available()

    val bos: ByteArrayOutputStream = new ByteArrayOutputStream()
    var next: Int = fis.read()
    while (next > -1) {
      bos.write(next)
      next = fis.read()
    }
    bos.flush()
    bos.toByteArray()
  }

  def getAvailableProjectFiles: HashMap[String, String] = {

    var projectFiles: HashMap[String, String] = HashMap[String, String]()

    val fileList = new File(projectsPath).list()

    if (fileList.length > 0) {
      for (file <- fileList) {

        val f = new File(projectsPath + File.separator + file)

        if (f.isFile()) {
          projectFiles += ((f.getName().split('.')(0) -> f.getPath()))
        }
      }
    }

    projectFiles
  }
}