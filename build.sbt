name := "build-tools"

version := "0.1.0-SNAPSHOT"

organization := "org.scala-ide"

scalaVersion := "2.9.1"

publishMavenStyle := true

publishTo := Some("Typesafe IDE" at "https://typesafe.artifactoryonline.com/typesafe/ide-2.9")

credentials += Credentials(Path.userHome / ".credentials")
