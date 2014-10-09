package org.scalaide.buildtools

import java.io.File
import scala.io.Source
import java.io.FileWriter
import org.osgi.framework.Version
import dispatch.Http
import java.util.zip.ZipFile

object UpdateScalaIDEManifests {

  import Ecosystem._

  final val BundleVersion = "Bundle-Version: (.*)".r

  final val projectsToUpdate = List(ScalaIDEId, "org.scala-ide.sdt.debug")

  def main(args: Array[String]) {

    val (scalaVersion, m2Repo, scalaIDERootOpt) = parseArgs(args.toList)

    val scalaIDERoot = scalaIDERootOpt match {
      case Some(folder) =>
        new File(folder)
      case None =>
        new File(System.getProperty("user.dir"))
    }

    new UpdateScalaIDEManifests(scalaVersion, new File(m2Repo), scalaIDERoot)()

  }

  private def parseArgs(args: List[String]): (String, String, Option[String]) = {

    def usageError(message: String): Nothing = {
      Console.err.println(s"Error: $message. Was : ${args.mkString(" ")}")
      Console.err.println("usage: app [--root=<Scala IDE root folder>] <scala version> <m2 repository folder>")
      sys.exit(1)
    }

    def parseArgsVersion(args: List[String], scalaIDERootOpt: Option[String]): (String, String, Option[String]) = {
      args match {
        case RootOption(root) :: tail =>
          parseArgsVersion(tail, Some(root))
        case version :: tail =>
          parseArgsM2Repo(tail, version, scalaIDERootOpt)
        case _ =>
          usageError("Missing arguments")
      }
    }

    def parseArgsM2Repo(args: List[String], scalaVersion: String, scalaIDERootOpt: Option[String]): (String, String, Option[String]) = {
      args match {
        case RootOption(root) :: tail =>
          parseArgsM2Repo(tail, scalaVersion, Some(root))
        case m2Repo :: tail =>
          parseArgsRemainder(tail, scalaVersion, m2Repo, scalaIDERootOpt)
        case _ =>
          usageError("Missing arguments")
      }
    }

    def parseArgsRemainder(args: List[String], scalaVersion: String, m2Repo: String, scalaIDERootOpt: Option[String]): (String, String, Option[String]) = {
      args match {
        case RootOption(root) :: tail =>
          parseArgsRemainder(tail, scalaVersion, m2Repo, Some(root))
        case Nil =>
          (scalaVersion, m2Repo, scalaIDERootOpt)
        case _ =>
          usageError("Bad arguments")
      }
    }

    parseArgsVersion(args, None)
  }

}

class UpdateScalaIDEManifests(scalaVersion: String, m2Repo: File, scalaIDERoot: File) {

  import UpdateScalaIDEManifests._
  import Ecosystem._

  def apply() {

    println("Build tools: Updating versions in Scala IDE manifests.")

    val version = getScalaVersionFromM2Repo()

    projectsToUpdate foreach {
      updateManifest(_, new Version(version))
    }

    println("Build tools: Updating versions in Scala IDE manifests - Done.")

  }

  private def getScalaVersionFromM2Repo(): String = {
    val scalaLibraryJar = new File(m2Repo, s"org/scala-lang/scala-library/${scalaVersion}/scala-library-${scalaVersion}.jar")

    val zipFile = new ZipFile(scalaLibraryJar, ZipFile.OPEN_READ)
    val manifestEntry = zipFile.getEntry(PluginManifest)
    val manifestInputStream = zipFile.getInputStream(manifestEntry)

    Source.fromInputStream(manifestInputStream, "UTF-8").getLines().collectFirst { case BundleVersion(v) => v }.get
  }

  /** Set strict version dependency to Scala library and compiler in the given project.
   */
  private def updateManifest(projectPath: String, scalaLibraryVersion: Version) {
    val projectFolder = new File(scalaIDERoot, projectPath)
    val manifestFile = new File(projectFolder, PluginManifest)
    val templateFiles = new File(projectFolder, PluginManifestTemplatesLocation).listFiles().to[List].filterNot(_.getName.endsWith(".original"))

    val files = manifestFile :: templateFiles

    files.foreach { file =>
      updateBundleManifest(file,
        updateVersionInManifest(ScalaLangCompilerId, scalaLibraryVersion).
          orElse {
            case line =>
              line
          })
    }
  }

}