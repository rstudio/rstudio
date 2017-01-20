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
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;
import com.google.gwt.thirdparty.guava.common.base.Preconditions;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A class that manages a persistent cache of {@link CompilationUnit} instances.
 * Writes out {@link CompilationUnit} instances to a cache in a
 * background thread.
 * <p>
 * The persistent cache is implemented as a directory of log files with a date
 * timestamp. A new log file gets created each time a new PersistentUnitCache is
 * instantiated, (once per invocation of the compiler or DevMode). The design is
 * intended to support only a single PersistentUnitCache instance in the
 * compiler at a time.
 * <p>
 * As new units are compiled, the cache data is appended to a log. This allows
 * Java serialization to efficiently store references. The next time the cache
 * is started, all logs are replayed and loaded into the cache in chronological
 * order, with newer units taking precedence. A new cache file is created for
 * any newly compiled units in this session. After a threshold of a certain
 * number of files in the directory is reached
 * {@link PersistentUnitCache#CACHE_FILE_THRESHOLD} , the cache files are
 * consolidated back into a single file.
 *
 * <p>
 * System Properties (see {@link UnitCacheSingleton}).
 *
 * <ul>
 * <li>gwt.persistentunitcache : enables the persistent cache (eventually will
 * be default)</li>
 * <li>gwt.persistentunitcachedir=<dir>: sets or overrides the cache directory</li>
 * </ul>
 *
 * <p>
 * Known Issues:
 *
 * <ul>
 * <li>This design uses an eager cache to load every unit in the cache on the
 * first reference to find() or add(). When the cache is large (10000 units), it
 * uses lots of heap and takes 5-10 seconds. Once the PersistentUnitCache is
 * created, it starts eagerly loading the cache in a background thread).</li>
 *
 * <li>Although units logged to disk with the same resource path are eventually
 * cleaned up, the most recently compiled unit stays in the cache forever. This
 * means that stale units that are no longer referenced will never be purged,
 * unless by some external action (e.g. ant clean).</li>
 *
 * <li>Unless ant builds are made aware of the cache directory, the cache will
 * persist if a user does an ant clean.</li>
 * </ul>
 *
 */
class PersistentUnitCache extends MemoryUnitCache {

  /**
   * If there are more than this many files in the cache, clean up the old
   * files.
   */
  static final int CACHE_FILE_THRESHOLD = 40;

  /**
   * Note: to avoid deadlock, methods on backgroundService should not be called from
   * within a synchronized method. (The BackgroundService lock should be acquired first.)
   */
  private final BackgroundService backgroundService;

  private Semaphore cleanupInProgress = new Semaphore(1);
  private AtomicInteger newUnitsSinceLastCleanup = new AtomicInteger();
  private final String relevantOptionsHash;

  PersistentUnitCache(final TreeLogger logger, File parentDir, String relevantOptionsHash)
      throws UnableToCompleteException {
    this.relevantOptionsHash = relevantOptionsHash;
    this.backgroundService = new BackgroundService(logger, parentDir, this);
  }

  /**
   * Enqueue a unit to be written by the background thread.
   */
  @Override
  public void add(CompilationUnit newUnit) {
    internalAdd(newUnit);
  }

  @VisibleForTesting
  Future<?> internalAdd(CompilationUnit newUnit) {
    Preconditions.checkNotNull(newUnit);
    backgroundService.waitForCacheToLoad();
    addNewUnit(newUnit);
    return backgroundService.asyncWriteUnit(newUnit);
  }

  @Override
  public void clear() throws UnableToCompleteException {
    backgroundService.asyncClearCache();
    backgroundService.finishAndShutdown();
    synchronized (this) {
      super.clear();
    }
    backgroundService.start();
  }

  /**
   * Rotates to a new file and/or starts garbage collection if needed after a compile is finished.
   *
   * Normally, only newly compiled units are written to the current log, but
   * when it is time to cleanup, valid units from older log files need to be
   * re-written.
   */
  @Override
  public void cleanup(TreeLogger logger) {
    logger.log(Type.TRACE, "PersistentUnitCache cleanup requested");
    backgroundService.waitForCacheToLoad();

    if (backgroundService.isShutdown()) {
      logger.log(TreeLogger.TRACE, "Skipped PersistentUnitCache cleanup because it's shut down");
      return;
    }

    if (!cleanupInProgress.tryAcquire()) {
      return; // some other thread is already doing this.
    }

    int addCallCount = newUnitsSinceLastCleanup.getAndSet(0);
    logger.log(TreeLogger.TRACE, "Added " + addCallCount +
        " units to PersistentUnitCache since last cleanup");
    if (addCallCount == 0) {
      // Don't clean up until we compiled something.
      logger.log(TreeLogger.TRACE, "Skipped PersistentUnitCache because no units were added");
      cleanupInProgress.release();
      return;
    }

    int closedCount = backgroundService.getClosedCacheFileCount();
    if (closedCount < CACHE_FILE_THRESHOLD) {
      // Not enough files yet, so just rotate to a new file.
      logger.log(TreeLogger.TRACE, "Rotating PersistentUnitCache file because only " +
          closedCount + " files were added.");
      backgroundService.asyncRotate(cleanupInProgress);
      return;
    }

    logger.log(Type.TRACE, "Compacting persistent unit cache files");
    backgroundService.asyncCompact(getUnitsToSaveToDisk(), cleanupInProgress);
  }

