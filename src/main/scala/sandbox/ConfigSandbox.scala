package sandbox

import com.typesafe.config.ConfigFactory

object ConfigSandbox extends App {

  val config = ConfigFactory.load()

  println(config.getString("env"))
  println(config.getString("topic"))
}
