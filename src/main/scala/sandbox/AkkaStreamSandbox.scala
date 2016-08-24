package sandbox

import akka.stream.scaladsl.Source
import scala.concurrent.duration._
import akka.stream.scaladsl.Sink
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Keep
import scala.concurrent.Await
import akka.Done

object AkkaStreamSandbox extends App {

  val config = ConfigFactory.empty

  implicit val system = ActorSystem("AkkaStreamSandbox",config)
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  try {
    val start = System.currentTimeMillis
    val source = Source.tick(1.second,1.second,()).map(_ => System.currentTimeMillis - start)

    val mainSink = Sink.foreach((x: String) => println(s"Sink Received: $x"))

    val (cancelable,done) =
      source
        .map(x => if(x > 17000L) throw new Exception("Yikes!") else x)
        .groupedWithin(2, 2500.milliseconds)
        .map(_.toString)
        .recover{ case ex => s"Exception in stream: $ex" }
        .toMat(mainSink)(Keep.both)
        .run

    done.onSuccess {
      case Done => println("Stream completed")
    }

    println("Waiting for some values to be produced")
    Thread.sleep(10000L)
    println("Stopping producer")

    cancelable.cancel()

    Await.result(done,10.seconds)

  }
  finally {
    system.terminate()
  }
}
