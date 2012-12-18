package org.scalaide.buildtools

import org.osgi.framework.Version
import Ecosystem._

object ScalaIDEVersion {

  def apply(iu: InstallableUnit, repository: P2Repository, existingAddOns: Map[PluginDescriptor, Seq[AddOn]], availableAddOns: Map[PluginDescriptor, Seq[AddOn]], siteRepo: P2Repository): ScalaIDEVersion = {
    val (associatedExistingAddOns, associatedAvailableAddOns) = latestAssociated(availableAddOns, iu.version).foldLeft(latestAssociated(existingAddOns, iu.version)) {
      (acc, availableAddOn) =>
        acc.get(availableAddOn._1) match {
          case Some(existingAddOn) if (existingAddOn.version.compareTo(availableAddOn._2.version) >= 0) =>
            acc
          case _ =>
            acc + availableAddOn
        }
    }.partition(_._2.repository == siteRepo)
    
    val eclipseVersion = (for {
       sdtCoreDep <- iu.dependencies.find(_.id == ScalaIDEId)
       sdtCoreVersion = findStrictVersion(sdtCoreDep.range)
       sdtCore <- repository.findIU(ScalaIDEId).find(_.version == sdtCoreVersion)
       jdtCoreDep <- sdtCore.dependencies.find(_.id == JDTId)
       eclipseVersion <- EclipseVersion(jdtCoreDep.range)
    } yield eclipseVersion) match {
      case Some(version) =>
        version
      case None =>
        throw new Exception("failed to find the Eclipse version for %s".format(iu))
    }
    
    new ScalaIDEVersion(iu, repository, eclipseVersion, associatedExistingAddOns, associatedAvailableAddOns)
  }

  private def latestAssociated(addOns: Map[PluginDescriptor, Seq[AddOn]], version: Version): Map[PluginDescriptor, AddOn] = {
    addOns.flatMap {
      case (conf, addOns) =>
        addOns.filter(_.scalaIDEVersion == version).headOption.map(conf -> _)
    }
  }
}

case class ScalaIDEVersion private (iu: InstallableUnit, repository: P2Repository, eclipseVersion: EclipseVersion, associatedExistingAddOns: Map[PluginDescriptor, AddOn], associatedAvailableAddOns: Map[PluginDescriptor, AddOn]) {

  def version = iu.version

}