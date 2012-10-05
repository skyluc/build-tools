package org.scalaide.buildtools

import java.net.MalformedURLException
import java.net.URL

import scala.collection.JavaConversions._
import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.collection.Map

import org.junit.Assert
import org.junit.Before

import org.junit.Test

import com.typesafe.config.ConfigFactory

import PluginDescriptor.Keys.categoryKey
import PluginDescriptor.Keys.documentationKey
import PluginDescriptor.Keys.issueTrackerKey
import PluginDescriptor.Keys.sourceFeatureIdKey
import PluginDescriptor.Keys.sourceRepoKey
import PluginDescriptor.Keys.updateSitesKey
import PluginDescriptor.Keys.websiteKey

class PluginDescriptorTest {

  import PluginDescriptor._
  import PluginDescriptor.Keys._

  private var defaultConfigValues: Map[String, Any] = _

  @Before
  def beforeTest() {
    defaultConfigValues = Map(
      sourceRepoKey -> "http://github.com/plugin",
      documentationKey -> "http://github.com/plugin/documentation",
      issueTrackerKey -> "http://github.com/plugin/issues",
      updateSitesKey -> List("http://github.com/plugin/download").asJava,
      sourceFeatureIdKey -> "org.scala-ide.sdt.feature")
  }

  @Test
  def websiteURLIsOptional() {
    val config = ConfigFactory.parseMap(defaultConfigValues)
    val descriptor = new PluginDescriptor("test", config)
    val websiteURL = descriptor.website
    Assert.assertTrue(websiteURL.isEmpty)
  }

  @Test(expected = classOf[MalformedURLException])
  def invalidWebsiteURL() {
    val config = ConfigFactory.parseMap(defaultConfigValues + (websiteKey -> "invalid url"))
    val descriptor = new PluginDescriptor("test", config)
  }

  @Test
  def websiteHttpURLIsOK() {
    val config = ConfigFactory.parseMap(defaultConfigValues + (websiteKey -> "http://scala-ide.org/"))
    val descriptor = new PluginDescriptor("test", config)
    val websiteURL = descriptor.website
    Assert.assertTrue(websiteURL.isDefined)
  }

  @Test
  def websiteHttpsURLIsOK() {
    val config = ConfigFactory.parseMap(defaultConfigValues + (websiteKey -> "https://scala-ide.org/"))
    val descriptor = new PluginDescriptor("test", config)
    val websiteURL = descriptor.website
    Assert.assertTrue(websiteURL.isDefined)
  }

  @Test
  def categoryIncubationIsOK() {
    val config = ConfigFactory.parseMap(defaultConfigValues + (categoryKey -> "incubation"))
    val descriptor = new PluginDescriptor("test", config)
    val category = descriptor.category
    Assert.assertTrue(category.get.name == "Incubation")
  }

  @Test
  def categoryStableIsOK() {
    val config = ConfigFactory.parseMap(defaultConfigValues + (categoryKey -> "stable"))
    val descriptor = new PluginDescriptor("test", config)
    val category = descriptor.category
    Assert.assertTrue(category.get.name == "Stable")
  }

  @Test
  def invalidCategory() {
    val config = ConfigFactory.parseMap(defaultConfigValues + (categoryKey -> "wrong"))
    val descriptor = new PluginDescriptor("test", config)
    val category = descriptor.category
    Assert.assertTrue(category.isEmpty)
  }

  @Test
  def loadPluginDescriptor() {
    val resource = PluginDescriptor.getClass().getResource("org.scala-ide.worksheet.feature")
    val file = new java.io.File(resource.toURI())
    PluginDescriptor.load(file) match {
      case Left(ex) => Assert.fail(ex.getMessage())
      case Right(descriptor) =>
        Assert.assertEquals(descriptor.sourceRepo, new URL("https://github.com/scala-ide/scala-worksheet"))
        Assert.assertEquals(descriptor.documentation, new URL("https://github.com/scala-ide/scala-worksheet/wiki/Getting-Started"))
        Assert.assertEquals(descriptor.issueTracker, new URL("https://github.com/scala-ide/scala-worksheet/issues/"))
        Assert.assertEquals(descriptor.updateSites, List(new URL("http://scala-ide.dreamhosters.com/nightly-update-worksheet-scalaide21-29/site/"), new URL("http://scala-ide.dreamhosters.com/nightly-update-worksheet-scalaide21-210/site/")))
        Assert.assertEquals(descriptor.category.get.name, "Incubation")
        Assert.assertEquals(descriptor.sourceFeatureId, "org.scala-ide.worksheet.source.feature")
    }
  }

  @Test(expected = classOf[FailedToRetrieveKeyOrValue])
  def loadFailsIfMandatoryKeyIsMissing() {
    val resource = PluginDescriptor.getClass().getResource("org.scala-ide.worksheet.feature-incorrect")
    val file = new java.io.File(resource.toURI())
    PluginDescriptor.load(file)
  }

  @Test(expected = classOf[FailedToRetrieveKeyOrValue])
  def loadFailsIfMandatoryValueIsMissing() {
    val config = ConfigFactory.parseMap(defaultConfigValues + (updateSitesKey -> Nil.asJava))
    val descriptor = new PluginDescriptor("test", config)
  }
}