package org.scalaide.buildtools

import org.junit.Test
import org.junit.Assert._
import org.osgi.framework.Version
import scala.collection.immutable.TreeSet
import FakeURLStreamHandlerTest._
import scala.collection.immutable.SortedSet
import scala.concurrent.Await
import scala.concurrent.duration._
import org.osgi.framework.VersionRange
import java.io.ByteArrayOutputStream
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry
import org.junit.Before

class P2RepositoryTest {

  final val ContentXMLContent = """
      <repository name='org.scala-ide.sdt.update-site' type='org.eclipse.equinox.internal.p2.metadata.repository.LocalMetadataRepository' version='1'>
        <properties size='2'>
          <property name='p2.timestamp' value='1348644444907'/>
          <property name='p2.compressed' value='true'/>
        </properties>
        <references size='4'>
        </references>
        <units size='28'>
          <unit id='org.scala-ide.scala.library' version='2.10.0.v20120924-042318-ffaa3cb89e' singleton='false'>
            <update id='org.scala-ide.scala.library' range='[0.0.0,2.10.0.v20120924-042318-ffaa3cb89e)' severity='0'/>
            <properties size='3'>
              <property name='org.eclipse.equinox.p2.name' value='Scala Library for Eclipse'/>
              <property name='org.eclipse.equinox.p2.description' value='Bundle containing the Scala library'/>
              <property name='org.eclipse.equinox.p2.provider' value='scala-ide.org'/>
            </properties>
            <artifacts>
              <artifact classifier='osgi.bundle'/>
            </artifacts>
            <provides size='71'/>
            <requires size='34'>
              <required namespace='org.eclipse.equinox.p2.iu' name='scalariform' range='[0.1.4.201303191507-12ea0f8,0.1.4.201303191507-12ea0f8]'/>
            </requires>
          </unit>
        </units>
      </repository>
""".getBytes()

  final val CompositeXMLContent = """<?xml version='1.0' encoding='UTF-8'?>
<?compositeMetadataRepository version='1.0.0'?>
<repository name='Test composite'
        type='org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository' version='1.0.0'>
      <properties size='1'>
        <property name='p2.timestamp' value='1243822502499'/>
      </properties>
      <children size='1'>
        <child location='fake:/test/content/'/>
      </children>
    </repository>
""".getBytes()

  @Before
  def clearCaches {
    FakeURLStreamHandler.deregisterAll()
    P2Repositories.clear()
  }

  @Test
  def repositoryWithContent {
    val path = "/test/site/content.xml"
    FakeURLStreamHandler.register(path, ContentXMLContent)

    val repository = Await.result(P2Repositories(createFakeURL("/test/site/")), 5.second)

    val units = repository.findIU("org.scala-ide.scala.library")

    assertEquals("Right Scala Library not found", SortedSet(createInstallableUnit("org.scala-ide.scala.library", "2.10.0.v20120924-042318-ffaa3cb89e", List(createDependencyUnit("scalariform", "0.1.4.201303191507-12ea0f8")))), units)
  }

  @Test
  def repositoryWithContentZippedFile {
    val zippedContent = createZippedContent("content.xml", ContentXMLContent)

    val path = "/test/site/content.jar"
    FakeURLStreamHandler.register(path, zippedContent)

    val repository = Await.result(P2Repositories(createFakeURL("/test/site/")), 5.second)

    val units = repository.findIU("org.scala-ide.scala.library")

    assertEquals("Right Scala Library not found", SortedSet(createInstallableUnit("org.scala-ide.scala.library", "2.10.0.v20120924-042318-ffaa3cb89e", List(createDependencyUnit("scalariform", "0.1.4.201303191507-12ea0f8")))), units)
  }

  @Test
  def combinedRepository {
    FakeURLStreamHandler.register("/test/content/content.xml", ContentXMLContent)
    FakeURLStreamHandler.register("/test/composite/compositeContent.xml", CompositeXMLContent)

    val repository = Await.result(P2Repositories(createFakeURL("/test/composite/")), 5.second)

    val units = repository.findIU("org.scala-ide.scala.library")

    assertEquals("Right Scala Library not found", SortedSet(createInstallableUnit("org.scala-ide.scala.library", "2.10.0.v20120924-042318-ffaa3cb89e", List(createDependencyUnit("scalariform", "0.1.4.201303191507-12ea0f8")))), units)
  }

  @Test
  def combinedRepositoryZippedFile {
    FakeURLStreamHandler.register("/test/content/content.jar", createZippedContent("content.xml", ContentXMLContent))
    FakeURLStreamHandler.register("/test/composite/compositeContent.jar", createZippedContent("compositeContent.xml", CompositeXMLContent))

    val repository = Await.result(P2Repositories(createFakeURL("/test/composite/")), 5.second)

    val units = repository.findIU("org.scala-ide.scala.library")

    assertEquals("Right Scala Library not found", SortedSet(createInstallableUnit("org.scala-ide.scala.library", "2.10.0.v20120924-042318-ffaa3cb89e", List(createDependencyUnit("scalariform", "0.1.4.201303191507-12ea0f8")))), units)
  }

  @Test
  def invalidRepositoryURL {
    val repository = Await.result(P2Repositories(createFakeURL("/test/site/")), 5.second)

    assertEquals("Unexpected repository", InvalidP2Repository, repository)
  }

  private def createInstallableUnit(id: String, version: String, dependencies: List[DependencyUnit] = Nil): InstallableUnit =
    new InstallableUnit(id, new Version(version), dependencies)

  private def createDependencyUnit(id: String, versionString: String): DependencyUnit = {
    val version = new Version(versionString)
    new DependencyUnit(id, new VersionRange(VersionRange.LEFT_CLOSED, version, version, VersionRange.RIGHT_CLOSED))
  }

  private def createZippedContent(fileName: String, fileContent: Array[Byte]): Array[Byte] = {
    val buffer = new ByteArrayOutputStream()
    val zip = new ZipOutputStream(buffer)

    val zipEntry = new ZipEntry(fileName)

    zip.putNextEntry(zipEntry)
    zip.write(fileContent)

    zip.close()
    buffer.toByteArray()
  }

}