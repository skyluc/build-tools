package org.scalaide.buildtools

import java.io.File
import scala.Option.option2Iterable
import Ecosystem.EcosystemConfigFile
import Ecosystem.RootOption
import dispatch.Http

object GenerateEcosystemBuilds {

  import Ecosystem._

  def main(args: Array[String]) {
    // parse arguments

    val rootFolder = args.collectFirst {
      case RootOption(root) =>
        root
    }.getOrElse(System.getProperty("user.dir"))

    new GenerateEcosystemBuilds(new File(rootFolder))()
  }
}

class GenerateEcosystemBuilds(rootFolder: File) {

  import Ecosystem._

  def apply() {
    try {
      println("Generating ecosystem builds")

      val ecosystemConfs = EcosystemsDescriptor.load(new File(rootFolder, EcosystemConfigFile)).ecosystems
      val featureConfs = PluginDescriptor.loadAll(new File(rootFolder, "features")).flatMap(_ match {
        case Right(conf) =>
          Some(conf)
        case Left(_) =>
          None
      })

      val builds = EcosystemBuilds(ecosystemConfs, featureConfs)

      val targetFolder = new File(rootFolder, "target")
      if (!targetFolder.exists()) {
        targetFolder.mkdirs()
      }

      EcosystemBuildsReport.generate(builds, targetFolder)
      EcosystemBuildsMavenProjects.generate(builds, targetFolder)

      println("Generating ecosystem builds - Done")
    } finally {
      // need to stop Dispatch in any cases
      Http.shutdown()
    }
  }

}