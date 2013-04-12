package org.scalaide.buildtools

import org.junit.Test
import org.junit.Assert
import org.osgi.framework.Version

class RemoteRepositoryTest {
  val expectedIds = Map(
    "org.scala-ide.scala.compiler" -> new Version("2.10.0.v20120820-123254-M7-1ab4994990"),
    "org.scala-ide.scala.library" -> new Version("2.10.0.v20120820-123254-M7-1ab4994990"),
    "org.scala-ide.sdt.core" -> new Version("2.1.0.m2-2_10-201209130850-f6ab297"))

  @Test def milestonesTest {
    val p2repo = P2RepositoryOld.fromUrl("http://download.scala-ide.org/ecosystem/dev-milestone-milestone/site/")

    for {
      (id, version) <- expectedIds
      found <- p2repo.findIU(id)
    } {
      Assert.assertEquals("Couldn't find right version: " + found, version, found.version)
    }
  }

  @Test def httpErrorTest {
    val maybeRepo = P2RepositoryOld.fromUrl("http://download.scala-ide.org/nosuchrepository")

    Assert.assertTrue("Error downloading contents: " + maybeRepo, maybeRepo.isInstanceOf[ErrorP2RepositoryOld])
  }

}