package org.scalaide.buildtools

import java.net.URL
import org.osgi.framework.Version
import scala.collection.immutable.SortedSet
import scala.collection.immutable.HashMap
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import java.io.InputStream
import scala.collection.immutable.TreeSet
import java.util.zip.ZipInputStream
import scala.xml.XML
import scala.xml.Node
import org.osgi.framework.VersionRange
import java.net.ProtocolException
import scala.annotation.tailrec

/** Cache of p2 repositories by their URL.
 *
 *  The cache is thread safe, but it possible to fetch the content of a repository in multiple time in parallels.
 *  This is not a real problem as the result should be consistent.
 */
object P2Repositories {

  private val cacheLock = new Object
  private var cache = HashMap[URL, P2Repository]()

  def apply(url: URL): Future[P2Repository] = {
    import ExecutionContext.Implicits.global
    cacheLock.synchronized {
      cache.get(url)
    } match {
      case Some(repository) =>
        Future(repository)
      case None =>
        P2Repository(url).map {
          repository =>
            cacheLock.synchronized {
              cache += ((url, repository))
            }
            repository
        }
    }
  }

  def clear() {
    cacheLock.synchronized {
      cache = HashMap()
    }
  }

}

object P2Repository {
  import ExecutionContext.Implicits.global

  def apply(siteUrl: URL): Future[P2Repository] = {

    tryContent(siteUrl, "content.jar", true) recoverWith {
      case e: Exception =>
        tryComposite(siteUrl, "compositeContent.jar", true)
    } recoverWith {
      case e: Exception =>
        tryContent(siteUrl, "content.xml", false)
    } recoverWith {
      case e: Exception =>
        tryComposite(siteUrl, "compositeContent.xml", false)
    } recover {
      case e: Exception =>
        InvalidP2Repository
    }

  }

  private def tryContent(siteUrl: URL, fileName: String, compressed: Boolean): Future[P2Repository] =
    Future {
      val fileURL = new URL(siteUrl, fileName)

      val fileStream = fileURL.openConnection().getInputStream()

      if (compressed) {
        unzippedStreamFor(fileStream, "content.xml") match {
          case Some(stream) =>
            p2RepositoryFromContentXMLStream(stream, siteUrl)
          case None =>
            InvalidP2Repository
        }
      } else {
        p2RepositoryFromContentXMLStream(fileStream, siteUrl)
      }
    }

  private def p2RepositoryFromContentXMLStream(stream: InputStream, siteUrl: URL): P2Repository = {
    val xmlData = XML.load(stream)

    // TODO: handle case when file exists, but is invalid => return an InvalidP2Repository instance

    val unitsXML = (xmlData \ "units" \\ "unit")
    val units = unitsXML.flatMap(InstallableUnit(_))
    val groupedUnits = units.groupBy(_.id)
    val sortedUnits = for ((key, values) <- groupedUnits) yield (key, TreeSet(values: _*)(InstallableUnit.DescendingOrdering))
    ContentP2Repository(sortedUnits, siteUrl)

  }

  private def tryComposite(siteUrl: URL, fileName: String, compressed: Boolean): Future[P2Repository] = {
    Future {
      val fileURL = new URL(siteUrl, fileName)

      val fileStream = fileURL.openConnection().getInputStream()

      if (compressed) {
        unzippedStreamFor(fileStream, "compositeContent.xml") map {
          childrenFromCompositeXMLStream(_, siteUrl)
        }
      } else {
        Some(childrenFromCompositeXMLStream(fileStream, siteUrl))
      }
    } flatMap {
      case Some(children) =>
        createP2RepositoryFromChildren(siteUrl, children)
      case None =>
        Future(InvalidP2Repository)
    }
  }

  private def childrenFromCompositeXMLStream(stream: InputStream, siteUrl: URL): List[URL] = {
    val xmlData = XML.load(stream)

    // TODO: handle case when file exists, but is invalid => return an InvalidP2Repository instance

    val childrenXML = (xmlData \ "children" \\ "child")

    childrenXML map {
      child =>
        new URL(siteUrl, child \ "@location" text)
    } toList
  }

  private def createP2RepositoryFromChildren(siteUrl: URL, children: List[URL]): Future[P2Repository] = {
    Future.traverse(children)(P2Repositories(_)) map {
      CompositeP2Repository(_, siteUrl)
    }
  }

  private def unzippedStreamFor(zippedStream: InputStream, fileName: String): Option[InputStream] = {
    val unzippedStream = new ZipInputStream(zippedStream)

    @tailrec
    def selectEntry(): Option[InputStream] = {
      val entry = unzippedStream.getNextEntry()
      if (entry == null) {
        // unable to find the file
        None
      } else if (entry.getName() == fileName) {
        Some(unzippedStream)
      } else {
        selectEntry
      }
    }

    selectEntry

  }

  private def URLStream(url: URL): Future[InputStream] = {
    Future {
      url.openConnection().getInputStream()
    }
  }
}

trait P2Repository {

  def findIU(id: String): SortedSet[InstallableUnit]

}

case class ContentP2Repository(UIs: Map[String, TreeSet[InstallableUnit]], location: URL) extends P2Repository {
  override def findIU(id: String) = UIs.get(id) match {
    case Some(units) =>
      units
    case None =>
      InvalidP2Repository.emptyUISet
  }
}

case class CompositeP2Repository(children: List[P2Repository], location: URL) extends P2Repository {
  override def findIU(id: String) =
    children.flatMap(_.findIU(id)).to[SortedSet]
}

object InvalidP2Repository extends P2Repository {

  val emptyUISet = TreeSet[InstallableUnit]()(InstallableUnit.DescendingOrdering)

  override def findIU(id: String) = emptyUISet
}

object InstallableUnit {

  def apply(unit: Node): Option[InstallableUnit] = {
    if (isBundle(unit) || isFeature(unit))
      Some(InstallableUnit(unit \ "@id" text, new Version(unit \ "@version" text), getDependencies(unit)))
    else
      None
  }

  private def getDependencies(unit: Node): List[DependencyUnit] = {
    (unit \ "requires" \ "required" flatMap (DependencyUnit(_))).toList
  }

  private def isBundle(unit: Node) = unit \ "artifacts" \ "artifact" \ "@classifier" exists (a => a.text == "osgi.bundle")

  private def isFeature(unit: Node) = unit \ "properties" \ "property" exists (e => (e \ "@name" text) == "org.eclipse.equinox.p2.type.group" && (e \ "@value" text) == "true")

  implicit object DescendingOrdering extends Ordering[InstallableUnit] {
    override def compare(x: InstallableUnit, y: InstallableUnit): Int = {
      val diffId = x.id.compareTo(y.id)
      if (diffId == 0) -1 * x.version.compareTo(y.version) // same bundle name, compare versions
      else diffId
    }
  }
}

case class InstallableUnit(id: String, version: Version, dependencies: List[DependencyUnit])

object DependencyUnit {

  def apply(required: Node): Option[DependencyUnit] = {
    val namespace = (required \ "@namespace" text)
    if (namespace == "osgi.bundle" || namespace == "org.eclipse.equinox.p2.iu")
      Some(new DependencyUnit(required \ "@name" text, new VersionRange(required \ "@range" text)))
    else
      None
  }

}

case class DependencyUnit(id: String, range: VersionRange)