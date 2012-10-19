package org.scalaide.buildtools

import java.io.File
import scala.annotation.tailrec
import scala.collection.mutable.HashMap
import java.net.URL
import org.osgi.framework.Version
import scala.collection.immutable.TreeSet
import dispatch.Http

/**
 * !!! This object not thread safe !!! It was used in a single threaded system when implemented.
 */
object Repositories {

  val repos = HashMap[URL, Either[String, P2Repository]]()

  def apply(location: URL): Either[String, P2Repository] = {
    repos.get(location) match {
      case Some(repo) =>
        repo
      case None =>
        val repo = P2Repository.fromUrl(location)
        repos.put(location, repo)
        repo
    }
  }
}

object GenerateEcosystemBuilds {
  import Ecosystem._

  def main(args: Array[String]) {
    // parse arguments

    val rootFolder = args.collectFirst {
      case RootOption(root) =>
        root
    }.getOrElse(System.getProperty("user.dir"))

    new GenerateEcosystemBuilds(rootFolder)()
  }

}

class GenerateEcosystemBuilds(rootFolder: String) {
  import Ecosystem._

  def apply(): Either[String, AnyRef] = {

    val ecosystems = EcosystemsDescriptor.load(new File(rootFolder, EcosystemConfigFile)).ecosystems
    val requestedFeatures = PluginDescriptor.loadAll(new File(rootFolder, "features")).flatMap(_ match {
      case Right(conf) =>
        Some(conf)
      case Left(_) =>
        None
    })

    val res= for {
      availableFeatures <- findFeatures(requestedFeatures.toList).right
      ecosystemToScalaIDEToAvailableFeatures <- getAvailableScalaIDEs(ecosystems, requestedFeatures.toList, availableFeatures).right
    } yield MavenProject2.generateEcosystemsProjects(ecosystemToScalaIDEToAvailableFeatures, new File(rootFolder, "target/builds"))

    
    // need to stop Dispatch in any cases
    Http.shutdown()
    res

  }

  private def getAvailableScalaIDEs(ecosystems: List[EcosystemDescriptor], requestedFeatures: List[PluginDescriptor], availableFeatures: List[FeatureDefinition]): Either[String, Map[EcosystemDescriptor, Map[ScalaIDEDefinition, Features]]] = {
    @tailrec
    def loop(repositories: List[EcosystemDescriptor], definitions: Map[EcosystemDescriptor, Map[ScalaIDEDefinition, Features]]): Either[String, Map[EcosystemDescriptor, Map[ScalaIDEDefinition, Features]]] = {
      repositories match {
        case Nil =>
          Right(definitions)
        case head :: tail =>
          findScalaIDEsAndResolvedAvailableFeatures(head, requestedFeatures, availableFeatures) match {
            case Left(error) =>
              Left(error)
            case Right(scalaIDEs) =>
              loop(tail, definitions + (head -> scalaIDEs))
          }
      }
    }
    loop(ecosystems, Map())
  }

  private def findScalaIDEsAndResolvedAvailableFeatures(ecosystem: EcosystemDescriptor, requestedFeatures: List[PluginDescriptor], availableFeatures: List[FeatureDefinition]): Either[String, Map[ScalaIDEDefinition, Features]] = {
    for {
      repository <- Repositories(ecosystem.site).right
      baseRepository <- Repositories(ecosystem.baseSite).right
    } yield findScalaIDEsAndResolvedAvailableFeatures(repository, baseRepository, requestedFeatures, availableFeatures)
  }

  private def findScalaIDEsAndResolvedAvailableFeatures(repository: P2Repository, baseRepository: P2Repository, requestedFeatures: List[PluginDescriptor], availableFeatures: List[FeatureDefinition]): Map[ScalaIDEDefinition, Features] = {
    val allAvailableFeatures = mergeFeatureList(findExistingFeatures(requestedFeatures, repository), availableFeatures)

    baseRepository.findIU(ScalaIDEFeatureIdOsgi).foldLeft(Map[ScalaIDEDefinition, Features]())((m, ui) =>
      // TODO: might be a nice place to check versions
      m + (ScalaIDEDefinition(ui, baseRepository) -> filterFeaturesFor(ui, allAvailableFeatures, repository)))
  }

  private def filterFeaturesFor(scalaIDE: InstallableUnit, availableFeatures: List[FeatureDefinition], baseRepository: P2Repository): Features = {
    val lists= availableFeatures.filter(f => ScalaIDEDefinition.matches(scalaIDE.version, f.sdtFeatureRange.range)).groupBy(_.details.featureId).map(t => TreeSet(t._2: _*)(FeatureDefinition.DescendingOrdering).head).toList.partition(_.repository == baseRepository)
    Features(lists._2, lists._1)
  }

  private def findFeatures(requestedFeatures: List[PluginDescriptor]): Either[String, List[FeatureDefinition]] = {
    Right(requestedFeatures.flatMap(findFeatures(_)))
  }

