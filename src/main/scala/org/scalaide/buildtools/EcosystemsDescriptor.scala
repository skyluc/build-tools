package org.scalaide.buildtools

import java.net.URL
import com.typesafe.config.Config
import scala.collection.JavaConverters.asScalaBufferConverter
import com.typesafe.config.ConfigFactory
import java.io.File

/** In-memory representation of a ecosystems descriptor file.
 *  The descriptor file is parsed using the typesafe-config library.
 *  
 *  Instances of this class should be created via the factory methods `load` in the
 *  companion object. 
 *   
 * Example of a ecosystems descriptor file:
 * 
 * ecosystems-descriptor {
 *   ids = ["stable-scala29", "stable-scala210", "dev-scala29", "dev-scala210"] 
 *   stable-scala29 {
 *     site = "http://download.scala-ide.org/ecosystem/scala29/stable/site"
 *     base = "http://download.scala-ide.org/ecosystem/scala29/stable/staging"
 *   }
 *   stable-scala210 {
 *     site = "http://download.scala-ide.org/ecosystem/scala210/stable/site"
 *     base = "http://download.scala-ide.org/ecosystem/scala210/stable/staging"
 *   }
 *   dev-scala29 {
 *     site = "http://download.scala-ide.org/ecosystem/scala29/dev/site"
 *     base = "http://download.scala-ide.org/ecosystem/scala29/dev/staging"
 *   }
 *   dev-scala210 {
 *     site = "http://download.scala-ide.org/ecosystem/scala210/dev/site"
 *     base = "http://download.scala-ide.org/ecosystem/scala210/dev/staging"
 *   }
 * }
 */
class EcosystemsDescriptor(config: Config) {
  val ecosystems: List[EcosystemDescriptor] = {
    val ids = config.getStringList(EcosystemsDescriptor.Keys.idsKey).asScala.toList
    for(ecosystemId <- ids) yield {
      val site = config.getString(EcosystemsDescriptor.Keys.site(ecosystemId))
      val siteUrl = new URL(site)
      val base = config.getString(EcosystemsDescriptor.Keys.base(ecosystemId))
      val baseUrl = new URL(base)
      val nextSite = config.getString(EcosystemsDescriptor.Keys.nextSite(ecosystemId))
      val nextSiteUrl = new URL(nextSite)
      val nextBase = config.getString(EcosystemsDescriptor.Keys.nextBase(ecosystemId))
      val nextBaseUrl = new URL(nextBase)
      EcosystemDescriptor(ecosystemId, siteUrl, baseUrl, nextSiteUrl, nextBaseUrl)
    }
  }
}

object EcosystemsDescriptor {
  object Keys {
    val root = "ecosystems-descriptor"
    val idsKey = root + ".ids"
    def site(ecosystemId: String): String = root + "." + ecosystemId + ".site"
    def base(ecosystemId: String): String = root + "." + ecosystemId + ".base"
    def nextSite(ecosystemId: String): String = root + "." + ecosystemId + ".nextSite"
    def nextBase(ecosystemId: String): String = root + "." + ecosystemId + ".nextBase"
  }
  
  def load(file: File): EcosystemsDescriptor = {
    new EcosystemsDescriptor(ConfigFactory.parseFile(file))  
  }
}

final case class EcosystemDescriptor(id: EcosystemId, site: URL, base: URL, nextSite: URL, nextBase: URL)
object EcosystemDescriptor {
  def apply(id: String, site: URL, base: URL, nextSite: URL, nextBase: URL): EcosystemDescriptor = 
    new EcosystemDescriptor(EcosystemId(id), site, base, nextSite, nextBase)
}
case class EcosystemId(value: String) {
  override def toString = value
}