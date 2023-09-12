addCommandAlias("codeFmt", ";headerCreate;scalafmtAll;scalafmtSbt;scalafixAll")
addCommandAlias("codeVerify", ";scalafmtCheckAll;scalafmtSbtCheck;scalafixAll --check;headerCheck")

lazy val commonSettings = Seq(
  organization        := "com.firstbird",
  organizationName    := "Firstbird GmbH",
  sonatypeProfileName := "com.firstbird",
  homepage            := Some(url("https://github.com/firstbirdtech/backbone")),
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
  scalaVersion       := "2.13.12",
  crossScalaVersions := Seq(scalaVersion.value),
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding",
    "utf-8",
    "-explaintypes",
    "-feature",
    "-language:higherKinds",
    "-unchecked",
    "-Xcheckinit",
    "-Xfatal-warnings",
    "-Wdead-code",
    "-Wunused:imports"
  ),
  javacOptions ++= Seq(
    "-Xlint:unchecked",
    "-Xlint:deprecation"
  ),
  // show full stack traces and test case durations
  Test / testOptions += Tests.Argument("-oDF"),
  Test / parallelExecution := false,
  headerLicense            := Some(HeaderLicense.MIT("2021", "Backbone contributors")),
  semanticdbEnabled        := true,
  semanticdbVersion        := scalafixSemanticdb.revision
)

lazy val backbone = project
  .in(file("."))
  .settings(commonSettings)
  .settings(publish / skip := true)
  .aggregate(core, consumer, publisher, playJson, circe, gson, testutils, integrationtest)

lazy val core = project
  .in(file("core"))
  .settings(commonSettings)
  .settings(
    name := "backbone-core",
    libraryDependencies ++= Dependencies.core
  )
  .dependsOn(consumer, publisher, testutils % Test)

lazy val consumer = project
  .in(file("consumer"))
  .settings(commonSettings)
  .settings(
    name := "backbone-consumer",
    libraryDependencies ++= Dependencies.consumer
  )
  .dependsOn(testutils % Test)

lazy val publisher = project
  .in(file("publisher"))
  .settings(commonSettings)
  .settings(
    name := "backbone-publisher",
    libraryDependencies ++= Dependencies.publisher
  )
  .dependsOn(testutils % Test)

lazy val playJson = project
  .in(file("playJson"))
  .settings(commonSettings)
  .settings(
    name := "backbone-play-json",
    libraryDependencies ++= Dependencies.jsonPlay
  )
  .dependsOn(consumer)

lazy val circe = project
  .in(file("circe"))
  .settings(commonSettings)
  .settings(
    name := "backbone-circe",
    libraryDependencies ++= Dependencies.jsonCirce
  )
  .dependsOn(consumer)

lazy val gson = project
  .in(file("gson"))
  .settings(commonSettings)
  .settings(
    name := "backbone-gson",
    libraryDependencies ++= Dependencies.jsonGson
  )
  .dependsOn(consumer)

lazy val testutils = project
  .in(file("testutils"))
  .settings(commonSettings)
  .settings(
    name := "backbone-testutils",
    libraryDependencies ++= Dependencies.testutils,
    publish / skip := true
  )

lazy val integrationtest = project
  .in(file("integration-test"))
  .settings(commonSettings)
  .settings(
    name := "backbone-integration-test",
    libraryDependencies ++= Dependencies.integrationtest,
    run / fork     := true,
    publish / skip := true
  )
  .dependsOn(core, circe, consumer, publisher)
