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
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.impl.GwtAstBuilder;
import com.google.gwt.dev.util.log.speedtracer.DevModeEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.util.tools.Utility;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A class that manages a persistent cache of {@link CompilationUnit} instances.
 * Writes out {@CompilationUnit} instances to a cache in a
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
 * System Properties (see {@link UnitCacheFactory}).
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
   * Common prefix for creating directories and cache files.
   */
  static final String UNIT_CACHE_PREFIX = "gwt-unitCache";
  static final String CACHE_FILE_PREFIX = UNIT_CACHE_PREFIX + "-";

  /**
   * Creates a new file with a name based on the current system time.
   */
  private static File createCacheFile(TreeLogger logger, File cacheDirectory)
      throws UnableToCompleteException {
    File newFile = null;
    long timestamp = System.currentTimeMillis();
    try {
      do {
        newFile = new File(cacheDirectory, CACHE_FILE_PREFIX + String.format("%016X", timestamp++));
      } while (!newFile.createNewFile());
    } catch (IOException ex) {
      logger.log(TreeLogger.WARN, "Unable to create new cache log file "
          + (newFile == null ? "<not created>" : newFile.getAbsolutePath()) + ".", ex);
      throw new UnableToCompleteException();
    }

    if (!newFile.canWrite()) {
      logger.log(TreeLogger.WARN, "Unable to write to new cache log file "
          + newFile.getAbsolutePath() + ".");
      throw new UnableToCompleteException();
    }

    return newFile;
  }

  /**
   * Finds all files matching a pattern in the cache directory.
   * 
   * @return an array of sorted filenames. The file name pattern is such that
   *         sorting them alphabetically also sorts the files by age.
   */
  private static File[] getCacheFiles(File cacheDirectory) {
    if (cacheDirectory.isDirectory()) {
      File[] files = cacheDirectory.listFiles();
      List<File> cacheFiles = new ArrayList<File>();
      for (File file : files) {
        if (file.getName().startsWith(CACHE_FILE_PREFIX)) {
          cacheFiles.add(file);
        }
      }
      File[] retFiles = cacheFiles.toArray(new File[cacheFiles.size()]);
      Arrays.sort(retFiles);
      return retFiles;
    }
    return new File[0];
  }

  /**
   * There is no significance in the return value, we just want to be able
   * to tell if the purgeOldCacheFilesTask has completed.
   */
  private Future<Boolean> purgeTaskStatus;
  private AtomicBoolean purgeInProgress = new AtomicBoolean(false);
  
  private final Runnable purgeOldCacheFilesTask = new Runnable() {
    @Override
    public void run() {
      try {
        // Delete all cache files in the directory except for the currently open
        // file.
        SpeedTracerLogger.Event deleteEvent = SpeedTracerLogger.start(DevModeEventType.DELETE_CACHE);
        File[] filesToDelete = getCacheFiles(cacheDirectory);
        logger.log(TreeLogger.TRACE, "Purging cache files from " + cacheDirectory);
        for (File toDelete : filesToDelete) {
          if (!currentCacheFile.equals(toDelete)) {
            if (!toDelete.delete()) {
              logger.log(TreeLogger.WARN, "Couldn't delete file: " + toDelete);
            }
          }
        }
        deleteEvent.end();
        
        rotateCurrentCacheFile();
      } catch (UnableToCompleteException e) {
        backgroundService.shutdownNow();
      } finally {
        purgeInProgress.set(false);
      }
    }      
  };

  private final Runnable rotateCacheFilesTask = new Runnable() {
    @Override
    public void run() {
      try {
        rotateCurrentCacheFile();
      } catch (UnableToCompleteException e) {
        backgroundService.shutdownNow();
      }
      assert (currentCacheFile != null);
    }
  };

  private final Runnable shutdownThreadTask = new Runnable() {
    @Override
    public void run() {
      assert (currentCacheFile != null);
      closeCurrentCacheFile(currentCacheFile, currentCacheFileStream);
      logger.log(TreeLogger.TRACE, "Shutting down PersistentUnitCache thread");
      backgroundService.shutdownNow();
    }
  };

  /**
   * Saved to be able to wait for UNIT_MAP_LOAD_TASK to complete.
   */
  private Future<Boolean> unitMapLoadStatus;
  
  private final Runnable unitMapLoadTask = new Runnable() {
    @Override
    public void run() {
      loadUnitMap(logger, currentCacheFile);
    }
  };

  /**
   * Used to execute the above Runnables in a background thread.
   */
  private final ExecutorService backgroundService;
  
  private int unitsWritten = 0;

  private int addedSinceLastCleanup = 0;

  /**
   * A directory to store the cache files that should persist between
   * invocations.
   */
  private final File cacheDirectory;

  /**
   * Current file and stream being written to.
   */
  private File currentCacheFile;
  private ObjectOutputStream currentCacheFileStream;

  private final TreeLogger logger;

  PersistentUnitCache(final TreeLogger logger, File cacheDir) throws UnableToCompleteException {
    assert cacheDir != null;
    this.logger = logger;
    this.cacheDirectory = new File(cacheDir, UNIT_CACHE_PREFIX);
    if (logger.isLoggable(TreeLogger.TRACE)) {
      logger.log(TreeLogger.TRACE, "Persistent unit cache dir set to: "
          + this.cacheDirectory.getAbsolutePath());
    }

    if (!cacheDirectory.isDirectory() && !cacheDirectory.mkdirs()) {
      logger.log(TreeLogger.WARN, "Unable to initialize cache. Couldn't create directory "
          + cacheDirectory.getAbsolutePath() + ".");
      throw new UnableToCompleteException();
    }

    currentCacheFile = createCacheFile(logger, cacheDirectory);

    backgroundService = Executors.newSingleThreadExecutor();
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        try {
          Future<Boolean> status = backgroundService.submit(shutdownThreadTask, Boolean.TRUE);
          // Don't let the shutdown hang more than 5 seconds
          status.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          // ignore
        } catch (RejectedExecutionException e) {
          // already shutdown, ignore
        } catch (ExecutionException e) {
          logger.log(TreeLogger.ERROR, "Error during shutdown", e);
        } catch (TimeoutException e) {
          // ignore
        } finally {
          backgroundService.shutdownNow();
        }
      }
    });
    
    /**
     * Load up cached units from the persistent store in the background. The
     * {@link #add(CompilationUnit)} and {@link #find(String)} methods block if
     * invoked before this thread finishes.
     */
    unitMapLoadStatus = backgroundService.submit(unitMapLoadTask, Boolean.TRUE);

    FileOutputStream fstream = null;
    BufferedOutputStream bstream = null;

    try {
      fstream = new FileOutputStream(currentCacheFile);
      bstream = new BufferedOutputStream(fstream);
      currentCacheFileStream = new ObjectOutputStream(bstream);
    } catch (IOException ex) {
      closeCurrentCacheFile(currentCacheFile, currentCacheFileStream);
      logger.log(TreeLogger.ERROR, "Error creating cache " + currentCacheFile
          + ". Disabling cache.", ex);
      backgroundService.shutdownNow();
      throw new UnableToCompleteException();
    }
  }

  /**
   * Enqueue a unit to be written by the background thread.
   */
  @Override
  public void add(CompilationUnit newUnit) {
    awaitUnitCacheMapLoad();
    addedSinceLastCleanup++;
    super.add(newUnit);
    addImpl(unitMap.get(newUnit.getResourcePath()));
  }

  /**
   * Cleans up old cache files in the directory, migrating everything previously
   * loaded in them to the current cache file.
   * 
   * Normally, only newly compiled units are written to the current log, but
   * when it is time to cleanup, valid units from older log files need to be
   * re-written.
   */
  @Override
  public void cleanup(TreeLogger logger) {
    awaitUnitCacheMapLoad();

    if (backgroundService.isShutdown()) {
      return;
    }
    boolean shouldRotate = addedSinceLastCleanup > 0;
    logger.log(TreeLogger.TRACE, "Added " + addedSinceLastCleanup + " units to cache since last cleanup.");
    addedSinceLastCleanup = 0;
    try {
      File[] cacheFiles = getCacheFiles(cacheDirectory);
      if (cacheFiles.length < CACHE_FILE_THRESHOLD) {
        if (shouldRotate) {
          backgroundService.execute(rotateCacheFilesTask);
        }
        return;
      }

      // Check to see if the previous purge task finished.
      boolean inProgress = purgeInProgress.getAndSet(true);
      if (inProgress) {
        try {
          purgeTaskStatus.get(0, TimeUnit.NANOSECONDS);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        } catch (TimeoutException ex) {
          // purge is currently in progress.
          return;
        }
      }
      
      /*
       * Resend all units read in from the in-memory cache to the background
       * thread. They will be re-written out and the old cache files removed.
       */
      synchronized (unitMap) {
        for (UnitCacheEntry unitCacheEntry : unitMap.values()) {
          if (unitCacheEntry.getOrigin() == UnitOrigin.PERSISTENT) {
            addImpl(unitCacheEntry);
          }
        }
      }

      purgeTaskStatus = backgroundService.submit(purgeOldCacheFilesTask, Boolean.TRUE);

    } catch (ExecutionException ex) {
      throw new InternalCompilerException("Error purging cache", ex);
    } catch (RejectedExecutionException ex) {
      // Cache background thread is not running - ignore
    }
  }

  @Override
  public CompilationUnit find(ContentId contentId) {
    awaitUnitCacheMapLoad();
    return super.find(contentId);
  }

  @Override
  public CompilationUnit find(String resourcePath) {
    awaitUnitCacheMapLoad();
    return super.find(resourcePath);
  }

  public void rotateCurrentCacheFile() throws UnableToCompleteException {
    if (logger.isLoggable(TreeLogger.TRACE)) {
      logger.log(TreeLogger.TRACE, "Wrote " + unitsWritten + " units to persistent cache.");
    }

    // Close and re-open a new log file to drop object references kept
    // alive in the existing file by Java serialization.
    closeCurrentCacheFile(currentCacheFile, currentCacheFileStream);
    unitsWritten = 0;
    currentCacheFile = createCacheFile(logger, cacheDirectory);
    FileOutputStream fstream = null;
    BufferedOutputStream bstream = null;
    try {
      fstream = new FileOutputStream(currentCacheFile);
      bstream = new BufferedOutputStream(fstream);
      currentCacheFileStream = new ObjectOutputStream(bstream);
    } catch (IOException ex) {
      // Close all 3 streams, not sure where the exception occurred.
      Utility.close(bstream);
      Utility.close(fstream);
      closeCurrentCacheFile(currentCacheFile, currentCacheFileStream);
      logger.log(TreeLogger.ERROR, "Error rotating file.  Shutting down cache thread.", ex);
      throw new UnableToCompleteException();
    }
  }

  /**
   * For Unit testing - shutdown the persistent cache.
   * 
   * @throws ExecutionException
   * @throws InterruptedException
   */
  void shutdown() throws InterruptedException, ExecutionException {
    try {
      Future<Runnable> future = backgroundService.submit(shutdownThreadTask, shutdownThreadTask);
      backgroundService.shutdown();
      future.get();
    } catch (RejectedExecutionException ex) {
      // background thread is not running - ignore
    }
  }

  private void addImpl(final UnitCacheEntry entry) {
    try {
      backgroundService.execute(new Runnable() {
        @Override
        public void run() {
          try {
            assert entry.getOrigin() != UnitOrigin.ARCHIVE;
            CompilationUnit unit = entry.getUnit();
            assert unit != null;
            currentCacheFileStream.writeObject(unit);
            unitsWritten++;
          } catch (IOException ex) {
            backgroundService.shutdownNow();
            if (logger.isLoggable(TreeLogger.TRACE)) {
              logger.log(TreeLogger.TRACE, "Error saving unit to cache in: "
                  + cacheDirectory.getAbsolutePath(), ex);
            }
          }
        }
      });
    } catch (RejectedExecutionException ex) {
      // background thread is not running, ignore
    }
  }

  private synchronized void awaitUnitCacheMapLoad() {
    // wait on initial load of unit map to complete.
    try {
      if (unitMapLoadStatus != null) {
        unitMapLoadStatus.get();
        // no need to check any more.
        unitMapLoadStatus = null;
      }
    } catch (InterruptedException e) {
      throw new InternalCompilerException("Interrupted waiting for unit cache map to load.", e);
    } catch (ExecutionException e) {
      logger.log(TreeLogger.ERROR, "Failure in unit cache map load.", e);
      // keep going
      unitMapLoadStatus = null;
    }
  }

  private void closeCurrentCacheFile(File openFile, ObjectOutputStream stream) {
    Utility.close(stream);
    if (unitsWritten == 0) {
      // Remove useless empty file.
      openFile.delete();
    }
  }

  /**
   * Load everything cached on disk into memory.
   */
  private void loadUnitMap(TreeLogger logger, File currentCacheFile) {
    Event loadPersistentUnitEvent =
        SpeedTracerLogger.start(DevModeEventType.LOAD_PERSISTENT_UNIT_CACHE);
    if (logger.isLoggable(TreeLogger.TRACE)) {
      logger.log(TreeLogger.TRACE, "Looking for previously cached Compilation Units in "
          + cacheDirectory.getAbsolutePath());
    }
    try {
      if (cacheDirectory.isDirectory() && cacheDirectory.canRead()) {
        File[] files = getCacheFiles(cacheDirectory);
        for (File cacheFile : files) {
          FileInputStream fis = null;
          BufferedInputStream bis = null;
          ObjectInputStream inputStream = null;
          if (cacheFile.equals(currentCacheFile)) {
            continue;
          }
          boolean deleteCacheFile = false;
          try {
            fis = new FileInputStream(cacheFile);
            bis = new BufferedInputStream(fis);
            /*
             * It is possible for the next call to throw an exception, leaving
             * inputStream null and fis still live.
             */
            inputStream = new ObjectInputStream(bis);
            while (true) {
              CachedCompilationUnit unit = (CachedCompilationUnit) inputStream.readObject();
              if (unit == null) {
                break;
              }
              if (unit.getTypesSerializedVersion() != GwtAstBuilder.getSerializationVersion()) {
                continue;
              }
              UnitCacheEntry entry = new UnitCacheEntry(unit, UnitOrigin.PERSISTENT);
              UnitCacheEntry existingEntry = unitMap.get(unit.getResourcePath());
              /*
               * Don't assume that an existing entry is stale - an entry might
               * have been loaded already from another source like a
               * CompilationUnitArchive that is more up to date. If the
               * timestamps are the same, accept the latest version. If it turns
               * out to be stale, it will be recompiled and the updated unit
               * will win this test the next time the session starts.
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
          } catch (EOFException ex) {
            // Go on to the next file.
          } catch (IOException ex) {
            deleteCacheFile = true;
            if (logger.isLoggable(TreeLogger.TRACE)) {
              logger.log(TreeLogger.TRACE, "Ignoring and deleting cache log "
                  + cacheFile.getAbsolutePath() + " due to read error.", ex);
            }
          } catch (ClassNotFoundException ex) {
            deleteCacheFile = true;
            if (logger.isLoggable(TreeLogger.TRACE)) {
              logger.log(TreeLogger.TRACE, "Ignoring and deleting cache log "
                  + cacheFile.getAbsolutePath() + " due to deserialization error.", ex);
            }
          } finally {
            Utility.close(inputStream);
            Utility.close(bis);
            Utility.close(fis);
          }
          if (deleteCacheFile) {
            cacheFile.delete();
          } else {
            if (logger.isLoggable(TreeLogger.TRACE)) {
              logger.log(TreeLogger.TRACE, cacheFile.getName() + ": Load complete");
            }
          }
        }
      } else {
        logger
            .log(TreeLogger.TRACE,
                "Starting with empty Cache: CompilationUnit cache directory does not exist or is not readable.");
      }
    } finally {
      loadPersistentUnitEvent.end();
    }
  }
}
