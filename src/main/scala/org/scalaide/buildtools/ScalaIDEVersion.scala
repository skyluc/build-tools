package org.scalaide.buildtools

import org.osgi.framework.Version
import Ecosystem._

object ScalaIDEVersion {

  def apply(iu: InstallableUnit, repository: P2Repository, existingAddOns: Map[PluginDescriptor, Seq[AddOn]], availableAddOns: Map[PluginDescriptor, Seq[AddOn]], siteRepo: P2Repository): ScalaIDEVersion = {
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
      eclipseVersion <- EclipseVersion(jdtCoreDep.range)
    } yield eclipseVersion) match {
      case Some(version) =>
        version
      case None =>
        throw new Exception("failed to find the Eclipse version for %s".format(iu))
    }

    // the version of Scala it depends on
    val scalaCompilerVersion =
      sdtCore.dependencies.find(d => d.id == ScalaCompilerId || d.id == ScalaLangCompilerId)
        .map { libDep => findStrictVersion(libDep.range) }
        .getOrElse(throw new Exception("failed to find the Scala compiler version for %s".format(iu)))

    val scalaLibraryVersionRange =
      sdtCore.dependencies.find(d => d.id == ScalaLibraryId || d.id == ScalaLangLibraryId)
        .map { libDep => VersionRange(libDep.range) }
        .getOrElse(throw new Exception("failed to find the Scala library version for %s".format(iu)))

    val addOnsFilter: AddOn => Boolean =
      if (Version4_0_0rc1.compareTo(iu.version) <= 0) {
        // Scala IDE 4.0.0-rc1 or better
        { a: AddOn =>
          a.scalaIDEVersionRange.contains(iu.version) &&
            a.scalaLibraryVersionRange.map(_ == scalaLibraryVersionRange).getOrElse(false) &&
            a.scalaCompilerVersion.map(_ == scalaCompilerVersion).getOrElse(true)
        }
      } else {
        // before Scala IDE 4.0.0
        { a: AddOn =>
          a.scalaIDEVersion == iu.version
        }
      }

    val (associatedExistingAddOns, associatedAvailableAddOns) = latestAssociated(availableAddOns, addOnsFilter).foldLeft(latestAssociated(existingAddOns, addOnsFilter)) {
      (acc, availableAddOn) =>
        acc.get(availableAddOn._1) match {
          case Some(existingAddOn) if (existingAddOn.version.compareTo(availableAddOn._2.version) >= 0) =>
            acc
          case _ =>
            acc + availableAddOn
        }
    }.partition(_._2.repository == siteRepo)

    // the version of the sbt feature it depends on
    val sbtFeatureVersion: Option[Version] =
      iu.dependencies.find(_.id == SbtFeatureIdOsgi).map { libDep => findStrictVersion(libDep.range) }

    // the version of the sbt feature it depends on

    val scalaFeature: Option[DependencyUnit] =
      iu.dependencies.find { d =>
        d.id match {
          case ScalaFeatureIdOsgiRegex() =>
            true
          case _ =>
            false
        }
      }

    new ScalaIDEVersion(
      iu,
      repository,
      scalaCompilerVersion,
      eclipseVersion,
      sbtFeatureVersion,
      scalaFeature,
      associatedExistingAddOns,
      associatedAvailableAddOns)
  }

  private def latestAssociated(addOns: Map[PluginDescriptor, Seq[AddOn]], filter: AddOn => Boolean): Map[PluginDescriptor, AddOn] = {
    addOns.flatMap {
      case (conf, addOns) =>
        addOns.filter(filter).headOption.map(conf -> _)
    }
  }
}

case class ScalaIDEVersion private (
  iu: InstallableUnit,
  repository: P2Repository,
  scalaVersion: Version,
  eclipseVersion: EclipseVersion,
  sbtFeatureVersion: Option[Version],
  scalaFeature: Option[DependencyUnit],
  associatedExistingAddOns: Map[PluginDescriptor, AddOn],
  associatedAvailableAddOns: Map[PluginDescriptor, AddOn]) {

  def version = iu.version

}