  private def findFeatures(requestedFeature: PluginDescriptor): List[FeatureDefinition] = {
    requestedFeature.updateSites.flatMap { location =>
      Repositories(location) match {
        case Right(p2repo) =>
          findFeatures(requestedFeature, p2repo)
        case Left(_) =>
          Nil
      }
    }
  }

  private def findFeatures(feature: PluginDescriptor, repository: P2Repository): List[FeatureDefinition] = {
    repository.findIU(feature.featureId + FeatureSuffix).toList.map(FeatureDefinition(feature, _, repository))
  }

  private def mergeFeatureList(base: List[FeatureDefinition], toMerge: List[FeatureDefinition]): List[FeatureDefinition] = {

    def loop(toProcess: List[FeatureDefinition]): List[FeatureDefinition] = {
      toProcess match {
        case Nil =>
          base
        case head :: tail =>
          if (base.exists(f => f.details == head.details && f.version == head.version)) {
            loop(tail)
          } else {
            loop(tail) :+ head
          }
      }
    }
    val res = loop(toMerge)
    res
  }

  private def findExistingFeatures(requestedFeatures: List[PluginDescriptor], repository: P2Repository): List[FeatureDefinition] = {
    requestedFeatures.flatMap(findFeatures(_, repository))
  }

  private def findAssociatedFeatures() {

  }

}

case class ScalaIDEDefinition(
  sdtFeatureVersion: Version,
  sdtCoreVersion: Option[DependencyUnit],
  scalaLibraryVersion: Option[DependencyUnit],
  scalaCompilerVersion: Option[DependencyUnit], repository: P2Repository)

object ScalaIDEDefinition {
  import Ecosystem._

  private val RangeRegex = "\\[([^,]*),([^\\]]*)\\]".r

  def apply(feature: InstallableUnit, repository: P2Repository): ScalaIDEDefinition = {

    val dependencies = allDependencies(feature, repository)

    val sdtCore = dependencies.find(_.id == ScalaIDEId)
    val scalaLibrary = dependencies.find(_.id == ScalaLibraryId)
    val scalaCompiler = dependencies.find(_.id == ScalaCompilerId)

    new ScalaIDEDefinition(feature.version, sdtCore, scalaLibrary, scalaCompiler, repository)
  }

  def allDependencies(iu: InstallableUnit, repository: P2Repository): List[DependencyUnit] = {
    iu.dependencies ++ iu.dependencies.flatMap(allDependencies(_, repository))
  }

  def allDependencies(du: DependencyUnit, repository: P2Repository): List[DependencyUnit] = {
    repository.findIU(du.id).toList.filter(iu => matches(iu.version, du.range)) match {
      case Nil =>
        // not part of this repository, fine
        Nil
      case List(iu) =>
        // this is the one we are looking for
        val dep = allDependencies(iu, repository)
        dep
      case _ =>
        // more than one?
        Nil // TODO: better return value?
    }
  }

  def matches(version: Version, range: String): Boolean = {
    range match {
      case RangeRegex(low, high) if (low == high) =>
        // we care only about strict versions so far
        // TODO: may need to improve that
        version.equals(new Version(low))
      case _ =>
        false
    }
  }
}

case class FeatureDefinition(
  details: PluginDescriptor,
  version: Version,
  sdtFeatureRange: DependencyUnit,
  sdtCoreRange: DependencyUnit,
  scalaLibraryRange: DependencyUnit,
  scalaCompilerRange: DependencyUnit,
  repository: P2Repository)

object FeatureDefinition {
  import Ecosystem._

  def apply(details: PluginDescriptor, iu: InstallableUnit, repository: P2Repository): FeatureDefinition = {
    val dependencies = ScalaIDEDefinition.allDependencies(iu, repository)

    val sdtCore = dependencies.find(_.id == ScalaIDEId)
    val scalaLibrary = dependencies.find(_.id == ScalaLibraryId)
    val scalaCompiler = dependencies.find(_.id == ScalaCompilerId)
    val sdtFeature = dependencies.find(_.id == ScalaIDEFeatureIdOsgi)

    // TODO: add support for source features

    // TODO: need to handle error cases, and check versions ...

    new FeatureDefinition(details, iu.version, sdtFeature.get, sdtCore.get, scalaLibrary.get, scalaCompiler.get, repository)
  }

  implicit object DescendingOrdering extends Ordering[FeatureDefinition] {
    override def compare(x: FeatureDefinition, y: FeatureDefinition): Int = {
      val diffId = x.details.featureId.compareTo(y.details.featureId)
      if (diffId == 0) -1 * x.version.compareTo(y.version) // same bundle name, compare versions
      else diffId
    }
  }

}

case class Features(available: List[FeatureDefinition], existing: List[FeatureDefinition])
