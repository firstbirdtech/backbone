lazy val backbone = project
  .in(file("."))
  .disablePlugins(Publish)
  .enablePlugins(NoPublish)
  .aggregate(core)

lazy val core = project
  .in(file("core"))
  .settings(
    name := "backbone-core",
    libraryDependencies ++= Seq(
      "com.amazonaws"          % "aws-java-sdk-sqs"         % "1.11.76",
      "com.amazonaws"          % "aws-java-sdk-sns"         % "1.11.76",
      "com.typesafe.akka"      %% "akka-stream"             % "2.4.17",
      "com.lightbend.akka"     %% "akka-stream-alpakka-sqs" % "0.6",
      "org.typelevel"          %% "cats"                    % "0.9.0",
      "org.scala-lang.modules" %% "scala-java8-compat"      % "0.8.0",
      "com.typesafe.play"      %% "play-json"               % "2.5.12" % Test,
      "org.mockito"            % "mockito-core"             % "2.6.8" % Test,
      "org.scalatest"          %% "scalatest"               % "3.0.1" % Test,
      "junit"                  % "junit"                    % "4.12" % Test
    )
  )

lazy val playJson = project
  .in(file("play-json"))
  .settings(
    name := "backbone-play-json",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % "2.5.12"
    )
  )
  .dependsOn(core)

lazy val circe = project
  .in(file("circe"))
  .settings(
    name := "backbone-circe",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % "2.5.12"
    )
  )
  .dependsOn(core)
