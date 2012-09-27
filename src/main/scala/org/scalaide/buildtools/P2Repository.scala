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

/** An installable unit in a P2 repository */
case class InstallableUnit(id: String, version: String)

class P2Repository(contentXml: Elem) {

  /** Return all installable units in this repository that match the given regex pattern. */
  def findIU(pattern: String): Seq[InstallableUnit] = {
    val units = (contentXml \ "units" \\ "unit")

    units.filter(_.attributes.exists(a => a.key == "id" && a.value.text.matches(pattern))) map { unit =>
      InstallableUnit(unit \ "@id" text, unit \ "@version" text)
    }
  }
}

object P2Repository {

  def fromString(content: String): P2Repository = {
    new P2Repository(XML.loadString(content))
  }

  /** Connect to the given repository URL and download content.jar, unzip and
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
    val tmpFile = File.createTempFile("downloaded-content", ".jar")
    val svc = url(repoUrl) / "content.jar"

    val downloadHandle = Http(svc > as.File(tmpFile)(null)).either
    //    try {
    // get rid of the exception
    val stringExceptionHandle = for (ex <- downloadHandle.left) yield "Error downloading file " + ex.getMessage()

    for {
      val d <- stringExceptionHandle().right
      val xml <- getContentsFromZipFile(tmpFile).right
    } yield new P2Repository(xml)
    //  } finally Http.shutdown()
  }

  def getContentsFromZipFile(file: File): Either[String, Elem] = {
    try {
      val zipFile = new ZipFile(file)
      val entry = zipFile.getEntry("content.xml")
      if (entry == null) Left("Could not find 'content.xml' in 'content.jar'")
      else {
        val is = zipFile.getInputStream(entry)
        Right(XML.load(is))
      }
    } catch {
      case io: IOException  => Left("Error reading zip file: " + io.getMessage())
      case ze: ZipException => Left("Invalid zip file: " + ze.getMessage())
      case se: SAXException => Left("Error parsing XML file: " + se.getMessage())
    }
  }
}