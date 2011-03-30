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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.Util;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

/**
 * Unit test for {@link PersistentUnitCache}.
 */
public class PersistentUnitCacheTest extends TestCase {

  File lastCacheDir = null;

  public void tearDown() {
    if (lastCacheDir != null) {
      Util.recursiveDelete(lastCacheDir, false);
    }
    lastCacheDir = null;
  }

  /**
   * The cache should recursively create the directories it needs.
   */
  public void testNewDir() throws IOException, UnableToCompleteException {
    TreeLogger logger = TreeLogger.NULL;
    File baseDir = File.createTempFile("PersistentUnitTest-newDir", "");
    assertNotNull(baseDir);
    assertTrue(baseDir.exists());
    assertTrue(baseDir.delete());
    File newDir = lastCacheDir = new File(baseDir, "sHoUlDnOtExi57");
    new PersistentUnitCache(logger, newDir);
    assertTrue(newDir.isDirectory());
  }

  /**
   * Test if a file already exists with the name we want to put the
   * cache dir in.
   */
  public void testFileInTheWay() throws IOException {
    TreeLogger logger = TreeLogger.NULL;
    File fileInTheWay = File.createTempFile("PersistentUnitTest-inTheWay", "");
    assertNotNull(fileInTheWay);
    assertTrue(fileInTheWay.exists());
    fileInTheWay.deleteOnExit();
    try {
      new PersistentUnitCache(logger, fileInTheWay);
      fail("Expected an exception to be thrown");
    } catch (UnableToCompleteException expected) {
    }
  }

  public void testPersistentCache() throws IOException, InterruptedException,
      UnableToCompleteException {
    TreeLogger logger = TreeLogger.NULL;

    File cacheDir = null;
    lastCacheDir = cacheDir = File.createTempFile("persistentCacheTest", "");
    assertNotNull(cacheDir);
    // Wait, this needs to be a directory, not a file.
    assertTrue(cacheDir.delete());
    // directory will get cleaned up in tearDown()
    assertTrue(cacheDir.mkdir());

    File unitCacheDir = new File(cacheDir, PersistentUnitCache.UNIT_CACHE_PREFIX);
    assertNull(unitCacheDir.list());
    PersistentUnitCache cache = new PersistentUnitCache(logger, cacheDir);

    MockCompilationUnit foo1 = new MockCompilationUnit("com.example.Foo", "Foo: source1");
    cache.add(foo1);
    MockCompilationUnit bar1 = new MockCompilationUnit("com.example.Bar", "Bar: source1");
    cache.add(bar1);
    CompilationUnit result;

    // Find by content Id
    result = cache.find(foo1.getContentId());
    assertNotNull(result);
    assertEquals("com.example.Foo", result.getTypeName());
    result = cache.find(bar1.getContentId());
    assertNotNull(result);
    assertEquals("com.example.Bar", result.getTypeName());

    // Find by type name
    result = cache.find("/mock/com/example/Foo.java");
    assertNotNull(result);
    assertEquals("com.example.Foo", result.getTypeName());
    result = cache.find("/mock/com/example/Bar.java");
    assertNotNull(result);
    assertEquals("com.example.Bar", result.getTypeName());

    // Replace Foo with a new version
    MockCompilationUnit foo2 = new MockCompilationUnit("com.example.Foo", "Foo: source2");
    cache.add(foo2);
    result = cache.find(foo1.getContentId());
    assertNull(result);
    result = cache.find(foo2.getContentId());
    assertNotNull(result);
    assertEquals("com.example.Foo", result.getTypeName());
    result = cache.find("/mock/com/example/Foo.java");
    assertNotNull(result);
    assertEquals("com.example.Foo", result.getTypeName());
    assertEquals(foo2.getContentId(), result.getContentId());
    cache.cleanup(logger);

    // Shutdown the cache and re -load it
    cache.shutdown();

    // There should be a single file in the cache dir.
    assertNumCacheFiles(unitCacheDir, 1);

    // Fire up the cache again. It be pre-populated.
    // Search by type name
    cache = new PersistentUnitCache(logger, cacheDir);
    result = cache.find("/mock/com/example/Foo.java");
    assertNotNull(result);
    assertEquals("com.example.Foo", result.getTypeName());
    assertEquals(foo2.getContentId(), result.getContentId());
    result = cache.find("/mock/com/example/Bar.java");
    assertNotNull(result);
    assertEquals("com.example.Bar", result.getTypeName());
    assertEquals(bar1.getContentId(), result.getContentId());

    // Search by Content ID.
    // old version of Foo should not be there.
    result = cache.find(foo1.getContentId());
    assertNull(result);
    result = cache.find(bar1.getContentId());
    assertNotNull(result);
    assertEquals(bar1.getTypeName(), result.getTypeName());
    assertEquals(bar1.getContentId(), result.getContentId());
    result = cache.find(foo2.getContentId());
    assertNotNull(result);
    assertEquals(foo2.getTypeName(), result.getTypeName());
    assertEquals(foo2.getContentId(), result.getContentId());
    cache.cleanup(logger);

    // Now there should be 2 files.
    cache.shutdown();
    assertNumCacheFiles(unitCacheDir, 2);

    // keep making more files
    MockCompilationUnit lastUnit = null;
    assertTrue(PersistentUnitCache.CACHE_FILE_THRESHOLD > 3);
    for (int i = 3; i < PersistentUnitCache.CACHE_FILE_THRESHOLD; ++i) {
      cache = new PersistentUnitCache(logger, cacheDir);
      lastUnit = new MockCompilationUnit("com.example.Foo", "Foo Source" + i);
      cache.add(lastUnit);
      cache.cleanup(logger);
      cache.shutdown();
      assertNumCacheFiles(unitCacheDir, i);
    }

    // One last check, we should load the last unit added to the cache.
    cache = new PersistentUnitCache(logger, cacheDir);
    result = cache.find(lastUnit.getContentId());
    assertNotNull(result);
    assertEquals("com.example.Foo", result.getTypeName());
    assertEquals(lastUnit.getContentId(), result.getContentId());

    result = cache.find(bar1.getContentId());
    assertNotNull(result);
    assertEquals("com.example.Bar", result.getTypeName());
    assertEquals(bar1.getContentId(), result.getContentId());

    result = cache.find("/mock/com/example/Foo.java");
    assertNotNull(result);
    assertEquals("com.example.Foo", result.getTypeName());
    assertEquals(lastUnit.getContentId(), result.getContentId());

    result = cache.find("/mock/com/example/Bar.java");
    assertNotNull(result);
    assertEquals("com.example.Bar", result.getTypeName());
    assertEquals(bar1.getContentId(), result.getContentId());

    lastUnit = new MockCompilationUnit("com.example.Foo", "Foo Source");
    cache.add(lastUnit);

    // This time, the cleanup logic should coalesce the logs into one file
    // again.
    cache.cleanup(logger);
    cache.shutdown();
    assertNumCacheFiles(unitCacheDir, 1);
  }

  private void assertNumCacheFiles(File unitCacheDir, int expected) {
    assertEquals(expected, unitCacheDir.list().length);
  }
}
