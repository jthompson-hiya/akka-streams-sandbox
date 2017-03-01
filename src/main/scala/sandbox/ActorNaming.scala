package sandbox

import akka.actor.ActorSystem
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.actor.Actor
import akka.actor.Props

object ActorNaming extends App {

  val system = ActorSystem("ActorNaming")

  try {
    val ref1 = system.actorOf(Props[FooActor],java.net.URLEncoder.encode("-[foo1:8080,bar:8080]","UTF-8"))
    val ref2 = system.actorOf(Props[FooActor],java.net.URLEncoder.encode("foo2:8080(baz)","UTF-8"))

    ref1 ! "Hello"
    ref2 ! "World"
  }
  finally {
    Await.ready(system.terminate(),Duration.Inf)
  }

}

class FooActor extends Actor {
  def receive: Actor.Receive = {
    case anything@_ => println(s"${self} - $anything")
  }
}
