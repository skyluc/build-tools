package org.scalaide.buildtools

import org.junit.BeforeClass
import org.junit.Test
import org.junit.Assert._
import java.net.URL
import java.io.InputStream
import java.io.ByteArrayOutputStream
import java.net.ProtocolException

class FakeURLStreamHandlerTest {
  
  @Test
  def testDefined() {
    val path = "/testDefined"
    val content = "some value for testDefined".getBytes
    FakeURLStreamHandler.register(path, content)
    val url= new URL("fake:" + path)
    
    
    val inputStream = url.openStream()
    val actual= readFully(inputStream)
    
    assertArrayEquals("Content doesn't match", content, actual)
  }
  
  @Test(expected= classOf[ProtocolException])
  def testUndefined() {
    // need to register something, to be sure that 'fake' is a valid protocol
    FakeURLStreamHandler.register("/something", Array[Byte](1, 3))
    
    new URL("fake:/testUndefined").openStream()
  }
  

  private def readFully(stream: InputStream): Array[Byte] = {
    val BufferSize= 512
    val buffer= new Array[Byte](BufferSize)
    
    val fullBuffer= new ByteArrayOutputStream()
    def read() {
      val bufferRead = stream.read(buffer)
      if (bufferRead >=0) {
        fullBuffer.write(buffer, 0, bufferRead)
        read()
      }
    }
    
    read()
    
    fullBuffer.toByteArray()
  }
}