package org.scalaide.buildtools

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
    val nextScalaIDEVersions = findScalaIDEVersions(nextBaseRepo, nextExistingAddOns, availableAddOns, nextSiteRepo, featureConfs)

    EcosystemBuild(
      ecosystemConf.id,
      baseRepo,
      baseScalaIDEVersions,
      nextBaseRepo,
      nextScalaIDEVersions,
      siteRepo,
      nextSiteRepo,
      existingAddOns,
      nextExistingAddOns)
  }

  private def findScalaIDEVersions(baseRepo: P2Repository, existingAddOns: Map[PluginDescriptor, Seq[AddOn]], availableAddOns: Map[PluginDescriptor, Seq[AddOn]], siteRepo: P2Repository, featureConfs: Seq[PluginDescriptor]): Seq[ScalaIDEVersion] = {
    baseRepo.findIU(ScalaIDEFeatureIdOsgi).toSeq.map(ScalaIDEVersion(_, baseRepo, existingAddOns, availableAddOns, siteRepo))
  }

}

case class EcosystemBuild(
  id: String,
  baseRepo: P2Repository,
  baseScalaIDEVersions: Seq[ScalaIDEVersion],
  nextRepo: P2Repository,
  nextScalaIDEVersions: Seq[ScalaIDEVersion],
  siteRepo: P2Repository,
  nextSiteRepo: P2Repository,
  existingAddOns: Map[PluginDescriptor, Seq[AddOn]],
  nextExistingAddOns: Map[PluginDescriptor, Seq[AddOn]]) {
  
  val regenerateEcosystem: Boolean = !baseScalaIDEVersions.forall(_.associatedAvailableAddOns.isEmpty)
  
  val regenerateNextEcosystem: Boolean = !nextScalaIDEVersions.forall(_.associatedAvailableAddOns.isEmpty)
  
  val zippedVersion: Option[ScalaIDEVersion] = baseScalaIDEVersions.sortBy(_.version).headOption
}