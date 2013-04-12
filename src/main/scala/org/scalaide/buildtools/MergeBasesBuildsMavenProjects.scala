package org.scalaide.buildtools

import java.io.File

object MergeBasesBuildsMavenProjects {

  def generate(builds: Seq[MergeBasesBuild], targetFolder: File) {

    val buildFolder = new File(targetFolder, "builds")

    if (buildFolder.exists) {
      FileUtils.deleteFull(buildFolder)
    }
    buildFolder.mkdirs()

    val ecosystemFolders = builds.flatMap(generateEcosystemProject(_, buildFolder))

    if (ecosystemFolders.isEmpty) {
      println("Nothing to do")
    } else {
      FileUtils.saveXml(new File(buildFolder, "pom.xml"), EcosystemBuildsMavenProjects.createTopPomXml(ecosystemFolders))
    }
  }

  private def generateEcosystemProject(build: MergeBasesBuild, buildFolder: File): Option[File] = {
    if (build.toMerge) {
      val ecosystemFolder = new File(buildFolder, build.id)
      ecosystemFolder.mkdirs()

      FileUtils.saveXml(new File(ecosystemFolder, "pom.xml"), createEcosystemPomXml(build))

      Some(ecosystemFolder)
    } else {
      None
    }
  }

  private def createEcosystemPomXml(build: MergeBasesBuild) = {
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
      <artifactId>{ build.id }</artifactId>
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
                {
                  if (build.baseRepo.isValid)
                    repoReference(build.baseRepo)
                }
                { repoReference(build.nextBaseRepo) }
              </source>
              <destination>${{project.build.directory}}/base</destination>
            </configuration>
          </plugin>
        </plugins>
      </build>
    </project>
  }

  private def repoReference(repo: P2RepositoryOld) =
    <repository>
      <url>{ repo.location }</url>
      <layout>p2</layout>
    </repository>

}