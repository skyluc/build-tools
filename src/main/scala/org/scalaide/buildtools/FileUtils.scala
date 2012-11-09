package org.scalaide.buildtools

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import scala.xml.XML
import scala.xml.Elem

object FileUtils {

  def copyFile(in: File, out: File) {
    val inChannel = new FileInputStream(in).getChannel();
    val outChannel = new FileOutputStream(out).getChannel();
    try {
      inChannel.transferTo(0, inChannel.size(),
        outChannel);
    } finally {
      if (inChannel != null) inChannel.close();
      if (outChannel != null) outChannel.close();
    }
  }

  def deleteFull(file: File) {
    if (file.isDirectory()) {
      file.listFiles().foreach(deleteFull(_))
    }
    file.delete()
  }

  def saveXml(file: File, xml: Elem) {
    XML.save(file.getAbsolutePath(), xml, "UTF-8", true)
  }
  
  def relativePath(folder: File, file: File): String = {
    val folderSegments= extractFileSegments(folder)
    val fileSegments= extractFileSegments(file)
    
    def commonSegments(zipped: List[(String, String)]): Int = {
      zipped match {
        case (a, b) :: tail if a == b =>
          1 + commonSegments(tail)
        case _ =>
          0
      }
    }
    
    val common= commonSegments(folderSegments.zip(fileSegments))
    
    val sb= new StringBuilder(".")
    
    (0 until folderSegments.size - common) map {
      _ => sb.append("/..")
    }
    
    (fileSegments.drop(common)) map {
      s => sb.append('/').append(s)
    }
    
    sb.toString
  }
  
  private def extractFileSegments(file: File): List[String] = {
    def loop(file: File): List[String] = {
      file match {
        case null =>
          Nil
        case _ =>
          loop(file.getParentFile()) :+ file.getName()
      }
    }
    loop(file.getAbsoluteFile())
  }

}