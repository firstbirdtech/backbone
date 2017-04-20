import sbt.Keys._
import sbt._

object Common extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins      = plugins.JvmPlugin

  override lazy val projectSettings = Seq(
    organization := "com.firstbird",
    organizationName := "Firstbird GmbH",
    homepage := Some(url("https://github.com/firstbirdtech/backbone")),
    scmInfo := Some(ScmInfo(url("https://github.com/firstbirdtech/backbone"), "git@github.com:firstbirdtech/backbone.git")),
    developers += Developer("contributors", "Contributors", "hello@firstbird,com", url("https://github.com/firstbirdtech/backbone/graphs/contributors")),
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),

    scalaVersion := "2.11.11",
    scalacOptions ++= Seq(
      "-encoding",
      "UTF-8",
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

object NoCrossBuild extends AutoPlugin{
  override def requires: Plugins      = plugins.JvmPlugin
  override lazy val projectSettings = Seq(
    scalaVersion := "2.11.11"
  )
}

object CrossBuild extends AutoPlugin{
  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins      = plugins.JvmPlugin
  override lazy val projectSettings = Seq(
    crossScalaVersions := Seq("2.11.11", "2.12.2"),
    scalaVersion := "2.11.11"
  )
}
