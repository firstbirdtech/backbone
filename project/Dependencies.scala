import sbt._
import sbt.librarymanagement.ModuleID

object Dependencies {

  private[this] val pekkoVersion           = "1.0.2"
  private[this] val pekkoConnectorsVersion = "1.0.0"
  private[this] val circeVersion           = "0.14.6"
  private[this] val logbackClassicVersion  = "1.4.14"
  private[this] val scalaTestVersion       = "3.2.17"
  private[this] val slf4jVersion           = "2.0.9"
  private[this] val java8CompatVersion     = "1.0.2"

  private[this] val testDependencies: Seq[ModuleID] = Seq(
    "ch.qos.logback" % "logback-classic" % logbackClassicVersion % Test,
    "org.scalatest" %% "scalatest"       % scalaTestVersion      % Test
  )

  val core: Seq[ModuleID] = Seq(
    "org.slf4j" % "slf4j-api" % slf4jVersion,
    "junit"     % "junit"     % "4.13.2" % Test
  ) ++ testDependencies

  val consumer: Seq[ModuleID] = Seq(
    "org.scala-lang.modules" %% "scala-java8-compat"   % java8CompatVersion,
    "org.slf4j"               % "slf4j-api"            % slf4jVersion,
    "org.apache.pekko"       %% "pekko-stream"         % pekkoVersion,
    "org.apache.pekko"       %% "pekko-connectors-sqs" % pekkoConnectorsVersion
  ) ++ testDependencies

  val publisher: Seq[ModuleID] = Seq(
    "org.scala-lang.modules" %% "scala-java8-compat"   % java8CompatVersion,
    "org.slf4j"               % "slf4j-api"            % slf4jVersion,
    "org.apache.pekko"       %% "pekko-stream"         % pekkoVersion,
    "org.apache.pekko"       %% "pekko-connectors-sns" % pekkoConnectorsVersion
  ) ++ testDependencies

  val jsonCirce: Seq[ModuleID] = Seq(
    "io.circe" %% "circe-parser" % circeVersion,
    "io.circe" %% "circe-core"   % circeVersion
  ) ++ testDependencies

  val jsonPlay: Seq[ModuleID] = Seq(
    "org.playframework" %% "play-json" % "3.0.1"
  ) ++ testDependencies

  val jsonGson: Seq[ModuleID] = Seq(
    "com.google.code.gson" % "gson" % "2.10.1"
  ) ++ testDependencies

  val testutils: Seq[ModuleID] = Seq(
    "com.github.pjfanning"  %% "aws-spi-pekko-http" % "0.1.0",
    "com.github.sbt"         % "junit-interface"    % "0.13.3",
    "org.apache.pekko"      %% "pekko-slf4j"        % pekkoVersion,
    "org.apache.pekko"      %% "pekko-testkit"      % pekkoVersion,
    "io.circe"              %% "circe-parser"       % circeVersion,
    "org.elasticmq"         %% "elasticmq-rest-sqs" % "1.5.3",
    "org.mockito"           %% "mockito-scala"      % "1.17.30",
    "org.scalatest"         %% "scalatest"          % scalaTestVersion,
    "software.amazon.awssdk" % "sqs"                % "2.21.45"
  )

  val integrationtest: Seq[ModuleID] = Seq(
    "org.apache.pekko" %% "pekko-slf4j"     % pekkoVersion,
    "ch.qos.logback"    % "logback-classic" % logbackClassicVersion
  )

}
