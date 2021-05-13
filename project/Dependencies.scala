import sbt._
import sbt.librarymanagement.ModuleID

object Dependencies {

  private[this] val alpakkaVersion               = "2.0.2"
  private[this] val akkaVersion                  = "2.6.10"
  private[this] val catsVersion                  = "2.6.1"
  private[this] val circeVersion                 = "0.13.0"
  private[this] val elasticMqVersion             = "1.0.0"
  private[this] val gsonVersion                  = "2.8.6"
  private[this] val java8CompatVersion           = "0.9.1"
  private[this] val junitVersion                 = "4.13.2"
  private[this] val logbackVersion               = "1.2.3"
  private[this] val mockitoScalaVersion          = "1.16.37"
  private[this] val playJsonVersion              = "2.9.2"
  private[this] val scalaTestVersion             = "3.2.8"
  private[this] val scalaCollectionCompatVersion = "2.4.3"
  private[this] val slf4jVersion                 = "1.7.30"

  private[this] val testDependencies: Seq[ModuleID] = Seq(
    "ch.qos.logback" % "logback-classic" % logbackVersion   % Test,
    "org.scalatest" %% "scalatest"       % scalaTestVersion % Test
  )

  val core: Seq[ModuleID] = Seq(
    "com.typesafe.akka"      %% "akka-actor"              % akkaVersion,
    "com.typesafe.akka"      %% "akka-stream"             % akkaVersion,
    "com.lightbend.akka"     %% "akka-stream-alpakka-sqs" % alpakkaVersion,
    "com.lightbend.akka"     %% "akka-stream-alpakka-sns" % alpakkaVersion,
    "org.typelevel"          %% "cats-core"               % catsVersion,
    "org.scala-lang.modules" %% "scala-collection-compat" % scalaCollectionCompatVersion,
    "org.scala-lang.modules" %% "scala-java8-compat"      % java8CompatVersion,
    "org.slf4j"               % "slf4j-api"               % slf4jVersion,
    "org.elasticmq"          %% "elasticmq-rest-sqs"      % elasticMqVersion    % Test,
    "io.circe"               %% "circe-parser"            % circeVersion        % Test,
    "io.circe"               %% "circe-core"              % circeVersion        % Test,
    "com.typesafe.akka"      %% "akka-testkit"            % akkaVersion         % Test,
    "org.mockito"            %% "mockito-scala"           % mockitoScalaVersion % Test,
    "junit"                   % "junit"                   % junitVersion        % Test
  ) ++ testDependencies

  val jsonCirce: Seq[ModuleID] = Seq(
    "io.circe" %% "circe-parser" % circeVersion,
    "io.circe" %% "circe-core"   % circeVersion
  ) ++ testDependencies

  val jsonPlay: Seq[ModuleID] = Seq("com.typesafe.play" %% "play-json" % playJsonVersion) ++ testDependencies

  val jsonGson: Seq[ModuleID] = Seq("com.google.code.gson" % "gson" % gsonVersion) ++ testDependencies

}
