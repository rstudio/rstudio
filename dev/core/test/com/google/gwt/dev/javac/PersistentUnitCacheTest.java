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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.ExecutionException;

/**
 * Unit test for {@link PersistentUnitCache}.
 */
public class PersistentUnitCacheTest extends TestCase {

  private static class ThrowsClassNotFoundException implements Serializable {
    @SuppressWarnings("unused")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
      throw new ClassNotFoundException();
    }
  }

  private static class ThrowsIOException implements Serializable {
    @SuppressWarnings("unused")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
      throw new IOException();
    }
  }

  File lastCacheDir = null;

  @Override
  public void tearDown() {
    if (lastCacheDir != null) {
      Util.recursiveDelete(lastCacheDir, false);
    }
    lastCacheDir = null;
  }

  /**
   * When a cache file encounters a serialization error, the logic should assume
   * the cache log is stale and remove it.
   */
  public void testClassNotFoundException() throws IOException, UnableToCompleteException,
      InterruptedException, ExecutionException {
    checkInvalidObjectInCache(new ThrowsClassNotFoundException());
  }

  /**
   * Test if a file already exists with the name we want to put the cache dir
   * in.
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

  /**
   * If a cache file has some kind of IO exception, (this can happen with a
   * stale cache file), then the exception should be ignored and the cache file
   * removed.
   */
  public void testIOException() throws IOException, UnableToCompleteException,
      InterruptedException, ExecutionException {
    checkInvalidObjectInCache(new ThrowsIOException());
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

  public void testPersistentCache() throws IOException, InterruptedException,
      UnableToCompleteException, ExecutionException {
    TreeLogger logger = TreeLogger.NULL;

    File cacheDir = lastCacheDir = File.createTempFile("persistentCacheTest", "");
    File unitCacheDir = mkCacheDir(cacheDir);

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
    result = cache.find("com/example/Foo.java");
    assertNotNull(result);
    assertEquals("com.example.Foo", result.getTypeName());
    result = cache.find("com/example/Bar.java");
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
    result = cache.find("com/example/Foo.java");
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
    result = cache.find("com/example/Foo.java");
    assertNotNull(result);
    assertEquals("com.example.Foo", result.getTypeName());
    assertEquals(foo2.getContentId(), result.getContentId());
    result = cache.find("com/example/Bar.java");
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

    // We didn't write anything, still 1 file.
    cache.shutdown();
    assertNumCacheFiles(unitCacheDir, 1);

    // keep making more files
    MockCompilationUnit lastUnit = null;
    assertTrue(PersistentUnitCache.CACHE_FILE_THRESHOLD > 3);
    for (int i = 2; i <= PersistentUnitCache.CACHE_FILE_THRESHOLD - 1; i++) {
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

    result = cache.find("com/example/Foo.java");
    assertNotNull(result);
    assertEquals("com.example.Foo", result.getTypeName());
    assertEquals(lastUnit.getContentId(), result.getContentId());

    result = cache.find("com/example/Bar.java");
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

  private void checkInvalidObjectInCache(Object toSerialize) throws IOException,
      FileNotFoundException, UnableToCompleteException, InterruptedException, ExecutionException {
    TreeLogger logger = TreeLogger.NULL;
    File cacheDir = lastCacheDir = File.createTempFile("PersistentUnitTest-CNF", "");
    File unitCacheDir = mkCacheDir(cacheDir);

    /*
     * Create a cache file that has the right filename, but the wrong kind of
     * object in it.
     */
    File errorFile = new File(unitCacheDir, PersistentUnitCache.CACHE_FILE_PREFIX + "12345");
    ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(errorFile));
    os.writeObject(toSerialize);
    os.close();

    assertNumCacheFiles(unitCacheDir, 1);

    PersistentUnitCache cache = new PersistentUnitCache(logger, cacheDir);
    cache.cleanup(logger);
    cache.shutdown();

    // The bogus file should have been removed.
    assertNumCacheFiles(unitCacheDir, 0);
  }

  private File mkCacheDir(File cacheDir) {
    assertNotNull(cacheDir);
    assertTrue(cacheDir.exists());
    cacheDir.delete();
    File unitCacheDir = new File(cacheDir, PersistentUnitCache.UNIT_CACHE_PREFIX);
    unitCacheDir.mkdirs();
    return unitCacheDir;
  }
}
