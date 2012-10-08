package org.scalaide.buildtools

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.io.File
import java.net.URL
import scala.util.control.Exception._
import scala.Array.canBuildFrom
import scala.collection.JavaConverters.asScalaBufferConverter

/** In-memory representation of a plugin descriptor file.
 *  The descriptor file is parsed using the typesafe-config library.
 *
 *  A light validation of the descriptor is performed in the constructor.
 *
 *  Instances of this class should be created via the factory methods `load` and `loadAll` in the
 *  companion object.
 *  
 *  Example of a plugin descriptor file:
 *  
 *  plugin-descriptor {
 *    source-repository = "https://github.com/scala-ide/scala-worksheet"
 *    documentation = "https://github.com/scala-ide/scala-worksheet/wiki/Getting-Started"
 *    issue-tracker = "https://github.com/scala-ide/scala-worksheet/issues/"
 *    update-sites = ["http://scala-ide.dreamhosters.com/nightly-update-worksheet-scalaide21-29/site/",
 *                    "http://scala-ide.dreamhosters.com/nightly-update-worksheet-scalaide21-210/site/"]
 *    source-feature-id = org.scala-ide.worksheet.source.feature
 *  }
 *
 *  @param name Is the plugin's feature id.
 */
final class PluginDescriptor(name: String, config: Config) {
  require(name.nonEmpty)

  import PluginDescriptor._
  import PluginDescriptor.Keys._

  val featureId: String = name
  val website: Option[URL] = maybeURL(config, websiteKey)
  val sourceRepo: URL = obtainURL(config, sourceRepoKey)
  val documentation: URL = obtainURL(config, documentationKey)
  val issueTracker: URL = obtainURL(config, issueTrackerKey)
  val updateSites: List[URL] = {
    val urls = obtainURLs(config, updateSitesKey)
    if (urls.isEmpty) throw new FailedToRetrieveKeyOrValue(name, updateSitesKey, new IllegalStateException("Expected at leats one value for " + updateSitesKey))
    urls
  }
  val category: Option[Category] = maybeString(config, categoryKey) flatMap (Category(_))
  val sourceFeatureId: String = obtainString(config, sourceFeatureIdKey)

  private def maybeURL(config: Config, path: String): Option[URL] = maybeString(config, path) map (new URL(_))

  private def maybeString(config: Config, path: String): Option[String] = maybe(config, path) {
    Option(config.getString(path))
  }

  private def maybe[T](config: Config, path: String)(f: => Option[T]): Option[T] = {
    if (config.hasPath(path)) f
    else None
  }

  private def obtainURLs(config: Config, path: String): List[URL] = {
    import scala.collection.JavaConverters._
    try {
      for (raw <- config.getStringList(path).asScala.toList) yield new URL(raw)
    } catch {
      case e: Exception => throw new FailedToRetrieveKeyOrValue(name, path, e)
    }
  }

  private def obtainURL(config: Config, path: String): URL = maybeURL(config, path) match {
    case None => throw new FailedToRetrieveKeyOrValue(name, path, new NoSuchElementException(path))
    case Some(url) => url
  }

  private def obtainString(config: Config, path: String): String = maybeString(config, path) match {
    case None => throw new FailedToRetrieveKeyOrValue(name, path, new NoSuchElementException(path))
    case Some(string) => string
  }
}

object PluginDescriptor {
  object Keys {
    private val root = "plugin-descriptor"
    protected[buildtools] val websiteKey = root + ".website"
    protected[buildtools] val sourceRepoKey = root + ".source-repository"
    protected[buildtools] val documentationKey = root + ".documentation"
    protected[buildtools] val issueTrackerKey = root + ".issue-tracker"
    protected[buildtools] val updateSitesKey = root + ".update-sites"
    protected[buildtools] val categoryKey = root + ".category"
    protected[buildtools] val sourceFeatureIdKey = root + ".source-feature-id"
  }

  sealed abstract class Category {
    def name: String = toString()
  }
  private case object Category {
    def apply(value: String): Option[Category] = {
      if (value.equalsIgnoreCase(Stable.toString)) Some(Stable)
      else if (value.equalsIgnoreCase(Incubation.toString)) Some(Incubation)
      else None
    }
  }
  private case object Incubation extends Category
  private case object Stable extends Category

  /** Load in memory all plugin descriptors files in the passed `folder`.*/
  def loadAll(folder: File): Array[Either[PluginDescriptorException, PluginDescriptor]] = {
    require(folder.isDirectory, folder.getAbsolutePath + " is not a folder")

    val files = folder.listFiles()
    stopIfDuplicates(files)

    files map (load)
  }

  /** Load in memory the passed plugin descriptor `file`.*/
  def load(file: File): Either[PluginDescriptorException, PluginDescriptor] = {
    require(file.isFile, file.getAbsolutePath + " is not a file")
    parse(file).right.map(new PluginDescriptor(file.getName, _))
  }

  /** Parse the configuration `file`.*/
  private def parse(file: File): Either[FailedParsingDescriptor, Config] =
    try {
      Right(ConfigFactory.parseFile(file))
    } catch {
      case e: Exception => Left(new FailedParsingDescriptor(file.getName, e))
    }

  /** Throws an exception if two (or more) of the passed `files` have the same (case-insensitive) name.*/
  private def stopIfDuplicates(files: Array[File]): Unit = {
    val seen = new collection.mutable.HashMap[String, File]()
    val duplicates = new collection.mutable.ListBuffer[(String, String)]
    for {
      file <- files
      name = file.getName.toLowerCase
    } seen get name match {
      case None => seen update (name, file)
      case Some(other) => duplicates += ((file.getName, other.getName))
    }

    if (duplicates.nonEmpty) throw new DuplicatePluginDescriptors(duplicates.toList)
  }

  sealed abstract class PluginDescriptorException(cause: String, t: Exception) extends Exception(cause, t)
  final class DuplicatePluginDescriptors(duplicates: List[(String, String)]) extends PluginDescriptorException("Duplicated plugin descriptor files: " + duplicates.mkString("{", ",", "}"), null)
  final class FailedParsingDescriptor(descriptorFileName: String, t: Exception) extends PluginDescriptorException(descriptorFileName, t)
  final class FailedToRetrieveKeyOrValue(descriptorFileName: String, key: String, t: Exception) extends PluginDescriptorException("In %s, key: %s is missing or its value is incorrect.".format(descriptorFileName, key), t)
}