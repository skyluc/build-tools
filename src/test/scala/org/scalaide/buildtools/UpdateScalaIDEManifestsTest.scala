package org.scalaide.buildtools

import org.junit.Test
import org.junit.Assert._
import java.io.File
import org.apache.commons.io.{ FileUtils => IOFileUtils }
import scala.collection.mutable.ListBuffer
import scala.io.Source

object TestData {

  final val resourcesFolder = new File(System.getProperty("user.dir"), "src/test/resources")
}

class UpdateScalaIDEManifestsTest {

  import TestData._
  import Ecosystem._

  @Test
  def releaseScalaVersion {
    val List(m2Repo, scalaIDERoot) = createTestEnv("m2repo-stub_01", "scala-ide-stub_01")

    new UpdateScalaIDEManifests("2.10.2", m2Repo, scalaIDERoot)()

    val sdtCoreManifest = new File(scalaIDERoot, "org.scala-ide.sdt.core/META-INF/MANIFEST.MF")
    val sdtDebugManifest = new File(scalaIDERoot, "org.scala-ide.sdt.debug/META-INF/MANIFEST.MF")

    checkRequiredVersion(sdtCoreManifest, ScalaLangCompilerId, "2.10.2.v20130530-074427-VFINAL-60d462ef6e")
    checkRequiredVersion(sdtCoreManifest, ScalaLangLibraryId, "2.10.2.v20130530-074427-VFINAL-60d462ef6e")
    checkRequiredVersion(sdtDebugManifest, ScalaLangCompilerId, "2.10.2.v20130530-074427-VFINAL-60d462ef6e")
    checkRequiredVersion(sdtDebugManifest, ScalaLangLibraryId, "2.10.2.v20130530-074427-VFINAL-60d462ef6e")
  }

  @Test
  def snapshotScalaVersion {
    val List(m2Repo, scalaIDERoot) = createTestEnv("m2repo-stub_01", "scala-ide-stub_01")

    new UpdateScalaIDEManifests("2.10.3-SNAPSHOT", m2Repo, scalaIDERoot)()

    val sdtCoreManifest = new File(scalaIDERoot, "org.scala-ide.sdt.core/META-INF/MANIFEST.MF")
    val sdtDebugManifest = new File(scalaIDERoot, "org.scala-ide.sdt.debug/META-INF/MANIFEST.MF")

    checkRequiredVersion(sdtCoreManifest, ScalaLangCompilerId, "2.10.3.v20130924-143159-9f62900145")
    checkRequiredVersion(sdtCoreManifest, ScalaLangLibraryId, "2.10.3.v20130924-143159-9f62900145")
    checkRequiredVersion(sdtDebugManifest, ScalaLangCompilerId, "2.10.3.v20130924-143159-9f62900145")
    checkRequiredVersion(sdtDebugManifest, ScalaLangLibraryId, "2.10.3.v20130924-143159-9f62900145")
  }

  private def checkRequiredVersion(file: File, bundleId: String, version: String) {
    val s = Source.fromFile(file, "UTF-8")

    val lines = s.getLines.filter(_.contains(bundleId)).toList

    assertEquals("Too many lines with " + bundleId, 1, lines.size)
    lines.foreach { l =>
      assertEquals("Invalid required bundle line", s""" ${bundleId};bundle-version="[${version},${version}]",""", l)
    }

  }

  private def createTestEnv(baseFolders: String*): List[File] = {
    val tmpFolder = new File("target/tmp-test")

    tmpFolder.mkdirs()
    IOFileUtils.cleanDirectory(tmpFolder)

    val result = ListBuffer[File]()

    for (folder <- baseFolders) {
      val newFolder = new File(tmpFolder, folder)
      IOFileUtils.copyDirectory(new File(resourcesFolder, folder), newFolder)
      result.append(newFolder)
    }

    result.toList
  }

}