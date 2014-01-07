/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.util;

import com.google.gwt.util.tools.Utility;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

/**
 * Tests for {@link OutputFileSetOnDirectory}
 */
public class OutputFileSetOnDirectoryTest extends TestCase {

  public void testCreateNewOutputStream() throws IOException {
    File work = Utility.makeTemporaryDirectory(null, "outputfileset");
    try {

      OutputFileSetOnDirectory output = new OutputFileSetOnDirectory(work, "test/");
      int tstamp = OutputFileSet.TIMESTAMP_UNAVAILABLE;

      output.createNewOutputStream("path/to/file", tstamp).close();
      assertTrue(new File(work, "test/path/to/file").exists());

      output.createNewOutputStream("path/../to/file", tstamp).close();
      assertTrue(new File(work, "test/to/file").exists());

      output.createNewOutputStream("/../path/../to/./file", tstamp).close();
      assertTrue(new File(work, "to/file").exists());

    } finally {
      Util.recursiveDelete(work, false);
    }
  }

}
