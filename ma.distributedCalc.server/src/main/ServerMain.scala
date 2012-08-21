package main

import com.typesafe.config.ConfigFactory
import java.io.File
import akka.actor.{ Actor, ActorRef, ActorSystem, Props }
import java.net.{ InetAddress, URL, URLClassLoader }
import java.util.Scanner
import actors.ServerManager
import actors.ClusterManager

object ServerMain {
  
  def run() {
    val config = ConfigFactory.parseFile(new File("ServerConfig.conf")).getConfig("ServerConfig")
    
    val system = ActorSystem("ServerSystem", config)
    println(system.name + " running ...")
    system.actorOf(Props(new ServerManager(defaultPath)),"ServerManager")
    
    system.actorOf(Props[ClusterManager],"ClusterManager")
  }

  def defaultPath:String = {
    val clazz:Class[_] = this.getClass()
    val pd = clazz.getProtectionDomain()
    val cs = pd.getCodeSource()
    val loc = cs.getLocation()
    var path = loc.getPath()
    if (path.endsWith(".jar")) {
      path = path.substring(0, path.lastIndexOf("/") + 1)
    }
    
    path + "ProjectsFiles/"
  }
  
  def main(args: Array[String]): Unit = {

    run()
  }
}