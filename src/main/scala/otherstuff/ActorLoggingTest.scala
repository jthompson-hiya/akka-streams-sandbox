package otherstuff

import akka.actor.ActorSystem
import akka.actor.Actor
import akka.actor.ActorLogging
import java.time.LocalDateTime
import akka.actor.Props
import scala.concurrent.duration._

class TickActor extends Actor with ActorLogging {
  def receive: Actor.Receive = {
    case "Tick" =>
      log.debug(s"Received a tick at ${LocalDateTime.now}")
//      log.info(s"Received a tick at ${LocalDateTime.now}")
//      log.warning(s"Received a tick at ${LocalDateTime.now}")
//      log.error(s"Received a tick at ${LocalDateTime.now}")
  }
}

object ActorLoggingTest extends App {
  val system = ActorSystem("ActorLoggingTest")
  import system.dispatcher

  val tickRef = system.actorOf(Props[TickActor])

  system.scheduler.schedule(0.seconds, 1.second)(tickRef ! "Tick")

  Thread.sleep(30000)

  system.terminate

}
