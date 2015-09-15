package org.scalaide.buildtools

import org.junit.Test
import org.junit.Assert._
import org.osgi.framework.Version
import Ecosystem._

class EcosystemTest {

  @Test
  def findStrictVersionOK {
    val actual = Ecosystem.findStrictVersion("[2.0.1.abc,2.0.1.abc]")
    assertEquals("Wrong version", new Version(2, 0, 1, "abc"), actual)
  }

  @Test
  def findStrictVersionDifferentVersions {
    val actual = Ecosystem.findStrictVersion("[2.0.1.abc,2.1.0.bcd]")
    assertEquals("Unexpected version", Ecosystem.UndefinedVersion, actual)
  }

  @Test
  def findStrictVersionNoVersion {
    val actual = Ecosystem.findStrictVersion("")
    assertEquals("Unexpected version", Ecosystem.UndefinedVersion, actual)
  }
  
  @Test
  def findStrictVersionNotStrict {
    val actual = Ecosystem.findStrictVersion("2.0.1.abc")
    assertEquals("Unexpected version", Ecosystem.UndefinedVersion, actual)
  }
  
  @Test
  def findStrictVersionWithoutClassifier {
    val actual = Ecosystem.findStrictVersion("2.0.1")
    assertEquals("Unexpected version", Ecosystem.UndefinedVersion, actual)
  }

  @Test
  def parseVersionRangeSingleVersion {
    val actual = VersionRange("4.2.0")
    assertEquals("Unexpected range", VersionRange(new Version("4.2.0"), true, MaxVersion, true), actual)
  }
}
