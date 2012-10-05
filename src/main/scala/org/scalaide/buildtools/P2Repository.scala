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
import java.net.URL
import scala.xml.Node

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

case class InstallableUnit(id: String, version: String, dependencies: Seq[DependencyUnit])

object InstallableUnit {
  def apply(unit: Node): Option[InstallableUnit] = {
    if (isBundle(unit) || isFeature(unit))
      Some(InstallableUnit(unit \ "@id" text, unit \ "@version" text, getDependencies(unit)))
    else
      None
  }

  private def getDependencies(unit: Node): Seq[DependencyUnit] = {
    unit \ "requires" \ "required" flatMap (DependencyUnit(_))
  }

  private def isBundle(unit: Node) = unit \ "artifacts" \ "artifact" \ "@classifier" exists (a => a.text == "osgi.bundle")

  private def isFeature(unit: Node) = unit \ "properties" \ "property" exists (e => (e \ "@name" text) == "org.eclipse.equinox.p2.type.group" && (e \ "@value" text) == "true")
}

case class P2Repository(uis: Map[String, Seq[InstallableUnit]], location: String) {

  def findIU(pattern: String): Seq[InstallableUnit] = {
    uis.flatMap { entry =>
      if (entry._1.matches(pattern)) entry._2 else Nil
    }.toSeq
  }
  
  override def toString = "P2Repository(%s)".format(location)

}

object P2Repository {

  final val CompressedContentFile = "content.jar"

  def fromXML(contentXml: Elem, location: String): P2Repository = {
    val unitsXML = (contentXml \ "units" \\ "unit")
    val units = unitsXML.flatMap(InstallableUnit(_))

    P2Repository(units.groupBy(_.id), location)
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
    val url = new URL(repoUrl)
    url.getProtocol() match {
      case "file" =>
        fromLocalFolder(url.getFile())
      case _ =>
        fromHttpUrl(repoUrl)
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