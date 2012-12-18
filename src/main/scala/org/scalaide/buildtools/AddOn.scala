package org.scalaide.buildtools

import org.osgi.framework.Version
import Ecosystem._

case class AddOn(conf: PluginDescriptor, iu: InstallableUnit, repository: P2Repository) {
  
  // TODO: need to check for missing information
  
  lazy val scalaIDEVersion: Version = findStrictVersion(findDependency(ScalaIDEFeatureIdOsgi).get.range)
  
  val version = iu.version
  
  def id= conf.featureId
  
  private def findDependency(id: String) = iu.dependencies.find(_.id == id)
  
}