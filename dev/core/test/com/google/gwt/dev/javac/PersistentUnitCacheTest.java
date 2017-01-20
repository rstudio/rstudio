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
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.Util;
import com.google.gwt.thirdparty.guava.common.util.concurrent.Futures;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
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

  private TreeLogger logger;
  private File lastParentDir = null;
  private String hash1 = "HASH1";
  private String hash2 = "HASH2";

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    logger = TreeLogger.NULL;
    // logger = new PrintWriterTreeLogger(); // uncomment for debugging
    logger.log(Type.INFO, "\n\n*** Running " + getName());
  }

  @Override
  public void tearDown() {
    if (lastParentDir != null) {
      Util.recursiveDelete(lastParentDir, false);
    }
    lastParentDir = null;
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
    File fileInTheWay = File.createTempFile("PersistentUnitTest-inTheWay", "");
    assertNotNull(fileInTheWay);
    assertTrue(fileInTheWay.exists());
    fileInTheWay.deleteOnExit();
    try {
      new PersistentUnitCache(logger, fileInTheWay, hash1);
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
    File baseDir = File.createTempFile("PersistentUnitTest-newDir", "");
    assertNotNull(baseDir);
    assertTrue(baseDir.exists());
    assertTrue(baseDir.delete());
    File parentDir = lastParentDir = new File(baseDir, "sHoUlDnOtExi57");
    new PersistentUnitCache(logger, parentDir, hash1);
    assertTrue(parentDir.isDirectory());
  }

  public void testPersistentCache() throws IOException, InterruptedException,
      UnableToCompleteException, ExecutionException {

    File parentDir = lastParentDir = File.createTempFile("persistentCacheTest", "");
    File unitCacheDir = mkCacheDir(parentDir);

    PersistentUnitCache cache = new PersistentUnitCache(logger, parentDir, hash1);

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
    cache.waitForCleanup();

    // Shutdown the cache and re -load it
    cache.shutdown();

    // There should be a single file in the cache dir.
    assertNumCacheFiles(unitCacheDir, 1);

    // Fire up the cache again. It should be pre-populated.
    // Search by type name
    cache = new PersistentUnitCache(logger, parentDir, hash1);
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
    cache.waitForCleanup();

    // We didn't write anything, still 1 file.
    cache.shutdown();

    // There should be a single file in the cache dir.
    assertNumCacheFiles(unitCacheDir, 1);

    // Fire up the cache again. (Creates a second file in the background.)
    cache = new PersistentUnitCache(logger, parentDir, hash1);

    // keep making more files
    MockCompilationUnit lastUnit = null;
    assertTrue(PersistentUnitCache.CACHE_FILE_THRESHOLD > 3);
    for (int i = 2; i <= PersistentUnitCache.CACHE_FILE_THRESHOLD - 2; i++) {
      lastUnit = new MockCompilationUnit("com.example.Foo", "Foo Source" + i);
      cache.internalAdd(lastUnit).get();
      assertNumCacheFiles(unitCacheDir, i);
      // force rotation to a new file
      // (Normally async but we overrode it.)
      cache.cleanup(logger);
      cache.waitForCleanup();
      assertNumCacheFiles(unitCacheDir, i + 1);
    }

    // One last check, we should load the last unit added to the cache.
    cache = new PersistentUnitCache(logger, parentDir, hash1);
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

    lastUnit = new MockCompilationUnit("com.example.Baz", "Baz Source");
    cache.add(lastUnit);
    cache.cleanup(logger);
    cache.waitForCleanup();

    // Add one more to put us over the top.
    lastUnit = new MockCompilationUnit("com.example.Qux", "Qux Source");
    Futures.getUnchecked(cache.internalAdd(lastUnit));

    // The currently open file isn't included in this count.
    assertNumCacheFiles(unitCacheDir, PersistentUnitCache.CACHE_FILE_THRESHOLD + 1);

    cache.cleanup(logger);
    cache.waitForCleanup();
    cache.shutdown();

    // There should be a single file in the cache dir.
    assertNumCacheFiles(unitCacheDir, 1);

    // Fire up the cache on this one coalesced file.
    cache = new PersistentUnitCache(logger, parentDir, hash1);

    // Verify that we can still find the content that was coalesced.
    assertNotNull(cache.find("com/example/Foo.java"));
    assertNotNull(cache.find("com/example/Bar.java"));
    assertNotNull(cache.find("com/example/Baz.java"));
    assertNotNull(cache.find("com/example/Qux.java"));

    cache.shutdown();

    // There should be a single file in the cache dir.
    assertNumCacheFiles(unitCacheDir, 1);

    cache = new PersistentUnitCache(logger, parentDir, hash2);
    // A different hash implies a new cache.
    assertNull(cache.find("com/example/Foo.java"));
    assertNull(cache.find("com/example/Bar.java"));
    assertNull(cache.find("com/example/Baz.java"));
    assertNull(cache.find("com/example/Qux.java"));

    cache.internalAdd(
        new MockCompilationUnit("com.example.Hash2", "Foo Source")).get();

    assertNotNull(cache.find("com/example/Hash2.java"));

    cache.shutdown();

    // There should be a single file in the cache dir.
    assertNumCacheFiles(unitCacheDir, 2);

    cache = new PersistentUnitCache(logger, parentDir, hash1);
    // Verify that we can still find the content with the original hash.
    assertNotNull(cache.find("com/example/Foo.java"));
    assertNotNull(cache.find("com/example/Bar.java"));
    assertNotNull(cache.find("com/example/Baz.java"));
    assertNotNull(cache.find("com/example/Qux.java"));
    assertNull(cache.find("com/example/Hash2.java"));

    cache.shutdown();

    cache = new PersistentUnitCache(logger, parentDir, hash2);
    // A different hash implies a new cache.
    assertNull(cache.find("com/example/Foo.java"));
    assertNull(cache.find("com/example/Bar.java"));
    assertNull(cache.find("com/example/Baz.java"));
    assertNull(cache.find("com/example/Qux.java"));
    assertNotNull(cache.find("com/example/Hash2.java"));

    cache.shutdown();
  }

  private void assertNumCacheFiles(File unitCacheDir, int expected) {
    String[] actualFiles = unitCacheDir.list();
    if (expected == actualFiles.length) {
      return;
    }

    // list the files
    Arrays.sort(actualFiles);
    System.out.println("\nCache file dump (exected " + expected + ")");
    for (String file : actualFiles) {
      System.out.println(file);
    }
    System.out.println();

    fail("expected " + expected + " cache files but got " + actualFiles.length
      + " (see stdout for list)");
  }

  private void checkInvalidObjectInCache(Object toSerialize) throws IOException,
      UnableToCompleteException, InterruptedException, ExecutionException {
    File parentDir = lastParentDir = File.createTempFile("PersistentUnitTest-CNF", "");
    File unitCacheDir = mkCacheDir(parentDir);

    /*
     * Create a cache file that has the right filename, but the wrong kind of
     * object in it.
     */
    File errorFile = new File(unitCacheDir,
        PersistentUnitCacheDir.CURRENT_VERSION_CACHE_FILE_PREFIX + "-"  + hash1 + "-" + "12345");
    ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(errorFile));
    os.writeObject(toSerialize);
    os.close();

    assertNumCacheFiles(unitCacheDir, 1);

    PersistentUnitCache cache = new PersistentUnitCache(logger, parentDir, hash1);
    cache.cleanup(logger);
    cache.shutdown();

    // The bogus file should have been removed.
    assertNumCacheFiles(unitCacheDir, 0);
  }

  private File mkCacheDir(File parentDir) {
    assertNotNull(parentDir);
    assertTrue(parentDir.exists());
    parentDir.delete();
    File unitCacheDir = PersistentUnitCacheDir.chooseCacheDir(parentDir);
    unitCacheDir.mkdirs();
    return unitCacheDir;
  }
}
