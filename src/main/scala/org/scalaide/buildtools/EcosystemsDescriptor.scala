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
 *   }
 *   stable-scala210 {
 *     site = "http://download.scala-ide.org/ecosystem/scala210/stable/site"
 *   }
 *   dev-scala29 {
 *     site = "http://download.scala-ide.org/ecosystem/scala29/dev/site"
 *   }
 *   dev-scala210 {
 *     site = "http://download.scala-ide.org/ecosystem/scala210/dev/site"
 *   }
 * }
 */
class EcosystemsDescriptor(config: Config) {
  val ecosystems: List[Ecosystem] = {
    val ids = config.getStringList(EcosystemsDescriptor.Keys.idsKey).asScala.toList
    for(ecosystemId <- ids) yield {
      val site = config.getString(EcosystemsDescriptor.Keys.site(ecosystemId))
      val siteUrl = new URL(site)
      Ecosystem(ecosystemId, siteUrl)
    }
  }
}

object EcosystemsDescriptor {
  object Keys {
    val root = "ecosystems-descriptor"
    val idsKey = root + ".ids"
    def site(ecosystemId: String): String = root + "." + ecosystemId + ".site"
  }
  
  def load(file: File): EcosystemsDescriptor = {
    new EcosystemsDescriptor(ConfigFactory.parseFile(file))  
  }
}

final case class Ecosystem(id: String, site: URL)