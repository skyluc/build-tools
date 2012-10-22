package org.scalaide.buildtools

import org.osgi.framework.Version

case class AddOn(conf: PluginDescriptor, iu: InstallableUnit, repository: P2Repository) {
  import Ecosystem._
  
  // TODO: need to check for missing information
  
  lazy val scalaIDEVersion: Version = findStrictVersion(findDependency(ScalaIDEFeatureIdOsgi).get.range)
  
  val version = iu.version
  
  def id= conf.featureId
  
  private def findDependency(id: String) = iu.dependencies.find(_.id == id)
  
  private def findStrictVersion(range: String) = {
    range match {
      case RangeRegex(low, high) if (low == high) =>
        new Version(low)
      case _ =>
        UndefinedVersion
    }
  }

}