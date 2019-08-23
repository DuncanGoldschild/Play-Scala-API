name := """server"""
organization := "com.github.DuncanGold"

version := "1.0.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.0"

routesGenerator := InjectedRoutesGenerator

// only for Play 2.7.x (Scala 2.12)
libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % "0.18.1-play27",
  "com.pauldijou"     %% "jwt-core"                          % "3.1.0"
)

libraryDependencies +=  "org.mindrot" % "jbcrypt" % "0.3m"
libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test

