package org.scalaide.buildtools

import org.osgi.framework.Version
import Ecosystem._

object ScalaIDEVersion {

  def apply(iu: InstallableUnitOld, repository: P2RepositoryOld, existingAddOns: Map[PluginDescriptor, Seq[AddOn]], availableAddOns: Map[PluginDescriptor, Seq[AddOn]], siteRepo: P2RepositoryOld): ScalaIDEVersion = {
    val (associatedExistingAddOns, associatedAvailableAddOns) = latestAssociated(availableAddOns, iu.version).foldLeft(latestAssociated(existingAddOns, iu.version)) {
      (acc, availableAddOn) =>
        acc.get(availableAddOn._1) match {
          case Some(existingAddOn) if (existingAddOn.version.compareTo(availableAddOn._2.version) >= 0) =>
            acc
          case _ =>
            acc + availableAddOn
        }
    }.partition(_._2.repository == siteRepo)

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
    val scalaVersion =
      sdtCore.dependencies.find(_.id == ScalaLibraryId)
        .map(libDep => findStrictVersion(libDep.range))
        .getOrElse(throw new Exception("failed to find the Scala version for %s".format(iu)))

    new ScalaIDEVersion(iu, repository, scalaVersion, eclipseVersion, associatedExistingAddOns, associatedAvailableAddOns)
  }

  private def latestAssociated(addOns: Map[PluginDescriptor, Seq[AddOn]], version: Version): Map[PluginDescriptor, AddOn] = {
    addOns.flatMap {
      case (conf, addOns) =>
        addOns.filter(_.scalaIDEVersion == version).headOption.map(conf -> _)
    }
  }
}

case class ScalaIDEVersion private (iu: InstallableUnitOld, repository: P2RepositoryOld, scalaVersion: Version, eclipseVersion: EclipseVersion, associatedExistingAddOns: Map[PluginDescriptor, AddOn], associatedAvailableAddOns: Map[PluginDescriptor, AddOn]) {

  def version = iu.version

}