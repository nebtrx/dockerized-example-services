import sbt.Keys.organization

lazy val root = project
  .in(file("."))
  .settings(settings)
  .aggregate(
    common,
    producer,
    webConsumer
  )

lazy val common = project
  .in(file("common"))
  .settings(
    name := "com.github.nebtrx.microexample.common",
    version := "0.0.1-SNAPSHOT",
    settings,
    libraryDependencies ++= commonDependencies,
  )

lazy val producer = project
  .in(file("producer"))
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .enablePlugins(AshScriptPlugin)
  .settings(
    name := "com.github.nebtrx.microexample.producer",
    version := "0.0.1-SNAPSHOT",
    dockerizedSettings,
    libraryDependencies ++= commonDependencies ++ mtlDependencies ++ webClientDependencies,
    mainClass in Compile := Some("com.github.nebtrx.microexample.producer.ProducerApp")
  )
  .dependsOn(common)


lazy val webConsumer = project
  .in(file("webconsumer"))
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .enablePlugins(AshScriptPlugin)
  .settings(
    name := "com.github.nebtrx.microexample.webconsumer",
    version := "0.0.1-SNAPSHOT",
    dockerizedSettings,
    libraryDependencies ++= commonDependencies ++ mtlDependencies ++ webServerDependencies,
    mainClass in Compile := Some("com.github.nebtrx.microexample.webconsumer.WebConsumerApp")
  )
  .dependsOn(common)


// Dependencies

lazy val dependencies =
  new {
    val Http4sVersion = "0.19.0"
    val CirceVersion = "0.10.0"
    val Specs2Version = "4.2.0"
    val LogbackVersion = "1.2.3"
    val Fs2RabbitVersion = "1.0-RC3"
    val CatsMtlVersion = "0.4.0"
    val JawnFs2Version = "0.13.0"

    val http4sBlazeServer = "org.http4s"          %% "http4s-blaze-server" % Http4sVersion
    val http4sBlazeClient = "org.http4s"          %% "http4s-blaze-client" % Http4sVersion
    val http4sCirce       = "org.http4s"          %% "http4s-circe"        % Http4sVersion
    val http4sDsl         = "org.http4s"          %% "http4s-dsl"          % Http4sVersion
    val specs2Core        = "org.specs2"          %% "specs2-core"         % Specs2Version  % "test"
    val logbackClassic    = "ch.qos.logback"      %  "logback-classic"     % LogbackVersion
    val circeGeneric      = "io.circe"            %% "circe-generic"       % CirceVersion
    val circeFs2          = "io.circe"            %% "circe-fs2"           % CirceVersion
    val fs2Rabbit         = "com.github.gvolpe"   %% "fs2-rabbit"          % Fs2RabbitVersion
    val fs2RabbitCirce    = "com.github.gvolpe"   %% "fs2-rabbit-circe"    % Fs2RabbitVersion
    val catsMtlCore       = "org.typelevel"       %% "cats-mtl-core"       % CatsMtlVersion
    val jawnFs2           = "org.http4s"          %% "jawn-fs2"            % JawnFs2Version
  }

lazy val commonDependencies = {
  import dependencies._
  Seq(
    circeGeneric,
    specs2Core,
    logbackClassic,
    circeFs2,
    fs2Rabbit,
    fs2RabbitCirce
  )
}

lazy val webClientDependencies =  {
  import dependencies._
  Seq(
    http4sBlazeClient,
    jawnFs2
  )
}


lazy val webServerDependencies =  {
  import dependencies._
  Seq(
    http4sBlazeServer,
    http4sCirce,
    http4sDsl,
  )
}

lazy val mtlDependencies =  {
  import dependencies._
  Seq(
    catsMtlCore
  )
}


// Settings

lazy val settings = commonSettings

lazy val dockerizedSettings = commonSettings ++ dockerSettings

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-Ypartial-unification"
)

lazy val commonSettings = Seq(
  scalacOptions ++= compilerOptions,
  organization := "com.github.nebtrx",
  version := "0.0.1-SNAPSHOT",
  scalaVersion := "2.12.7",
  resolvers ++= Seq(
    "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository",
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  ),
  addCompilerPlugin("org.spire-math" %% "kind-projector"     % "0.9.6"),
  addCompilerPlugin("com.olegpy"     %% "better-monadic-for" % "0.2.4"),
)

lazy val dockerSettings = Seq(
  dockerBaseImage := "openjdk:jre-alpine"
)
