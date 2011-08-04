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
import com.google.gwt.util.tools.Utility;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

public class CompilationUnitArchiveTest extends TestCase {
  private static final String MOCK_TYPE_1 = "com.example.Foo";
  private static final String MOCK_TYPE_2 = "com.example.Bar";
  private static final String MOCK_TYPE_3 = "com.example.Baz";

  /**
   * Some build systems insist that given the same inputs, the compiler produce
   * the same output every time.
   */
  public void testDeterministicOutput() throws IOException {
    int numMockTypes = 100;
    CompilationUnit mockUnits[] = new CompilationUnit[numMockTypes];
    for (int i = 0; i < numMockTypes; i++) {
      mockUnits[i] = new MockCompilationUnit("com.example.MockType" + i, "Dummy Source " + i);
    }

    File tmpDir = Utility.makeTemporaryDirectory(null, "cgmt-");
    int numLoops = 100;
    String lastStrongName = null;
    for (int i = 0; i < numLoops; i++) {
      File tmpFile = new File(tmpDir, "module" + i + ".ser");
      tmpFile.deleteOnExit();
      scrambleArray(mockUnits);
      CompilationUnitArchive archive = new CompilationUnitArchive("com.example.Module");
      for (int j = 0; j < numMockTypes; j++) {
        archive.addUnit(mockUnits[j]);
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

  public void testReadWrite() throws IOException, ClassNotFoundException {
    CompilationUnitArchive archive1 = new CompilationUnitArchive("com.example.Foo");
    MockCompilationUnit unit1 = new MockCompilationUnit(MOCK_TYPE_1, "Foo");
    MockCompilationUnit unit2 = new MockCompilationUnit(MOCK_TYPE_2, "Bar");
    MockCompilationUnit unit3 = new MockCompilationUnit(MOCK_TYPE_3, "Baz");

    archive1.addUnit(unit1);
    archive1.addUnit(unit2);
    archive1.addUnit(unit3);
    
    assertEquals(3, archive1.getUnits().size());
    compareUnits(unit1, archive1, MOCK_TYPE_1);
    compareUnits(unit2, archive1, MOCK_TYPE_2);
    compareUnits(unit3, archive1, MOCK_TYPE_3);

    File tmp = File.createTempFile("cu-archive-test", ".ser");
    tmp.deleteOnExit();
    archive1.writeToFile(tmp);
    CompilationUnitArchive archive2 = CompilationUnitArchive.createFromFile(tmp);
    
    assertEquals(3, archive2.getUnits().size());
    compareUnits(unit1, archive2, MOCK_TYPE_1);
    compareUnits(unit2, archive2, MOCK_TYPE_2);
    compareUnits(unit3, archive2, MOCK_TYPE_3);
  }

  private void compareUnits(MockCompilationUnit unit, CompilationUnitArchive archive, String lookupType) {
    CompilationUnit found = archive.findUnit(unit.getResourcePath());
    assertEquals(found.getTypeName(), lookupType);
    assertEquals(found.getResourceLocation(), unit.getResourceLocation());
  }

  private void scrambleArray(Object[] array) {
    final int max = array.length;
    for (int i = 0; i < max; i++) {
      int randomIdx;
      do {
        randomIdx = (int) (Math.random() * (max - 1));
      } while (i == randomIdx);

      Object tmp = array[randomIdx];
      array[randomIdx] = array[i % array.length];
      array[i % array.length] = tmp;
    }
  }
}
