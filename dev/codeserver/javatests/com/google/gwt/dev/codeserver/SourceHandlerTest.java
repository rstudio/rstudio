/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.dev.codeserver;

import com.google.gwt.dev.util.Util;

import junit.framework.TestCase;

/**
 * Tests for {@link SourceHandler}
 */
public class SourceHandlerTest extends TestCase {

  private static final String VALID_STRONG_NAME = Util.computeStrongName("foo-bar".getBytes());

  public void testIsSourceMapRequest() {
    checkSourceMapRequest("/sourcemaps/myModule/");
    checkSourceMapRequest("/sourcemaps/myModule/whatever");
    checkSourceMapRequest("/sourcemaps/myModule/folder/");
    checkSourceMapRequest("/sourcemaps/myModule/folder/file.ext");
    checkSourceMapRequest("/sourcemaps/myModule/" + VALID_STRONG_NAME + "_sourcemap.json");

    checkNotSourceMapRequest("/sourcemaps/myModule");
    checkNotSourceMapRequest("whatever/sourcemaps/myModule/");
  }

  public void testGetModuleNameFromRequest() {
    assertEquals("myModule", SourceHandler.getModuleNameFromRequest(
        "/sourcemaps/myModule/"));
    assertEquals("myModule", SourceHandler.getModuleNameFromRequest(
        "/sourcemaps/myModule/1234_sourcemap.json"));
  }

  public void testGetStrongNameFromSourcemapFilename() {
    assertEquals(VALID_STRONG_NAME, SourceHandler
        .getStrongNameFromSourcemapFilename(VALID_STRONG_NAME + "_sourcemap.json"));
    checkNoStrongName("invalid_hash_sourcemap.json");
    checkNoStrongName("whatever/" + VALID_STRONG_NAME + "_sourcemap.json");
    checkNoStrongName(VALID_STRONG_NAME + "_sourcemap/json");
  }

  private void checkSourceMapRequest(String validUrl) {
    assertTrue("should be a valid sourcemap URL but isn't: " + validUrl,
      SourceHandler.isSourceMapRequest(validUrl));
  }

  private void checkNotSourceMapRequest(String validUrl) {
    assertFalse("should not be a valid sourcemap URL but is: " + validUrl,
      SourceHandler.isSourceMapRequest(validUrl));
  }

  private void checkNoStrongName(String rest) {
    assertNull("shouldn't have returned a strong name for: " + rest,
      SourceHandler.getStrongNameFromSourcemapFilename(rest));
  }
}
