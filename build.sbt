name := "build-tools"

version := "0.2.0"

organization := "org.scala-ide"

scalaVersion := "2.9.2"

publishMavenStyle := true

publishTo := Some("Typesafe IDE" at "https://typesafe.artifactoryonline.com/typesafe/ide-2.9")
//publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

credentials += Credentials(Path.userHome / ".credentials")

libraryDependencies ++= Seq(
  "org.osgi" % "org.osgi.core" % "4.3.0", 
  "net.databinder.dispatch" %% "core" % "0.9.1",
  "com.typesafe" % "config" % "0.5.2",
  "junit" % "junit" % "4.8.1" % "test",
  "com.novocode" % "junit-interface" % "0.10-M1" % "test")
