package org.scalaide.buildtools

import java.io.File
import scala.xml.Elem
import scala.xml.XML

object MavenProject {
  import Ecosystem._
/*
  def generateEcosystemsProjects(ecosystemToScalaIDEToAvailableFeatures: Map[EcosystemRepository, Map[ScalaIDEDefinition, List[FeatureDefinition]]], location: File): String = {

    val baseFolder = new File(location, "target/build")

    println("Generating build: %s".format(ecosystemToScalaIDEToAvailableFeatures))

    if (baseFolder.exists()) {
      FileUtils.deleteFull(baseFolder)
    }
    baseFolder.mkdirs()

    ecosystemToScalaIDEToAvailableFeatures.foreach(e => generateEcosystemProjects(e._1, e._2, baseFolder))

    "OK"
  }

  private def generateEcosystemProjects(ecosystem: EcosystemRepository, scalaIDEToAvailableFeatures: Map[ScalaIDEDefinition, List[FeatureDefinition]], baseFolder: File) {
    val ecosystemFolder = new File(baseFolder, "ecosystem-" + ecosystem.id)
    ecosystemFolder.mkdir()

    val c = Iterator.from(1)

    scalaIDEToAvailableFeatures.foreach(s => generateScalaIDEProject(ecosystem, s._1, s._2, ecosystemFolder, c.next))

    generateRepositoryBuilder(ecosystemFolder, c.next - 1)
  }

  private def generateRepositoryBuilder(ecosystemFolder: File, nbSubProject: Int) {
    XML.save(new File(ecosystemFolder, "pom.xml").getAbsolutePath(), createBuilderPomXml(ecosystemFolder, nbSubProject), "UTF-8", true)
  }

  private def generateScalaIDEProject(ecosystem: EcosystemRepository, scalaIDE: ScalaIDEDefinition, features: List[FeatureDefinition], ecosystemFolder: File, id: Int) {
    val projectFolder = new File(ecosystemFolder, "v" + id)
    projectFolder.mkdir()

    XML.save(new File(projectFolder, "site.xml").getAbsolutePath(), createSiteXml(scalaIDE, features), "UTF-8", true)

    XML.save(new File(projectFolder, "pom.xml").getAbsolutePath(), createPomXml(ecosystem.getRepository.right.get, features), "UTF-8", true)
  }

  private def createBuilderPomXml(ecosystemFolder: File, nbSubProject: Int) = {
    <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <modelVersion>4.0.0</modelVersion>
      <prerequisites>
        <maven>3.0</maven>
      </prerequisites>
      <groupId>org.scala-ide</groupId>
      <artifactId>org.scala-ide.composite.generated</artifactId>
      <version>0.1.0-SNAPSHOT</version>
      <description>Build a repository containing multiple versions</description>
      <packaging>pom</packaging>
      <properties>
        <encoding>UTF-8</encoding>
        <tycho.version>0.15.0</tycho.version>
      </properties>
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
                <!-- source repositories to mirror from -->
                {
                  1 to nbSubProject map (i =>
                    <repository>
                      <url>file://{ new File(ecosystemFolder, "v" + i + "/target/site") }</url>
                      <layout>p2</layout>
                    </repository>)
                }
              </source>
              <destination>${{project.build.directory}}/site</destination>
            </configuration>
          </plugin>
        </plugins>
      </build>
    </project>
  }

  private def createSiteXml(scalaIDE: ScalaIDEDefinition, features: List[FeatureDefinition]) = {
    <site>
      { featureXml(scalaIDE) }
      { features.flatMap(featureXml(_)) }
      <category-def name="sdt" label="Scala IDE for Eclipse"/>
      <category-def name="stable" label="Scala IDE plugins"/>
      <category-def name="incubation" label="Scala IDE plugins (incubation)"/>
      <category-def name="dev" label="Scala IDE for Eclipse development support"/>
      <category-def name="source" label="Sources"/>
    </site>
  }

  private def featureXml(scalaIDE: ScalaIDEDefinition): List[Elem] =
    List(featureXml(ScalaIDEFeatureId, scalaIDE.sdtFeatureVersion, "sdt"),
      featureXml(ScalaIDESourceFeatureId, scalaIDE.sdtFeatureVersion, "source"),
      featureXml(ScalaIDEDevFeatureId, scalaIDE.sdtFeatureVersion, "dev"))

  private def featureXml(feature: FeatureDefinition): List[Elem] =
    List(featureXml(feature.details.id, feature.version, feature.details.category)) ++ feature.details.source.map(featureXml(_, feature.version, "source"))

  private def featureXml(id: String, version: String, category: String): Elem = {
    <feature url={ "features/" + id + "_0.0.0.jar" } id={ id } version={ version }>
      <category name={ category }/>
    </feature>
  }

  private def createPomXml(repository: P2Repository, features: List[FeatureDefinition]) = {

    // TODO: add support for Juno

    val repositories = features.map(_.repository) :+ repository distinct

    <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <modelVersion>4.0.0</modelVersion>
      <prerequisites>
        <maven>3.0</maven>
      </prerequisites>
      <groupId>org.scala-ide</groupId>
      <artifactId>org.scala-ide.ecosystem.generated</artifactId>
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
        {
          val counter = Iterator.from(1)
          repositories.map(repositoryXml(_, counter.next))
        }
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

  private def repositoryXml(repository: P2Repository, id: Int) = {
    <repository>
      <id>{ "repo-" + id }</id>
      <layout>p2</layout>
      <url>{ repository.location }</url>
    </repository>
  }
*/
}