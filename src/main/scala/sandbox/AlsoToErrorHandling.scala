package sandbox

import com.typesafe.config.ConfigFactory
import akka.stream.ActorMaterializer
import akka.actor.ActorSystem
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.Await
import akka.stream.scaladsl._
import akka.stream.Graph
import akka.stream.SinkShape
import akka.stream.FlowShape
import scala.annotation.unchecked.uncheckedVariance

object AlsoToErrorHandling extends App {
  val config = ConfigFactory.empty

  implicit val system = ActorSystem("AlsoToErrorHandling",config)
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  try {
    val source =
      Source(0 to 15).via(Flow[Int].alsoTo(Sink.foreach(n => println(s"HttpRequest: $n"))))

    val kafkaSink =
      Flow[Int]
        .map(n => if(n == 10) throw new Exception() else n)
        .to(Sink.foreach(n => println(s"Published to Kafka: $n")))

//    val flow = Flow[Int].via(safeAlsoToGraph(kafkaSink))

    val flow = Flow[Int].alsoTo(kafkaSink)

    val httpSink = Sink.foreach((n:Int) => println(s"HttpResponse: $n"))

    val res = waitFor(source.via(flow).toMat(httpSink)(Keep.right).run)

    println(res)

  }
  finally {
    waitFor(system.terminate)
  }

  def safeAlsoToGraph[Out,M](that: Graph[SinkShape[Out], M]): Graph[FlowShape[Out @uncheckedVariance, Out], M] =
    GraphDSL.create(that) { implicit b ⇒ r ⇒
      import GraphDSL.Implicits._
      val bcast = b.add(Broadcast[Out](2,true))
      bcast.out(1) ~> r
      FlowShape(bcast.in, bcast.out(0))
    }

  def waitFor[T](f: Future[T], d: Duration = 10.seconds): T = Await.result(f,d)

}
