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

/**
 * Tests related FileResources.
 */
public class FileResourceTest extends TestCase {

  public void testBasic() {
    File f = createTempFile();

    FileResource r = FileResource.of(f.getName(), f);
    assertEquals(f.getAbsoluteFile().toURI().toString(), r.getLocation());

    /*
     * In this case, there's no subdirectory, so the path should match the
     * simple filename.
     */
    assertEquals(f.getName(), r.getPath());
  }

  public void testDeletion() {
    File f = createTempFile();

    FileResource r = FileResource.of(f.getName(), f);
    assertEquals(f.getAbsoluteFile().toURI().toString(), r.getLocation());

    /*
     * In this case, there's no subdirectory, so the path should match the
     * simple filename.
     */
    assertEquals(f.getName(), r.getPath());

    // Delete the file.
    f.delete();

    /*
     *  The resource is no longer available.  Check to make sure we can't access its contents
     *  through the API.
     */
    try {
      r.openContents();
      fail("Open contents unexpectedly succeeded.");
    } catch (IOException expected) {
    }
  }

  public void testInterning() throws Exception {
    File f = createTempFile();

    assertSame(FileResource.of(f.getName(), f), FileResource.of(f.getName(), f));
    // A different FileResourse will return even files match if the pathname is different.
    assertNotSame(FileResource.of(f.getName(), f), FileResource.of(f.getName() + "x", f));
  }

  private File createTempFile() {
    File f = null;
    try {
      f = File.createTempFile("com.google.gwt.dev.javac.impl.FileResourceTest", ".tmp");
      f.deleteOnExit();
      Util.writeStringAsFile(f, "contents 1");
    } catch (IOException e) {
      fail("Failed to create test file");
    }
    return f;
  }

}
