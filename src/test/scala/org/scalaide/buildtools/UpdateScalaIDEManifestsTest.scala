package org.scalaide.buildtools

import org.junit.Test
import org.junit.Assert._
import java.io.File
import org.apache.commons.io.{ FileUtils => IOFileUtils }
import scala.collection.mutable.ListBuffer
import scala.io.Source

class UpdateScalaIDEManifestsTest {

  import Ecosystem._
  import TestSupport._

  @Test
  def releaseScalaVersion {
    val List(m2Repo, scalaIDERoot) = createTestEnv("m2repo-stub_01", "scala-ide-stub_01")

    new UpdateScalaIDEManifests("2.10.2", m2Repo, scalaIDERoot)()

    val sdtCoreManifest = new File(scalaIDERoot, "org.scala-ide.sdt.core/META-INF/MANIFEST.MF")
    val sdtDebugManifest = new File(scalaIDERoot, "org.scala-ide.sdt.debug/META-INF/MANIFEST.MF")

    checkRequiredVersionInManifest(sdtCoreManifest, ScalaLangCompilerId, "2.10.2.v20130530-074427-VFINAL-60d462ef6e")
    checkRequiredVersionInManifest(sdtCoreManifest, ScalaLangLibraryId, "2.10.2.v20130530-074427-VFINAL-60d462ef6e")
    checkRequiredVersionInManifest(sdtDebugManifest, ScalaLangCompilerId, "2.10.2.v20130530-074427-VFINAL-60d462ef6e")
    checkRequiredVersionInManifest(sdtDebugManifest, ScalaLangLibraryId, "2.10.2.v20130530-074427-VFINAL-60d462ef6e")
  }

  @Test
  def snapshotScalaVersion {
    val List(m2Repo, scalaIDERoot) = createTestEnv("m2repo-stub_01", "scala-ide-stub_01")

    new UpdateScalaIDEManifests("2.10.3-SNAPSHOT", m2Repo, scalaIDERoot)()

    val sdtCoreManifest = new File(scalaIDERoot, "org.scala-ide.sdt.core/META-INF/MANIFEST.MF")
    val sdtDebugManifest = new File(scalaIDERoot, "org.scala-ide.sdt.debug/META-INF/MANIFEST.MF")

    checkRequiredVersionInManifest(sdtCoreManifest, ScalaLangCompilerId, "2.10.3.v20130924-143159-9f62900145")
    checkRequiredVersionInManifest(sdtCoreManifest, ScalaLangLibraryId, "2.10.3.v20130924-143159-9f62900145")
    checkRequiredVersionInManifest(sdtDebugManifest, ScalaLangCompilerId, "2.10.3.v20130924-143159-9f62900145")
    checkRequiredVersionInManifest(sdtDebugManifest, ScalaLangLibraryId, "2.10.3.v20130924-143159-9f62900145")
  }


}