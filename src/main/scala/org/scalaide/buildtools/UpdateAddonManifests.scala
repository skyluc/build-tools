package org.scalaide.buildtools

import java.io.File
import scala.xml.Attribute
import scala.xml.Elem
import scala.xml.Null
import scala.xml.XML
import dispatch.Http
import org.osgi.framework.Version

object UpdateAddonManifests {
  import Ecosystem._

  final val usage = "Usage: app [--root=<Addon root folder>] <repository URL>"

  def main(args: Array[String]) {
    // parse arguments

    val (repoURL, rootFolder) = args.toList match {
      case RootOption(_) :: Nil | Nil =>
        Console.err.println(usage)
        Console.err.println("ERROR: missing repository URL")
        System.exit(1).asInstanceOf[Nothing]
      case RootOption(root) :: url :: _ =>
        (url, root)
      case url :: _ =>
        (url, System.getProperty("user.dir"))
    }

    try {
      // does the job
      val result = new UpdateAddonManifests(repoURL, rootFolder)()

      // check the result
      for {
        error <- result.left
      } {
        Console.err.println("ERROR: %s".format(error))
        System.exit(2)
      }

    } finally {
      // need to stop Dispatch in any cases
      Http.shutdown()
    }
  }

}

class UpdateAddonManifests(repoURL: String, rootFolder: String) {

  import Ecosystem._

  def apply(): Either[String, String] = {
    val res = P2Repository.fromUrl(repoURL) match {
      case r: ValidP2Repository =>
        updateVersions(r)
      case ErrorP2Repository(msg, _) =>
        Left(msg)
    }

    res
  }

  /**
   * Set strict version dependency to Scala IDE in the plugin and features found under the root, using the
   * version numbers found in the given p2 repository
   */
  private def updateVersions(p2Repo: P2Repository): Either[String, String] = {
    for {
      scalaIDE <- getLatest(p2Repo, ScalaIDEId).right
      scalaLibrary <- getLatest(p2Repo, ScalaLangLibraryId, ScalaLibraryId).right
      scalaCompiler <- getLatest(p2Repo, ScalaLangCompilerId, ScalaCompilerId).right
      scalaIDEFeature <- getLatest(p2Repo, ScalaIDEFeatureIdOsgi).right
      result <- updateVersions(scalaIDE, scalaLibrary, scalaCompiler, scalaIDEFeature).right
    } yield result

  }

  /**
   * Set strict version dependency to Scala IDE in the plugin and features found under the root.
   */
  private def updateVersions(scalaIDE: InstallableUnit, scalaLibrary: InstallableUnit, scalaCompiler: InstallableUnit, scalaIDEFeature: InstallableUnit): Either[String, String] = {
    val root = new File(rootFolder)

    if (root.exists && root.isDirectory) {
      // update the plugin manifest files
      val versionUpdater: PartialFunction[String, String] = updateVersionInManifest(scalaLibrary.id, scalaLibrary.version).
        orElse(updateVersionInManifest(scalaCompiler.id, scalaCompiler.version)).
        orElse(updateVersionInManifest(ScalaIDEId, scalaIDE.version)).
        orElse {
          case line =>
            line
        }
      findPlugins(root).foreach(updateVersionInPluginManifest(_, versionUpdater))

      // update the feature definition files
      findFeatures(root).foreach(updateVersionInFeature(_, scalaIDEFeature.version))

      Right("OK")
    } else {
      Left("%s doesn't exist or is not a directory".format(root.getAbsolutePath()))
    }
  }

  /**
   * Set strict version dependency to the Scala IDE plugin in the given manifest file.
   */
  private def updateVersionInPluginManifest(manifest: File, versionUpdater: PartialFunction[String, String]) {
    println(manifest.getAbsoluteFile())

    updateBundleManifest(manifest, versionUpdater)
  }

  /**
   * Go through the feature definition XML tree, and add version and match attribute for the reference to the scala IDE feature.
   */
  private def transformXML(e: Elem, version: Version): Elem = {
    if (e.attributes.get("feature").exists(_.text == ScalaIDEFeatureId)) {
      e.copy(attributes = e.attributes.append(Attribute(null, "version", version.toString, Attribute(null, "match", "perfect", Null))))
    } else {
      e.copy(child = e.child.map(_ match {
        case e: Elem => transformXML(e, version)
        case o => o
      }))
    }
  }

  /**
   * Set strict version dependency to Scala IDE feature in the given feature file.
   */
  private def updateVersionInFeature(feature: File, scalaIDEFeatureVersion: Version): Either[String, String] = {
    println(feature.getAbsoluteFile())

    val savedFeature = new File(feature.getAbsolutePath() + OriginalSuffix)

    if (!savedFeature.exists) {
      FileUtils.copyFile(feature, savedFeature)
    }

    val xml = transformXML(XML.loadFile(savedFeature), scalaIDEFeatureVersion)

    XML.save(feature.getAbsolutePath(), xml, "UTF-8", true)

    Right("OK")
  }

  /**
   * Return the latest version of a plugin available in the repository.
   * Return an error if the plugin is not available.
   */
  private def getLatest(p2Repo: P2Repository, pluginId: String): Either[String, InstallableUnit] = {
    p2Repo.findIU(pluginId).headOption match {
      case Some(iu) =>
        Right(iu)
      case None =>
        Left("No version found for %s. You may not be using the right repository.".format(pluginId))
    }
  }

  /**
   * Return the latest version of the plugin pluginId available in the repository, if pluginId cannot be found, check with altPluginID
   */
  private def getLatest(p2Repo: P2Repository, pluginId: String, altPluginId: String): Either[String, InstallableUnit] = {
    val res = getLatest(p2Repo, pluginId)
    if (res.isRight) {
      res
    } else {
      getLatest(p2Repo, altPluginId)
    }
  }

  /**
   * Return the list of manifest files found in the direct subfolders of the root
   */
  private def findPlugins(root: File): List[File] = {
    findFileInSubFolders(root, PluginManifest)
  }

  /**
   * Return the list of feature definition files found in the direct subfolders of the root
   */
  private def findFeatures(root: File): List[File] = {
    findFileInSubFolders(root, FeatureDescriptor)
  }

  /**
   * Return the list of instances of the given file existing in the direct subfolders of the root
   */
  private def findFileInSubFolders(root: File, fileName: String): List[File] = {
    (for {
      folder <- root.listFiles().toList if (folder.isDirectory())
    } yield new File(folder, fileName)) filter (_.exists)
  }

}