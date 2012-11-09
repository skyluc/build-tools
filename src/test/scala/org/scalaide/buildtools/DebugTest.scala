package org.scalaide.buildtools

import java.io.File

object DebugTest {

  def main(args: Array[String]) {
    new GenerateEcosystemBuilds(new File("/home/luc/dev/scala-ide/ecosystem"))()
    println("done")
    
  }

}