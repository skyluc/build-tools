package org.scalaide.buildtools

import java.io.File

import Ecosystem._

object EcosystemBuildsMavenProjects {

  private val artifactIdSuffix = Iterator.from(1)

  def generate(builds: EcosystemBuilds, targetFolder: File) {
    val buildFolder = new File(targetFolder, "builds")

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

  def generateEcosystemProjects(ecosystemBuild: EcosystemBuild, buildFolder: File): Seq[File] = {
    val ecosystemFolder = new File(buildFolder, ecosystemBuild.id.value)

    val siteFolder = if (ecosystemBuild.regenerateEcosystem) {
      generateEcosystemProject(ecosystemBuild.baseScalaIDEVersions, "%s-base".format(ecosystemBuild.id), ecosystemBuild.baseRepo, buildFolder, ecosystemBuild.zippedVersion)
    } else {
      Nil
    }

    val nextFolder = if (ecosystemBuild.regenerateNextEcosystem) {
      generateEcosystemProject(ecosystemBuild.nextBaseScalaIDEVersions, "%s-next".format(ecosystemBuild.id), ecosystemBuild.nextBaseRepo, buildFolder, None)
    } else {
      Nil
    }

    Seq(siteFolder, nextFolder).flatten
  }

  def generateEcosystemProject(scalaIDEVersions: Seq[org.scalaide.buildtools.ScalaIDEVersion], tag: String, baseRepository: P2Repository, buildFolder: File, zippedVersion: Option[ScalaIDEVersion]): List[File] = {
    val siteFolder = new File(buildFolder, tag)
    siteFolder.mkdirs()

    val artifactId = "ecosystem.%s".format(tag)

    val featureFolders = scalaIDEVersions.map(s => s -> generateScalaIDEProjects(s, artifactId, siteFolder))

    FileUtils.saveXml(new File(siteFolder, "pom.xml"), createEcosystemPomXml(artifactId, baseRepository, featureFolders.flatMap(_._2), siteFolder))

    zippedVersion match {
      case Some(latestScalaIDEVersion) =>
        val zipTag = "%s-zip".format(tag)

        val zipFolder = new File(buildFolder, zipTag)
        zipFolder.mkdirs()

        val featureFoldersLatestVersion = featureFolders.find(_._1 == latestScalaIDEVersion).get._2
        val scalaIDEFolder = generateScalaIDECloneProject(latestScalaIDEVersion, zipTag, zipFolder)

        FileUtils.saveXml(new File(zipFolder, "pom.xml"), createZipPomXml(zipTag, latestScalaIDEVersion, scalaIDEFolder, featureFoldersLatestVersion, zipFolder))

        List(siteFolder, zipFolder)
      case None =>

        List(siteFolder)
    }
  }

  def generateScalaIDEProjects(scalaIDEVersion: ScalaIDEVersion, parentId: String, siteFolder: File): Seq[File] = {
    val scalaIDEFolder = new File(siteFolder, scalaIDEVersion.version.toString)
    scalaIDEFolder.mkdir()

    (scalaIDEVersion.associatedExistingAddOns ++ scalaIDEVersion.associatedAvailableAddOns).map {
      case (_, addOn) =>
        generateAddOnProject(addOn, scalaIDEVersion, parentId, scalaIDEFolder)
    }.toSeq
  }

  def generateScalaIDECloneProject(scalaIDEVersion: ScalaIDEVersion, parentId: String, zipFolder: File): File = {
    val scalaIDEFolder = new File(zipFolder, "scalaIDE")
    scalaIDEFolder.mkdir()

    FileUtils.saveXml(new File(scalaIDEFolder, "pom.xml"), createScalaIDEPomXml(scalaIDEVersion, parentId))
    FileUtils.saveXml(new File(scalaIDEFolder, "site.xml"), createScalaIDESiteXml(scalaIDEVersion))

    scalaIDEFolder
  }

  def generateAddOnProject(addOn: AddOn, scalaIDEVersion: ScalaIDEVersion, parentId: String, scalaIDEFolder: File): File = {
    val addOnFolder = new File(scalaIDEFolder, addOn.id)
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

  def createEcosystemPomXml(artifactId: String, baseRepository: P2Repository, featureFolders: Seq[File], buildFolder: File) = {
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
                <module>{ FileUtils.relativePath(buildFolder, f) }</module>
              }
            }
          </modules>
        </profile>
      </profiles>
    </project>
  }

  def createZipPomXml(artifactId: String, latestScalaIDEVersion: ScalaIDEVersion, scalaIDEFolder: File, featureFolders: Seq[File], buildFolder: File) = {
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
                      <url>{ new File(scalaIDEFolder, "target/site").toURI() }</url>
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
            <module>{ FileUtils.relativePath(buildFolder, scalaIDEFolder) }</module>
          </modules>
        </profile>
      </profiles>
    </project>
  }

  def createScalaIDEPomXml(scalaIDEVersion: ScalaIDEVersion, parentId: String) = {
    <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <modelVersion>4.0.0</modelVersion>
      <prerequisites>
        <maven>3.0</maven>
      </prerequisites>
      <parent>
        <groupId>org.scalaide</groupId>
        <artifactId>{ parentId }</artifactId>
        <version>0.1.0-SNAPSHOT</version>
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
          <id>eclipse.{ scalaIDEVersion.eclipseVersion.id }</id>
          <name>Eclipse p2 repository</name>
          <layout>p2</layout>
          <url>{ scalaIDEVersion.eclipseVersion.repoLocation }</url>
        </repository>
        <repository>
          <id>scalaide.repo</id>
          <layout>p2</layout>
          <url>{ scalaIDEVersion.repository.location }</url>
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

  def createScalaIDESiteXml(scalaIDEVersion: ScalaIDEVersion) = {
    <site>
      { featureXml(Ecosystem.ScalaIDEFeatureId, scalaIDEVersion.version.toString, "sdt") }
      { featureXml(Ecosystem.ScalaIDESourceFeatureId, scalaIDEVersion.version.toString, "sdt-source") }
      { featureXml(Ecosystem.ScalaIDEDevFeatureId, scalaIDEVersion.version.toString, "dev") }
      <category-def name="sdt" label="Scala IDE for Eclipse"/>
      <category-def name="sdt-source" label="Scala IDE for Eclipse Source Feature"/>
      <category-def name="dev" label="Scala IDE for Eclipse development support"/>
    </site>
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
      </properties>
      <repositories>
        <repository>
          <id>eclipse.{ scalaIDEVersion.eclipseVersion.id }</id>
          <name>Eclipse p2 repository</name>
          <layout>p2</layout>
          <url>{ scalaIDEVersion.eclipseVersion.repoLocation }</url>
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

  def createAddOnSiteXml(addOn: AddOn) = {
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