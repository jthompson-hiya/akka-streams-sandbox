package sandbox

import akka.actor.ActorSystem
import cakesolutions.kafka.akka.KafkaConsumerActor.Confirm
import cakesolutions.kafka.akka.ConsumerRecords
import scala.concurrent.duration._
import cakesolutions.kafka.KafkaConsumer
import cakesolutions.kafka.akka.KafkaConsumerActor
import org.apache.kafka.common.serialization.StringDeserializer
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import cakesolutions.kafka.akka.KafkaConsumerActor.Subscribe

class ReceiverActor extends Actor with ActorLogging {

  // Extractor for ensuring type safe cast of records
  val recordsExt = ConsumerRecords.extractor[String, String]

  // Akka will dispatch messages here sequentially for processing.  The next batch is prepared in parallel and can be dispatched immediately
  // after the Confirm.  Performance is only limited by processing time and network bandwidth.
  override def receive: Receive = {
    // Type safe cast of records to correct serialisation type
    case recordsExt(records) =>

      // Or provide them using the raw Java type of ConsumerRecords[Key,Value]
      records.pairs.foreach{ rec => log.info(rec.toString) }

      // Confirm and commit back to Kafka
      sender() ! Confirm(records.offsets, commit = true)
  }
}

object CakeSolutionsConsumerActorTest extends App {

//  val system = ActorSystem()
//
//  val receiverActor = system.actorOf(Props[ReceiverActor])
//
//  val consumerConf = KafkaConsumer.Conf(
//    keyDeserializer = new StringDeserializer,
//    valueDeserializer = new StringDeserializer,
//    bootstrapServers = "localhost:9092",
//    groupId = "group",
//    enableAutoCommit = false
//  )
//
//  // Configuration specific to the Async Consumer Actor
//  val actorConf = KafkaConsumerActor.Conf(
//    scheduleInterval = 1.seconds,   // scheduling interval for Kafka polling when consumer is inactive
//    unconfirmedTimeout = 3.seconds, // duration for how long to wait for a confirmation before redelivery
//    maxRedeliveries = 3             // maximum number of times a unconfirmed message will be redelivered
//  )
//
//  // Create the Actor
//  val consumer = system.actorOf(
//    KafkaConsumerActor.props(consumerConf, actorConf, receiverActor)
//  )
//
//  consumer ! Subscribe.AutoPartition(Seq("topic1"))

}
