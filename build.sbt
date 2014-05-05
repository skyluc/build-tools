name := "build-tools"

version := "0.4.2"

organization := "org.scala-ide"

scalaVersion := "2.10.3"

publishMavenStyle := true

publishTo := Some("Typesafe IDE" at "https://private-repo.typesafe.com/typesafe/ide-2.10")
//publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

credentials += Credentials(Path.userHome / ".credentials")

libraryDependencies ++= Seq(
  "org.osgi" % "org.osgi.core" % "4.3.0", 
  "net.databinder.dispatch" %% "core" % "0.9.1",
  "com.typesafe" % "config" % "0.5.2",
  "junit" % "junit" % "4.8.1" % "test",
  "org.slf4j" % "slf4j-simple" % "1.6.4",
  "com.novocode" % "junit-interface" % "0.10-M1" % "test",
  "commons-io" % "commons-io" % "2.4")
