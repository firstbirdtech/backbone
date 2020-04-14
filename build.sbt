lazy val backbone = project
  .in(file("."))
  .disablePlugins(Publish)
  .enablePlugins(NoPublish)
  .aggregate(core, playJson, circe, gson)

lazy val core = project
  .in(file("core"))
  .settings(
    name := "backbone-core",
    libraryDependencies ++= Dependencies.core
  )

lazy val playJson = project
  .in(file("playJson"))
  .settings(
    name := "backbone-play-json",
    libraryDependencies ++= Dependencies.jsonPlay
  )
  .dependsOn(core)

lazy val circe = project
  .in(file("circe"))
  .settings(
    name := "backbone-circe",
    libraryDependencies ++= Dependencies.jsonCirce
  )
  .dependsOn(core)

lazy val gson = project
  .in(file("gson"))
  .settings(
    name := "backbone-gson",
    libraryDependencies ++= Dependencies.jsonGson
  )
  .dependsOn(core)