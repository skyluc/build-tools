package org.scalaide.buildtools

import org.junit.Test
import org.junit.Assert
import org.osgi.framework.Version
import scala.collection.immutable.TreeSet

class P2RepositoryTest {
  
  private def InstallableUnit(id: String, version: String, dependencies: Seq[DependencyUnit] = Nil): InstallableUnit = 
    new InstallableUnit(id, new Version(version), dependencies)

  @Test def testSimpleRepo() {
    val repoText = """
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
          </unit>
        </units>
      </repository>
"""
    val repo = P2Repository.fromString(repoText)
    
    val units = repo.findIU("org.scala-ide.scala.library")
    
    Assert.assertEquals("Scala Library not found", TreeSet(InstallableUnit("org.scala-ide.scala.library", "2.10.0.v20120924-042318-ffaa3cb89e")), units)
  }
  
    @Test def multipleMatchesAreOrderedByVersion() {
    val repoText = """
      <repository name='org.scala-ide.sdt.update-site' type='org.eclipse.equinox.internal.p2.metadata.repository.LocalMetadataRepository' version='1'>
        <properties size='2'>
          <property name='p2.timestamp' value='1348644444907'/>
          <property name='p2.compressed' value='true'/>
        </properties>
        <references size='4'>
        </references>
        <units size='28'>
          <unit id='org.scala-ide.scala.library' version='2.10.0.v20121024-050000-ffaa3cb89e' singleton='false'>
            <update id='org.scala-ide.scala.library' range='[0.0.0,2.10.0.v20121024-050000-ffaa3cb89e)' severity='0'/>
            <properties size='3'>
              <property name='org.eclipse.equinox.p2.name' value='Scala Library for Eclipse'/>
              <property name='org.eclipse.equinox.p2.description' value='Bundle containing the Scala library'/>
              <property name='org.eclipse.equinox.p2.provider' value='scala-ide.org'/>
            </properties>
            <artifacts>
              <artifact classifier='osgi.bundle'/>
            </artifacts>
            <provides size='71'/>
          </unit>
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
          </unit>
          <unit id='org.scala-ide.scala.library' version='2.10.0.v20121024-042318-ffaa3cb89e' singleton='false'>
            <update id='org.scala-ide.scala.library' range='[0.0.0,2.10.0.v20121024-042318-ffaa3cb89e)' severity='0'/>
            <properties size='3'>
              <property name='org.eclipse.equinox.p2.name' value='Scala Library for Eclipse'/>
              <property name='org.eclipse.equinox.p2.description' value='Bundle containing the Scala library'/>
              <property name='org.eclipse.equinox.p2.provider' value='scala-ide.org'/>
            </properties>
            <artifacts>
              <artifact classifier='osgi.bundle'/>
            </artifacts>
            <provides size='71'/>
          </unit>
        </units>
      </repository>
"""
    val repo = P2Repository.fromString(repoText)
    
    val units = repo.findIU("org.scala-ide.scala.library")
    
    val expected = TreeSet[InstallableUnit](
        InstallableUnit("org.scala-ide.scala.library", "2.10.0.v20121024-050000-ffaa3cb89e"),
        InstallableUnit("org.scala-ide.scala.library", "2.10.0.v20121024-042318-ffaa3cb89e"),
        InstallableUnit("org.scala-ide.scala.library", "2.10.0.v20120924-042318-ffaa3cb89e")
        )
    Assert.assertEquals("Scala Library not found", expected, units)
  }
}