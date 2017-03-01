package sandbox

import akka.kafka.ConsumerSettings
import com.typesafe.config.ConfigFactory
import org.apache.kafka.common.serialization.StringDeserializer
import akka.stream.ActorMaterializer
import akka.actor.ActorSystem
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.clients.producer.ProducerConfig
import akka.kafka.ProducerSettings
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import sandbox.util.DataGen
import akka.stream.scaladsl.Source
import scala.concurrent.duration._
import org.apache.kafka.clients.producer.ProducerRecord
import scala.concurrent.Await
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.Sink
import scala.concurrent.Future
import akka.stream.scaladsl.Keep
import akka.kafka.Subscriptions
import akka.kafka.scaladsl.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig

object ProduceConsume extends App {
  val config = ConfigFactory.empty

  implicit val system = ActorSystem("NameAggregator",config)
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val topicName = "pandc"
  val consumerGroupId = "pandc-consumer"
  val broker = "localhost:9092"

  val consumerProps =
    ConsumerSettings(system,new ByteArrayDeserializer, new StringDeserializer)
      .withBootstrapServers(broker)
      .withGroupId(consumerGroupId)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  val producerProps =
    ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
      .withBootstrapServers(broker)
      .withProperty(ProducerConfig.LINGER_MS_CONFIG, "3000")

  val rng = new scala.util.Random(new java.util.Random())
  try {
    val nameSource = Source.unfold(DataGen.name("US")){ gen =>
      gen.sample.map(n => (gen,n))
    }

    val throttle = Source.tick(0.seconds, 1.second, ())

    val producer =
      (nameSource.take(5) zip throttle).map{ case(a,b) => a }
        .map(n => new ProducerRecord[Array[Byte],String](topicName, n))
        .alsoTo(Sink.foreach( n =>println("Publishing: "+n)))
        .toMat(Producer.plainSink(producerProps))(Keep.right)

    println("Producing Messages")
    val producerDoneF = producer.run()

    val consumer = Consumer.committableSource(consumerProps, Subscriptions.topics(topicName))

    println("Consuming Messages")
    val consumerDoneF = consumer
      .alsoTo(Sink.foreach(rec => println(s"Consuming: ${rec}")))
//      .alsoTo(Sink.foreach(rec => println(s"===> Committing: offset=${rec.committableOffset.partitionOffset.offset}")))
      .mapAsync(1){ _.committableOffset.commitScaladsl() }
      .runWith(Sink.ignore)

    waitFor(consumerDoneF)
  }
  finally {
    waitFor(system.terminate)
  }

  def waitFor[T](f: Future[T], d: Duration = 30.seconds): T = Await.result(f,d)
}
