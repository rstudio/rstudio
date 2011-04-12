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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
 * <li>Although units logged to disk with the same resource Location are
 * eventually cleaned up, the most recently compiled unit stays in the cache
 * forever. This means that stale units that are no longer referenced will never
 * be purged, unless by some external action (e.g. ant clean).</li>
 * 
 * <li>Unless ant builds are made aware of the cache directory, the cache will
 * persist if a user does an ant clean.</li>
 * </ul>
 * 
 */
class PersistentUnitCache extends MemoryUnitCache {
  /**
   * A thread used when the cache is instantiated to load up cached units from
   * the persistent store in the background. The
   * {@link UnitCacheFactory#addUnit(CompilationUnit)} and
   * {@link UnitCacheFactory#findUnit(String)} methods block if invoked before
   * this thread finishes.
   */
  private class UnitCacheMapLoader extends Thread {
    private final CountDownLatch loadCompleteLatch = new CountDownLatch(1);
    private final TreeLogger logger;

    public UnitCacheMapLoader(TreeLogger logger) {
      this.logger = logger;
      setDaemon(true);
      setName("UnitCacheLoader");
      setPriority(Thread.NORM_PRIORITY);
    }

    public void await() {
      try {
        loadCompleteLatch.await();
      } catch (InterruptedException ex) {
        logger.log(TreeLogger.ERROR, "Interrupted waiting for PersistentUnitCache to load.", ex);
      }
    }

    public void run() {
      try {
        loadUnitMap(logger);
      } finally {
        loadCompleteLatch.countDown();
        logger.log(TreeLogger.TRACE, "Loaded " + unitMap.size() + " units from persistent store.");
      }
    }
  }

  /**
   * Used to pass messages to the unitWriteThread.
   */
  private static class UnitWriteMessage {
    private static final UnitWriteMessage DELETE_OLD_CACHE_FILES = new UnitWriteMessage();
    private static final UnitWriteMessage SHUTDOWN_THREAD = new UnitWriteMessage();
    private final UnitCacheEntry unitCacheEntry;

    public UnitWriteMessage() {
      unitCacheEntry = null;
    }

    public UnitWriteMessage(UnitCacheEntry unitCacheEntry) {
      this.unitCacheEntry = unitCacheEntry;
    }
  }

  /**
   * Thread that reads units from a queue and writes out to a cache file for
   * this session.
   */
  private class UnitWriter extends Thread {
    private final CountDownLatch shutDownLatch = new CountDownLatch(1);
    private final TreeLogger logger;
    private boolean errorLogged = false;
    private Thread shutdownHook = new Thread() {
      @Override
      public void run() {
        try {
          doShutdown();
        } catch (InterruptedException ex) {
          // ignore
        }
      }
    };

    public UnitWriter(TreeLogger logger) {
      this.logger = logger;
      setDaemon(true);
      setName("UnitWriteThread");
      setPriority(Thread.MIN_PRIORITY);

      Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    public void run() {
      logger.log(TreeLogger.TRACE, "Starting UnitWriteThread.");

      FileOutputStream fstream = null;
      BufferedOutputStream bstream = null;
      ObjectOutputStream stream = null;
      try {
        fstream = new FileOutputStream(currentCacheFile);
        bstream = new BufferedOutputStream(fstream);
        stream = new ObjectOutputStream(bstream);
      } catch (IOException ex) {
        logger.log(TreeLogger.ERROR, "Error creating cache " + currentCacheFile
            + ". Disabling cache.", ex);
      }
      int recentUnitsWritten = 0;
      int totalUnitsWritten = 0;
      try {
        while (true) {
          UnitWriteMessage msg = null;
          try {
            msg = unitWriteQueue.take();
          } catch (InterruptedException e) {
            // Allow shutdown to interrupt
            break;
          }
          if (stream == null) {
            // if there is no output stream, just ignore the unit
            continue;
          }

          try {
            if (msg != null) {
              if (msg == UnitWriteMessage.DELETE_OLD_CACHE_FILES) {
                logger.log(TreeLogger.TRACE, "Wrote " + recentUnitsWritten
                    + " units to persistent cache.");
                recentUnitsWritten = 0;
                deleteOldCacheFiles(logger, currentCacheFile);
              } else if (msg == UnitWriteMessage.SHUTDOWN_THREAD) {
                stream.flush();
                assert unitWriteQueue.size() == 0;
                break;
              } else {
                CompilationUnit unit = msg.unitCacheEntry.getUnit();
                assert unit != null;
                stream.writeObject(unit);
                recentUnitsWritten++;
                totalUnitsWritten++;
              }
            }

            if (unitWriteQueue.isEmpty()) {
              stream.flush();
            }
          } catch (IOException ex) {
            if (!errorLogged) {
              errorLogged = true;
              logger.log(TreeLogger.TRACE, "Error saving unit to file: "
                  + currentCacheFile.getAbsolutePath(), ex);
            }
          }
        }
      } finally {
        Utility.close(stream);
        // Paranoia - close all streams
        Utility.close(bstream);
        Utility.close(fstream);
        if (totalUnitsWritten == 0) {
          // Remove useless empty output.
          currentCacheFile.delete();
        }
        shutDownLatch.countDown();
        logger.log(TreeLogger.TRACE, "Shutting down PersistentUnitCache thread");
      }
    }

    /**
     * Shutdown the thread and wait for it.
     */
    private void doShutdown() throws InterruptedException {
      // force the shutdown to finish after 5 seconds
      unitWriteQueue.add(UnitWriteMessage.SHUTDOWN_THREAD);
      // wait for shutdown
      shutDownLatch.await(5000, TimeUnit.MILLISECONDS);
      try {
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
      } catch (IllegalStateException ex) {
        // ignore.
      }
    }
  }

