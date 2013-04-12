package org.scalaide.buildtools

import org.junit.Test
import java.net.URL
import scala.concurrent.Await
import scala.concurrent.duration._

class SandboxTestDoNotCheckIn {
  
  @Test
  def test {
    
    val f = P2Repositories(new URL("file:/home/luc/tmp/ecosystem/all-base/org.scala-ide.sdt.feature/3.0.0.v-2_10-201303191408-1e64c72"))
    
    Await.result(f, 5.hour)
    
  }

}