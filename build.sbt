lazy val backbone = project
  .in(file("."))
  .disablePlugins(Publish)
  .enablePlugins(NoPublish)
  .aggregate(core, playJson, circe, gson)

lazy val core = project
  .in(file("core"))
  .settings(
    name := "backbone-core",
    libraryDependencies ++= Seq(
      "com.amazonaws"          % "aws-java-sdk-sqs"         % Versions.aws,
      "com.amazonaws"          % "aws-java-sdk-sns"         % Versions.aws,
      "com.typesafe.akka"      %% "akka-stream"             % Versions.akka,
      "com.lightbend.akka"     %% "akka-stream-alpakka-sqs" % Versions.alpakka,
      "com.lightbend.akka"     %% "akka-stream-alpakka-sns" % Versions.alpakka,
      "org.typelevel"          %% "cats-core"               % Versions.cats,
      "org.scala-lang.modules" %% "scala-java8-compat"      % Versions.java8Compat,
      "org.slf4j"              % "slf4j-api"                % Versions.slf4j,
      "org.elasticmq"          %% "elasticmq-rest-sqs"      % Versions.elasticMq % Test,
      "io.circe"               %% "circe-parser"            % Versions.circe % Test,
      "io.circe"               %% "circe-core"              % Versions.circe % Test,
      "com.typesafe.akka"      %% "akka-testkit"            % Versions.akka % Test,
      "org.mockito"            % "mockito-core"             % Versions.mockito % Test,
      "junit"                  % "junit"                    % Versions.junit % Test
    )
  )

lazy val playJson = project
  .in(file("playJson"))
  .settings(
    name := "backbone-play-json",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % Versions.playJson
    )
  )
  .dependsOn(core)

lazy val circe = project
  .in(file("circe"))
  .settings(
    name := "backbone-circe",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-parser" % Versions.circe,
      "io.circe" %% "circe-core"   % Versions.circe
    )
  )
  .dependsOn(core)

lazy val gson = project
  .in(file("gson"))
  .settings(
    name := "backbone-gson",
    libraryDependencies ++= Seq(
      "com.google.code.gson" % "gson" % Versions.gson
    )
  )
  .dependsOn(core)
