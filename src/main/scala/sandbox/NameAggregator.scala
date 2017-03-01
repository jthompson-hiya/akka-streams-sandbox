package sandbox

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.scalacheck.Gen
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Keep
import scala.concurrent.duration._
import sandbox.util.DataGen
import akka.kafka.ConsumerSettings
import akka.kafka.ProducerSettings
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.clients.producer.ProducerRecord
import akka.kafka.scaladsl._
import akka.kafka.Subscriptions
import org.apache.kafka.clients.producer.ProducerConfig
import scala.concurrent.Future
import scala.concurrent.Await
import java.util.concurrent.TimeoutException

object NameAggregator extends App {

  val config = ConfigFactory.empty

  implicit val system = ActorSystem("NameAggregator",config)
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val topicName = "names"
  val consumerGroupId = "name-consumer"
  val broker = "localhost:9092"

  val consumerProps =
    ConsumerSettings(system,new ByteArrayDeserializer, new StringDeserializer)
      .withBootstrapServers(broker)

  val producerProps =
    ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
      .withBootstrapServers(broker)
      .withProperty(ProducerConfig.LINGER_MS_CONFIG, "3000")

  val rng = new scala.util.Random(new java.util.Random())
  try {
    val nameSource = Source.unfold(DataGen.name("US")){ gen =>
 //     val delay = Math.abs(rng.nextGaussian()*1000L).toLong
 //     Thread.sleep(delay)
      gen.sample.map(n => (gen,n))
    }

    val throttle = Source.tick(0.seconds, 1.second, ())

    val producer =
      (nameSource zip throttle).map{ case(a,b) => a }
        .map(n => new ProducerRecord[Array[Byte],String](topicName, n))
        .alsoTo(Sink.foreach( n =>println("Publishing: "+n)))
        .to(Producer.plainSink(producerProps))

    val start = System.currentTimeMillis
    val consumer = Consumer.committableSource(consumerProps.withGroupId(consumerGroupId), Subscriptions.topics(topicName))

    val done = consumer
      .alsoTo(Sink.foreach(rec => println(s"Consuming: ${rec}")))
      .groupedWithin(15,30.seconds)
      .alsoTo(Sink.foreach( recs =>println(s"==> Aggregated: ${recs.map(_.record.value)} - ${recs.size} - ${System.currentTimeMillis - start}")))
      .mapAsync(1)(recs => Future{ if(recs.find(_.record.value.contains("SMITHY")).isDefined) throw new Exception("SMITH!!") else recs})
      .map(_.last)
      .alsoTo(Sink.foreach(rec => println(s"===> Committing: offset=${rec.committableOffset.partitionOffset.offset}")))
      .mapAsync(1){ _.committableOffset.commitScaladsl() }
      .runWith(Sink.ignore)

    val bar = producer.run()

    try {
      val res = waitFor(done,60.seconds)
      println("Success: "+res)
    }
    catch {
      case to: TimeoutException => println("Success")
      case ex: Throwable => println("Failure: "+ex.getClass.getSimpleName+" - "+ex.getMessage)
    }
  }
  finally {
    system.terminate()
  }

  def waitFor[T](f: Future[T], d: Duration): T = Await.result(f,d)
}
