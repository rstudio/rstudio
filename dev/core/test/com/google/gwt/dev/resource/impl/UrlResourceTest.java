/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.dev.resource.impl;

import com.google.gwt.dev.util.Util;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Tests for {@link UrlResource}.
 */
public class UrlResourceTest extends TestCase {

  private static File createFile(String contents) throws IOException {
    File file = File.createTempFile("Something.gwt", ".xml");
    file.deleteOnExit();
    Util.writeStringAsFile(file, contents);
    return file;
  }

  @SuppressWarnings("deprecation")
  private static UrlResource createUrlResource(File file)
      throws MalformedURLException, IOException {
    URL url = file.toURL();
    UrlResource urlResource =
        new UrlResource(url, file.getName(), url.openConnection().getLastModified());
    return urlResource;
  }

  public void testOpen() throws IOException {
    String expectedModuleContents = "<module></module>";

    File file = createFile(expectedModuleContents);
    UrlResource urlResource = createUrlResource(file);

    assertEquals(expectedModuleContents, Util.readStreamAsString(urlResource.openContents()));
  }

  public void testPaths() throws IOException {
    File file = createFile("<module></module>");
    UrlResource urlResource = createUrlResource(file);

    assertEquals(file.getAbsoluteFile().toURI().toString(), urlResource.getLocation());
    assertEquals(file.getName(), urlResource.getPath());
  }
}
