/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dev.resource.impl;

import com.google.gwt.dev.util.Util;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

public class FileResourceTest extends TestCase {

  public void testDeletion() {
    File f = null;
    try {
      f = File.createTempFile("com.google.gwt.dev.javac.impl.FileResourceTest",
          ".tmp");
      f.deleteOnExit();
      Util.writeStringAsFile(f, "contents 1");
    } catch (IOException e) {
      fail("Failed to create test file");
    }

    File dir = f.getParentFile();
    DirectoryClassPathEntry cpe = new DirectoryClassPathEntry(dir);
    FileResource r = new FileResource(cpe, f.getName(), f);
    assertEquals(f.getAbsoluteFile().toURI().toString(), r.getLocation());

    /*
     * In this case, there's no subdirectory, so the path should match the
     * simple filename.
     */
    assertEquals(f.getName(), r.getPath());

    // Shouldn't be stale yet.
    assertFalse(r.isStale());

    /*
     * Touch the file at more than one second to ensure there's a noticeable
     * difference on every platform.
     */
    // Ignore failure of setLastModified 
    f.setLastModified(f.lastModified() + 1500);

    // Should be stale now.
    assertTrue(r.isStale());
  }

  public void testModification() {
    File f = null;
    try {
      f = File.createTempFile("com.google.gwt.dev.javac.impl.FileResourceTest",
          ".tmp");
      f.deleteOnExit();
      Util.writeStringAsFile(f, "contents 1");
    } catch (IOException e) {
      fail("Failed to create test file");
    }

    File dir = f.getParentFile();
    DirectoryClassPathEntry cpe = new DirectoryClassPathEntry(dir);
    FileResource r = new FileResource(cpe, f.getName(), f);
    assertEquals(f.getAbsoluteFile().toURI().toString(), r.getLocation());

    /*
     * In this case, there's no subdirectory, so the path should match the
     * simple filename.
     */
    assertEquals(f.getName(), r.getPath());

    // Shouldn't be stale yet.
    assertFalse(r.isStale());

    // Delete the file.
    f.delete();

    // Should be stale now.
    assertTrue(r.isStale());

    // Get can't contents anymore, either.
    assertNull(r.openContents());
  }
}
