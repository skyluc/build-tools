package org.scalaide.buildtools

import java.io.File
import java.beans.FeatureDescriptor
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import java.net.URL
import org.osgi.framework.Version
import Ecosystem._
import scala.concurrent.Await
import scala.concurrent.duration._

object GenerateBuildAndDataForEcosystems {

  import ExecutionContext.Implicits.global

  def main(args: Array[String]) {
    // parse arguments

    val rootFolder = args.collectFirst {
      case RootOption(root) =>
        root
    }.getOrElse(System.getProperty("user.dir"))

    generateEcosystemBuilds(new File(rootFolder))
  }

  private def generateEcosystemBuilds(rootFolder: File) {

    // TODO: make that a future
    val ecosystemConfigurations = EcosystemsDescriptor.load(new File(rootFolder, EcosystemConfigFile)).ecosystems

    // TODO: a future too
    val featureConfigurations = PluginDescriptor.loadAll(new File(rootFolder, "features")).flatMap(_ match {
      case Right(conf) =>
        Some(conf)
      case Left(_) =>
        None
    })
    
    val availableFeatures = EcosystemContent.fetchAvailableFeatures(featureConfigurations)
    
    val currentEcosystems = EcosystemContent.fetchEcosystemContents(ecosystemConfigurations, featureConfigurations, availableFeatures)
    
    println(Await.result(currentEcosystems, 10.minute))
    

  }
    
}

