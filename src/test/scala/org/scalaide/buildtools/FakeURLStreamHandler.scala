package org.scalaide.buildtools

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.ProtocolException
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler
import java.net.URLStreamHandlerFactory

import sun.net.www.protocol.file.{ Handler => FileHandler }

object FakeURLStreamHandler {

  // initialize
  URL.setURLStreamHandlerFactory(FakeURLStreamHandlerFactory)

  private val mapLock = new Object
  private var map = Map[String, Array[Byte]]()

  def register(path: String, content: Array[Byte]) {
    mapLock.synchronized {
      map += ((path, content))
    }
  }

  def openConnection(url: URL): URLConnection = {
    map.get(url.getPath()) match {
      case Some(content) =>
        new FakeURLConnection(url, content)
      case None =>
        throw new ProtocolException("Unknow fake url: %s".format(url))
    }
  }

  def deregisterAll() {
    mapLock.synchronized {
      map = Map()
    }
  }

  private class FakeURLConnection(url: URL, content: Array[Byte]) extends URLConnection(url) {

    private val inputStream = new ByteArrayInputStream(content)
    private val outputStream = new ByteArrayOutputStream()

    setDoInput(true);
    setDoOutput(true)

    override def connect() {
      // nothing to do
    }

    override def getInputStream = inputStream
    override def getOutputStream = outputStream

  }
}

class FakeURLStreamHandler extends URLStreamHandler {
  def openConnection(url: URL): URLConnection = FakeURLStreamHandler.openConnection(url)
}

object FakeURLStreamHandlerFactory extends URLStreamHandlerFactory {

  def createURLStreamHandler(protocol: String): URLStreamHandler =
    protocol match {
      case "file" =>
        new FileHandler()
      case "fake" =>
        new FakeURLStreamHandler()
      case _ =>
        throw new IllegalArgumentException
    }

}