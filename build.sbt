ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"

// akka-actor 2.12 depends on version < 1.x, but 0.x and 1.x are still binary compatible
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-java8-compat" % "always"

addCommandAlias("codeFmt", ";headerCreate;scalafmtAll;scalafmtSbt;scalafixAll")
addCommandAlias("codeVerify", ";scalafmtCheckAll;scalafmtSbtCheck;scalafixAll --check;headerCheck")

lazy val commonSettings = Seq(
  organization := "com.firstbird",
  organizationName := "Firstbird GmbH",
  sonatypeProfileName := "com.firstbird",
  homepage := Some(url("https://github.com/firstbirdtech/backbone")),
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  scmInfo := Some(
    ScmInfo(homepage.value.get, "scm:git:https://github.com/firstbirdtech/backbone.git")
  ),
  developers += Developer(
    "contributors",
    "Contributors",
    "hello@firstbird.com",
    url("https://github.com/firstbirdtech/backbone/graphs/contributors")
  ),
  scalaVersion := "2.13.7",
  crossScalaVersions := Seq("2.12.15", scalaVersion.value),
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding",
    "utf-8",
    "-explaintypes",
    "-feature",
    "-language:higherKinds",
    "-unchecked",
    "-Xcheckinit",
    "-Xfatal-warnings"
  ),
  scalacOptions ++= (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 13)) => Seq("-Wdead-code", "-Wunused:imports")
      case _             => Seq("-Xfuture", "-Ywarn-dead-code", "-Ywarn-unused:imports", "-Yno-adapted-args")
    }
  ),
  javacOptions ++= Seq(
    "-Xlint:unchecked",
    "-Xlint:deprecation"
  ),
  // show full stack traces and test case durations
  Test / testOptions += Tests.Argument("-oDF"),
  headerLicense := Some(HeaderLicense.MIT("2021", "Backbone contributors")),
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision
)

lazy val backbone = project
  .in(file("."))
  .settings(commonSettings)
  .settings(publish / skip := true)
  .aggregate(core, playJson, circe, gson)

lazy val core = project
  .in(file("core"))
  .settings(commonSettings)
  .settings(
    name := "backbone-core",
    libraryDependencies ++= Dependencies.core,
    dependencyOverrides ++= Dependencies.coreOverrides
  )

lazy val playJson = project
  .in(file("playJson"))
  .settings(commonSettings)
  .settings(
    name := "backbone-play-json",
    libraryDependencies ++= Dependencies.jsonPlay
  )
  .dependsOn(core)

lazy val circe = project
  .in(file("circe"))
  .settings(commonSettings)
  .settings(
    name := "backbone-circe",
    libraryDependencies ++= Dependencies.jsonCirce
  )
  .dependsOn(core)

lazy val gson = project
  .in(file("gson"))
  .settings(commonSettings)
  .settings(
    name := "backbone-gson",
    libraryDependencies ++= Dependencies.jsonGson
  )
  .dependsOn(core)
