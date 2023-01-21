import sbt._
import sbt.librarymanagement.ModuleID

object Dependencies {

  private[this] val alpakkaVersion        = "4.0.0"
  private[this] val akkaVersion           = "2.6.20"
  private[this] val circeVersion          = "0.14.3"
  private[this] val logbackClassicVersion = "1.4.5"
  private[this] val scalaTestVersion      = "3.2.14"
  private[this] val slf4jVersion          = "2.0.4"

  private[this] val testDependencies: Seq[ModuleID] = Seq(
    "ch.qos.logback" % "logback-classic" % logbackClassicVersion % Test,
    "org.scalatest" %% "scalatest"       % scalaTestVersion      % Test
  )

  val core: Seq[ModuleID] = Seq(
    "org.slf4j" % "slf4j-api" % slf4jVersion,
    "junit"     % "junit"     % "4.13.2" % Test
  ) ++ testDependencies

  val consumer: Seq[ModuleID] = Seq(
    "org.slf4j"           % "slf4j-api"               % slf4jVersion,
    "com.typesafe.akka"  %% "akka-stream"             % akkaVersion,
    "com.lightbend.akka" %% "akka-stream-alpakka-sqs" % alpakkaVersion
  ) ++ testDependencies

  val publisher: Seq[ModuleID] = Seq(
    "org.slf4j"           % "slf4j-api"               % slf4jVersion,
    "com.typesafe.akka"  %% "akka-stream"             % akkaVersion,
    "com.lightbend.akka" %% "akka-stream-alpakka-sns" % alpakkaVersion
  ) ++ testDependencies

  val jsonCirce: Seq[ModuleID] = Seq(
    "io.circe" %% "circe-parser" % circeVersion,
    "io.circe" %% "circe-core"   % circeVersion
  ) ++ testDependencies

  val jsonPlay: Seq[ModuleID] = Seq(
    "com.typesafe.play" %% "play-json" % "2.9.3"
  ) ++ testDependencies

  val jsonGson: Seq[ModuleID] = Seq(
    "com.google.code.gson" % "gson" % "2.10"
  ) ++ testDependencies

  val testutils: Seq[ModuleID] = Seq(
    "com.github.matsluni"   %% "aws-spi-akka-http"  % "0.0.11",
    "com.github.sbt"         % "junit-interface"    % "0.13.3",
    "com.typesafe.akka"     %% "akka-slf4j"         % akkaVersion,
    "com.typesafe.akka"     %% "akka-testkit"       % akkaVersion,
    "io.circe"              %% "circe-parser"       % circeVersion,
    "org.elasticmq"         %% "elasticmq-rest-sqs" % "1.3.14",
    "org.mockito"           %% "mockito-scala"      % "1.17.12",
    "org.scalatest"         %% "scalatest"          % scalaTestVersion,
    "software.amazon.awssdk" % "sqs"                % "2.19.21"
  )

  val integrationtest: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-slf4j"      % akkaVersion,
    "ch.qos.logback"     % "logback-classic" % logbackClassicVersion
  )

}
