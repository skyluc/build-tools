package org.scalaide.buildtools

import java.io.File

object EcosystemBuildsMavenProjects {
  
  private val artifactIdSuffix = Iterator.from(1)

  def generate(builds: EcosystemBuilds, targetFolder: File) {
    val buildFolder= new File(targetFolder, "builds")
    
    if (buildFolder.exists) {
      FileUtils.deleteFull(buildFolder)
    }
    buildFolder.mkdirs()
    
    val ecosystemFolders = builds.ecosystems.flatMap(generateEcosystemProjects(_, buildFolder))
    
    if (ecosystemFolders.isEmpty) {
      println("Nothing to do")
    } else {
      FileUtils.saveXml(new File(buildFolder, "pom.xml"), createTopPomXml(ecosystemFolders))
    }
    
  }
  
  def generateEcosystemProjects(ecosystemBuild: EcosystemBuild, buildFolder: File): Seq[File]= {
    val ecosystemFolder= new File(buildFolder, ecosystemBuild.id)
    
    val siteFolder = if (ecosystemBuild.regenerateEcosystem) {
      Some(generateEcosystemProject(ecosystemBuild.baseScalaIDEVersions, "%s-base".format(ecosystemBuild.id), ecosystemBuild.baseRepo, buildFolder))
    } else {
      None
    }
    
    val nextFolder = if (ecosystemBuild.regenerateNextEcosystem) {
      Some(generateEcosystemProject(ecosystemBuild.nextScalaIDEVersions, "%s-next".format(ecosystemBuild.id), ecosystemBuild.nextRepo, buildFolder))
    } else {
      None
    }
    
    Seq(siteFolder, nextFolder).flatten
  }
  
  def generateEcosystemProject(scalaIDEVersions: Seq[org.scalaide.buildtools.ScalaIDEVersion], tag: String, baseRepository: P2Repository, buildFolder: File): File = {
      val siteFolder= new File(buildFolder, tag)
      siteFolder.mkdirs()
      
      val artifactId = "ecosystem.%s".format(tag)
      
      val featureFolders = scalaIDEVersions.flatMap(generateScalaIDEProjects(_, artifactId, siteFolder))
      
      FileUtils.saveXml(new File(siteFolder, "pom.xml"), createEcosystemPomXml(artifactId, baseRepository, featureFolders))
      
      siteFolder
  }
  
  def generateScalaIDEProjects(scalaIDEVersion: ScalaIDEVersion, parentId: String, siteFolder: File): Seq[File] = {
    val scalaIDEFolder= new File(siteFolder, scalaIDEVersion.version.toString)
    scalaIDEFolder.mkdir()
    
    (scalaIDEVersion.associatedExistingAddOns ++ scalaIDEVersion.associatedAvailableAddOns).map {
      case (_, addOn) =>
        generateAddOnProject(addOn, scalaIDEVersion, parentId, scalaIDEFolder)
    }.toSeq
  }
  
  def generateAddOnProject(addOn: AddOn, scalaIDEVersion: ScalaIDEVersion, parentId: String, scalaIDEFolder: File): File = {
    val addOnFolder= new File(scalaIDEFolder, addOn.id)
    addOnFolder.mkdir()
    
    FileUtils.saveXml(new File(addOnFolder, "pom.xml"), createAddOnPomXml(addOn, scalaIDEVersion, parentId))
    FileUtils.saveXml(new File(addOnFolder, "site.xml"), createAddOnSiteXml(addOn))
    
    addOnFolder
  }
  
  def createTopPomXml(ecosystemFolders: Seq[File]) = {
    <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <modelVersion>4.0.0</modelVersion>
      <prerequisites>
        <maven>3.0</maven>
      </prerequisites>
      <groupId>org.scalaide</groupId>
      <artifactId>ecosystem.build</artifactId>
      <version>0.1.0-SNAPSHOT</version>
      <description>Build a repository containing multiple versions</description>
      <packaging>pom</packaging>
      <properties>
        <encoding>UTF-8</encoding>
        <tycho.version>0.15.0</tycho.version>
      </properties>
      <modules>
        {
          ecosystemFolders.map { f =>
            <module>{ f.getName }</module>
          }
        }
      </modules>
    </project>
  }



