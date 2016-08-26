package sandbox

import org.scalacheck.Gen
import sandbox.util.DataGen
import java.nio.file.Files
import java.time.LocalDateTime
import java.io.File
import java.io.PrintWriter
import akka.stream.scaladsl.Source
import akka.actor.ActorSystem
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Keep
import scala.concurrent.Future
import akka.stream.scaladsl.FileIO
import java.nio.file.Path

object StreamFromFilesInDirectory extends App {

  implicit val system = ActorSystem("StreamFromFilesInDirectory")
  implicit val ec = system.dispatcher
  implicit val mat = ActorMaterializer()

  val dataDir = Files.createTempDirectory(s"").toFile()
  dataDir.deleteOnExit()

  println(dataDir.getAbsolutePath)

  try {
    val names = Gen.listOfN(100, DataGen.name("US")).sample.get

    names.zipWithIndex.foreach{ case (contents,idx) =>
      val file = new File(dataDir,s"File$idx.txt")
      file.deleteOnExit()

      val pw = new PrintWriter(file)
      pw.write(contents)
      pw.close
    }

    val source = Source(dataDir.list().toList)

    val done =
      source
        .map(fn => new File(dataDir,fn))
        .flatMapConcat(fileSource)
        .toMat(Sink.seq)(Keep.right).run

    waitFor(done).foreach(println)
  }
  finally {
    waitFor(system.terminate())
  }

  def fileSource(file: File) = FileIO.fromPath(file.toPath).map(_.decodeString("UTF-8"))

  def waitFor[T](f: Future[T],d: Duration= 10.seconds): T = Await.result(f,d)
}
