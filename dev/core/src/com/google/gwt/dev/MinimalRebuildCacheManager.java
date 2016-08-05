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
import com.google.gwt.dev.util.CompilerVersion;
import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;
import com.google.gwt.thirdparty.guava.common.cache.Cache;
import com.google.gwt.thirdparty.guava.common.cache.CacheBuilder;
import com.google.gwt.thirdparty.guava.common.util.concurrent.Futures;
import com.google.gwt.thirdparty.guava.common.util.concurrent.MoreExecutors;
import com.google.gwt.util.tools.Utility;
import com.google.gwt.util.tools.shared.Md5Utils;
import com.google.gwt.util.tools.shared.StringUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Manages caching of MinimalRebuildCache instances.
 * <p>
 * Changes are immediately performed in memory and are asynchronously persisted to disk in original
 * request order.
 */
public class MinimalRebuildCacheManager {

  private static final int MEMORY_CACHE_COUNT_LIMIT = 3;
  private static final String REBUILD_CACHE_PREFIX = "gwt-rebuildCache";

  private final ExecutorService executorService =
      MoreExecutors.getExitingExecutorService((ThreadPoolExecutor) Executors.newFixedThreadPool(1));
  private final TreeLogger logger;
  private final File minimalRebuildCacheDir;
  private final Cache<String, MinimalRebuildCache> minimalRebuildCachesByName =
      CacheBuilder.newBuilder().maximumSize(MEMORY_CACHE_COUNT_LIMIT).build();
  private final Map<String, String> options = new LinkedHashMap<>();

  public MinimalRebuildCacheManager(
      TreeLogger logger, File baseCacheDir, Map<String, String> options) {
    this.logger = logger;
    this.options.putAll(options);
    if (baseCacheDir != null) {
      minimalRebuildCacheDir = new File(baseCacheDir, REBUILD_CACHE_PREFIX);
      minimalRebuildCacheDir.mkdir();
    } else {
      minimalRebuildCacheDir = null;
    }
  }

  /**
   * Synchronously delete all in memory caches managed here and all on disk in the managed folder.
   */
  public synchronized void deleteCaches() {
    syncDeleteMemoryCaches();
    if (haveCacheDir()) {
      Futures.getUnchecked(enqueueAsyncDeleteDiskCaches());
    }
  }

  /**
   * Synchronously return the MinimalRebuildCache specific to the given module and binding
   * properties.
   * <p>
   * If no cache is found in memory then it will be synchronously loaded from disk.
   * <p>
   * If it is still not found a new empty cache will be returned.
   */
  public synchronized MinimalRebuildCache getCache(String moduleName,
      PermutationDescription permutationDescription) {
    String cacheName =
        computeMinimalRebuildCacheName(moduleName, permutationDescription);

    MinimalRebuildCache minimalRebuildCache = minimalRebuildCachesByName.getIfPresent(cacheName);

    // If there's no cache already in memory, try to load a cache from disk.
    if (minimalRebuildCache == null && haveCacheDir()) {
      // Might return null.
      minimalRebuildCache = syncReadDiskCache(moduleName, permutationDescription);
      if (minimalRebuildCache != null) {
        minimalRebuildCachesByName.put(cacheName, minimalRebuildCache);
      }
    }

    // If there's still no cache loaded, just create a blank one.
    if (minimalRebuildCache == null) {
      minimalRebuildCache = new MinimalRebuildCache();
      minimalRebuildCachesByName.put(cacheName, minimalRebuildCache);
      return minimalRebuildCache;
    }

    // Return a copy.
    MinimalRebuildCache mutableMinimalRebuildCache = new MinimalRebuildCache();
    mutableMinimalRebuildCache.copyFrom(minimalRebuildCache);
    return mutableMinimalRebuildCache;
  }

  /**
   * Stores a MinimalRebuildCache specific to the given module and binding properties.
   * <p>
   * A copy of the cache will be lazily persisted to disk as well.
   */
  public synchronized void putCache(String moduleName,
      PermutationDescription permutationDescription,
      MinimalRebuildCache knownGoodMinimalRebuildCache) {
    syncPutMemoryCache(moduleName, permutationDescription, knownGoodMinimalRebuildCache);
    if (haveCacheDir()) {
      enqueueAsyncWriteDiskCache(moduleName, permutationDescription, knownGoodMinimalRebuildCache);
    }
  }

  /**
   * Enqueue to asynchronously delete all on disk caches in the managed cache folder.
   */
  @VisibleForTesting
  synchronized Future<Void> enqueueAsyncDeleteDiskCaches() {
    return executorService.submit(new Callable<Void>() {
      @Override
      public Void call() {
        for (File cacheFile : minimalRebuildCacheDir.listFiles()) {
          if (!cacheFile.delete()) {
            logger.log(TreeLogger.WARN, "Couldn't delete " + cacheFile);
          }
        }
        return null;
      }
    });
  }

