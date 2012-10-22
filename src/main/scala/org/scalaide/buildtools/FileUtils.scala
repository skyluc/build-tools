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

}