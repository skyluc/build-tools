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
import java.net.{ URL => jURL }
import scala.xml.Node
import org.osgi.framework.Version
import scala.collection.immutable.TreeSet
import java.net.URL
import scala.collection.mutable.HashMap
import scala.util.control.Exception.catching
import java.net.MalformedURLException

/**
 * !!! This object not thread safe !!! It was used in a single threaded system when implemented.
 */
object RepositoriesOld {

  val cache = HashMap[URL, P2RepositoryOld]()

  def apply(location: URL): P2RepositoryOld = {
    cache.get(location) match {
      case Some(repo) =>
        repo
      case None =>
        val repo = P2RepositoryOld.fromUrl(location)
        cache.put(location, repo)
        repo
    }
  }
}

case class DependencyUnitOld(id: String, range: String)

object DependencyUnitOld {

  def apply(required: Node): Option[DependencyUnitOld] = {
    val namespace = (required \ "@namespace" text)
    if (namespace == "osgi.bundle" || namespace == "org.eclipse.equinox.p2.iu")
      Some(new DependencyUnitOld(required \ "@name" text, required \ "@range" text))
    else
      None
  }

}

case class InstallableUnitOld(id: String, version: Version, dependencies: List[DependencyUnitOld])

object InstallableUnitOld {
  def apply(unit: Node): Option[InstallableUnitOld] = {
    if (isBundle(unit) || isFeature(unit))
      Some(InstallableUnitOld(unit \ "@id" text, new Version(unit \ "@version" text), getDependencies(unit)))
    else
      None
  }

  implicit object DescendingOrdering extends Ordering[InstallableUnitOld] {
    override def compare(x: InstallableUnitOld, y: InstallableUnitOld): Int = {
      val diffId = x.id.compareTo(y.id)
      if (diffId == 0) -1 * x.version.compareTo(y.version) // same bundle name, compare versions
      else diffId
    }
  }

  private def getDependencies(unit: Node): List[DependencyUnitOld] = {
    (unit \ "requires" \ "required" flatMap (DependencyUnitOld(_))).toList
  }

  private def isBundle(unit: Node) = unit \ "artifacts" \ "artifact" \ "@classifier" exists (a => a.text == "osgi.bundle")

  private def isFeature(unit: Node) = unit \ "properties" \ "property" exists (e => (e \ "@name" text) == "org.eclipse.equinox.p2.type.group" && (e \ "@value" text) == "true")
}

trait P2RepositoryOld {
  def uis: Map[String, TreeSet[InstallableUnitOld]]
  def findIU(unitId: String): TreeSet[InstallableUnitOld]
  def isValid: Boolean
  def location: String
}

case class ValidP2RepositoryOld (uis: Map[String, TreeSet[InstallableUnitOld]], location: String) extends P2RepositoryOld {

  override def findIU(unitId: String): TreeSet[InstallableUnitOld] =
    uis get (unitId) getOrElse (TreeSet.empty[InstallableUnitOld])

  override def isValid = true
    
  override def toString = "P2Repository(%s)".format(location)

  override def equals(o: Any): Boolean = {
    o match {
      case ValidP2RepositoryOld(_, `location`) => true
      case _ => false
    }
  }

  override def hashCode: Int = location.hashCode()

}

case class ErrorP2RepositoryOld (errorMessage: String, location: String) extends P2RepositoryOld {
  override def findIU(unitId: String): TreeSet[InstallableUnitOld] = TreeSet()
  override def uis: Map[String, TreeSet[InstallableUnitOld]] = Map()
  override def isValid = false
}

object P2RepositoryOld {

  final val CompressedContentFile = "content.jar"

  def fromXML(contentXml: Elem, location: String): P2RepositoryOld = {
    val unitsXML = (contentXml \ "units" \\ "unit")
    val units = unitsXML.flatMap(InstallableUnitOld(_))
    val grouped = units.groupBy(_.id)
    val sorted = for ((key, values) <- grouped) yield (key, TreeSet(values: _*))
    ValidP2RepositoryOld(sorted, location)
  }

  def fromString(content: String): P2RepositoryOld = {
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
  def fromUrl(repoUrl: String): P2RepositoryOld = {
    catching(classOf[MalformedURLException]) either fromUrl(new jURL(repoUrl)) match {
      case Left(exception) => ErrorP2RepositoryOld(exception.getLocalizedMessage, repoUrl)
      case Right(repo) => repo 
    }
  }

  def fromUrl(repoUrl: jURL): P2RepositoryOld = {
    repoUrl.getProtocol() match {
      case "file" =>
        fromLocalFolder(repoUrl.getFile())
      case _ =>
        fromHttpUrl(repoUrl.toExternalForm())
    }
  }

  def fromLocalFolder(repoFolder: String): P2RepositoryOld = {
    val contentFile = new File(repoFolder, CompressedContentFile)
    val folderUrl = "file://" + repoFolder
    if (contentFile.exists && contentFile.isFile) {
      getContentsFromZipFile(contentFile) match {
        case Right(xml) =>
          fromXML(xml, folderUrl)
        case Left(msg) =>
          ErrorP2RepositoryOld(msg, folderUrl)
      }
    } else {
      ErrorP2RepositoryOld("%s doesn't exist, or is not a file".format(contentFile.getAbsolutePath()), folderUrl)
    }
  }

  def fromHttpUrl(repoUrl: String): P2RepositoryOld = {
    val tmpFile = File.createTempFile("downloaded-content", ".jar")
    val svc = url(repoUrl) / CompressedContentFile

    val downloadHandle = Http(svc > as.File(tmpFile)(null)).either
    //    try {
    // get rid of the exception
    val stringExceptionHandle = for (ex <- downloadHandle.left) yield "Error downloading file " + ex.getMessage()

    val res= for {
      d <- stringExceptionHandle().right
      xml <- getContentsFromZipFile(tmpFile).right
    } yield fromXML(xml, repoUrl)
    
    res match {
      case Right(p2Repository) =>
        p2Repository
      case Left(msg) =>
        ErrorP2RepositoryOld(msg, repoUrl)
    }
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
