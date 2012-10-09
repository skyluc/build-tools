package org.scalaide.buildtools

import org.junit.Test
import org.osgi.framework.Version
import org.junit.Assert

class VersionTest {
  @Test def versionIsIdenticalBeforeAndAfterBoxing() {
    val rawVersion = "2.10.0.v20120924-042318-ffaa3cb89e"
    val version = new Version(rawVersion)
    Assert.assertEquals(rawVersion, version.toString)
  }

  @Test def versionMajorNumber() {
    val rawVersion = "2.10.0.v20120924-042318-ffaa3cb89e"
    val version = new Version(rawVersion)
    Assert.assertEquals(version.getMajor(), 2)
  }

  @Test def versionMinorNumber() {
    val rawVersion = "2.10.0.v20120924-042318-ffaa3cb89e"
    val version = new Version(rawVersion)
    Assert.assertEquals(version.getMinor(), 10)
  }
  
  @Test def versionMicroNumber() {
    val rawVersion = "2.10.0.v20120924-042318-ffaa3cb89e"
    val version = new Version(rawVersion)
    Assert.assertEquals(version.getMicro(), 0)
  }
  
  @Test def versionQualifier() {
    val rawVersion = "2.10.0.v20120924-042318-ffaa3cb89e"
    val version = new Version(rawVersion)
    Assert.assertEquals(version.getQualifier(), "v20120924-042318-ffaa3cb89e")
  }
  
  @Test def comparingVersionsWithDifferentQualifier() {
    val beforeVersion = new Version("2.10.0.v20120924-042318-ffaa3cb89e")
    val afterVersion = new Version("2.10.0.v20120924-050020-ffaa3cb89e")
    Assert.assertTrue(beforeVersion.compareTo(afterVersion) < 0)
  } 
}