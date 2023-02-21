externalResolvers := Seq(
  Resolver.url("Evolution Gaming", url("https://rms.evolutiongaming.com/pub-ivy/"))(Resolver.ivyStylePatterns),
  "Evolution Gaming repository" at "https://rms.evolutiongaming.com/public/"
)

import Dependencies._

ThisBuild / scalaVersion     := "2.13.10"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "fold-jmh-test",
    libraryDependencies += munit % Test,
    libraryDependencies += "org.typelevel" %% "cats-core" % "2.9.0",
    libraryDependencies += "org.typelevel" %% "cats-effect" % "3.4.7",
    libraryDependencies += "com.evolutiongaming" %% "scache" % "4.3.1",
  )
  .enablePlugins(JmhPlugin)

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
