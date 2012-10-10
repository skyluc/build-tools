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

  }

}

class UpdateAddonManifests(repoURL: String, rootFolder: String) {

  import Ecosystem._

  def apply(): Either[String, String] = {
    val res= P2Repository.fromUrl(repoURL).right.flatMap(updateVersions(_))
    
    // need to stop Dispatch in any cases
    Http.shutdown()
    res
  }

  /**
   * Set strict version dependency to Scala IDE in the plugin and features found under the root, using the
   * version numbers found in the given p2 repository
   */
  private def updateVersions(p2Repo: P2Repository): Either[String, String] = {
    for {
      scalaIDEVersion <- getOneVersion(p2Repo, ScalaIDEId).right
      scalaLibraryVersion <- getOneVersion(p2Repo, ScalaLibraryId).right
      scalaCompilerVersion <- getOneVersion(p2Repo, ScalaCompilerId).right
      scalaIDEFeatureVersion <- getOneVersion(p2Repo, ScalaIDEFeatureIdOsgi).right
      result <- updateVersions(scalaIDEVersion, scalaLibraryVersion, scalaCompilerVersion, scalaIDEFeatureVersion).right
    } yield result

  }

  /**
   * Set strict version dependency to Scala IDE in the plugin and features found under the root.
   */
  private def updateVersions(scalaIDEVersion: Version, scalaLibraryVersion: Version, scalaCompilerVersion: Version, scalaIDEFeatureVersion: Version): Either[String, String] = {
    println("%s, %s, %s, %s".format(scalaIDEVersion, scalaLibraryVersion, scalaCompilerVersion, scalaIDEFeatureVersion))

    val root = new File(rootFolder)


    if (root.exists && root.isDirectory) {
      // update the plugin manifest files
      val versionUpdater: PartialFunction[String, String] = updateVersionInManifest(ScalaLibraryId, scalaLibraryVersion).
          orElse(updateVersionInManifest(ScalaCompilerId, scalaCompilerVersion)).
          orElse(updateVersionInManifest(ScalaIDEId, scalaIDEVersion)).
          orElse {
          case line =>
          line
      }
      findPlugins(root).foreach(updateVersionInPluginManifest(_, versionUpdater))
      
      // update the feature definition files
      findFeatures(root).foreach(updateVersionInFeature(_, scalaIDEFeatureVersion))
      
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
    
    val savedFeature= new File(feature.getAbsolutePath() + OriginalSuffix)
    
    if (!savedFeature.exists) {
      FileUtils.copyFile(feature, savedFeature)
    }
    
    val xml= transformXML(XML.loadFile(savedFeature), scalaIDEFeatureVersion)
    
    XML.save(feature.getAbsolutePath(), xml, "UTF-8", true)
    
    Right("OK")
  }

  /**
   * Return the latest version of a plugin available in the repository.
   * Return an error if the plugin is not available.
   */
  private def getOneVersion(p2Repo: P2Repository, pluginId: String): Either[String, Version] = {
    p2Repo.findIU(pluginId).headOption match {
      case Some(iu) =>
        Right(iu.version)
      case None =>
        Left("No version found for %s. You may not be using the right repository.".format(pluginId))
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