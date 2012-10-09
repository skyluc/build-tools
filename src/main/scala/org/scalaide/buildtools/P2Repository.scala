package org.scalaide.buildtools

import scala.util.matching.Regex
import scala.xml.XML
import dispatch._
import java.io.File
import java.util.zip.ZipFile
import java.io.IOException
import java.util.zip.ZipException
import scala.xml.Elem
import org.xml.sax.SAXException
import java.net.{URL => jURL}
import scala.xml.Node
import org.osgi.framework.Version
import scala.collection.immutable.TreeSet

case class DependencyUnit(id: String, range: String)

object DependencyUnit {

  def apply(required: Node): Option[DependencyUnit] = {
    val namespace= (required \ "@namespace" text)
    if (namespace == "osgi.bundle" || namespace == "org.eclipse.equinox.p2.iu")
      Some(new DependencyUnit(required \ "@name" text, required \ "@range" text))
    else
      None
  }

}

case class InstallableUnit(id: String, version: Version, dependencies: List[DependencyUnit])

object InstallableUnit {
  def apply(unit: Node): Option[InstallableUnit] = {
    if (isBundle(unit) || isFeature(unit))
      Some(InstallableUnit(unit \ "@id" text, new Version(unit \ "@version" text), getDependencies(unit)))
    else
      None
  }

  implicit object DescendingOrdering extends Ordering[InstallableUnit] {
    override def compare(x: InstallableUnit, y: InstallableUnit): Int = {
      val diffId = x.id.compareTo(y.id)
      if(diffId == 0) -1 * x.version.compareTo(y.version) // same bundle name, compare versions
      else diffId
    }
  }
  
  private def getDependencies(unit: Node): List[DependencyUnit] = {
    (unit \ "requires" \ "required" flatMap (DependencyUnit(_))).toList
  }

  private def isBundle(unit: Node) = unit \ "artifacts" \ "artifact" \ "@classifier" exists (a => a.text == "osgi.bundle")

  private def isFeature(unit: Node) = unit \ "properties" \ "property" exists (e => (e \ "@name" text) == "org.eclipse.equinox.p2.type.group" && (e \ "@value" text) == "true")
}

case class P2Repository private(uis: Map[String, TreeSet[InstallableUnit]], location: String) {

  def findIU(unitId: String): TreeSet[InstallableUnit] = 
    uis get (unitId) getOrElse (TreeSet.empty[InstallableUnit])
  
  override def toString = "P2Repository(%s)".format(location)

}

object P2Repository {

  final val CompressedContentFile = "content.jar"

  def fromXML(contentXml: Elem, location: String): P2Repository = {
    val unitsXML = (contentXml \ "units" \\ "unit")
    val units = unitsXML.flatMap(InstallableUnit(_))
    val grouped = units.groupBy(_.id)
    val sorted = for((key, values) <- grouped) yield (key, TreeSet(values : _*))
    P2Repository(sorted, location)
  }

  def fromString(content: String): P2Repository = {
    fromXML(XML.loadString(content), "from String")
  }

  /**
   * Connect to the given repository URL and download content.jar, unzip and
   *  read the contents. Any error is returned in an instace of `Left`.
   *
   *  This method can handle simple P2 repositories, with zipped metadata. It
   *  does not support composite repositories, nor non-archived metadata.
   *
   *  @note The caller of this method has to call `Http.shutdown()`
   *        before exiting the application, otherwise threads may hang on
   *        to the current process.
   */
  def fromUrl(repoUrl: String): Either[String, P2Repository] = {
    fromUrl(new jURL(repoUrl))
  }
  
  def fromUrl(repoUrl: jURL): Either[String, P2Repository] = {
    repoUrl.getProtocol() match {
      case "file" =>
        fromLocalFolder(repoUrl.getFile())
      case _ =>
        fromHttpUrl(repoUrl.toExternalForm())
    }
  }

  def fromLocalFolder(repoFolder: String): Either[String, P2Repository] = {
    val contentFile = new File(repoFolder, CompressedContentFile)
    if (contentFile.exists && contentFile.isFile) {
      for {
        xml <- getContentsFromZipFile(contentFile).right
      } yield fromXML(xml, "file://" + repoFolder)
    } else {
      Left("%s doesn't exist, or is not a file".format(contentFile.getAbsolutePath()))
    }
  }

  def fromHttpUrl(repoUrl: String): Either[String, P2Repository] = {
    val tmpFile = File.createTempFile("downloaded-content", ".jar")
    val svc = url(repoUrl) / CompressedContentFile

    val downloadHandle = Http(svc > as.File(tmpFile)(null)).either
    //    try {
    // get rid of the exception
    val stringExceptionHandle = for (ex <- downloadHandle.left) yield "Error downloading file " + ex.getMessage()

    for {
      d <- stringExceptionHandle().right
      xml <- getContentsFromZipFile(tmpFile).right
    } yield fromXML(xml, repoUrl)
    //  } finally Http.shutdown()
  }

  def getContentsFromZipFile(file: File): Either[String, Elem] = {
    val zipFile = new ZipFile(file)
    try {
      val entry = zipFile.getEntry("content.xml")
      if (entry == null) Left("Could not find 'content.xml' in 'content.jar'")
      else {
        val is = zipFile.getInputStream(entry)
        Right(XML.load(is))
      }
    } catch {
      case io: IOException => Left("Error reading zip file: " + io.getMessage())
      case ze: ZipException => Left("Invalid zip file: " + ze.getMessage())
      case se: SAXException => Left("Error parsing XML file: " + se.getMessage())
    } finally
      zipFile.close()
  }
}