  /**
   * Waits for any cleanup in progress to finish.
   */
  @VisibleForTesting
  void waitForCleanup() throws InterruptedException {
    cleanupInProgress.acquire();
    cleanupInProgress.release();
  }

  @VisibleForTesting
  void shutdown() throws InterruptedException, ExecutionException {
    backgroundService.shutdown();
  }

  // Methods that read or write the in-memory cache

  @Override
  public CompilationUnit find(ContentId contentId) {
    backgroundService.waitForCacheToLoad();
    synchronized (this) {
      return super.find(contentId);
    }
  }

  @Override
  public CompilationUnit find(String resourcePath) {
    backgroundService.waitForCacheToLoad();
    synchronized (this) {
      return super.find(resourcePath);
    }
  }

  @Override
  public synchronized void remove(CompilationUnit unit) {
    super.remove(unit);
  }

  /**
   * Saves a newly compiled unit to the in-memory cache.
   */
  private synchronized void addNewUnit(CompilationUnit unit) {
    newUnitsSinceLastCleanup.incrementAndGet();
    super.add(unit);
  }

  /**
   * Adds a compilation unit from disk into the in-memory cache.
   * (Callback from {@link PersistentUnitCacheDir}.)
   */
  synchronized void maybeAddLoadedUnit(CachedCompilationUnit unit) {
    UnitCacheEntry entry = new UnitCacheEntry(unit, UnitOrigin.PERSISTENT);
    UnitCacheEntry existingEntry = unitMap.get(unit.getResourcePath());
    /*
     * Don't assume that an existing entry is stale - an entry might have been loaded already from
     * another source that is more up to date. If the timestamps are the same, accept the latest
     * version. If it turns out to be stale, it will be recompiled and the updated unit will win
     * this test the next time the session starts.
     */
    if (existingEntry != null
        && unit.getLastModified() >= existingEntry.getUnit().getLastModified()) {
      super.remove(existingEntry.getUnit());
      unitMap.put(unit.getResourcePath(), entry);
      unitMapByContentId.put(unit.getContentId(), entry);
    } else if (existingEntry == null) {
      unitMap.put(unit.getResourcePath(), entry);
      unitMapByContentId.put(unit.getContentId(), entry);
    }
  }

  private synchronized List<CompilationUnit> getUnitsToSaveToDisk() {
    List<CompilationUnit> result = Lists.newArrayList();
    for (UnitCacheEntry entry : unitMap.values()) {
      result.add(Preconditions.checkNotNull(entry.getUnit()));
    }
    return result;
  }

  /**
   * Implements async methods that run in the background.
   */
  private static class BackgroundService {

    private final TreeLogger logger;
    private final PersistentUnitCacheDir cacheDir;
    private ExecutorService service;
    private PersistentUnitCache cacheToLoad;

    /**
     * Non-null while the unit cache is loading.
     */
    private Future<?> loadingDone;

    /**
     * Starts the background thread and starts loading the given unit cache in the background.
     */
    BackgroundService(TreeLogger logger, File parentDir, final PersistentUnitCache cacheToLoad)
        throws UnableToCompleteException {
      this.logger = logger;
      this.cacheDir =
          new PersistentUnitCacheDir(logger, parentDir, cacheToLoad.relevantOptionsHash);
      this.cacheToLoad = cacheToLoad;

      start();
    }

    /**
     * Blocks addition of any further tasks and waits for current tasks to finish.
     */
    public void finishAndShutdown() throws UnableToCompleteException {
      service.shutdown();
      try {
        if (!service.awaitTermination(30, TimeUnit.SECONDS)) {
          logger.log(TreeLogger.WARN,
              "Persistent Unit Cache shutdown tasks took longer than 30 seconds to complete.");
          throw new UnableToCompleteException();
        }
      } catch (InterruptedException e) {
        // JVM is shutting down, ignore it.
      }
    }

