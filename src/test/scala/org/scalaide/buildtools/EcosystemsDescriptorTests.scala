package org.scalaide.buildtools

import scala.collection.JavaConversions._
import scala.collection.Map
import org.junit.Before
import org.junit.Test
import com.typesafe.config.ConfigFactory
import java.net.URL
import org.junit.Assert

class EcosystemsDescriptorTests {
  import EcosystemsDescriptor.Keys._

  @Test
  def ecosystemsDescriptorIsCorrectlyLoaded() {
    val expected = List(EcosystemDescriptor("stable-scala29", new URL("http://download.scala-ide.org/ecosystem/scala29/stable/site"), new URL("http://download.scala-ide.org/ecosystem/scala29/stable/base/site")),
                        EcosystemDescriptor("stable-scala210", new URL("http://download.scala-ide.org/ecosystem/scala210/stable/site"), new URL("http://download.scala-ide.org/ecosystem/scala210/stable/base/site")),
                        EcosystemDescriptor("dev-scala29", new URL("http://download.scala-ide.org/ecosystem/scala29/dev/site"), new URL("http://download.scala-ide.org/ecosystem/scala29/dev/base/site")),
                        EcosystemDescriptor("dev-scala210", new URL("http://download.scala-ide.org/ecosystem/scala210/dev/site"), new URL("http://download.scala-ide.org/ecosystem/scala210/dev/base/site")))    
    val resource = EcosystemsDescriptor.getClass().getResource("ecosystems.conf")
    val file = new java.io.File(resource.toURI())

    val descriptor = EcosystemsDescriptor.load(file)
    
    Assert.assertEquals(expected, descriptor.ecosystems)
  }
}