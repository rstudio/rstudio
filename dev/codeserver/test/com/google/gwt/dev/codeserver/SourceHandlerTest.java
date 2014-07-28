package com.google.gwt.dev.codeserver;

import static com.google.gwt.dev.codeserver.SourceHandler.SOURCEMAP_PATH;
import static com.google.gwt.dev.codeserver.SourceHandler.SOURCEMAP_SUFFIX;

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

  private static final String VALID_MODULE_NAME = "myModule";
  private static final String VALID_STRONG_NAME = Util.computeStrongName("foo-bar".getBytes());

  /**
   * Test {@link SourceHandler#isSourceMapRequest(String)}
   */
  @Test
  public void testIsSourceMapRequest() {
    assertTrue(SourceHandler.isSourceMapRequest(SOURCEMAP_PATH + VALID_MODULE_NAME + "/"));
    assertTrue(SourceHandler.isSourceMapRequest(SOURCEMAP_PATH + VALID_MODULE_NAME + "/whatever"));
    assertTrue(SourceHandler.isSourceMapRequest(SOURCEMAP_PATH + VALID_MODULE_NAME + "/folder/"));
    assertTrue(SourceHandler.isSourceMapRequest(
        SOURCEMAP_PATH + VALID_MODULE_NAME + "/folder/file.ext"));
    assertTrue(SourceHandler.isSourceMapRequest(
        SOURCEMAP_PATH + VALID_MODULE_NAME + "/" + VALID_STRONG_NAME + SOURCEMAP_SUFFIX));

    assertFalse(SourceHandler.isSourceMapRequest(SOURCEMAP_PATH + VALID_MODULE_NAME));
    assertFalse(SourceHandler.isSourceMapRequest(
        "whatever" + SOURCEMAP_PATH + VALID_MODULE_NAME + "/"));
  }

  /**
   * Test {@link SourceHandler#getModuleNameFromRequest(String)}
   */
  @Test
  public void testGetModuleNameFromRequest() {
    assertEquals(VALID_MODULE_NAME, SourceHandler.getModuleNameFromRequest(
        SOURCEMAP_PATH + VALID_MODULE_NAME + "/"));
    assertEquals(VALID_MODULE_NAME, SourceHandler.getModuleNameFromRequest(
        SOURCEMAP_PATH + VALID_MODULE_NAME + "/" + VALID_STRONG_NAME + SOURCEMAP_SUFFIX));
  }

  /**
   * Test {@link SourceHandler#getStrongNameFromSourcemapFilename(String)}
   */
  @Test
  public void testGwtStrongNameFromSourcemapFilename() {
    assertEquals(VALID_STRONG_NAME, SourceHandler
        .getStrongNameFromSourcemapFilename(VALID_STRONG_NAME + SOURCEMAP_SUFFIX));
    assertNull(SourceHandler.getStrongNameFromSourcemapFilename("invalid_hash" + SOURCEMAP_SUFFIX));
    assertNull(SourceHandler.getStrongNameFromSourcemapFilename(
        "whatever/" + VALID_STRONG_NAME + SOURCEMAP_SUFFIX));
  }
}
