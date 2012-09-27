package org.scalaide.buildtools

import org.junit.Test
import org.junit.Assert

class P2RepositoryTest {

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
            <provides size='71'/>
          </unit>
        </units>
      </repository>
"""
    val repo = P2Repository.fromString(repoText)
    
    val units = repo.findIU("org.scala-ide.scala.library")
    
    Assert.assertEquals("Scala Library not found", Seq(InstallableUnit("org.scala-ide.scala.library", "2.10.0.v20120924-042318-ffaa3cb89e")), units)
  }
  
    @Test def testMultipleMatches() {
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
            <provides size='71'/>
          </unit>
          <unit id='org.scala-ide.scala.compiler' version='2.10.0.v20120924-042318-ffaa3cb89e' singleton='false'>
            <update id='org.scala-ide.scala.library' range='[0.0.0,2.10.0.v20120924-042318-ffaa3cb89e)' severity='0'/>
            <properties size='3'>
              <property name='org.eclipse.equinox.p2.name' value='Scala Library for Eclipse'/>
              <property name='org.eclipse.equinox.p2.description' value='Bundle containing the Scala library'/>
              <property name='org.eclipse.equinox.p2.provider' value='scala-ide.org'/>
            </properties>
            <provides size='71'/>
          </unit>
          <unit id='org.scala-ide.sdt.core' version='2.10.0.v20120924-042318-ffaa3cb89e' singleton='false'>
            <update id='org.scala-ide.scala.library' range='[0.0.0,2.10.0.v20120924-042318-ffaa3cb89e)' severity='0'/>
            <properties size='3'>
              <property name='org.eclipse.equinox.p2.name' value='Scala Library for Eclipse'/>
              <property name='org.eclipse.equinox.p2.description' value='Bundle containing the Scala library'/>
              <property name='org.eclipse.equinox.p2.provider' value='scala-ide.org'/>
            </properties>
            <provides size='71'/>
          </unit>
        </units>
      </repository>
"""
    val repo = P2Repository.fromString(repoText)
    
    val units = repo.findIU("org.scala-ide.*")
    
    Assert.assertEquals("Scala Library not found", Seq(
        InstallableUnit("org.scala-ide.scala.library", "2.10.0.v20120924-042318-ffaa3cb89e"),
        InstallableUnit("org.scala-ide.scala.compiler", "2.10.0.v20120924-042318-ffaa3cb89e"),
        InstallableUnit("org.scala-ide.sdt.core", "2.10.0.v20120924-042318-ffaa3cb89e")
        ), units)
  }
}