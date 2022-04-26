import sbt._

object Dependencies
{
  val ScalaVersion = "2.13.8"
  val Http4sVersion = "0.23.11"
  val CirceVersion = "0.14.1"

  lazy val catsEffect = "org.typelevel" %% "cats-effect" % "3.3.11"

  lazy val http4sDsl = "org.http4s" %% "http4s-dsl" % Http4sVersion

  lazy val http4sBlazeServer = "org.http4s" %% "http4s-blaze-server" % Http4sVersion

  lazy val http4sCirce = "org.http4s" %% "http4s-circe" % Http4sVersion

  lazy val circeGeneric = "io.circe" %% "circe-generic" % CirceVersion

  lazy val circeParser = "io.circe" %% "circe-parser" % CirceVersion

  lazy val circeLiteral = "io.circe" %% "circe-literal" % CirceVersion

  lazy val jodaTime = "joda-time" % "joda-time" % "2.10.14"

  lazy val flyway = "org.flywaydb" % "flyway-core" % "8.5.7"

  lazy val doobieHikari = "org.tpolecat" %% "doobie-hikari" % "1.0.0-RC2"

  lazy val postgres = "org.postgresql" % "postgresql" % "42.3.4"

  lazy val h2 = "com.h2database" % "h2" % "2.1.212"

  lazy val bcrypt = "org.mindrot" % "jbcrypt" % "0.4"

  lazy val redis4CatsEffect = "dev.profunktor" %% "redis4cats-effects" % "1.1.1"

  lazy val kafkaClients = "org.apache.kafka" % "kafka-clients" % "3.1.0"

  lazy val kafkaAvroSerializer = "io.confluent" % "kafka-avro-serializer" % "7.1.0"

  lazy val chatAvroSchemas = "com.ruchij" % "chat-avro-schemas" % "1.0-SNAPSHOT"

  lazy val pureconfig = "com.github.pureconfig" %% "pureconfig" % "0.17.1"

  lazy val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.2.11"

  lazy val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4"

  lazy val enumeratum = "com.beachape" %% "enumeratum" % "1.7.0"

  lazy val kindProjector = "org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full

  lazy val betterMonadicFor = "com.olegpy" %% "better-monadic-for" % "0.3.1"

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.11"

  lazy val scalaMock = "org.scalamock" %% "scalamock" % "5.2.0"

  lazy val pegdown = "org.pegdown" % "pegdown" % "1.6.0"
}
