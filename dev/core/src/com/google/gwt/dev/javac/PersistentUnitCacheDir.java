/*
 * Copyright 2014 Google Inc.
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
import com.google.gwt.dev.jjs.impl.GwtAstBuilder;
import com.google.gwt.dev.util.CompilerVersion;
import com.google.gwt.dev.util.StringInterningObjectInputStream;
import com.google.gwt.dev.util.log.speedtracer.DevModeEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
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
import java.util.Collections;
import java.util.List;

/**
 * The directory containing persistent unit cache files.
 * (Helper class for {@link PersistentUnitCache}.)
 */
class PersistentUnitCacheDir {

  private static final String DIRECTORY_NAME = "gwt-unitCache";
  private static final String CACHE_FILE_PREFIX = "gwt-unitCache-";

  static final String CURRENT_VERSION_CACHE_FILE_PREFIX =
      CACHE_FILE_PREFIX + CompilerVersion.getHash();

  private final TreeLogger logger;
  private final File dir;
  private final String filePrefix;

  // Non-null when a a cache file is open for writing. (Always true in normal operation.)
  private OpenFile openFile;

  /**
   * Finds the child directory where the cache files will be stored and opens a new cache
   * file for appending.
   */
  PersistentUnitCacheDir(TreeLogger logger, File parentDir, String cacheFilePrefix)
      throws UnableToCompleteException {
    this.logger = logger;
    this.filePrefix = CURRENT_VERSION_CACHE_FILE_PREFIX + "-" + cacheFilePrefix + "-";

    /*
     * We must canonicalize the path here, otherwise we might set cacheDirectory
     * to something like "/path/to/x/../gwt-unitCache". If this were to happen,
     * the mkdirs() call below would create "/path/to/gwt-unitCache" but
     * not "/path/to/x".
     * Further accesses via the uncanonicalized path will fail if "/path/to/x"
     * had not been created by other means.
     *
     * Fixes issue 6443
     */
    try {
      parentDir = parentDir.getCanonicalFile();
    } catch (IOException e) {
      logger.log(TreeLogger.WARN, "Can't get canonical directory for "
          + parentDir.getAbsolutePath(), e);
      throw new UnableToCompleteException();
    }

    dir = chooseCacheDir(parentDir);
    if (!dir.isDirectory() && !dir.mkdirs()) {
      logger.log(TreeLogger.WARN, "Can't create directory: " + dir.getAbsolutePath());
      throw new UnableToCompleteException();
    }

    if (!dir.canRead()) {
      logger.log(Type.WARN, "Can't read directory: " + dir.getAbsolutePath());
      throw new UnableToCompleteException();
    }

    logger.log(TreeLogger.TRACE, "Persistent unit cache dir set to: " + dir.getAbsolutePath());

    openFile = new OpenFile(logger, createEmptyCacheFile(logger, dir, filePrefix));
  }

  /**
   * Returns the absolute path of the directory where cache files are stored.
   */
  String getPath() {
    return dir.getAbsolutePath();
  }

  /**
   * Returns the number of files written to the cache directory and closed.
   */
  synchronized int getClosedCacheFileCount() {
    return selectClosedFiles(listFiles(filePrefix)).size();
  }

  /**
   * Load everything cached on disk into memory.
   */
  synchronized void loadUnitMap(PersistentUnitCache destination) {
    Event loadPersistentUnitEvent =
        SpeedTracerLogger.start(DevModeEventType.LOAD_PERSISTENT_UNIT_CACHE);
    if (logger.isLoggable(TreeLogger.TRACE)) {
      logger.log(TreeLogger.TRACE, "Looking for previously cached Compilation Units in "
          + getPath());
    }
    try {
      List<File> files = selectClosedFiles(listFiles(filePrefix));
      for (File cacheFile : files) {
        loadOrDeleteCacheFile(cacheFile, destination);
      }
    } finally {
      loadPersistentUnitEvent.end();
    }
  }

