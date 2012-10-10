package org.scalaide.buildtools

import java.io.File
import scala.io.Source
import java.io.FileWriter
import org.osgi.framework.Version
import dispatch.Http

object UpdateScalaIDEManifests {

  import Ecosystem._

  final val Usage = "Usage: app [--root=<Scala IDE root folder>]"

  final val PackagedManifestPath = "target/" + PluginManifest

  final val BundleVersion = "Bundle-Version: (.*)".r

  final val projectsToUpdate = List(ScalaIDEId, "org.scala-ide.sdt.debug")

  def main(args: Array[String]) {
    // parse arguments

    val rootFolder = args.collectFirst {
      case RootOption(root) =>
        root
    }.getOrElse(System.getProperty("user.dir"))

    new UpdateScalaIDEManifests(rootFolder)()
    
  }

}

class UpdateScalaIDEManifests(root: String) {
  import UpdateScalaIDEManifests._
  import Ecosystem._

  val rootFolder = new File(root)

  def apply() {

    println("Build tools: Updating versions in Scala IDE manifests.")

    val scalaLibraryVersion = getPackagedBundleVersion(ScalaLibraryId)
    val scalaCompilerVersion = getPackagedBundleVersion(ScalaCompilerId)

    projectsToUpdate foreach {
      updateManifest(_, scalaLibraryVersion, scalaCompilerVersion)
    }

    println("Build tools: Updating versions in Scala IDE manifests - Done.")
    
    // need to stop Dispatch in any cases
    Http.shutdown()
  }

  /**
   * Returns the version contained in the generated manifest file
   */
  private def getPackagedBundleVersion(projectPath: String): Version = {
    val projectFolder = new File(rootFolder, projectPath)
    val manifestFile = new File(projectFolder, PackagedManifestPath)

    // TODO: check if file exists

    val lines = Source.fromFile(manifestFile).getLines
    val version = lines.collectFirst {
      case BundleVersion(v) => v
    }

    // TODO: check if version found
    new Version(version.get)
  }

  /**
   * Set strict version dependency to Scala library and compiler in the given project.
   */
  private def updateManifest(projectPath: String, scalaLibraryVersion: Version, scalaCompilerVersion: Version) {
    val projectFolder = new File(rootFolder, projectPath)
    val baseManifest = new File(projectFolder, PluginManifest)

    updateBundleManifest(baseManifest,
      updateVersionInManifest(ScalaLibraryId, scalaLibraryVersion).
        orElse(updateVersionInManifest(ScalaCompilerId, scalaCompilerVersion)).
        orElse {
          case line =>
            line
        })
  }

  private def warning(message: String) {
    println("WARNING: %s".format(message))
  }

}