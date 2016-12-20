import sbt.Keys._
import sbt._

object Common extends AutoPlugin{
  override def trigger = allRequirements
  override def requires = plugins.JvmPlugin

  override lazy val projectSettings = Seq(
    organization := "com.firstbird",
    version := "2.0.0-SNAPSHOT",
    organizationName := "Firstbird GmbH",
    scalaVersion := "2.11.8",
    crossScalaVersions := Seq("2.11.8, 2.12.1"),
    scalacOptions ++= Seq(
      "-encoding", "UTF-8",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-Xfatal-warnings",
      "-Xlint",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Xfuture"
    ),

    javacOptions ++= Seq(
      "-Xlint:unchecked"
    ),
    // show full stack traces and test case durations
    testOptions in Test += Tests.Argument("-oDF"),
    resolvers ++= Seq(
      Resolver.mavenLocal
    )
  )
}