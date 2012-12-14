package org.scalaide.buildtools

import java.io.File

import dispatch.Http

object GenerateMergeBasesBuilds {

  import Ecosystem._

  def main(args: Array[String]) {
    // parse arguments

    val rootFolder = args.collectFirst {
      case RootOption(root) =>
        root
    }.getOrElse(System.getProperty("user.dir"))

    new GenerateMergeBasesBuilds(new File(rootFolder))
  }
}

class GenerateMergeBasesBuilds(rootFolder: File) {

  import Ecosystem._

  def apply() {
    try {

      println("Generating merge-base builds")

      val ecosystemConfs = EcosystemsDescriptor.load(new File(rootFolder, EcosystemConfigFile)).ecosystems

      val mergeBasesBuilds = MergeBasesBuild(ecosystemConfs)

      val targetFolder = new File(rootFolder, "target")
      if (!targetFolder.exists()) {
        targetFolder.mkdirs()
      }

      MergeBasesBuildsMavenProjects.generate(mergeBasesBuilds, targetFolder)

      println("Generating merge-base builds - Done")

    } finally {
      // need to stop Dispatch in any cases
      Http.shutdown()
    }
  }

}