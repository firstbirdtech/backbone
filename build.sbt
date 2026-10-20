addCommandAlias("codeFmt", ";headerCreate;scalafmtAll;scalafmtSbt;scalafixAll")
addCommandAlias("codeVerify", ";scalafmtCheckAll;scalafmtSbtCheck;scalafixAll --check;headerCheck")

ThisBuild / resolvers += "Sonatype Nexus Maven Group" at "https://nexus.firstbird.io/repository/maven-group/"

lazy val runtimeFamily         = settingKey[String]("Expected runtime: akka, pekko, or neutral")
lazy val checkRuntimeIsolation = taskKey[Unit]("Reject dependencies from the opposite runtime")

lazy val commonSettings = Seq(
  runtimeFamily         := "neutral",
  checkRuntimeIsolation := {
    val family         = runtimeFamily.value
    val configurations = if (family == "neutral") Set("compile", "runtime", "test") else Set("compile", "runtime")
    val unexpected     = update.value.configurations
      .filter(report => configurations(report.configuration.name))
      .flatMap(_.modules.filterNot(_.evicted).map(_.module))
      .filter { dependency =>
        val akka = dependency.organization == "com.typesafe.akka" ||
          dependency.organization.startsWith("com.lightbend.akka") ||
          dependency.name.startsWith("aws-spi-akka-http")
        val pekko = dependency.organization == "org.apache.pekko" ||
          dependency.name.startsWith("aws-spi-pekko-http")
        (family != "akka" && akka) || (family != "pekko" && pekko)
      }
      .distinct
    if (unexpected.nonEmpty) sys.error(s"${name.value}: unexpected runtime dependencies: ${unexpected.mkString(", ")}")
    streams.value.log.info(s"${name.value}: $family runtime isolation verified")
  },
  organization        := "com.firstbird",
  organizationName    := "Firstbird GmbH",
  sonatypeProfileName := "com.firstbird",
  homepage            := Some(url("https://github.com/firstbirdtech/backbone")),
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  startYear := Some(2016),
  scmInfo   := Some(
    ScmInfo(homepage.value.get, "scm:git:https://github.com/firstbirdtech/backbone.git")
  ),
  developers += Developer(
    "contributors",
    "Contributors",
    "hello@firstbird.com",
    url("https://github.com/firstbirdtech/backbone/graphs/contributors")
  ),
  scalaVersion       := "2.13.18",
  crossScalaVersions := Seq("2.13.18", "3.3.8"),
  scalacOptions ++= {
    Seq(
      "-deprecation",
      "-encoding",
      "utf-8",
      "-explaintypes",
      "-feature",
      "-language:higherKinds",
      "-unchecked",
      "-Xfatal-warnings",
      "-Wunused:imports"
    ) ++
      (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((3, _)) =>
          Seq(
            "unchecked",
            "-source:3.0-migration",
            "-explain"
          )
        case _ =>
          Seq(
            "-Xsource:3",
            "-Wdead-code",
            "-Xcheckinit"
          )
      })
  },
  javacOptions ++= Seq(
    "-Xlint:unchecked",
    "-Xlint:deprecation"
  ),
  // show full stack traces and test case durations
  Test / testOptions += Tests.Argument("-oDF"),
  Test / parallelExecution := false,
  headerLicense            := Some(HeaderLicense.MIT("2025", "Backbone contributors")),
  semanticdbEnabled        := true,
  semanticdbVersion        := scalafixSemanticdb.revision
)

lazy val backbone = project
  .in(file("."))
  .settings(commonSettings)
  .settings(publish / skip := true)
  .aggregate(
    consumerApi,
    core,
    consumer,
    publisher,
    playJson,
    circe,
    gson,
    testutils,
    integrationtest,
    corePekko,
    consumerPekko,
    publisherPekko,
    testutilsPekko,
    integrationtestPekko
  )

lazy val consumerApi = Project("consumer-api", file("consumer-api"))
  .settings(commonSettings)
  .settings(
    name := "backbone-consumer-api",
    libraryDependencies ++= Dependencies.consumerApi,
    Test / unmanagedSources += baseDirectory.value.getParentFile / "testutils" / "src" / "main" / "scala" / "backbone" / "testutil" / "BaseTest.scala"
  )

lazy val core = project
  .in(file("core"))
  .settings(commonSettings)
  .settings(
    name          := "backbone-core",
    runtimeFamily := "akka",
    libraryDependencies ++= Dependencies.core,
    dependencyOverrides ++= Dependencies.dependencyOverrides
  )
  .dependsOn(consumer, publisher, testutils % Test)

