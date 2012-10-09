package org.scalaide.buildtools

import java.io.File
import java.net.URL
import scala.xml.Elem
import scala.xml.XML

object MavenProject2 {
  
  val artifactIdSuffix= Iterator.from(1)

  private def saveXml(file: File, xml: Elem) {
    XML.save(file.getAbsolutePath(), xml, "UTF-8", true)
  }

  def generateEcosystemsProjects(ecosystemToScalaIDEToAvailableFeatures: Map[EcosystemDescriptor, Map[ScalaIDEDefinition, Seq[FeatureDefinition]]], baseFolder: File): String = {

    if (baseFolder.exists()) {
      FileUtils.deleteFull(baseFolder)
    }
    baseFolder.mkdirs()

    val ecosystemFolders = ecosystemToScalaIDEToAvailableFeatures.map(e => generateEcosystemProject(e._1, e._2, baseFolder)).toSeq

    saveXml(new File(baseFolder, "pom.xml"), createTopPomXml(ecosystemFolders))

    "OK"
  }

  def generateEcosystemProject(ecosystem: EcosystemDescriptor, scalaIDEToAvailableFeatures: Map[ScalaIDEDefinition, Seq[FeatureDefinition]], baseFolder: File): File = {
    val ecosystemFolder = new File(baseFolder, ecosystem.id)
    ecosystemFolder.mkdir()

    val artifactId = "ecosystem.%s".format(ecosystem.id)

    val featureFolders: Seq[File] = scalaIDEToAvailableFeatures.flatMap(s => generateScalaIDEProjects(s._1, s._2, artifactId, ecosystemFolder)).toSeq

    saveXml(new File(ecosystemFolder, "pom.xml"), createEcosystemPomXml(artifactId, ecosystem.baseSite, ecosystemFolder, featureFolders))

    ecosystemFolder
  }

  def generateScalaIDEProjects(scalaIDE: ScalaIDEDefinition, features: Seq[FeatureDefinition], parentId: String, baseFolder: File): Seq[File] = {
    val scalaIDEFolder = new File(baseFolder, scalaIDE.sdtFeatureVersion.toString())
    scalaIDEFolder.mkdir()

    features.map(f => generateFeatureProject(f, parentId, scalaIDE.repository, scalaIDEFolder))
  }

  def generateFeatureProject(feature: FeatureDefinition, parentId: String, scalaIDERepository: P2Repository, baseFolder: File): File = {
    val featureFolder = new File(baseFolder, feature.details.featureId)
    featureFolder.mkdir()

    saveXml(new File(featureFolder, "pom.xml"), createFeaturePomXml(feature.repository, scalaIDERepository, parentId))
    saveXml(new File(featureFolder, "site.xml"), createSiteXml(feature))

    featureFolder
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

  def createEcosystemPomXml(id: String, baseSite: URL, ecosystemFolder: File, featureFolders: Seq[File]) = {
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
      <artifactId>{ id }</artifactId>
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
                      <url>{ baseSite }</url>
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

  def createFeaturePomXml(featureRepository: P2Repository, scalaIDERepository: P2Repository, parentId: String) = {
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
          <url>{ scalaIDERepository.location }</url>
        </repository>
        <repository>
          <id>feature.repo</id>
          <layout>p2</layout>
          <url>{ featureRepository.location }</url>
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

  private def createSiteXml(feature: FeatureDefinition) = {
    <site>
      { featureXml(feature) }
      <category-def name="stable" label="Scala IDE plugins"/>
      <category-def name="incubation" label="Scala IDE plugins (incubation)"/>
      <category-def name="source" label="Sources"/>
    </site>
  }

  private def featureXml(feature: FeatureDefinition): List[Elem] =
    // TODO: fix category
    // TODO: fix sourceFeatureId, should be option
    List(featureXml(feature.details.featureId, feature.version.toString, "incubation")) :+  featureXml(feature.details.sourceFeatureId, feature.version.toString, "source")

  private def featureXml(id: String, version: String, category: String): Elem = {
    <feature url={ "features/" + id + "_0.0.0.jar" } id={ id } version={ version }>
      <category name={ category }/>
    </feature>
  }

}