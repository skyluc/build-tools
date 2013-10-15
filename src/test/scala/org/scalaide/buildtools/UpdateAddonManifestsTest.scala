package org.scalaide.buildtools

import org.junit.Test
import scala.io.Source
import java.io.File

class UpdateAddonManifestsTest {
  
  import TestSupport._
  import Ecosystem._
  
  @Test
  def setVersionsTest() {
    val List(scalaIDESite, addonRoot) = createTestEnv("site_scala-ide-stub_01", "addon-stub_01")
    
    new UpdateAddonManifests(scalaIDESite.toURI().toASCIIString(), addonRoot.getAbsolutePath())()
    
    val scalaVersion = "2.10.3.v20130923-060037-VFINAL-e2fec6b28d"
      
    val scalaIDEVersion = "3.0.2.rc02-2_10-201310081317-0880865"
      
    val addonManifest = new File(addonRoot, "com.example.plugin/" + PluginManifest)
    val addonFeatureXml = new File(addonRoot, "com.example.feature/" + FeatureDescriptor)
    
    checkRequiredVersionInManifest(addonManifest, ScalaLangLibraryId, scalaVersion)
    checkRequiredVersionInManifest(addonManifest, ScalaLangCompilerId, scalaVersion)
    checkRequiredVersionInManifest(addonManifest, ScalaIDEId, scalaIDEVersion)
    
    checkRequireVersionInFeatureXml(addonFeatureXml, ScalaIDEFeatureId, scalaIDEVersion)
    
  }

}