  /**
   * Common prefix for creating directories and cache files.
   */
  static final String UNIT_CACHE_PREFIX = "gwt-unitCache";

  static final String CACHE_FILE_PREFIX = UNIT_CACHE_PREFIX + "-";

  /**
   * If there are more than this many files in the cache, clean up the old
   * files.
   */
  static final int CACHE_FILE_THRESHOLD = 10;

  /**
   * Used for communication to the unit write thread.
   */
  private final BlockingQueue<UnitWriteMessage> unitWriteQueue =
      new LinkedBlockingQueue<UnitWriteMessage>();
  private final AtomicInteger addCount = new AtomicInteger(0);
  private final UnitCacheMapLoader unitCacheMapLoader;
  private final UnitWriter unitWriter;
  private boolean cleanupHasRun = false;

  /**
   * A directory that ideally persists between invocations.
   */
  private final File cacheDirectory;

  /**
   * Cache log file currently being written to.
   */
  private File currentCacheFile;

  PersistentUnitCache(TreeLogger logger, File cacheDir) throws UnableToCompleteException {
    assert cacheDir != null;

    this.cacheDirectory = new File(cacheDir, UNIT_CACHE_PREFIX);
    logger.log(TreeLogger.TRACE, "Persistent unit cache dir set to: "
        + this.cacheDirectory.getAbsolutePath());

    if (!cacheDirectory.isDirectory() && !cacheDirectory.mkdirs()) {
      logger.log(TreeLogger.ERROR, "Unable to initialize cache. Couldn't create directory "
          + cacheDirectory.getAbsolutePath() + ".");
      throw new UnableToCompleteException();
    }

    long timestamp = System.currentTimeMillis();
    do {
      currentCacheFile =
          new File(cacheDirectory, CACHE_FILE_PREFIX + String.format("%016X", timestamp++));
    } while (currentCacheFile.exists());

    // this isn't 100% reliable if multiple processes are in contention
    try {
      currentCacheFile.createNewFile();
    } catch (IOException ex) {
      logger.log(TreeLogger.ERROR, "Unable to create new cache log file "
          + currentCacheFile.getAbsolutePath() + ".", ex);
      throw new UnableToCompleteException();
    }

    unitCacheMapLoader = new UnitCacheMapLoader(logger);
    unitCacheMapLoader.start();
    unitWriter = new UnitWriter(logger);
    unitWriter.start();
  }

  /**
   * Enqueue a unit to be written by the background thread.
   */
  @Override
  public void add(CompilationUnit newUnit) {
    unitCacheMapLoader.await();
    super.add(newUnit);
    addCount.getAndIncrement();
    unitWriteQueue.add(new UnitWriteMessage(unitMap.get(newUnit.getResourceLocation())));
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
    logger.log(TreeLogger.TRACE, "Added " + addCount.intValue() + " units to persistent cache.");
    addCount.set(0);

    if (cleanupHasRun) {
      return;
    }

    cleanupHasRun = true;
    unitCacheMapLoader.await();
    File[] cacheFiles = getCacheFiles();

    if (cacheFiles.length < CACHE_FILE_THRESHOLD) {
      return;
    }

    /*
     * Resend all units read in from the in-memory cache to the writer thread.
     * They will be re-written out and the old cache files removed.
     */
    synchronized (unitMap) {
      for (UnitCacheEntry unitCacheEntry : unitMap.values()) {
        if (unitCacheEntry.getOrigin() == UnitOrigin.PERSISTENT) {
          unitWriteQueue.add(new UnitWriteMessage(unitCacheEntry));
        }
      }
    }
    unitWriteQueue.add(UnitWriteMessage.DELETE_OLD_CACHE_FILES);
  }

