package org.scalaide.buildtools

import scala.collection.mutable.ListBuffer
import org.apache.commons.io.{ FileUtils => IOFileUtils }
import java.io.File
import scala.io.Source
import org.junit.Assert._
import scala.util.Random

object TestSupport {
  
  @volatile var testNb = 0
  
  cleanTmpFolders()

  final val resourcesFolder = new File(System.getProperty("user.dir"), "src/test/resources")
  
  private def cleanTmpFolders() {
    val tmpFolder = new File("target/tmp-test/")
    IOFileUtils.deleteDirectory(tmpFolder)
  }

  /** not thread safe */
  def createTestEnv(baseFolders: String*): List[File] = {
    val tmpFolder = new File("target/tmp-test/" + testNb)
    testNb += 1

    tmpFolder.mkdirs()

    val result = ListBuffer[File]()

    for (folder <- baseFolders) {
      val newFolder = new File(tmpFolder, folder)
      IOFileUtils.copyDirectory(new File(resourcesFolder, folder), newFolder)
      result.append(newFolder)
    }

    result.toList
  }
  
  def checkRequiredVersionInManifest(file: File, bundleId: String, version: String) {
    val s = Source.fromFile(file, "UTF-8")

    val lines = s.getLines.filter(_.contains(bundleId)).toList

    assertEquals("Too many lines with " + bundleId, 1, lines.size)
    lines.foreach { l =>
      assertEquals("Invalid required bundle line", s""" ${bundleId};bundle-version="[${version},${version}]",""", l)
    }
  }
  
  def checkRequireVersionInFeatureXml(file: File, featureId: String, version: String) {
    val s = Source.fromFile(file, "UTF-8")
    
    val lines = s .getLines.filter(_.contains(featureId)).toList
    
    assertEquals("Too many lines with " + featureId, 1, lines.size)
    lines.foreach { l =>
      assertEquals("Invalid required feature line", s"""<import match="perfect" version="${version}" feature="${featureId}"/>""", l.trim)
    }
  }
  
}