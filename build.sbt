name := "akka-stream-sandbox"

val akkaV = "2.4.9"
val reactiveKafkaV = "0.10.1"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka"    %% "akka-stream"    % akkaV,
  "com.softwaremill.reactivekafka" %% "reactive-kafka-core" % reactiveKafkaV,
  "com.typesafe.akka" %% "akka-http-experimental" % akkaV,
  "org.scalacheck"       %% "scalacheck"               % "1.13.2",
  "ch.qos.logback"       % "logback-classic"           % "1.1.3"
)
