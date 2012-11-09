package org.scalaide.buildtools

import java.io.File
import scala.xml.Elem
import java.util.Date
import java.text.SimpleDateFormat

object EcosystemBuildsReport {

  def generate(builds: EcosystemBuilds, targeFolder: File) {
    new EcosystemBuildsReport(builds, targeFolder).generate()
  }

}

private class EcosystemBuildsReport(builds: EcosystemBuilds, targetFolder: File) {

  val inlinedCss =
    """body {font-family: sans-serif; text-shadow: rgba(0, 0, 0, 0.5) 0px 1px 2px; background: #101010;}
div {padding: 2px 2px 2px 5px; margin-bottom: 0.5em;}
h1 {margin: 0.2em 0 0 0; margin-right: 3em;}
h2 {margin: 0.2em 0 0 0; margin-right: 3em;}
h3 {margin: 0.2em 0 0 0; margin-right: 3em;}
h4 {margin: 0.2em 0 0 0; margin-right: 3em;}
.availableAddOns {background: #7F7F7F;}
.availableAddOn {background: #E0E0E0;}
.ecosystem {background: #7F7F7F}
.currentNextAvailableScalaIDEVersions {background: #A0A0A0;}
.currentNextExistingAddOns {background: #A0A0A0;}
.updatedEcosystem {background: #A0A0A0;}
.nextEcosystem {background: #A0A0A0;}
.availableScalaIDEVersions {background: #C0C0C0;}
.existingAddOns {background: #C0C0C0;}
.usedAddOns {background: #C0C0C0;}
.usedAddOn {background: #E0E0E0;}

.addOnVersion {margin-bottom: 0;}
.scalaIDEVersion {margin-bottom: 0;}

.notAvailable {color: rgba(0, 0, 0, 0.5); text-shadow: none;}
.zipped {font-weight: normal; font-style: italic; color: #404040;}
.red {color: red;}
.blue {color: blue;}
.outer {display: inline-block;}
    
.footer {color: #E0E0E0; text-align: center;}"""

  def generate() {

    val content =
      <html>
        <head>
          <style type="text/css">
            { inlinedCss }
          </style>
        </head>
        <body>
          <span class="outer">
            <div class="availableAddOns">
              <h1>Available add-ons</h1>
              { availableAddOns(builds.availableAddOns) }
            </div>
            { builds.ecosystems.map(generate(_)) }
            <div class="footer">
              generated:{ timeStampPrettyPrinted }
            </div>
          </span>
        </body>
      </html>

    FileUtils.saveXml(new File(targetFolder, "builds.html"), content)
  }

  def generate(build: EcosystemBuild) = {
    <div class="ecosystem">
      <h1>{ build.id }</h1>
      <div class="currentNextAvailableScalaIDEVersions">
        <h2>Available Scala IDE versions:</h2>
        { availableScalaIDEVersions(build.baseRepo, build.baseScalaIDEVersions) }
        { availableScalaIDEVersions(build.nextRepo, build.nextScalaIDEVersions) }
      </div>
      <div class="currentNextExistingAddOns">
        <h2>Existing add-ons</h2>
        { existingAddOns(build.siteRepo, build.existingAddOns) }
        { existingAddOns(build.nextSiteRepo, build.nextExistingAddOns) }
      </div>
      <div class="updatedEcosystem">
        <h2>Updated ecosystem { rebuiltComment(build.regenerateEcosystem) }</h2>
        { build.baseScalaIDEVersions.map(s => scalaIDEVersionWithAddOns(s, build.zippedVersion.exists(_ == s))) }
      </div>
      {
        if (build.nextScalaIDEVersions.isEmpty)
          <div class="nextEcosystem notAvailable">
            <h2>Next ecosystem</h2>
          </div>
        else
          <div class="nextEcosystem">
            <h2>Next ecosystem { rebuiltComment(build.regenerateNextEcosystem) }</h2>
            { build.nextScalaIDEVersions.map(scalaIDEVersionWithAddOns(_, false)) }
          </div>
      }
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
        <div class="availableScalaIDEVersions">
          <h3>{ repo.location }:</h3>
          { scalaIDEVersions.map(s => <div class="scalaIDEVersion">{ s.version }</div>) }
        </div>
      case e: ErrorP2Repository =>
        <div class="availableScalaIDEVersions notAvailable"><h3>{ repo.location }:</h3><div>Not available.</div></div>
    }
  }

  def existingAddOns(repo: P2Repository, addOns: Map[PluginDescriptor, Seq[AddOn]]) = {
    repo match {
      case p: ValidP2Repository =>
        <div class="existingAddOns">
          <h3>{ repo.location }:</h3>
          { availableAddOns(addOns) }
        </div>
      case e: ErrorP2Repository =>
        <div class="existingAddOns notAvailable"><h3>{ repo.location }:</h3><div>Not available.</div></div>
    }
  }

  def availableAddOns(addOns: Map[PluginDescriptor, Seq[AddOn]]) = {
    addOns.map { p =>
      <div class="availableAddOn">
        <h4> { p._1.featureId }</h4>
        { p._2.map { a => <div class="addOnVersion">{ a.iu.version }</div> } }
      </div>
    }
  }

  def scalaIDEVersionWithAddOns(scalaIDEVersion: ScalaIDEVersion, zipped: Boolean) = {
    <div class="scalaIDEVersion">
      <h3>{ scalaIDEVersion.version }{ if (zipped) <span class="zipped"> zipped</span> }</h3>
      <div class="usedAddOns">
        <h4>Existing add-ons</h4>
        {
          scalaIDEVersion.associatedExistingAddOns.map {
            case (p, a) => usedAddOn(p, a)
          }
        }
      </div>
      <div class="usedAddOns">
        <h4>Updated add-ons</h4>
        {
          scalaIDEVersion.associatedAvailableAddOns.map {
            case (p, a) => usedAddOn(p, a)
          }
        }
      </div>
    </div>
  }

  private def usedAddOn(pluginDescriptor: PluginDescriptor, addon: AddOn) = {
    <div class="usedAddOn">
      <b>{ pluginDescriptor.featureId }</b>
      -&nbsp;{ addon.version }
    </div>
  }

  private def timeStampPrettyPrinted = new SimpleDateFormat("yyyyMMdd-HHmm z").format(new Date())

}