    private void start() {
      assert service == null || service.isTerminated();
      service = Executors.newSingleThreadExecutor();
      Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
          try {
            Future<?> status = asyncShutdown();
            // Don't let the shutdown hang more than 5 seconds
            status.get(5, TimeUnit.SECONDS);
          } catch (InterruptedException e) {
            // ignore
          } catch (RejectedExecutionException e) {
            // already shutdown, ignore
          } catch (ExecutionException e) {
            BackgroundService.this.logger.log(TreeLogger.ERROR, "Error during shutdown", e);
          } catch (TimeoutException e) {
            // ignore
          } finally {
            shutdownNow();
          }
        }
      });

      /**
       * Load up cached units from the persistent store in the background. The
       * {@link #add(CompilationUnit)} and {@link #find(String)} methods block if
       * invoked before this thread finishes.
       */
      loadingDone = service.submit(new Runnable() {
        @Override
        public void run() {
          cacheDir.loadUnitMap(cacheToLoad);
        }
      });
    }

    /**
     * Blocks until the background service is done loading units into the in-memory cache.
     * Note: don't call this from a synchronized method on PersistentUnitCache.
     */
    synchronized void waitForCacheToLoad() {
      if (loadingDone == null) {
        return; // fast path
      }

      try {
        loadingDone.get();
        loadingDone = null;
      } catch (InterruptedException e) {
        throw new InternalCompilerException(
            "Interrupted waiting for PersistentUnitCache to load.", e);
      } catch (ExecutionException e) {
        logger.log(TreeLogger.ERROR, "Failed to load PersistentUnitCache.", e);
        // Keep going. We didn't load anything but will still save units to the cache.
        loadingDone = null;
      }
    }

    boolean isShutdown() {
      return service.isShutdown();
    }

    @VisibleForTesting
    void shutdown() throws InterruptedException, ExecutionException {
      logger.log(Type.INFO, "PersistentUnitCache shutdown requested");
      try {
        asyncShutdown().get();
      } catch (RejectedExecutionException ex) {
        // background thread is not running - ignore
      }
    }

    int getClosedCacheFileCount() {
      return cacheDir.getClosedCacheFileCount();
    }

    /**
     * Rotates to a new file.
     * @param cleanupInProgress a semaphore to release when done.
     * (The permit must already be acquired.)
     */
    Future<?> asyncRotate(final Semaphore cleanupInProgress) {
      return service.submit(new Runnable() {
        @Override
        public void run() {
          try {
            cacheDir.rotate();
          } catch (UnableToCompleteException e) {
            shutdownNow();
          } finally {
            cleanupInProgress.release();
          }
        }
      });
    }

    /**
     * Compacts the persistent unit cache and then rotates to a new file.
     * There will be one closed file and one empty, open file when done.
     * @param unitsToSave all compilation units to keep
     * @param cleanupInProgress a semaphore to release when done.
     * (The permit must already be acquired.)
     */
    Future<?> asyncCompact(final List<CompilationUnit> unitsToSave,
        final Semaphore cleanupInProgress) {

      return service.submit(new Runnable() {
        @Override
        public void run() {
          try {
            for (CompilationUnit unit : unitsToSave) {
              cacheDir.writeUnit(unit);
            }
            cacheDir.deleteClosedCacheFiles();
            cacheDir.rotate(); // Move to a new, empty file.
          } catch (UnableToCompleteException e) {
            shutdownNow();
          } finally {
            cleanupInProgress.release();
          }
        }
      });
    }

    Future<?> asyncClearCache() {
      Future<?> status = service.submit(new Runnable() {
        @Override
        public void run() {
          cacheDir.closeCurrentFile();
          cacheDir.deleteClosedCacheFiles();
        }
      });
      service.shutdown(); // Don't allow more tasks to be scheduled.
      return status;
    }

    Future<?> asyncWriteUnit(final CompilationUnit unit) {
      try {
        return service.submit(new Runnable() {
          @Override
          public void run() {
            try {
              cacheDir.writeUnit(unit);
            } catch (UnableToCompleteException e) {
              shutdownNow();
            }
          }
        });
      } catch (RejectedExecutionException ex) {
        // background thread is not running, ignore
        return null;
      }
    }

    Future<?> asyncShutdown() {
      Future<?> status = service.submit(new Runnable() {
        @Override
        public void run() {
          cacheDir.closeCurrentFile();
          shutdownNow();
        }
      });
      service.shutdown(); // Don't allow more tasks to be scheduled.
      return status;
    }

    private void shutdownNow() {
      logger.log(TreeLogger.TRACE, "Shutting down PersistentUnitCache thread");
      service.shutdownNow();
    }
  }
}
