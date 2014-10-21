package org.scalaide.buildtools

import java.io.File
import java.io.FileWriter
import scala.io.Source
import scala.util.matching.Regex
import org.osgi.framework.Version

object Ecosystem {

  /** suffix used by eclipse to mark feature bundles */
  val FeatureSuffix = ".feature.group"
  /** suffix used to mark the copy of the files which are modified */
  val OriginalSuffix = ".original"

  /* osgi ids of the relevant bundles */
  val ScalaLibraryId = "org.scala-ide.scala.library"
  val ScalaCompilerId = "org.scala-ide.scala.compiler"
  val ScalaLangLibraryId = "org.scala-lang.scala-library"
  val ScalaLangCompilerId = "org.scala-lang.scala-compiler"
  val ScalaIDEId = "org.scala-ide.sdt.core"
  val ScalaIDEFeatureId = "org.scala-ide.sdt.feature"
  val ScalaIDESourceFeatureId = "org.scala-ide.sdt.source.feature"
  val ScalaIDEDevFeatureId = "org.scala-ide.sdt.dev.feature"
  val ScalaIDEFeatureIdOsgi = ScalaIDEFeatureId + FeatureSuffix
  val JDTId = "org.eclipse.jdt.core"
  val SbtFeatureId = "org.scala-ide.sbt.feature"
  val SbtSourceFeatureId = "org.scala-ide.sbt.source.feature"
  val SbtFeatureIdOsgi = SbtFeatureId + FeatureSuffix
  val ScalaFeatureIdOsgiRegex = """org\.scala-ide\.scala\d+\.feature\.feature\.group""".r

  /** default location of the manifest file in a bundle project */
  val PluginManifest = "META-INF/MANIFEST.MF"
  val PluginManifestTemplatesLocation = "resources/META-INF/"
  /** default location of the feature description fise in a feature project */
  val FeatureDescriptor = "feature.xml"
  /** default location of the ecosystem configuration */
  val EcosystemConfigFile = "ecosystem.conf"
  /** default location of the feature configuration files */
  val EcosystemFeatureFolder = "features"

  /** regex to find the root option in the command line */
  val RootOption = "--root=(.*)".r

  val RangeRegex = """([\[\(])([^,]*),([^\]\)]*)([\]\)])""".r
  
  val UndefinedVersion = new Version(0, 0, 0)
  val Version4_0_0 = new Version(4, 0, 0)
  val Version4_0_0rc1 = new Version(4, 0, 0, "rc1")
  val UndefinedRange = new VersionRange(UndefinedVersion, true, UndefinedVersion, true)
  
  /** regex to find the given bundle id dependency in a manifest file */
  def idInManifest(id: String) = ("(.*" + id + ")(,?.*)").r
  /** regex to find the given bundle id dependency, with a version number defined, in a manifest file */
  def idInManifestWithVersion(id: String) = ("(.*" + id + """);bundle-version="([^"]*)"(,?.*)""").r

  /** create the partial function finding the dependency line corresponding to the given id, and set the version */
  def updateVersionInManifest(pluginId: String, version: Version): PartialFunction[String, String] =
    updateVersionInManifest(idInManifestWithVersion(pluginId), idInManifest(pluginId), version)

  /** method need to create the partial function */
  private def updateVersionInManifest(idWithVersion: Regex, id: Regex, version: Version): PartialFunction[String, String] = {
    case line @ idWithVersion(prefixWithId, currentVersion, suffix) =>
      warning(s"$prefixWithId has already a version number defined: $currentVersion. Replacing.")
      s"""$prefixWithId;bundle-version="[$version,$version]"$suffix"""
    case id(prefixWithId, suffix) =>
      s"""$prefixWithId;bundle-version="[$version,$version]"$suffix"""
  }

  /** update the given manifest, using the provided version updater */
  def updateBundleManifest(baseManifest: File, versionUpdater: PartialFunction[String, String]) {
    val savedManifest = new File(baseManifest.getAbsolutePath() + OriginalSuffix)

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

  def getContent(file: File): Either[String, Iterator[String]] = {
    if (file.exists && file.isFile) {

      Right(Source.fromFile(file).getLines)
    } else {
      Left("%s doesn't exist or is not a file".format(file))
    }
  }

  private def warning(message: String) {
    println("WARNING: %s".format(message))
  }

  object EclipseVersion {
    def apply(range: String): Option[EclipseVersion] = {
      range match {
        case RangeRegex(_, low, high, _) =>
          val v = new Version(low)
          if (v.getMajor() == 3 && v.getMinor() < 8) {
            Some(EclipseIndigo)
          } else if (v.getMajor() == 3 && v.getMinor() < 9) {
            Some(EclipseJuno)
          } else if (v.getMajor() == 3 && v.getMinor() < 10) {
            Some(EclipseKepler)
          } else {
            Some(EclipseLuna)
          }
      }
    }
  }

  abstract class EclipseVersion(val id: String, val name: String, val repoLocation: String)

  case object EclipseIndigo extends EclipseVersion("indigo", "Indigo", "http://download.eclipse.org/releases/indigo/")

  case object EclipseJuno extends EclipseVersion("juno", "Juno", "http://download.eclipse.org/releases/juno/")

  case object EclipseKepler extends EclipseVersion("kepler", "Kepler", "http://download.eclipse.org/releases/kepler/")

  case object EclipseLuna extends EclipseVersion("luna", "Luna", "http://download.eclipse.org/releases/luna/")

  def findStrictVersion(range: String): Version =
    findStrictVersion(VersionRange(range))
  
  def findStrictVersion(range: VersionRange): Version = 
    range match {
    case VersionRange(min, true, max, true) if min == max =>
      min
    case _ =>
      UndefinedVersion
  }
  
  case class VersionRange(
      minVersion: Version,
      minInclusive: Boolean,
      maxVersion: Version,
      maxInclusive: Boolean) {
    
    /** Returns `true` if the given version part of this range
     */
    def contains(version: Version): Boolean = 
      (minVersion.compareTo(version) < 0  || minInclusive && minVersion == version) &&
        (maxVersion.compareTo(version) > 0 || maxInclusive && maxVersion == version)
    
  }
      
  object VersionRange {
    def apply(v: String): VersionRange = v match {
      case RangeRegex("[", min, max, "]") =>
        new VersionRange(new Version(min), true, new Version(max), true)
      case RangeRegex("[", min, max, ")") =>
        new VersionRange(new Version(min), true, new Version(max), false)
      case RangeRegex("(", min, max, "]") =>
        new VersionRange(new Version(min), false, new Version(max), true)
      case RangeRegex("(", min, max, ")") =>
        new VersionRange(new Version(min), false, new Version(max), false)
      case _ =>
        UndefinedRange
    }
  }

}
