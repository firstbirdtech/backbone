import sbt._
import sbt.librarymanagement.ModuleID

object Dependencies {

  private[this] val pekkoVersion           = "1.0.3"
  private[this] val pekkoConnectorsVersion = "1.0.2"
  private[this] val circeVersion          = "0.14.14"
  private[this] val logbackClassicVersion = "1.5.18"
  private[this] val scalaTestVersion      = "3.2.19"
  private[this] val slf4jVersion          = "2.0.17"
  private[this] val awsSdkVersion         = "2.34.5"
  private[this] val java8CompatVersion    = "1.0.2"

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
    "org.apache.pekko"       %% "pekko-connectors-sqs" % pekkoConnectorsVersion exclude ("software.amazon.awssdk", "sqs"),
    "software.amazon.awssdk"  % "sqs"                  % awsSdkVersion
  ) ++ testDependencies

  val publisher: Seq[ModuleID] = Seq(
    "org.scala-lang.modules" %% "scala-java8-compat"   % java8CompatVersion,
    "org.slf4j"               % "slf4j-api"            % slf4jVersion,
    "org.apache.pekko"       %% "pekko-stream"         % pekkoVersion,
    "org.apache.pekko"       %% "pekko-connectors-sns" % pekkoConnectorsVersion exclude ("software.amazon.awssdk", "sns"),
    "software.amazon.awssdk"  % "sns"                  % awsSdkVersion
  ) ++ testDependencies

  val jsonCirce: Seq[ModuleID] = Seq(
    "io.circe" %% "circe-parser" % circeVersion,
    "io.circe" %% "circe-core"   % circeVersion
  ) ++ testDependencies

  val jsonPlay: Seq[ModuleID] = Seq(
    "org.playframework" %% "play-json" % "3.0.1"
  ) ++ testDependencies

  val jsonGson: Seq[ModuleID] = Seq(
    "com.google.code.gson" % "gson" % "2.13.2"
  ) ++ testDependencies

  val testutils: Seq[ModuleID] = Seq(
    "com.github.pjfanning"  %% "aws-spi-pekko-http" % "0.1.1",
    "com.github.sbt"         % "junit-interface"    % "0.13.3",
    "org.apache.pekko"      %% "pekko-slf4j"        % pekkoVersion,
    "org.apache.pekko"      %% "pekko-testkit"      % pekkoVersion,
    "io.circe"              %% "circe-parser"       % circeVersion,
    "org.elasticmq"         %% "elasticmq-rest-sqs" % "1.6.0",
    "org.mockito"            % "mockito-core"       % "5.20.0",
    "org.scalatest"         %% "scalatest"          % scalaTestVersion,
    "software.amazon.awssdk" % "sqs"                % "2.25.70"
  )

  val integrationtest: Seq[ModuleID] = Seq(
    "org.apache.pekko" %% "pekko-slf4j"     % pekkoVersion,
    "ch.qos.logback"    % "logback-classic" % logbackClassicVersion
  )

  val dependencyOverrides = Seq.empty
}
