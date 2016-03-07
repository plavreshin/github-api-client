import NativePackagerHelper._

name := "GitHubApiClient"

version := "2.4.0"

scalaVersion := "2.11.7"

val akkaVersion = "2.4.2"
val sprayVersion = "1.3.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe" % "config" % "1.3.0",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "com.typesafe.play" %% "play-json" % "2.4.6",
  "io.spray" %% "spray-can" % sprayVersion,
  "io.spray" %% "spray-http" % sprayVersion,
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "org.mockito" % "mockito-all" % "1.10.19" % "test",
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"
)

enablePlugins(JavaServerAppPackaging)

mainClass in Compile := Some("com.github.api.GitHubClient")

fork in Test := true

parallelExecution in Test := false

