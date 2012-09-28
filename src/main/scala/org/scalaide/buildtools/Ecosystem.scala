package org.scalaide.buildtools

object Ecosystem {
  
  final val ScalaLibraryId = "org.scala-ide.scala.library"
  final val ScalaCompilerId = "org.scala-ide.scala.compiler"
  final val ScalaIDEId = "org.scala-ide.sdt.core"
    
  final val PluginManifest = "META-INF/MANIFEST.MF"
  final val FeatureDescriptor = "feature.xml"

  final val RootOption = "--root=(.*)".r
}