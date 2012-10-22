package org.scalaide.buildtools

import java.io.File
import scala.xml.Elem

object EcosystemBuildsReport {

  def generate(builds: EcosystemBuilds, targeFolder: File) {
    new EcosystemBuildsReport(builds, targeFolder).generate()
  }

}

private class EcosystemBuildsReport(builds: EcosystemBuilds, targetFolder: File) {

  def generate() {

    val content =
      <html>
        <head>
          <style type="text/css">
            div.ecosystem {{border-width: 1px; border: solid; border-color: gray;}}
            div.availableAddOns {{border-width: 1px; border: solid; border-color: gray;}}
            .red {{color: red;}}
            .blue {{color: blue;}}
          </style>
        </head>
        <body>
          <div class="availableAddOns">
            <h1>Available add-ons</h1>
            { availableAddOns(builds.availableAddOns) }
          </div>
          { builds.ecosystems.map(generate(_)) }
        </body>
      </html>

    FileUtils.saveXml(new File(targetFolder, "builds.html"), content)
  }

  def generate(build: EcosystemBuild) = {
    <div class="ecosystem">
      <h1>{ build.id }</h1>
      <div>
        <h2>Available Scala IDE versions:</h2>
        { availableScalaIDEVersions(build.baseRepo, build.baseScalaIDEVersions) }
        { availableScalaIDEVersions(build.nextRepo, build.nextScalaIDEVersions) }
      </div>
      <div>
        <h2>Existing add-ons</h2>
        { existingAddOns(build.siteRepo, build.existingAddOns) }
        { existingAddOns(build.nextSiteRepo, build.nextExistingAddOns) }
      </div>
      <div>
        <h2>Updated ecosystem { rebuiltComment(build.regenerateEcosystem) }</h2>
        { build.baseScalaIDEVersions.map(scalaIDEVersionWithAddOns(_)) }
      </div>
      <div>
        <h2>Next ecosystem { rebuiltComment(build.regenerateNextEcosystem) }</h2>
        { build.nextScalaIDEVersions.map(scalaIDEVersionWithAddOns(_)) }
      </div>
    </div>
  }

  def rebuiltComment(rebuilt: Boolean) = {
    if (rebuilt)
      <span class="blue">- rebuilt</span>
    else
      <span class="red">- not rebuilt</span>
  }

  def availableScalaIDEVersions(repo: P2Repository, scalaIDEVersions: Seq[ScalaIDEVersion]) = {
    repo match {
      case p: ValidP2Repository =>
        <div>
          <h3>{ repo.location }:</h3>
          { scalaIDEVersions.map(s => <div>{ s.version }</div>) }
        </div>
      case e: ErrorP2Repository =>
        <div><h3>{ repo.location }:</h3>Not available.</div>
    }
  }

  def existingAddOns(repo: P2Repository, addOns: Map[PluginDescriptor, Seq[AddOn]]) = {
    repo match {
      case p: ValidP2Repository =>
        <div>
          <h3>{ repo.location }:</h3>
          { availableAddOns(addOns) }
        </div>
      case e: ErrorP2Repository =>
        <div><h3>{ repo.location }:</h3>Not available.</div>
    }
  }

  def availableAddOns(addOns: Map[PluginDescriptor, Seq[AddOn]]) = {
    addOns.map { p =>
      <div>
        <h4> { p._1.featureId }</h4>
        { p._2.map { a => <div>{ a.iu.version }</div> } }
      </div>
    }
  }

  def scalaIDEVersionWithAddOns(scalaIDEVersion: ScalaIDEVersion) = {
    <div>
      <h3>{ scalaIDEVersion.version }</h3>
      <div>
        <h4>Existing add-ons</h4>
        {
          scalaIDEVersion.associatedExistingAddOns.map {
            case (p, a) => usedAddOns(p, a)
          }
        }
      </div>
      <div>
        <h4>Updated add-ons</h4>
        {
          scalaIDEVersion.associatedAvailableAddOns.map {
            case (p, a) => usedAddOns(p, a)
          }
        }
      </div>
    </div>
  }

  def usedAddOns(pluginDescriptor: PluginDescriptor, addon: AddOn) = {
    <div>
      <b>{ pluginDescriptor.featureId }</b>
      -&nbsp;{ addon.version }
    </div>
  }

}