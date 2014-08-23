package com.google.gwt.dev.codeserver;

import com.google.gwt.dev.util.Util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests for {@link SourceHandler}
 */
public class SourceHandlerTest {

  private static final String VALID_STRONG_NAME = Util.computeStrongName("foo-bar".getBytes());

  /**
   * Test {@link SourceHandler#isSourceMapRequest(String)}
   */
  @Test
  public void testIsSourceMapRequest() {
    checkSourceMapRequest("/sourcemaps/myModule/");
    checkSourceMapRequest("/sourcemaps/myModule/whatever");
    checkSourceMapRequest("/sourcemaps/myModule/folder/");
    checkSourceMapRequest("/sourcemaps/myModule/folder/file.ext");
    checkSourceMapRequest("/sourcemaps/myModule/" + VALID_STRONG_NAME + "_sourcemap.json");

    checkNotSourceMapRequest("/sourcemaps/myModule");
    checkNotSourceMapRequest("whatever/sourcemaps/myModule/");
  }

  /**
   * Test {@link SourceHandler#getModuleNameFromRequest(String)}
   */
  @Test
  public void testGetModuleNameFromRequest() {
    assertEquals("myModule", SourceHandler.getModuleNameFromRequest(
        "/sourcemaps/myModule/"));
    assertEquals("myModule", SourceHandler.getModuleNameFromRequest(
        "/sourcemaps/myModule/1234_sourcemap.json"));
  }

  /**
   * Test {@link SourceHandler#getStrongNameFromSourcemapFilename(String)}
   */
  @Test
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
