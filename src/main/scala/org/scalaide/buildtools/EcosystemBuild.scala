package org.scalaide.buildtools

import org.osgi.framework.Version

object EcosystemBuild {

  import Ecosystem._

  def apply(ecosystemConf: EcosystemDescriptor, availableAddOns: Map[PluginDescriptor, Seq[AddOn]], featureConfs: Seq[PluginDescriptor]): EcosystemBuild = {

    val siteRepo = Repositories(ecosystemConf.site)
    val nextSiteRepo = Repositories(ecosystemConf.nextSite)
    val existingAddOns = EcosystemBuilds.findAvailableFeaturesFrom(featureConfs, siteRepo)
    val nextExistingAddOns = EcosystemBuilds.findAvailableFeaturesFrom(featureConfs, nextSiteRepo)
    val baseRepo = Repositories(ecosystemConf.base)
    val baseScalaIDEVersions = findScalaIDEVersions(baseRepo, existingAddOns, availableAddOns, siteRepo, featureConfs)
    val nextBaseRepo = Repositories(ecosystemConf.nextBase)
    val nextBaseScalaIDEVersions = findScalaIDEVersions(nextBaseRepo, nextExistingAddOns, availableAddOns, nextSiteRepo, featureConfs)
    val nextSiteScalaIDEVersions = siteRepo.findIU(ScalaIDEFeatureIdOsgi)

    EcosystemBuild(
      ecosystemConf.id,
      baseRepo,
      baseScalaIDEVersions,
      nextBaseRepo,
      nextBaseScalaIDEVersions,
      siteRepo,
      nextSiteRepo,
      existingAddOns,
      nextExistingAddOns,
      shouldBeRegenerated(baseScalaIDEVersions, siteRepo.findIU(ScalaIDEFeatureIdOsgi)),
      shouldBeRegenerated(nextBaseScalaIDEVersions, nextSiteRepo.findIU(ScalaIDEFeatureIdOsgi)))
  }

  private def findScalaIDEVersions(baseRepo: P2Repository, existingAddOns: Map[PluginDescriptor, Seq[AddOn]], availableAddOns: Map[PluginDescriptor, Seq[AddOn]], siteRepo: P2Repository, featureConfs: Seq[PluginDescriptor]): Seq[ScalaIDEVersion] = {
    baseRepo.findIU(ScalaIDEFeatureIdOsgi).toSeq.map(ScalaIDEVersion(_, baseRepo, existingAddOns, availableAddOns, siteRepo))
  }

  private def shouldBeRegenerated(baseScalaIDEVersions: Seq[ScalaIDEVersion], siteScalaIDEVersions: Set[InstallableUnit]): Boolean = {
    if (!baseScalaIDEVersions.forall(_.associatedAvailableAddOns.isEmpty)) {
      true
    } else {
      val baseVersions: Set[Version] = baseScalaIDEVersions.map(_.version).toSet
      val siteVersions: Set[Version] = siteScalaIDEVersions.map(_.version).toSet
      baseVersions != siteVersions
    }
  }

}

case class EcosystemBuild(
  id: String,
  baseRepo: P2Repository,
  baseScalaIDEVersions: Seq[ScalaIDEVersion],
  nextBaseRepo: P2Repository,
  nextBaseScalaIDEVersions: Seq[ScalaIDEVersion],
  siteRepo: P2Repository,
  nextSiteRepo: P2Repository,
  existingAddOns: Map[PluginDescriptor, Seq[AddOn]],
  nextExistingAddOns: Map[PluginDescriptor, Seq[AddOn]],
  val regenerateEcosystem: Boolean,
  val regenerateNextEcosystem: Boolean) {

  val zippedVersion: Option[ScalaIDEVersion] = baseScalaIDEVersions.sortBy(_.version).lastOption
}