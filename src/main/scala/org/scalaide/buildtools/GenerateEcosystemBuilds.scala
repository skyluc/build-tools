package org.scalaide.buildtools

import java.io.File
import scala.annotation.tailrec
import scala.collection.mutable.HashMap

/**
 * !!! This object not thread safe !!! It was used in a single threaded system when implemented.
 */
object Repositories {

  val repos = HashMap[String, Either[String, P2Repository]]()

  def apply(location: String): Either[String, P2Repository] = {
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
    for {
      config <- EcosystemConfig(new File(rootFolder, EcosystemConfigFile)).right
      requestedFeatures <- RequestedFeature.parseConfigFiles(new File(rootFolder, EcosystemFeatureFolder)).right
      availableFeatures <- findFeatures(requestedFeatures).right
      ecosystemToScalaIDEToAvailableFeatures <- getAvailableScalaIDEs(config, requestedFeatures, availableFeatures).right
    } yield MavenProject.generateEcosystemsProjects(ecosystemToScalaIDEToAvailableFeatures, new File(rootFolder))

  }

  private def getAvailableScalaIDEs(config: EcosystemConfig, requestedFeatures: List[RequestedFeature], availableFeatures: List[FeatureDefinition]): Either[String, Map[EcosystemRepository, Map[ScalaIDEDefinition, List[FeatureDefinition]]]] = {
    @tailrec
    def loop(repositories: List[EcosystemRepository], definitions: Map[EcosystemRepository, Map[ScalaIDEDefinition, List[FeatureDefinition]]]): Either[String, Map[EcosystemRepository, Map[ScalaIDEDefinition, List[FeatureDefinition]]]] = {
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
    loop(config.repositories, Map())
  }

  private def findScalaIDEsAndResolvedAvailableFeatures(repositoryDefinition: EcosystemRepository, requestedFeatures: List[RequestedFeature], availableFeatures: List[FeatureDefinition]): Either[String, Map[ScalaIDEDefinition, List[FeatureDefinition]]] = {
    for {
      repository <- repositoryDefinition.getRepository.right
    } yield findScalaIDEsAndResolvedAvailableFeatures(repository, requestedFeatures, availableFeatures)
  }

  private def findScalaIDEsAndResolvedAvailableFeatures(repository: P2Repository, requestedFeatures: List[RequestedFeature], availableFeatures: List[FeatureDefinition]): Map[ScalaIDEDefinition, List[FeatureDefinition]] = {
    val allAvailableFeatures= mergeFeatureList(availableFeatures, findExistingFeatures(requestedFeatures, repository))
    repository.findIU(ScalaIDEFeatureIdOsgi).map(ScalaIDEDefinition(_, repository))
    
    repository.findIU(ScalaIDEFeatureIdOsgi).foldLeft(Map[ScalaIDEDefinition, List[FeatureDefinition]]())((m, ui) =>
        // TODO: might be a nice place to check versions
        m + (ScalaIDEDefinition(ui, repository) -> allAvailableFeatures.filter(f => ScalaIDEDefinition.matches(ui.version, f.sdtFeatureRange.range)))
      )
  }

  private def findFeatures(requestedFeatures: List[RequestedFeature]): Either[String, List[FeatureDefinition]] = {
    Right(requestedFeatures.flatMap(findFeatures(_)))
  }

  private def findFeatures(requestedFeature: RequestedFeature): Seq[FeatureDefinition] = {
    requestedFeature.repositories.flatMap(location => findFeatures(requestedFeature, Repositories(location).right.get))
  }

  private def findFeatures(feature: RequestedFeature, repository: P2Repository): Seq[FeatureDefinition] = {
    repository.findIU(feature.id + FeatureSuffix).map(FeatureDefinition(feature, _, repository))
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
    val res= loop(toMerge)
    res
  }

  private def findExistingFeatures(requestedFeatures: List[RequestedFeature], repository: P2Repository): List[FeatureDefinition] = {
    requestedFeatures.flatMap(findFeatures(_, repository))
  }

  private def findAssociatedFeatures() {

  }

}

case class ScalaIDEDefinition(
  sdtFeatureVersion: String,
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

  def allDependencies(iu: InstallableUnit, repository: P2Repository): Seq[DependencyUnit] = {
    iu.dependencies ++ iu.dependencies.flatMap(allDependencies(_, repository))
  }

  def allDependencies(du: DependencyUnit, repository: P2Repository): Seq[DependencyUnit] = {
    repository.findIU(du.id).filter(iu => matches(iu.version, du.range)) match {
      case Nil =>
        // not part of this repository, fine
        Nil
      case Seq(iu) =>
        // this is the one we are looking for
        val dep = allDependencies(iu, repository)
        dep
      case _ =>
        // more than one?
        Nil // TODO: better return value?
    }
  }

  def matches(version: String, range: String): Boolean = {
    range match {
      case RangeRegex(low, high) if (low == high) =>
        // we care only about strict versions so far
        // TODO: may need to improve that
        version == low
      case _ =>
        false
    }
  }
}

case class FeatureDefinition(
  details: RequestedFeature,
  version: String,
  sdtFeatureRange: DependencyUnit,
  sdtCoreRange: DependencyUnit,
  scalaLibraryRange: DependencyUnit,
  scalaCompilerRange: DependencyUnit,
  repository: P2Repository)

object FeatureDefinition {
  import Ecosystem._

  def apply(details: RequestedFeature, iu: InstallableUnit, repository: P2Repository): FeatureDefinition = {
    val dependencies = ScalaIDEDefinition.allDependencies(iu, repository)

    val sdtCore = dependencies.find(_.id == ScalaIDEId)
    val scalaLibrary = dependencies.find(_.id == ScalaLibraryId)
    val scalaCompiler = dependencies.find(_.id == ScalaCompilerId)
    val sdtFeature = dependencies.find(_.id == ScalaIDEFeatureIdOsgi)

    // TODO: add support for source features

    // TODO: need to handle error cases, and check versions ...

    new FeatureDefinition(details, iu.version, sdtFeature.get, sdtCore.get, scalaLibrary.get, scalaCompiler.get, repository)
  }
}