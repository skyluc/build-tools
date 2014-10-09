package org.scalaide.buildtools

import org.osgi.framework.Version
import Ecosystem._

case class AddOn(conf: PluginDescriptor, iu: InstallableUnit, repository: P2Repository) {

  // TODO: need to check for missing information

  lazy val scalaIDEVersion: Version = findStrictVersion(scalaIDEVersionRange)

  lazy val scalaIDEVersionRange: VersionRange = VersionRange(findDependency(ScalaIDEFeatureIdOsgi).get.range)

  lazy val scalaCompilerVersion: Option[Version] = findSecondLevelTransitiveDependencyVersionRange(ScalaLangCompilerId).map(findStrictVersion(_))
  
  lazy val scalaLibraryVersionRange: Option[VersionRange] = findSecondLevelTransitiveDependencyVersionRange(ScalaLangLibraryId)

  val version = iu.version

  def id = conf.featureId

  private def findDependency(id: String) = iu.dependencies.find(_.id == id)

  /** Looks for a dependency on a bundle with the given `id`, at the second level.
   *  Doesn't look through feature dependencies.
   *  Returns [[None]] if the dependency is not found. Throws if there are multiple
   *
   *  In practice, look at the dependencies of the bundles of a feature.
   */
  private def findSecondLevelTransitiveDependencyVersionRange(id: String): Option[VersionRange] = {
    val matchingBundleVersions = for {
      dep1 <- iu.dependencies if !dep1.id.contains(".feature.feature.")
      version1 = findStrictVersion(dep1.range)
      iu1 <- repository.findIU(dep1.id) if iu1.version == version1
      dep2 <- iu1.dependencies if dep2.id == id
    } yield {
      VersionRange(dep2.range)
    }

    matchingBundleVersions.distinct.filterNot(_ == UndefinedRange) match {
      case Nil =>
        None
      case res :: Nil =>
        Some(res)
      case _ =>
        throw new Exception(s"more than one version found for $id in the tree of ${iu.id}")
    }
  }
}
