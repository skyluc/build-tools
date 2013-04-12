package org.scalaide.buildtools

object MergeBasesBuild {
  import Ecosystem._

  def apply(ecosystemConfs: Seq[EcosystemDescriptor]): Seq[MergeBasesBuild] = {

    ecosystemConfs.map(extractBasesToMerge(_))

  }

  private def extractBasesToMerge(ecosystemConf: EcosystemDescriptor): MergeBasesBuild = {
    val baseRepo = RepositoriesOld(ecosystemConf.base)
    val nextBaseRepo = RepositoriesOld(ecosystemConf.nextBase)
    val baseScalaIDEVersions = getScalaIDEVersions(baseRepo)
    val nextBaseScalaIDEVersions = getScalaIDEVersions(nextBaseRepo)

    MergeBasesBuild(ecosystemConf.id, baseRepo, nextBaseRepo, nextBaseRepo.isValid && !nextBaseScalaIDEVersions.forall(baseScalaIDEVersions.contains(_)))
  }

  private def getScalaIDEVersions(repo: P2RepositoryOld) = repo.findIU(ScalaIDEFeatureIdOsgi).map(_.version)

}

case class MergeBasesBuild(
  id: String,
  baseRepo: P2RepositoryOld,
  nextBaseRepo: P2RepositoryOld,
  toMerge: Boolean)