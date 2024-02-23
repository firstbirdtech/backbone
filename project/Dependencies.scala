import sbt._
import sbt.librarymanagement.ModuleID

object Dependencies {

  private[this] val alpakkaVersion        = "4.0.0"
  private[this] val akkaVersion           = "2.6.21"
  private[this] val circeVersion          = "0.14.6"
  private[this] val logbackClassicVersion = "1.5.0"
  private[this] val scalaTestVersion      = "3.2.18"
  private[this] val slf4jVersion          = "2.0.12"
  private[this] val awsSdkVersion         = "2.24.7"

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
    "com.typesafe.play" %% "play-json" % "2.10.4"
  ) ++ testDependencies

  val jsonGson: Seq[ModuleID] = Seq(
    "com.google.code.gson" % "gson" % "2.10.1"
  ) ++ testDependencies

  val testutils: Seq[ModuleID] = Seq(
    "com.github.matsluni"   %% "aws-spi-akka-http"  % "1.0.0",
    "com.github.sbt"         % "junit-interface"    % "0.13.3",
    "com.typesafe.akka"     %% "akka-slf4j"         % akkaVersion,
    "com.typesafe.akka"     %% "akka-testkit"       % akkaVersion,
    "io.circe"              %% "circe-parser"       % circeVersion,
    "org.elasticmq"         %% "elasticmq-rest-sqs" % "1.5.7",
    "org.mockito"           %% "mockito-scala"      % "1.17.30",
    "org.scalatest"         %% "scalatest"          % scalaTestVersion,
    "software.amazon.awssdk" % "sqs"                % "2.24.7"
  )

  val integrationtest: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-slf4j"      % akkaVersion,
    "ch.qos.logback"     % "logback-classic" % logbackClassicVersion
  )

}
