name := "build-tools"

version := "0.1.0-SNAPSHOT"

organization := "org.scala-ide"

scalaVersion := "2.9.2"

publishMavenStyle := true

publishTo := Some("Typesafe IDE" at "https://typesafe.artifactoryonline.com/typesafe/ide-2.9")

credentials += Credentials(Path.userHome / ".credentials")

libraryDependencies ++= Seq(
  "net.databinder.dispatch" %% "core" % "0.9.1",
  "junit" % "junit" % "4.8.1" % "test",
  "com.novocode" % "junit-interface" % "0.10-M1" % "test")
