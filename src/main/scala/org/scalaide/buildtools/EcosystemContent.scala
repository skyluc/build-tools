package org.scalaide.buildtools

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import Ecosystem._
import org.osgi.framework.Version
import scala.annotation.tailrec

object EcosystemContent {
  import ExecutionContext.Implicits.global

  def fetchEcosystemContents(ecosystemConfigurations: List[EcosystemDescriptor], featureConfigurations: Seq[PluginDescriptor], availableFeatures: Future[Map[PluginDescriptor, Seq[Feature]]]): Future[List[EcosystemContent]] = {

    Future.traverse(ecosystemConfigurations)(fetchCurrent(_, featureConfigurations, availableFeatures))
  }

  def fetchAvailableFeatures(featureConfigurations: Seq[PluginDescriptor]): Future[Map[PluginDescriptor, Seq[Feature]]] = {

    Future.traverse(featureConfigurations) {
      fetchAvailable(_)
    }.map {
      _.toMap
    }
  }

  private def fetchAvailable(featureConfiguration: PluginDescriptor): Future[(PluginDescriptor, Seq[Feature])] = {
    Future.traverse(featureConfiguration.updateSites) {
      P2Repositories(_)
    } map { repositories =>
      val features = repositories.flatMap(repo =>
        repo
          .findIU(featureConfiguration.featureId)
          .map(extractFeature(_, repo)))
      (featureConfiguration, features)
    }
  }

  private def fetchCurrent(configuration: EcosystemDescriptor, featureConfigurations: Seq[PluginDescriptor], availableFeatures: Future[Map[PluginDescriptor, Seq[Feature]]]): Future[EcosystemContent] = {
    val baseRepository = P2Repositories(configuration.base)
    val nextBaseRepository = P2Repositories(configuration.nextBase)

    val siteRepository = P2Repositories(configuration.site)
    val nextSiteRepository = P2Repositories(configuration.nextSite)

    val scalaIDEVersionsInSite = scalaIDEVersionsIn(siteRepository)
    val scalaIDEVersionsInNextSite = scalaIDEVersionsIn(nextSiteRepository)
    
    val featuresInSite = featuresIn(siteRepository, featureConfigurations)

    val scalaIDEsInSite = scalaIDEsIn(scalaIDEVersionsInSite, featuresInSite)
    val scalaIDEsInNextSite = scalaIDEsIn(scalaIDEVersionsInNextSite, featuresInSite)
    
    val visibleFeatures= groupFeatures(featuresInSite, availableFeatures)

    val scalaIDEsToBeInSite = scalaIDEsToBeIn(scalaIDEVersionsIn(baseRepository), visibleFeatures)
    val scalaIDEsToBeInNextSite = scalaIDEsToBeIn(scalaIDEVersionsIn(nextBaseRepository), visibleFeatures)

    for {
      inSite <- scalaIDEsInSite
      inNextSite <- scalaIDEsInNextSite
      toBeInSite <- scalaIDEsToBeInSite
      toBeInNextSite <- scalaIDEsInNextSite
    } yield EcosystemContent(configuration.id, inSite, inNextSite, toBeInSite, toBeInNextSite)
  }

  private def scalaIDEVersionsIn(repository: Future[P2Repository]): Future[Set[ScalaIDEVersions]] =
    repository.map {
      r =>
        r.findIU(ScalaIDEFeatureIdOsgi).map(extractScalaIDEVersions(_, r))
    }

  private def extractScalaIDEVersions(iu: InstallableUnit, repository: P2Repository): ScalaIDEVersions = {

    // the sdt.core bundle
    val sdtCore = (for {
      sdtCoreDep <- iu.dependencies.find(_.id == ScalaIDEId)
      sdtCoreVersion = findStrictVersion(sdtCoreDep.range)
      sdtCore <- repository.findIU(ScalaIDEId).find(_.version == sdtCoreVersion)
    } yield sdtCore) match {
      case Some(iu) =>
        iu
      case None =>
        throw new Exception("failed to find the SDT core bundle for %s".format(iu))
    }

    // the version of eclipse it depends on
    val eclipseVersion = (for {
      jdtCoreDep <- sdtCore.dependencies.find(_.id == JDTId)
    } yield EclipseVersion(jdtCoreDep.range)) match {
      case Some(version) =>
        version
      case None =>
        throw new Exception("failed to find the Eclipse version for %s".format(iu))
    }

    // the version of Scala it depends on
    val scalaVersion =
      sdtCore.dependencies.find(_.id == ScalaLibraryId)
        .map(libDep => findStrictVersion(libDep.range))
        .getOrElse(throw new Exception("failed to find the Scala version for %s".format(iu)))

    ScalaIDEVersions(iu, repository, scalaVersion, eclipseVersion)
  }

