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
package com.google.gwt.dev;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.cfg.PropertyCombinations.PermutationDescription;
import com.google.gwt.dev.jjs.ast.JTypeOracle;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableMap;
import com.google.gwt.thirdparty.guava.common.collect.Sets;
import com.google.gwt.thirdparty.guava.common.io.Files;

import junit.framework.TestCase;

import java.io.File;
import java.util.Map;

/**
 * Tests for {@link MinimalRebuildCacheManager}.
 */
public class MinimalRebuildCacheManagerTest extends TestCase {

  public void testCacheChange() throws InterruptedException {
    String moduleName = "com.google.FooModule";
    Map<String, String> initialCompilerOptions = ImmutableMap.of("option", "oldvalue");
    PermutationDescription permutationDescription = new PermutationDescription();
    File cacheDir = Files.createTempDir();

    MinimalRebuildCacheManager minimalRebuildCacheManager =
        new MinimalRebuildCacheManager(
            TreeLogger.NULL, cacheDir, initialCompilerOptions);

    // Make sure we start with a blank slate.
    minimalRebuildCacheManager.deleteCaches();

    // Construct and empty cache and also ask the manager to get a cache which does not exist.
    MinimalRebuildCache emptyCache = new MinimalRebuildCache();
    MinimalRebuildCache fooCacheOld = minimalRebuildCacheManager.getCache(moduleName,
        permutationDescription);

    // Show that the manager created a new empty cache for the request of a cache that does not
    // exist. Do not test instnace equality as getCache always returns a copy.
    assertTrue(emptyCache.hasSameContent(fooCacheOld));

    // Set some data into the cache
    fooCacheOld.addTypeReference("Type1", "Type2");
    assertFalse(emptyCache.hasSameContent(fooCacheOld));

    // Set the cache back and shut down the manager to wait for the cache to be written to disk.
    minimalRebuildCacheManager.enqueueAsyncWriteDiskCache(
        moduleName, permutationDescription, fooCacheOld);
    minimalRebuildCacheManager.shutdown();

    // Change compiler options and get a new cache manager.
    Map<String, String> newCompilerOptions = ImmutableMap.of("option", "newvalue");
    minimalRebuildCacheManager =
        new MinimalRebuildCacheManager(
            TreeLogger.NULL, cacheDir, newCompilerOptions);

    // Now get the cache for FooModule under different compiler flags
    MinimalRebuildCache fooCacheNew = minimalRebuildCacheManager.getCache(moduleName,
        permutationDescription);

    // Show that the manager created a new empty cache for the request of a cache that does not
    // exist.
    assertTrue(emptyCache.hasSameContent(fooCacheNew));
    assertFalse(fooCacheOld.hasSameContent(fooCacheNew));

    // Set the cache back and shut down the manager to wait for the cache to be written to disk.
    minimalRebuildCacheManager.enqueueAsyncWriteDiskCache(
        moduleName, permutationDescription, fooCacheNew);
    minimalRebuildCacheManager.shutdown();

    // Switch back to the initial option values and verify you get the same old cache.
    minimalRebuildCacheManager =
        new MinimalRebuildCacheManager(
            TreeLogger.NULL, cacheDir, initialCompilerOptions);

    // Now get the cache for FooModule under different under initial options values.
    MinimalRebuildCache fooCacheResetOptions = minimalRebuildCacheManager.getCache(
        moduleName,
        permutationDescription);

    // Show that the manager retrieved the already existing cache for FooModule under initial
    // compiler options.
    assertTrue(fooCacheOld.hasSameContent(fooCacheResetOptions));
    minimalRebuildCacheManager.deleteCaches();
    minimalRebuildCacheManager.shutdown();
    cacheDir.delete();
  }

  public void testReload() throws InterruptedException {
    File cacheDir = Files.createTempDir();

    String moduleName = "com.google.FooModule";
    MinimalRebuildCacheManager minimalRebuildCacheManager =
        new MinimalRebuildCacheManager(
            TreeLogger.NULL, cacheDir, ImmutableMap.<String, String>of());
    PermutationDescription permutationDescription = new PermutationDescription();

    // Make sure we start with a blank slate.
    minimalRebuildCacheManager.deleteCaches();

    MinimalRebuildCache startingCache =
        minimalRebuildCacheManager.getCache(moduleName, permutationDescription);

    // Record and compute a bunch of random data.
    Map<String, Long> currentModifiedBySourcePath = new ImmutableMap.Builder<String, Long>().put(
        "Foo.java", 0L).put("Bar.java", 0L).put("Baz.java", 0L).build();
    startingCache.recordDiskSourceResources(currentModifiedBySourcePath);
    startingCache.recordNestedTypeName("Foo", "Foo");
    startingCache.setJsForType(TreeLogger.NULL, "Foo", "Some Js for Foo");
    startingCache.addTypeReference("Bar", "Foo");
    startingCache.getImmediateTypeRelations().getImmediateSuperclassesByClass().put("Baz", "Foo");
    startingCache.addTypeReference("Foo", "Foo$Inner");
    Map<String, Long> laterModifiedBySourcePath = new ImmutableMap.Builder<String, Long>().put(
        "Foo.java", 9999L).put("Bar.java", 0L).put("Baz.java", 0L).build();
    startingCache.recordDiskSourceResources(laterModifiedBySourcePath);
    startingCache.setRootTypeNames(Sets.newHashSet("Foo", "Bar", "Baz"));
    StringAnalyzableTypeEnvironment typeEnvironment = startingCache.getTypeEnvironment();
    typeEnvironment.recordTypeEnclosesMethod("Foo", "Foo::$clinit()V");
    typeEnvironment.recordTypeEnclosesMethod("Bar", "Bar::$clinit()V");
    typeEnvironment.recordTypeEnclosesMethod("Baz", "Baz::$clinit()V");
    typeEnvironment.recordMethodInstantiatesType("Foo::start()V", "Bar");
    typeEnvironment.recordMethodCallsMethod("Foo::start()V", "Bar::run()V");
    typeEnvironment.recordMethodInstantiatesType("Bar::start()V", "Baz");
    typeEnvironment.recordMethodCallsMethod("Bar::run()V", "Baz::run()V");
    startingCache.computeReachableTypeNames();
    startingCache.computeAndClearStaleTypesCache(TreeLogger.NULL,
        new JTypeOracle(null, startingCache));
    startingCache.addExportedGlobalName("alert", "Window", "void Widow.alert()");

    // Save and reload the cache.
    minimalRebuildCacheManager.putCache(moduleName, permutationDescription, startingCache);

    // Shutdown the cache manager and make sure it was successful.
    assertTrue(minimalRebuildCacheManager.shutdown());

    // Start a new cache manager in the same folder.
    MinimalRebuildCacheManager reloadedMinimalRebuildCacheManager =
        new MinimalRebuildCacheManager(
            TreeLogger.NULL, cacheDir, ImmutableMap.<String, String>of());

    // Reread the previously saved cache.
    MinimalRebuildCache reloadedCache =
        reloadedMinimalRebuildCacheManager.syncReadDiskCache(moduleName, permutationDescription);

    // Show that the reread cache is a different instance.
    assertFalse(startingCache == reloadedCache);
    // Show that the reread cache contains the same data as the original.
    assertTrue(startingCache.hasSameContent(reloadedCache));
  }
}
