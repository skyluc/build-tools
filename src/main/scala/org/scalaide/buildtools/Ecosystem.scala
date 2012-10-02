package org.scalaide.buildtools

import java.io.File
import java.io.FileWriter

import scala.io.Source
import scala.util.matching.Regex

object Ecosystem {

  /** suffix used by eclipse to mark feature bundles */
  final val FeatureSuffix = ".feature.group"
  /** suffix used to mark the copy of the files which are modified */
  final val OriginalSuffix = ".original"

  /* osgi ids of the relevant bundles */
  final val ScalaLibraryId = "org.scala-ide.scala.library"
  final val ScalaCompilerId = "org.scala-ide.scala.compiler"
  final val ScalaIDEId = "org.scala-ide.sdt.core"
  final val ScalaIDEFeatureId = "org.scala-ide.sdt.feature"
  final val ScalaIDEFeatureIdOsgi = ScalaIDEFeatureId + FeatureSuffix

  /** default location of the manifest file in a bundle project */
  final val PluginManifest = "META-INF/MANIFEST.MF"
  /** default location of the feature description fise in a feature project */
  final val FeatureDescriptor = "feature.xml"

  /** regex to find the root option in the command line */
  final val RootOption = "--root=(.*)".r

  /** regex to find the given bundle id dependency in a manifest file */
  final def idInManifest(id: String) = ("(.*" + id + ")(,?.*)").r
  /** regex to find the given bundle id dependency, with a version number defined, in a manifest file */
  final def idInManifestWithVersion(id: String) = ("(.*)" + id + """;bundle-version="([^"]*)"(,?.*)""").r

  /** create the partial function finding the dependency line corresponding to the given id, and set the version */
  def updateVersionInManifest(id: String, version: String): PartialFunction[String, String] =
    updateVersionInManifest(idInManifestWithVersion(id), idInManifest(id), version)

  /** method need to create the partial function */
  private def updateVersionInManifest(idWithVersion: Regex, id: Regex, version: String): PartialFunction[String, String] = {
    case line @ idWithVersion(_, currentVersion, _) =>
      warning("%s has already a version number defined: %s".format(ScalaLibraryId, currentVersion))
      line
    case id(prefixWithId, suffix) =>
      """%s;bundle-version="[%s,%<s]"%s""".format(prefixWithId, version, suffix)
  }

  /** update the given manifest, using the provided version updater */
  def updateBundleManifest(baseManifest: File, versionUpdater: PartialFunction[String, String]) {
    val savedManifest= new File(baseManifest.getAbsolutePath() + OriginalSuffix)
    
    // make a copy if needed
    if (!savedManifest.exists) {
      FileUtils.copyFile(baseManifest, savedManifest)
    }

    // get the content
    val lines = Source.fromFile(savedManifest).getLines
    
    // update the content
    val newLines = lines.map(versionUpdater)

    // save the new content
    val writer = new FileWriter(baseManifest)
    newLines foreach { s =>
      writer.write(s)
      writer.append('\n')
    }
    writer.flush()
    writer.close()
  }

  private def warning(message: String) {
    println("WARNING: %s".format(message))
  }

}