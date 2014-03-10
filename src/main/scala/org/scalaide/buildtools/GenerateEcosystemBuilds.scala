package org.scalaide.buildtools

import java.io.File
import scala.Option.option2Iterable
import dispatch.Http

object GenerateEcosystemBuilds {

  import Ecosystem._

  /* Regular expression for splitting a list of comma separated ecosystem ids provided via the --force argument.
   * --force is used to force the recreation of the ecosystem update-site for the ids passed on the right-hand-side.  
   */
  private val ForceOption = "--force=([^,].+)".r

  def main(args: Array[String]) {
    // parse arguments

    val rootFolder = args.collectFirst {
      case RootOption(root) =>
        root
    }.getOrElse(System.getProperty("user.dir"))

    val forceEcosystemCreationIds: Set[EcosystemId] = args.collectFirst {
      case ForceOption(forced) =>
        forced.split(",").map(EcosystemId).toSet
    }.getOrElse(Set.empty)
    

    new GenerateEcosystemBuilds(new File(rootFolder))(forceEcosystemCreationIds)
  }
}

class GenerateEcosystemBuilds(rootFolder: File) {

  import Ecosystem._

  def apply(forced: Set[EcosystemId]) {
    try {
      println("Generating ecosystem builds")

      val ecosystemConfs = EcosystemsDescriptor.load(new File(rootFolder, EcosystemConfigFile)).ecosystems
      val featureConfs = PluginDescriptor.loadAll(new File(rootFolder, "features")).flatMap(_ match {
        case Right(conf) =>
          Some(conf)
        case Left(_) =>
          None
      })

      val builds = EcosystemBuilds(forced, ecosystemConfs, featureConfs)

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