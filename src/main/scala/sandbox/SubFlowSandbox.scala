package sandbox

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.stream.scaladsl._

object SubFlowSandbox extends App {

  val config = ConfigFactory.empty

  implicit val system = ActorSystem("AkkaStreamSandbox",config)
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  try {
    def partitioner: Int => Int = _ % 3

    //val source =
    val source = Source(List(0,1,2)) ++Source.unfold(3)(cur => Some((cur+1,cur))).filterNot(_ % 3 == 0 )
    source
//      .via(Flow[Int].alsoTo(Sink.foreach(n => println(s"Source: $n"))))
      .groupBy(3, partitioner)
//        .limit(100)
        .idleTimeout(1.second)
        .via(Flow[Int].map(n => partitioner(n) -> n))
        .alsoTo(Sink.foreach{ case (partition,value) => println(s"Partition: $partition, Value: $value, Thread: ${Thread.currentThread.getName}")})
        .to(Sink.onComplete(_ => println("Substream completed")))
      .run
    Thread.sleep(1200L)

  }
  finally {
    system.terminate()
  }

  def waitFor[T](f: Future[T], d: Duration = 10.seconds): T = Await.result(f,d)

}