  @Override
  public CompilationUnit find(ContentId contentId) {
    unitCacheMapLoader.await();
    return super.find(contentId);
  }

  @Override
  public CompilationUnit find(String resourceLocation) {
    unitCacheMapLoader.await();
    return super.find(resourceLocation);
  }

  /**
   * Delete all cache files in the directory except for the currently open file.
   * 
   * @param current Specifies the currently open cache file which will not be
   *          deleted.
   */
  void deleteOldCacheFiles(TreeLogger logger, File current) {
    assert current != null;

    SpeedTracerLogger.Event deleteEvent = SpeedTracerLogger.start(DevModeEventType.DELETE_CACHE);
    File[] filesToDelete = getCacheFiles();
    if (filesToDelete == null) {
      return;
    }
    logger.log(TreeLogger.INFO, "Purging cache files from " + cacheDirectory);
    for (File toDelete : filesToDelete) {
      if (!current.equals(toDelete)) {
        toDelete.delete();
      }
    }
    deleteEvent.end();
  }

  /**
   * Finds all files matching a pattern in the cache directory.
   * 
   * @return an array of sorted filenames. The file name pattern is such that
   *         sorting them alphabetically also sorts the files by age.
   */
  File[] getCacheFiles() {
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
   * For Unit testing - shutdown the persistent cache.
   */
  void shutdown() throws InterruptedException {
    unitWriter.doShutdown();
  }

  /**
   * Load everything cached on disk into memory.
   */
  private void loadUnitMap(TreeLogger logger) {
    Event loadPersistentUnitEvent =
        SpeedTracerLogger.start(DevModeEventType.LOAD_PERSISTENT_UNIT_CACHE);
    logger.log(TreeLogger.TRACE, "Looking for previously cached Compilation Units in "
        + cacheDirectory.getAbsolutePath());
    try {
      if (cacheDirectory.isDirectory() && cacheDirectory.canRead()) {
        File[] files = getCacheFiles();
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
              CompilationUnit unit = (CompilationUnit) inputStream.readObject();
              if (unit == null) {
                break;
              }
              UnitCacheEntry entry = new UnitCacheEntry(unit, UnitOrigin.PERSISTENT);
              UnitCacheEntry oldEntry = unitMap.get(unit.getResourceLocation());
              if (oldEntry != null && unit.getLastModified() > oldEntry.getUnit().getLastModified()) {
                super.remove(oldEntry.getUnit());
                unitMap.put(unit.getResourceLocation(), entry);
                unitMapByContentId.put(unit.getContentId(), entry);
              } else if (oldEntry == null) {
                unitMap.put(unit.getResourceLocation(), entry);
                unitMapByContentId.put(unit.getContentId(), entry);
              }
            }
          } catch (EOFException ex) {
            // Go on to the next file.
          } catch (IOException ex) {
            deleteCacheFile = true;
            logger.log(TreeLogger.TRACE, "Ignoring and deleting cache log "
                + cacheFile.getAbsolutePath() + " due to read error.", ex);
          } catch (ClassNotFoundException ex) {
            deleteCacheFile = true;
            logger.log(TreeLogger.TRACE, "Ignoring and deleting cache log "
                + cacheFile.getAbsolutePath() + " due to deserialization error.", ex);
          } finally {
            Utility.close(inputStream);
            Utility.close(bis);
            Utility.close(fis);
          }
          if (deleteCacheFile) {
            cacheFile.delete();
          } else {
            logger.log(TreeLogger.TRACE, cacheFile.getName() + ": Load complete");
          }
        }
      } else {
        logger.log(TreeLogger.TRACE,
            "Starting with empty Cache: CompilationUnit cache directory does "
                + "not exist or is not readable.");
      }
    } finally {
      loadPersistentUnitEvent.end();
    }
  }
}
