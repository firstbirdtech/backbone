import sbt._
import sbt.librarymanagement.ModuleID

object Dependencies {

  private val alpakkaVersion               = "2.0.0-RC2"
  private val akkaVersion                  = "2.6.4"
  private val catsVersion                  = "2.1.1"
  private val circeVersion                 = "0.13.0"
  private val elasticMqVersion             = "0.15.6"
  private val gsonVersion                  = "2.8.6"
  private val java8CompatVersion           = "0.9.1"
  private val junitVersion                 = "4.13"
  private val logbackVersion               = "1.2.3"
  private val mockitoScalaVersion          = "1.13.9"
  private val playJsonVersion              = "2.8.1"
  private val scalaTestVersion             = "3.1.1"
  private val scalaCollectionCompatVersion = "2.1.4"
  private val slf4jVersion                 = "1.7.30"

  private val testDependencies: Seq[ModuleID] = Seq(
    "ch.qos.logback" % "logback-classic" % logbackVersion   % Test,
    "org.scalatest"  %% "scalatest"      % scalaTestVersion % Test
  )

  val core: Seq[ModuleID] = Seq(
    "com.lightbend.akka"     %% "akka-stream-alpakka-sqs" % alpakkaVersion,
    "com.lightbend.akka"     %% "akka-stream-alpakka-sns" % alpakkaVersion,
    "org.typelevel"          %% "cats-core"               % catsVersion,
    "org.scala-lang.modules" %% "scala-collection-compat" % scalaCollectionCompatVersion,
    "org.scala-lang.modules" %% "scala-java8-compat"      % java8CompatVersion,
    "org.slf4j"              % "slf4j-api"                % slf4jVersion,
    "org.elasticmq"          %% "elasticmq-rest-sqs"      % elasticMqVersion % Test,
    "io.circe"               %% "circe-parser"            % circeVersion % Test,
    "io.circe"               %% "circe-core"              % circeVersion % Test,
    "com.typesafe.akka"      %% "akka-testkit"            % akkaVersion % Test,
    "org.mockito"            %% "mockito-scala"           % mockitoScalaVersion % Test,
    "junit"                  % "junit"                    % junitVersion % Test
  ) ++ testDependencies

  val jsonCirce: Seq[ModuleID] = Seq(
    "io.circe" %% "circe-parser" % circeVersion,
    "io.circe" %% "circe-core"   % circeVersion
  ) ++ testDependencies

  val jsonPlay: Seq[ModuleID] = Seq("com.typesafe.play" %% "play-json" % playJsonVersion) ++ testDependencies

  val jsonGson: Seq[ModuleID] = Seq("com.google.code.gson" % "gson" % gsonVersion) ++ testDependencies

}
