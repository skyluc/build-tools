package org.scalaide.buildtools

import org.osgi.framework.Version

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

    new ScalaIDEVersion(iu, repository, associatedExistingAddOns, associatedAvailableAddOns)
  }

  private def latestAssociated(addOns: Map[PluginDescriptor, Seq[AddOn]], version: Version): Map[PluginDescriptor, AddOn] = {
    addOns.flatMap {
      case (conf, addOns) =>
        addOns.filter(_.scalaIDEVersion == version).headOption.map(conf -> _)
    }
  }
}

case class ScalaIDEVersion private (iu: InstallableUnit, repository: P2Repository, associatedExistingAddOns: Map[PluginDescriptor, AddOn], associatedAvailableAddOns: Map[PluginDescriptor, AddOn]) {

  def version = iu.version

}