  /**
   * Delete all cache files in the directory except for the currently open file.
   */
  synchronized void deleteClosedCacheFiles() {
    SpeedTracerLogger.Event deleteEvent = SpeedTracerLogger.start(DevModeEventType.DELETE_CACHE);
    logger.log(TreeLogger.TRACE, "Deleting cache files from " + dir);

    // We want to delete cache files from previous versions as well.
    List<File> allVersionsList = listFiles(CACHE_FILE_PREFIX);
    int deleteCount = 0;
    for (File candidate : allVersionsList) {
      if (deleteUnlessOpen(candidate)) {
        deleteCount++;
      }
    }

    logger.log(TreeLogger.TRACE, "Deleted " + deleteCount + " cache files from " + dir);
    deleteEvent.end();
  }

  /**
   * Closes the current cache file and opens a new one.
   */
  synchronized void rotate() throws UnableToCompleteException {
    logger.log(Type.TRACE, "Rotating persistent unit cache");
    if (openFile != null) {
      openFile.close(logger);
      openFile = null;
    }
    openFile = new OpenFile(logger, createEmptyCacheFile(logger, dir, filePrefix));
  }

  /**
   * Deletes the given file unless it's currently open for writing.
   */
  synchronized boolean deleteUnlessOpen(File cacheFile) {
    if (isOpen(cacheFile)) {
      return false;
    }
    logger.log(Type.TRACE, "Deleting file: " + cacheFile);
    boolean deleted = cacheFile.delete();
    if (!deleted) {
      logger.log(Type.WARN, "Unable to delete file: " + cacheFile);
    }
    return deleted;
  }

  /**
   * Writes a compilation unit to the disk cache.
   */
  synchronized void writeUnit(CompilationUnit unit) throws UnableToCompleteException {
    if (openFile == null) {
      logger.log(Type.TRACE, "Skipped writing compilation unit to cache because no file is open");
      return;
    }
    openFile.writeUnit(logger, unit);
  }

  /**
   * Closes the file where cache entries are written.
   * (This should only be called at shutdown.)
   */
  synchronized void closeCurrentFile() {
    if (openFile != null) {
      openFile.close(logger);
      openFile = null;
    }
  }

  @VisibleForTesting
  static File chooseCacheDir(File parentDir) {
    return new File(parentDir, DIRECTORY_NAME);
  }

  private boolean isOpen(File f) {
    return openFile != null && openFile.file.equals(f);
  }

  /**
   * Loads all the units in a cache file into the given cache.
   * Delete it if unable to read it.
   */
  private void loadOrDeleteCacheFile(File cacheFile, PersistentUnitCache destination) {
    FileInputStream fis = null;
    BufferedInputStream bis = null;
    ObjectInputStream inputStream = null;

    boolean ok = false;
    int unitsLoaded = 0;
    try {
      fis = new FileInputStream(cacheFile);
      bis = new BufferedInputStream(fis);
      /*
       * It is possible for the next call to throw an exception, leaving
       * inputStream null and fis still live.
       */
      inputStream = new StringInterningObjectInputStream(bis);

      // Read objects until we get an EOF exception.
      while (true) {
        CachedCompilationUnit unit = (CachedCompilationUnit) inputStream.readObject();
        if (unit == null) {
          // Won't normally get here. Not sure why this check was here before.
          logger.log(Type.WARN, "unexpected null in cache file: " + cacheFile);
          break;
        }
        if (unit.getTypesSerializedVersion() != GwtAstBuilder.getSerializationVersion()) {
          continue;
        }
        destination.maybeAddLoadedUnit(unit);
        unitsLoaded++;
      }

    } catch (EOFException ignored) {
      // This is a normal exit. Go on to the next file.
      ok = true;
    } catch (IOException e) {
      logger.log(TreeLogger.TRACE, "Ignoring and deleting cache log "
          + cacheFile.getAbsolutePath() + " due to read error.", e);
    } catch (ClassNotFoundException e) {
      logger.log(TreeLogger.TRACE, "Ignoring and deleting cache log "
          + cacheFile.getAbsolutePath() + " due to deserialization error.", e);
    } finally {
      Utility.close(inputStream);
      Utility.close(bis);
      Utility.close(fis);
    }

    if (ok) {
      logger.log(TreeLogger.TRACE, "Loaded " + unitsLoaded +
          " units from cache file: " + cacheFile.getName());
    } else {
      deleteUnlessOpen(cacheFile);
      logger.log(TreeLogger.TRACE, "Loaded " + unitsLoaded +
          " units from invalid cache file before deleting it: " + cacheFile.getName());
    }
  }

