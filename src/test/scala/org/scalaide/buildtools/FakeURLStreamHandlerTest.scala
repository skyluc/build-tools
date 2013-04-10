package org.scalaide.buildtools

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.ProtocolException
import java.net.URL

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.fail
import org.junit.Test

object FakeURLStreamHandlerTest {
    def createFakeURL(path: String) = new URL("fake:" + path)

}

class FakeURLStreamHandlerTest {
  import FakeURLStreamHandlerTest._

  @Test
  def testDefined() {
    val path = "/testDefined"
    val content = "some value for testDefined".getBytes
    FakeURLStreamHandler.register(path, content)
    val url = createFakeURL(path)

    val inputStream = url.openStream()
    val actual = readFully(inputStream)

    assertArrayEquals("Content doesn't match", content, actual)
  }

  @Test(expected = classOf[ProtocolException])
  def testUndefined() {
    // need to register something, to be sure that 'fake' is a valid protocol
    FakeURLStreamHandler.register("/something", Array[Byte](1, 3))

    createFakeURL("/undefined").openStream()
  }

  @Test
  def deregisterAll() {
    val path = "/deregisterAll"
    val content = "some temp content".getBytes
    FakeURLStreamHandler.register(path, content)

    val url = createFakeURL(path)

    assertArrayEquals("Content doesn't match", content, readFully(url.openStream()))

    FakeURLStreamHandler.deregisterAll

    try {
      url.openStream
      fail("No ProtocolException thrown")
    } catch {
      case e: ProtocolException =>
      case e: Throwable =>
        fail("Wrong exception. Expected: ProtocolException, was: %s".format(e))
    }

  }

  private def readFully(stream: InputStream): Array[Byte] = {
    val BufferSize = 512
    val buffer = new Array[Byte](BufferSize)

    val fullBuffer = new ByteArrayOutputStream()
    def read() {
      val bufferRead = stream.read(buffer)
      if (bufferRead >= 0) {
        fullBuffer.write(buffer, 0, bufferRead)
        read()
      }
    }

    read()

    fullBuffer.toByteArray()
  }
}