lazy val consumer = project
  .in(file("consumer"))
  .settings(commonSettings)
  .settings(
    name          := "backbone-consumer",
    runtimeFamily := "akka",
    libraryDependencies ++= Dependencies.consumer,
    dependencyOverrides ++= Dependencies.dependencyOverrides
  )
  .dependsOn(consumerApi, testutils % Test)

lazy val publisher = project
  .in(file("publisher"))
  .settings(commonSettings)
  .settings(
    name          := "backbone-publisher",
    runtimeFamily := "akka",
    libraryDependencies ++= Dependencies.publisher,
    dependencyOverrides ++= Dependencies.dependencyOverrides
  )
  .dependsOn(testutils % Test)

lazy val playJson = project
  .in(file("playJson"))
  .settings(commonSettings)
  .settings(
    name := "backbone-play-json",
    libraryDependencies ++= Dependencies.jsonPlay,
    dependencyOverrides ++= Dependencies.dependencyOverrides
  )
  .dependsOn(consumerApi)

lazy val circe = project
  .in(file("circe"))
  .settings(commonSettings)
  .settings(
    name := "backbone-circe",
    libraryDependencies ++= Dependencies.jsonCirce,
    dependencyOverrides ++= Dependencies.dependencyOverrides
  )
  .dependsOn(consumerApi)

lazy val gson = project
  .in(file("gson"))
  .settings(commonSettings)
  .settings(
    name := "backbone-gson",
    libraryDependencies ++= Dependencies.jsonGson,
    dependencyOverrides ++= Dependencies.dependencyOverrides
  )
  .dependsOn(consumerApi)

lazy val testutils = project
  .in(file("testutils"))
  .settings(commonSettings)
  .settings(
    name                  := "backbone-testutils",
    runtimeFamily         := "akka",
    checkRuntimeIsolation := streams.value.log.info(
      s"${name.value}: ${runtimeFamily.value} fixtures allow the embedded Pekko-based ElasticMQ server"
    ),
    libraryDependencies ++= Dependencies.testutils,
    dependencyOverrides ++= Dependencies.dependencyOverrides,
    publish / skip := true
  )

lazy val integrationtest = project
  .in(file("integration-test"))
  .settings(commonSettings)
  .settings(
    name          := "backbone-integration-test",
    runtimeFamily := "akka",
    libraryDependencies ++= Dependencies.integrationtest,
    dependencyOverrides ++= Dependencies.dependencyOverrides,
    run / fork     := true,
    publish / skip := true
  )
  .dependsOn(core, circe, consumer, publisher)

lazy val consumerPekko = Project("consumer-pekko", file("consumer-pekko"))
  .settings(commonSettings)
  .settings(
    name          := "backbone-consumer-pekko",
    runtimeFamily := "pekko",
    libraryDependencies ++= Dependencies.consumerPekko
  )
  .dependsOn(consumerApi, testutilsPekko % Test)

lazy val publisherPekko = Project("publisher-pekko", file("publisher-pekko"))
  .settings(commonSettings)
  .settings(
    name          := "backbone-publisher-pekko",
    runtimeFamily := "pekko",
    libraryDependencies ++= Dependencies.publisherPekko
  )
  .dependsOn(testutilsPekko % Test)

lazy val corePekko = Project("core-pekko", file("core-pekko"))
  .settings(commonSettings)
  .settings(
    name          := "backbone-core-pekko",
    runtimeFamily := "pekko",
    libraryDependencies ++= Dependencies.core
  )
  .dependsOn(consumerPekko, publisherPekko, testutilsPekko % Test)

lazy val testutilsPekko = Project("testutils-pekko", file("testutils-pekko"))
  .settings(commonSettings)
  .settings(
    name          := "backbone-testutils-pekko",
    runtimeFamily := "pekko",
    libraryDependencies ++= Dependencies.testutilsPekko,
    publish / skip := true
  )

lazy val integrationtestPekko = Project("integrationtest-pekko", file("integration-test-pekko"))
  .settings(commonSettings)
  .settings(
    name          := "backbone-integration-test-pekko",
    runtimeFamily := "pekko",
    libraryDependencies ++= Dependencies.integrationtestPekko,
    run / fork     := true,
    publish / skip := true
  )
  .dependsOn(corePekko, circe, consumerPekko, publisherPekko)