  /**
   * Lists files in the cache directory that start with the given prefix.
   *
   * <p>The files will be sorted according to {@link java.io.File#compareTo}, which
   * differs on Unix versus Windows, but is good enough to sort by age
   * for the names we use.</p>
   */
  private List<File> listFiles(String prefix) {
    File[] files = dir.listFiles();
    if (files == null) {
      // Shouldn't happen, just satisfying null check warning.
      return Collections.emptyList();
    }
    List<File> out = Lists.newArrayList();
    for (File file : files) {
      if (file.getName().startsWith(prefix)) {
        out.add(file);
      }
    }
    Collections.sort(out);
    return out;
  }

  /**
   * Removes the currently open file from a list of files.
   * @return the new list.
   */
  private List<File> selectClosedFiles(Iterable<File> fileList) {
    List<File> closedFiles = Lists.newArrayList();
    for (File file : fileList) {
      if (!isOpen(file)) {
        closedFiles.add(file);
      }
    }
    return closedFiles;
  }

  /**
   * Creates a new, empty file with a name based on the current system time.
   */
  private static File createEmptyCacheFile(TreeLogger logger, File dir, String filePrefix)
      throws UnableToCompleteException {
    File newFile = null;
    long timestamp = System.currentTimeMillis();
    try {
      do {
        newFile = new File(dir, filePrefix + String.format("%016X", timestamp++));
      } while (!newFile.createNewFile());
    } catch (IOException ex) {
      logger.log(TreeLogger.WARN, "Can't create new cache log file "
          + newFile.getAbsolutePath() + ".", ex);
      throw new UnableToCompleteException();
    }

    if (!newFile.canWrite()) {
      logger.log(TreeLogger.WARN, "Can't write to new cache log file "
          + newFile.getAbsolutePath() + ".");
      throw new UnableToCompleteException();
    }

    return newFile;
  }

  /**
   * The current file and stream being written to by the persistent unit cache, if any.
   *
   * <p>Not thread safe. (The parent class handles concurrency.)
   */
  private static class OpenFile {
    private final File file;
    private final ObjectOutputStream stream;
    private int unitsWritten = 0;

    /**
     * Opens a file for writing compilation units.
     * Overwrites the file (it's typically empty).
     * A cache file may not already be open.
     */
    OpenFile(TreeLogger logger, File toOpen)
        throws UnableToCompleteException {
      logger.log(Type.TRACE, "Opening cache file: " + toOpen);
      ObjectOutputStream newStream = openObjectStream(logger, toOpen);

      this.file = toOpen;
      this.stream = newStream;
      unitsWritten = 0;
    }

    /**
     * Writes a compilation unit to the currently open file, if any.
     * @return true if written
     * @throws UnableToCompleteException if the file was open but we can't append.
     */
    boolean writeUnit(TreeLogger logger, CompilationUnit unit)
        throws UnableToCompleteException {
      try {
        stream.writeObject(unit);
        unitsWritten++;
        return true;
      } catch (IOException e) {
        logger.log(TreeLogger.ERROR, "Error saving compilation unit to cache file: " + file, e);
        throw new UnableToCompleteException();
      }
    }

    /**
     * Closes the current file and deletes it if it's empty. If no file is open, does nothing.
     */
    void close(TreeLogger logger) {
      logger.log(Type.TRACE,
          "Closing cache file: " + file + " (" + unitsWritten + " units written)");

      Utility.close(stream);

      if (unitsWritten == 0) {
        // Remove useless empty file.
        logger.log(Type.TRACE, "Deleting empty file: " + file);
        boolean deleted = file.delete();
        if (!deleted) {
          logger.log(Type.INFO, "Couldn't delete persistent unit cache file: " + file);
        }
      }
    }

    private static ObjectOutputStream openObjectStream(TreeLogger logger, File file)
        throws UnableToCompleteException {

      FileOutputStream fstream = null;
      try {
        fstream = new FileOutputStream(file);
        return new ObjectOutputStream(new BufferedOutputStream(fstream));
      } catch (IOException e) {
        logger.log(Type.ERROR, "Can't open persistent unit cache file", e);
        Utility.close(fstream);
        throw new UnableToCompleteException();
      }
    }
  }
}
