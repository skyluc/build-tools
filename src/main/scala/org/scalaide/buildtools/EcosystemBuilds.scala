package org.scalaide.buildtools

object EcosystemBuilds {

  import Ecosystem._

  def apply(ecosystemConfs: Seq[EcosystemDescriptor], featureConfs: Seq[PluginDescriptor]): EcosystemBuilds = {
    val availableAddOns = findAvailableFeatures(featureConfs)
    new EcosystemBuilds(ecosystemConfs.map(c => EcosystemBuild(c, availableAddOns, featureConfs)), availableAddOns)
  }

  def findAvailableFeatures(featureConfs: Seq[PluginDescriptor]): Map[PluginDescriptor, Seq[AddOn]] = {
    featureConfs.map {
      f =>
        (f -> f.updateSites.flatMap(r => findAvailableFeatureVersionsFrom(f, RepositoriesOld(r))))
    }.toMap
  }

  def findAvailableFeaturesFrom(featureConfs: Seq[PluginDescriptor], repo: P2RepositoryOld): Map[PluginDescriptor, Seq[AddOn]] = {
    featureConfs.flatMap {
      f =>
        findAvailableFeatureVersionsFrom(f, repo) match {
          case Nil =>
            None
          case l =>
            Some((f -> l))
        }
    }.toMap
  }

  def findAvailableFeatureVersionsFrom(featureConf: PluginDescriptor, repository: P2RepositoryOld): Seq[AddOn] = {
    repository.findIU(featureConf.featureId + FeatureSuffix).toSeq.map(AddOn(featureConf, _, repository))
  }

}

case class EcosystemBuilds(ecosystems: Seq[EcosystemBuild], availableAddOns: Map[PluginDescriptor, Seq[AddOn]]) {}