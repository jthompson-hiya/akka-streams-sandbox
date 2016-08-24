package sandbox

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.softwaremill.react.kafka.ConsumerProperties
import org.apache.kafka.common.serialization.StringDeserializer
import com.softwaremill.react.kafka.ProducerProperties
import org.apache.kafka.common.serialization.StringSerializer
import org.scalacheck.Gen
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Sink
import com.softwaremill.react.kafka.ProducerMessage
import com.softwaremill.react.kafka.ReactiveKafka
import akka.stream.scaladsl.Keep
import scala.concurrent.duration._

object NameAggregator extends App {

  val config = ConfigFactory.empty

  implicit val system = ActorSystem("NameAggregator",config)
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val topicName = "names"
  val consumerGroupId = "name-consumer"
  val broker = "192.168.99.100:9092"

  val consumerProps = ConsumerProperties(
   bootstrapServers = broker,
   topic = topicName,
   groupId = consumerGroupId,
   valueDeserializer = new StringDeserializer()
  )

  val producerProps = ProducerProperties(
    bootstrapServers = broker,
    topic = topicName,
    valueSerializer = new StringSerializer()
  )

  val rng = new scala.util.Random(new java.util.Random())
  try {
    val nameSource = Source.unfold(DataGen.name("US")){ gen =>
      val delay = Math.abs(rng.nextGaussian()*3000L).toLong
      Thread.sleep(delay)
      gen.sample.map(n => (gen,n))
    }

    val kafka = new ReactiveKafka()

    val producer =
      nameSource
        .map(n => ProducerMessage(n))
        .alsoTo(Sink.foreach( n =>println("Publishing: "+n)))
        .to(Sink.fromSubscriber(kafka.publish(producerProps)))

    val consumerWithOffsetSink =
      kafka.consumeWithOffsetSink(consumerProps)

    val start = System.currentTimeMillis
    val consumer =
      Source.fromPublisher(consumerWithOffsetSink.publisher)
        .alsoTo(Sink.foreach( n =>println("Consuming: "+n)))
        .groupedWithin(15,30.seconds)
        .alsoTo(Sink.foreach( ns =>println(s"==> Aggregated: ${ns.map(_.value)} - ${ns.size} - ${System.currentTimeMillis - start}")))
        .map(_.last)
        .alsoTo(Sink.foreach(rec => println(s"===> Committing offset=${rec.offset}")))
        .to(consumerWithOffsetSink.offsetCommitSink)

    consumer.run()

    producer.run()

    Thread.sleep(600000L)
  }
  finally {
    system.terminate()
  }

}

object DataGen {
  val strings = collection.mutable.HashMap[String, Seq[String]]()

  def stringsFor(resource: String, countryCode: String): Seq[String] = {
    val key = s"/${resource}_${countryCode}.txt"
    val fallbackKey = s"/${resource}_US.txt"
    val is = strings.get(key).orElse(strings.get(fallbackKey))

    val res = is.getOrElse(
      scala.io.Source.fromInputStream(
        Option(getClass.getResourceAsStream(key))
          .getOrElse(getClass.getResourceAsStream(fallbackKey))
      ).getLines().toSeq
    )

    if(is.isEmpty) {
      strings.put(key, res)
    }

    res
  }

  def givenName(cc: String):Gen[String] = Gen.oneOf(stringsFor("givenNames", cc))

  def familyName(cc: String): Gen[String] = Gen.oneOf(stringsFor("familyNames", cc))

  def name(cc: String): Gen[String] = for {
    first <- givenName(cc)
    last <- familyName(cc)
    middle <- Gen.alphaUpperChar
  } yield s"$first $middle. $last"

}