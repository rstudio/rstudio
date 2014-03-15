/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.javac;

import com.google.gwt.dev.util.Util;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Tests for {@link CompilationUnitArchive}.
 */
public class CompilationUnitArchiveTest extends CompilationStateTestBase {

  public void testReadWrite() throws IOException, ClassNotFoundException {
    rebuildCompilationState();
    List<CompilationUnit> compilationUnits =
        Lists.newArrayList(state.getCompilationUnitMap().values());

    CompilationUnitArchive archive1 = new CompilationUnitArchive("com.example.Foo");
    for (CompilationUnit compilationUnit : compilationUnits) {
      archive1.addUnit(compilationUnit);
    }

    assertEquals(compilationUnits.size(), archive1.getUnits().size());
    for (CompilationUnit compilationUnit : compilationUnits) {
      compareUnits(compilationUnit, archive1, compilationUnit.getTypeName());
    }

    File tmp = File.createTempFile("cu-archive-test", ".ser");
    tmp.deleteOnExit();
    archive1.writeToFile(tmp);
    CompilationUnitArchive archive2 = CompilationUnitArchive.createFromFile(tmp);

    assertEquals(archive1.getUnits().size(), archive2.getUnits().size());
    for (CompilationUnit compilationUnit : archive1.getUnits().values()) {
      compareUnits(compilationUnit, archive2, compilationUnit.getTypeName());
    }
  }

  /**
   * Some build systems insist that given the same inputs, the compiler produce
   * the same output every time.
   */
  public void testDeterministicOutput() throws IOException {
    rebuildCompilationState();
    List<CompilationUnit> compilationUnits =
        Lists.newArrayList(state.getCompilationUnitMap().values());

    assertTrue(compilationUnits.size() > 10);
    File tmpDir = Utility.makeTemporaryDirectory(null, "cgmt-");
    int numLoops = 100;
    String lastStrongName = null;
    for (int i = 0; i < numLoops; i++) {
      File tmpFile = new File(tmpDir, "module" + i + ".ser");
      tmpFile.deleteOnExit();
      Collections.shuffle(compilationUnits);
      CompilationUnitArchive archive = new CompilationUnitArchive("com.example.Module");
      for (CompilationUnit compilationUnit : compilationUnits) {
        archive.addUnit(compilationUnit);
      }
      archive.writeToFile(tmpFile);
      // grab the md5 signature of the file as a string
      byte[] bytes = Util.readFileAsBytes(tmpFile);
      tmpFile.delete();
      String thisStrongName = Util.computeStrongName(bytes);
      if (lastStrongName != null) {
        assertEquals("loop " + i, thisStrongName, lastStrongName);
      }
      lastStrongName = thisStrongName;
    }
    tmpDir.delete();
  }

  private void compareUnits(CompilationUnit compilationUnit, CompilationUnitArchive archive,
      String lookupType) {
    CompilationUnit found = archive.findUnit(compilationUnit.getResourcePath());
    assertEquals(found.getTypeName(), lookupType);
    assertEquals(found.getResourceLocation(), compilationUnit.getResourceLocation());
    assertTrue(!compilationUnit.getTypes().isEmpty());
    assertEquals(compilationUnit.getTypes().size(), found.getTypes().size());
  }
}
