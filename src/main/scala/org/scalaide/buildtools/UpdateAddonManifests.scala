package org.scalaide.buildtools

import dispatch.Http
import java.io.File

object UpdateAddonManifests {
  import Ecosystem._

  final val usage = "Usage: app [--root=<Scala IDE root folder>] <repository URL>"

  def main(args: Array[String]) {
    // parse arguments

    if (args.length < 1) {
      Console.err.println(usage)
      Console.err.println("ERROR: missing repository URL")
      System.exit(1)
    }

    val repoURL = args.last

    val rootFolder = args.init.collectFirst {
      case RootOption(root) =>
        root
    }.getOrElse(System.getProperty("user.dir"))

    // does the job
    val result = new UpdateAddonManifests(repoURL, rootFolder)()

    // check the result
    for {
      error <- result.left
    } {
      Console.err.println("ERROR: %s".format(error))
      System.exit(2)
    }

    // need to stop Dispatch in any cases
    Http.shutdown()
  }

}

class UpdateAddonManifests(repoURL: String, rootFolder: String) {

  import Ecosystem._

  def apply(): Either[String, String] = {
    P2Repository.fromUrl(repoURL).right.flatMap(updateVersions(_))
  }

  private def updateVersions(p2Repo: P2Repository): Either[String, String] = {
    for {
      scalaIDEVersion <- getOneVersion(p2Repo, ScalaIDEId).right
      scalaLibraryVersion <- getOneVersion(p2Repo, ScalaLibraryId).right
      scalaCompilerVersion <- getOneVersion(p2Repo, ScalaCompilerId).right
      result <- updateVersions(scalaIDEVersion, scalaLibraryVersion, scalaCompilerVersion).right
    } yield result

  }

  private def updateVersions(scalaIDEVersion: String, scalaLibraryVersion: String, scalaCompilerVersion: String): Either[String, String] = {
    println("%s, %s, %s".format(scalaIDEVersion, scalaLibraryVersion, scalaCompilerVersion))

    val root = new File(rootFolder)

    if (root.exists && root.isDirectory) {
      val pluginManifestsResult = findPlugins(root).foldLeft[Either[String, String]](
        Right("no plugin"))(
          (res, manifest) => res.right.flatMap(r => updateVersionInPluginManifest(manifest, scalaIDEVersion, scalaLibraryVersion, scalaCompilerVersion)))
      findFeatures(root).foldLeft(
        pluginManifestsResult)(
          (res, feature) => res.right.flatMap(r => updateVersionInFeature(feature, scalaIDEVersion, scalaLibraryVersion, scalaCompilerVersion)))
    } else {
      Left("%s doesn't exist or is not a directory".format(root.getAbsolutePath()))
    }
  }

  private def updateVersionInPluginManifest(manifest: File, scalaIDEVersion: String, scalaLibraryVersion: String, scalaCompilerVersion: String): Either[String, String] = {
    println(manifest.getAbsoluteFile())
    Right("OK")
  }

  private def updateVersionInFeature(feature: File, scalaIDEVersion: String, scalaLibraryVersion: String, scalaCompilerVersion: String): Either[String, String] = {
    println(feature.getAbsoluteFile())
    Right("OK")
  }

  private def getOneVersion(p2Repo: P2Repository, pluginId: String): Either[String, String] = {
    p2Repo.findIU(pluginId) match {
      case Seq(iu) =>
        Right(iu.version)
      case _ =>
        Left("More than one version found for %s. You may not be using the right repository.".format(pluginId))
    }
  }

  private def findPlugins(root: File): List[File] = {
    findFileInSubFolders(root, PluginManifest)
  }

  private def findFeatures(root: File): List[File] = {
    findFileInSubFolders(root, FeatureDescriptor)
  }

  private def findFileInSubFolders(root: File, fileName: String): List[File] = {
    (for {
      folder <- root.listFiles().toList if (folder.isDirectory())
    } yield new File(folder, fileName)) filter (_.exists)
  }

}