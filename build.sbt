name := "akka-stream-sandbox"

val akkaV = "2.4.9"
val reactiveKafkaV = "0.11"

scalaVersion := "2.11.8"

resolvers += Resolver.bintrayRepo("cakesolutions", "maven")

libraryDependencies ++= Seq(
  "com.typesafe.akka"    %% "akka-actor"               % akkaV,
  "com.typesafe.akka"    %% "akka-slf4j"               % akkaV,
  "com.typesafe.akka"    %% "akka-stream"              % akkaV,
  "com.typesafe.akka"    %% "akka-stream-kafka"        % reactiveKafkaV,
  "com.typesafe.akka"    %% "akka-http-experimental"   % akkaV,
  "net.cakesolutions"    %% "scala-kafka-client-akka"  % "0.10.0.0",
  "com.chuusai"          %% "shapeless"                % "2.0.0",
  "org.scalacheck"       %% "scalacheck"               % "1.13.2",
  "ch.qos.logback"       % "logback-classic"           % "1.1.3",
  "org.json4s"           %% "json4s-jackson"           % "3.5.0"
)
