package sandbox

import akka.actor.ActorSystem
import cakesolutions.kafka.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import scala.collection.JavaConverters._
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.{KafkaConsumer => JKafkaConsumer}
import java.util.function.Consumer
import org.apache.kafka.common.serialization.Deserializer
import scala.util.control.NonFatal
import org.json4s._
import org.json4s.jackson.JsonMethods._
import java.time.OffsetDateTime
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.Duration
import scala.collection.JavaConverters._
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition

object CakeSolutionKafkaClient extends App {

  val config = KafkaConsumer.Conf(
    new StringDeserializer(),
    new StringDeserializer(),
    bootstrapServers = "kafka1.prod.us-east-1.wtpgs.net:9092",
    groupId = java.util.UUID.randomUUID.toString,
    enableAutoCommit = false,
    sessionTimeoutMs = 30000,
    autoOffsetReset = OffsetResetStrategy.LATEST
  )

  val javaConsumer = KafkaConsumer(config)

  val userEventsTopicName = "nisp.m11.user-events"
  val topics = Seq(userEventsTopicName)

  javaConsumer.subscribe(topics.asJava)

  while(true) {
      doPoll(javaConsumer)
  }

  println(s"===> Starting to consume from ${topics.mkString("[","]",",")}:")
  def doPoll(consumer: JKafkaConsumer[String,String]) = {
//    println("Polling:")
    val records = consumer.poll(1000L)

    var newOffsets = Map.empty[Int,Long]

//    println("Records:")
    records.forEach(new Consumer[ConsumerRecord[String,String]] {
      def accept(rec: ConsumerRecord[String,String]) = {
        //println(s"Partition: ${rec.partition}, Offset: ${rec.offset} - ${rec.value}")
        val json = parse(rec.value)
        val eventTimes = json \\ "eventTime" match {
          case JString(ct) => List(OffsetDateTime.parse(ct))
          case JObject(cts) => cts.map(_._2).collect{ case JString(ct) => OffsetDateTime.parse(ct)}
        }

        val timestamp = json \ "metadata" \ "timestamp" match {
          case JLong(epochMilli) =>
            val ts = Instant.ofEpochMilli(epochMilli)
            OffsetDateTime.ofInstant(ts,ZoneId.of("GMT"))
          case JInt(epochMilli) =>
            val ts = Instant.ofEpochMilli(epochMilli.toLong)
            OffsetDateTime.ofInstant(ts,ZoneId.of("GMT"))
        }

        val trackingId = json \"metadata" \ "trackingId" match {
          case JString(s) => s
        }

        val now = OffsetDateTime.now.withOffsetSameInstant(ZoneOffset.UTC)

        println(rec.value)

        println(s"Events Created at: ")
        eventTimes.foreach(cd => println(s"                    ${cd}"))
        println(s"Events timestamp  : ${timestamp}, trackingId: $trackingId")
        println(s"Events received at: $now")
        println(s"Difference: ${Duration.between(timestamp,now)}")
        println("")

        newOffsets = newOffsets + (rec.partition -> rec.offset)
      }
    })

    val offsetsToCommit = newOffsets.map{ case(part,off) => new TopicPartition(userEventsTopicName,part) -> new OffsetAndMetadata(off) }

    if(! newOffsets.isEmpty) {
      println(s"Committing offsets: $newOffsets")
      consumer.commitSync(offsetsToCommit.asJava)
    }
  }
}
