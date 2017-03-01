package sandbox

import akka.stream.scaladsl.Flow
import akka.http.scaladsl.model.HttpRequest
import org.reactivestreams.Subscriber
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.apache.kafka.common.serialization.StringSerializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import akka.stream.scaladsl.Source
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.Http
import scala.io.StdIn
import org.apache.kafka.common.serialization.ByteArraySerializer
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.kafka.ProducerMessage
import org.apache.kafka.clients.producer.ProducerRecord

object AkkaHttpPlusReactiveKafka extends App {

  implicit val actorSystem = ActorSystem("ReactiveKafka")
  implicit val materializer = ActorMaterializer()
  implicit val ec = actorSystem.dispatcher

  val topic = "MessagesFromWebService"

  val producerSettings =
    ProducerSettings(actorSystem, new ByteArraySerializer, new StringSerializer)
      .withBootstrapServers("localhost:9092")

  val kafkaFlow =
    Flow[String]
      .map(s => ProducerMessage.Message(new ProducerRecord[Array[Byte], String](topic,s),s))
      .via(Producer.flow(producerSettings))
      .map(res => res.message.passThrough)

  val flow =
    Flow[HttpRequest]
      .flatMapConcat(req => req.entity.dataBytes.map(bs => bs.decodeString("UTF-8")))
      .via(kafkaFlow)
      .map(s => HttpResponse(StatusCodes.OK, scala.collection.immutable.Seq.empty, HttpEntity.Empty))

  val bindingFuture = Http().bindAndHandle(flow, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => actorSystem.terminate()) // and shutdown when done
}