  def createEcosystemPomXml(artifactId: String, baseRepository: P2Repository, featureFolders: Seq[File]) = {
    <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <modelVersion>4.0.0</modelVersion>
      <prerequisites>
        <maven>3.0</maven>
      </prerequisites>
      <parent>
        <groupId>org.scalaide</groupId>
        <artifactId>ecosystem.build</artifactId>
        <version>0.1.0-SNAPSHOT</version>
      </parent>
      <artifactId>{ artifactId }</artifactId>
      <version>0.1.0-SNAPSHOT</version>
      <description>Build a repository containing multiple versions</description>
      <packaging>pom</packaging>
      <properties>
        <encoding>UTF-8</encoding>
        <tycho.version>0.15.0</tycho.version>
      </properties>
      <profiles>
        <profile>
          <id>package</id>
          <build>
            <plugins>
              <plugin>
                <groupId>org.eclipse.tycho.extras</groupId>
                <artifactId>tycho-p2-extras-plugin</artifactId>
                <version>${{tycho.version}}</version>
                <executions>
                  <execution>
                    <phase>package</phase>
                    <goals>
                      <goal>mirror</goal>
                    </goals>
                  </execution>
                </executions>
                <configuration>
                  <source>
                    <repository>
                      <url>{ baseRepository.location }</url>
                      <layout>p2</layout>
                    </repository>
                    {
                      featureFolders.map { f =>
                        <repository>
                          <url>{ new File(f, "target/site").toURI() }</url>
                          <layout>p2</layout>
                        </repository>

                      }
                    }
                  </source>
                  <destination>${{project.build.directory}}/site</destination>
                </configuration>
              </plugin>
            </plugins>
          </build>
        </profile>
        <profile>
          <id>build</id>
          <modules>
            {
              featureFolders.map { f =>
                <module>{ "%s/%s".format(f.getParentFile.getName, f.getName) }</module>
              }
            }
          </modules>
        </profile>
      </profiles>
    </project>
  }
  
  
  def createAddOnPomXml(addOn: AddOn, scalaIDEVersion: ScalaIDEVersion, parentId: String) = {
    // TODO: support for non-indigo build
    <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <modelVersion>4.0.0</modelVersion>
      <prerequisites>
        <maven>3.0</maven>
      </prerequisites>
      <parent>
        <groupId>org.scalaide</groupId>
        <artifactId>{ parentId }</artifactId>
        <version>0.1.0-SNAPSHOT</version>
        <relativePath>../..</relativePath>
      </parent>
      <artifactId>org.scala-ide.ecosystem.generated{ artifactIdSuffix.next }</artifactId>
      <version>0.1.0-SNAPSHOT</version>
      <description>Build project for the scalatest support in Scala IDE</description>
      <packaging>eclipse-update-site</packaging>
      <properties>
        <encoding>UTF-8</encoding>
        <tycho.version>0.15.0</tycho.version>
        <!-- p2 repositories location -->
        <repo.eclipse.indigo>http://download.eclipse.org/releases/indigo/</repo.eclipse.indigo>
        <!-- dependencies repos -->
        <eclipse.codename>indigo</eclipse.codename>
      </properties>
      <repositories>
        <repository>
          <id>eclipse.indigo</id>
          <name>Eclipse p2 repository</name>
          <layout>p2</layout>
          <url>http://download.eclipse.org/releases/indigo/</url>
        </repository>
        <repository>
          <id>scalaide.repo</id>
          <layout>p2</layout>
          <url>{ scalaIDEVersion.repository.location }</url>
        </repository>
        <repository>
          <id>feature.repo</id>
          <layout>p2</layout>
          <url>{ addOn.repository.location }</url>
        </repository>
      </repositories>
      <build>
        <plugins>
          <plugin>
            <groupId>org.eclipse.tycho</groupId>
            <artifactId>tycho-maven-plugin</artifactId>
            <version>${{tycho.version}}</version>
            <extensions>true</extensions>
          </plugin>
          <plugin>
            <groupId>org.eclipse.tycho</groupId>
            <artifactId>tycho-packaging-plugin</artifactId>
            <version>${{tycho.version}}</version>
            <configuration>
              <archiveSite>true</archiveSite>
            </configuration>
          </plugin>
        </plugins>
      </build>
    </project>
  }
  
  def createAddOnSiteXml(addOn: AddOn)  = {
    // TODO: fix category
    // TODO: fix sourceFeatureId, should be option
    <site>
      { featureXml(addOn.conf.featureId, addOn.version.toString, "incubation") }
      { featureXml(addOn.conf.sourceFeatureId, addOn.version.toString, "source") }
      <category-def name="stable" label="Scala IDE plugins"/>
      <category-def name="incubation" label="Scala IDE plugins (incubation)"/>
      <category-def name="source" label="Sources"/>
    </site>
  }
    
  private def featureXml(id: String, version: String, category: String) = {
    <feature url={ "features/" + id + "_0.0.0.jar" } id={ id } version={ version }>
      <category name={ category }/>
    </feature>
  }
  
}