  private def extractFeature(iu: InstallableUnit, repository: P2Repository): Feature = {
    // the version of Scala IDE it depends on
    val scalaIDEVersion =
      iu.dependencies.find(_.id == ScalaIDEFeatureIdOsgi)
        .map(libDep => findStrictVersion(libDep.range))

    val scalaVersion =
      iu.dependencies.flatMap {
        dep =>
          repository.findIU(dep.id).find {
            i => dep.range.includes(i.version)
          }
      }.flatMap(_.dependencies)
        .find(_.id == ScalaLibraryId) match {
          case Some(scalaDep) =>
            findStrictVersion(scalaDep.range)
          case None =>
            throw new Exception("failed to find the Scala library bundle for %s".format(iu))
        }

    Feature(iu, repository, scalaIDEVersion, scalaVersion)
  }
  
  private def groupFeatures(existingFeatures: Future[Map[PluginDescriptor, Seq[Feature]]], availableFeatures: Future[Map[PluginDescriptor, Seq[Feature]]]): Future[Seq[(Seq[Feature], Seq[Feature])]] = {
    @tailrec
    def group(existing: List[(PluginDescriptor, Seq[Feature])], available: Map[PluginDescriptor, Seq[Feature]], acc: Seq[(Seq[Feature], Seq[Feature])]): Seq[(Seq[Feature], Seq[Feature])] = {
      existing match {
        case (descriptor, existingFeatures) :: tail =>
          available.get(descriptor) match {
            case Some(availableFeatures) =>
              group(tail, available - descriptor, acc :+ ((existingFeatures, availableFeatures)))
            case None =>
              group(tail, available, acc :+ ((existingFeatures, Seq())))
          }
        case _ =>
          acc ++ available.values.map(s => ((Seq(), s)))
      }
    }
    
    for {
      existing <- existingFeatures
      availableFeatures <- availableFeatures
    } yield group(existing.toList, availableFeatures, Seq())
  }

  private def featuresIn(repository: Future[P2Repository], featureConfigurations: Seq[PluginDescriptor]): Future[Map[PluginDescriptor, Seq[Feature]]] = {
    repository map {
      repo =>
        featureConfigurations.map(desc => (desc, repo.findIU(desc.featureId).map(extractFeature(_, repo)).toSeq)).toMap
    }
  }

  private def scalaIDEsIn(scalaIDEVersions: Future[Set[ScalaIDEVersions]], existingFeatures: Future[Map[PluginDescriptor, Seq[Feature]]]) = {

    for {
      versions <- scalaIDEVersions
      features <- existingFeatures
    } yield {
      versions.map {
        v =>
          // TODO make the filter do something
          ScalaIDE(v.iu, v.repository, v.scalaVersion, v.eclipseVersion, features.flatMap(_._2).filter(p => true).toSeq, Seq())
      }
    }

  }
  
  private def scalaIDEsToBeIn(scalaIDEVersions: Future[Set[ScalaIDEVersions]], visibleFeatures: Future[Seq[(Seq[Feature], Seq[Feature])]]): Future[Set[ScalaIDE]] = {
    
    for {
      versions <- scalaIDEVersions
      visible <- visibleFeatures
    } yield {
      versions.map {
        v =>
          ???
      }
    }
    
  }
  
  private def filterFeaturesFor(version: ScalaIDEVersions, existing: Map[PluginDescriptor, Seq[Feature]], available: Map[PluginDescriptor, Seq[Feature]]): (Seq[Feature], Seq[Feature]) = {
    
    def filter(existing: List[(PluginDescriptor, Seq[Feature])], available: Map[PluginDescriptor, Seq[Feature]], accExisting: Seq[Feature], accAvailable: Seq[Feature]): (Seq[Feature], Seq[Feature]) = {
      ???
    }
    
    filter(existing.toList, available, Seq(), Seq())
  }

  private case class ScalaIDEVersions(iu: InstallableUnit, repository: P2Repository, scalaVersion: Version, eclipseVersion: EclipseVersion)
}

case class ScalaIDE(iu: InstallableUnit, repository: P2Repository, scalaVersion: Version, eclipseVersion: EclipseVersion, existingFeatures: Seq[Feature], newFeatures: Seq[Feature])

case class Feature(iu: InstallableUnit, repository: P2Repository, scalaIDEVersion: Option[Version], scalaVersion: Version)

case class EcosystemContent(id: String, scalaIDEsInSite: Set[ScalaIDE], scalaIDEsInNextSite: Set[ScalaIDE], scalaIDEsToBeInSite: Set[ScalaIDE], scalaIDEsToBeInNextSite: Set[ScalaIDE])

