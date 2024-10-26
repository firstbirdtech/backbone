import sbt._
import sbt.librarymanagement.ModuleID

object Dependencies {

  private[this] val alpakkaVersion        = "8.0.0"
  private[this] val akkaVersion           = "2.9.3"
  private[this] val circeVersion          = "0.14.10"
  private[this] val logbackClassicVersion = "1.5.11"
  private[this] val scalaTestVersion      = "3.2.19"
  private[this] val slf4jVersion          = "2.0.16"
  private[this] val awsSdkVersion         = "2.29.1"

  private[this] val testDependencies: Seq[ModuleID] = Seq(
    "ch.qos.logback" % "logback-classic" % logbackClassicVersion % Test,
    "org.scalatest" %% "scalatest"       % scalaTestVersion      % Test
  )

  val core: Seq[ModuleID] = Seq(
    "org.slf4j" % "slf4j-api" % slf4jVersion,
    "junit"     % "junit"     % "4.13.2" % Test
  ) ++ testDependencies

  val consumer: Seq[ModuleID] = Seq(
    "org.slf4j"          % "slf4j-api"   % slf4jVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    // Transitive awssdk version is too old
    "com.lightbend.akka"    %% "akka-stream-alpakka-sqs" % alpakkaVersion exclude ("software.amazon.awssdk", "sqs"),
    "software.amazon.awssdk" % "sqs"                     % awsSdkVersion
  ) ++ testDependencies

  val publisher: Seq[ModuleID] = Seq(
    "org.slf4j"          % "slf4j-api"   % slf4jVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    // Transitive awssdk version is too old
    "com.lightbend.akka"    %% "akka-stream-alpakka-sns" % alpakkaVersion exclude ("software.amazon.awssdk", "sns"),
    "software.amazon.awssdk" % "sns"                     % awsSdkVersion
  ) ++ testDependencies

  val jsonCirce: Seq[ModuleID] = Seq(
    "io.circe" %% "circe-parser" % circeVersion,
    "io.circe" %% "circe-core"   % circeVersion
  ) ++ testDependencies

  val jsonPlay: Seq[ModuleID] = Seq(
    "com.typesafe.play" %% "play-json" % "2.10.6"
  ) ++ testDependencies

  val jsonGson: Seq[ModuleID] = Seq(
    "com.google.code.gson" % "gson" % "2.11.0"
  ) ++ testDependencies

  val testutils: Seq[ModuleID] = Seq(
    "com.github.matsluni" %% "aws-spi-akka-http" % "1.0.1",
    "com.github.sbt"       % "junit-interface"   % "0.13.3",
    "com.typesafe.akka"   %% "akka-slf4j"        % akkaVersion,
    "com.typesafe.akka"   %% "akka-testkit"      % akkaVersion,
    "io.circe"            %% "circe-parser"      % circeVersion,
    // elasticmq-rest-sqs updates scala-library version patch releases...
    // So anything after 1.6.0 requires at least scala 3.4.x...
    "org.elasticmq"         %% "elasticmq-rest-sqs" % "1.6.0", // scala-steward:off
    "org.mockito"            % "mockito-core"       % "5.14.2",
    "org.scalatest"         %% "scalatest"          % scalaTestVersion,
    "software.amazon.awssdk" % "sqs"                % "2.25.70"
  )

  val integrationtest: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-slf4j"      % akkaVersion,
    "ch.qos.logback"     % "logback-classic" % logbackClassicVersion
  )

}