  /**
   * Enqueue to asynchronously find, read and return the MinimalRebuildCache unique to this module
   * and binding properties combination in the managed cache folder.
   */
  @VisibleForTesting
  synchronized Future<MinimalRebuildCache> enqueueAsyncReadDiskCache(final String moduleName,
      final PermutationDescription permutationDescription) {
    return executorService.submit(new Callable<MinimalRebuildCache>() {
      @Override
      public MinimalRebuildCache call() {
        // Find the cache file unique to this module, binding properties and working directory.
        File minimalRebuildCacheFile =
            computeMinimalRebuildCacheFile(moduleName, permutationDescription);

        // If the file exists.
        if (minimalRebuildCacheFile.exists()) {
          ObjectInputStream objectInputStream = null;
          // Try to read it.
          try {
            objectInputStream = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(minimalRebuildCacheFile)));
            return (MinimalRebuildCache) objectInputStream.readObject();
          } catch (IOException e) {
            logger.log(TreeLogger.WARN,
                "Unable to read the rebuild cache in " + minimalRebuildCacheFile + ".");
            Utility.close(objectInputStream);
            minimalRebuildCacheFile.delete();
          } catch (ClassNotFoundException e) {
            logger.log(TreeLogger.WARN,
                "Unable to read the rebuild cache in " + minimalRebuildCacheFile + ".");
            Utility.close(objectInputStream);
            minimalRebuildCacheFile.delete();
          } finally {
            Utility.close(objectInputStream);
          }
        }
        return null;
      }
    });
  }

  /**
   * Enqueue to asynchronously write the provided MinimalRebuildCache to disk.
   * <p>
   * Persisted caches are uniquely named based on the compiler version, current module name, binding
   * properties and the location where the JVM was launched.
   * <p>
   * Care is taken to completely and successfully write a new cache (to a different location on
   * disk) before replacing the old cache (at the regular location on disk).
   * <p>
   * Write requests will occur in the order requested and will queue up if requests are made faster
   * than they can be completed.
   */
  @VisibleForTesting
  synchronized Future<Void> enqueueAsyncWriteDiskCache(final String moduleName,
      final PermutationDescription permutationDescription,
      final MinimalRebuildCache minimalRebuildCache) {
    return executorService.submit(new Callable<Void>() {
      @Override
      public Void call() {
        File oldMinimalRebuildCacheFile =
            computeMinimalRebuildCacheFile(moduleName, permutationDescription);
        File newMinimalRebuildCacheFile =
            new File(oldMinimalRebuildCacheFile.getAbsoluteFile() + ".new");

        // Ensure the cache folder exists.
        oldMinimalRebuildCacheFile.getParentFile().mkdirs();

        // Write the new cache to disk.
        ObjectOutputStream objectOutputStream = null;
        try {
          objectOutputStream = new ObjectOutputStream(
              new BufferedOutputStream(new FileOutputStream(newMinimalRebuildCacheFile)));
          objectOutputStream.writeObject(minimalRebuildCache);
          Utility.close(objectOutputStream);

          // Replace the old cache file with the new one.
          oldMinimalRebuildCacheFile.delete();
          newMinimalRebuildCacheFile.renameTo(oldMinimalRebuildCacheFile);
        } catch (IOException e) {
          logger.log(TreeLogger.WARN,
              "Unable to update the cache in " + oldMinimalRebuildCacheFile + ".");
          newMinimalRebuildCacheFile.delete();
        } finally {
          if (objectOutputStream != null) {
            Utility.close(objectOutputStream);
          }
        }
        return null;
      }
    });
  }

  /**
   * For testing only. Stops accepting any new tasks and waits for current tasks to complete.
   */
  @VisibleForTesting
  boolean shutdown() throws InterruptedException {
    executorService.shutdown();
    return executorService.awaitTermination(30, TimeUnit.SECONDS);
  }

  /**
   * Find, read and return the MinimalRebuildCache unique to this module, binding properties and
   * working directory.
   */
  @VisibleForTesting
  synchronized MinimalRebuildCache syncReadDiskCache(String moduleName,
      PermutationDescription permutationDescription) {
    return Futures.getUnchecked(enqueueAsyncReadDiskCache(moduleName, permutationDescription));
  }

  private File computeMinimalRebuildCacheFile(String moduleName,
      PermutationDescription permutationDescription) {
    return new File(minimalRebuildCacheDir,
        computeMinimalRebuildCacheName(moduleName, permutationDescription));
  }

  private String computeMinimalRebuildCacheName(String moduleName,
      PermutationDescription permutationDescription) {
    String currentWorkingDirectory = System.getProperty("user.dir");
    String compilerVersionHash = CompilerVersion.getHash();
    String permutationDescriptionString = permutationDescription.toString();
    String optionsDescriptionString = " Options [";
    String separator = "";
    for (Map.Entry entry : options.entrySet()) {
      optionsDescriptionString +=
          String.format("%s%s = %s", separator, entry.getKey(), entry.getValue());
      separator = ",";
    }
    optionsDescriptionString += "]";

    String consistentHash = StringUtils.toHexString(Md5Utils.getMd5Digest((
        compilerVersionHash
            + moduleName
            + currentWorkingDirectory
            + permutationDescriptionString
            + optionsDescriptionString)
        .getBytes()));
    return REBUILD_CACHE_PREFIX + "-" + consistentHash;
  }

  private boolean haveCacheDir() {
    return minimalRebuildCacheDir != null && minimalRebuildCacheDir.isDirectory();
  }

  private void syncDeleteMemoryCaches() {
    minimalRebuildCachesByName.invalidateAll();
  }

  private void syncPutMemoryCache(String moduleName, PermutationDescription permutationDescription,
      MinimalRebuildCache knownGoodMinimalRebuildCache) {
    String cacheName = computeMinimalRebuildCacheName(moduleName, permutationDescription);
    minimalRebuildCachesByName.put(cacheName, knownGoodMinimalRebuildCache);
  }
}
