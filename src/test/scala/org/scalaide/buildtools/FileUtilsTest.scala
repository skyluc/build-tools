package org.scalaide.buildtools

import org.junit.Test
import org.junit.Assert._
import java.io.File

class FileUtilsTest {
  
  @Test
  def relativePathSame() {
    test(".", new File("/one/two/tree/four"), new File("/one/two/tree/four"))
  }
  
  @Test
  def relativePathSubfolder() {
    test("./tree/four", new File("/one/two/"), new File("/one/two/tree/four"))
  }
  
  @Test
  def relativePathDiffBranch() {
    test("./../tree/four", new File("/one/two/five/"), new File("/one/two/tree/four"))
  }
  
  private def test(expected: String, folder: File, file: File) {
    assertEquals("Wrong relative path", expected, FileUtils.relativePath(folder, file))
  }

}