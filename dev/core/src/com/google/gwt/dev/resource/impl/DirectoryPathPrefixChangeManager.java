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
package com.google.gwt.dev.resource.impl;

import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * Manages changed file accumulation for DirectoryClassPathEntry + PathPrefixSet pairs.
 * <p>
 * Changed file lists need to be collected and processed separately for each pair since the
 * processing depends on both the directory and the path prefix set.
 * <p>
 * ChangedFileAccumulators consume native resources and so require very strict lifecycle management
 * but ClassPathEntry and PathPrefixSet lifecycle management is very loose. This makes it difficult
 * to release ChangedFileAccumulators at the proper time. This manager class uses weak references to
 * ClassPathEntry and PathPrefixSet instances to lazily discover when ChangedFileAccumulators become
 * eligible for destruction.
 */
class DirectoryPathPrefixChangeManager {

  /**
   * A hash key that is a combination of a DirectoryClassPathEntry and PathPrefixSet which also
   * takes special care not to block the garbage collection of either.
   */
  private static class DirectoryAndPathPrefix {

    private final WeakReference<DirectoryClassPathEntry> directoryClassPathEntryReference;
    private int hashCode;
    private final WeakReference<PathPrefixSet> pathPrefixSetReference;

    public DirectoryAndPathPrefix(DirectoryClassPathEntry directoryClassPathEntry,
        PathPrefixSet pathPrefixSet) {
      this.directoryClassPathEntryReference =
          new WeakReference<DirectoryClassPathEntry>(directoryClassPathEntry);
      this.pathPrefixSetReference = new WeakReference<PathPrefixSet>(pathPrefixSet);

      hashCode = Objects.hash(directoryClassPathEntry, pathPrefixSet);
    }

    @Override
    public boolean equals(Object object) {
      if (object instanceof DirectoryAndPathPrefix) {
        DirectoryAndPathPrefix directoryAndPathPrefix = (DirectoryAndPathPrefix) object;
        return Objects.equals(directoryClassPathEntryReference.get(),
            directoryAndPathPrefix.directoryClassPathEntryReference.get()) && Objects.equals(
            pathPrefixSetReference.get(), directoryAndPathPrefix.pathPrefixSetReference.get());
      }
      return false;
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    /**
     * If either the instance has been destroyed then it is no longer possible for a caller to
     * request the accumulated changed files list for the combination. This means the combination is
     * old and tracking can be stopped.
     */
    public boolean isOld() {
      return directoryClassPathEntryReference.get() == null || pathPrefixSetReference.get() == null;
    }
  }

  private static Map<DirectoryAndPathPrefix, ChangedFileAccumulator>
      changedFileAccumulatorsByDirectoryAndPathPrefix = Maps.newHashMap();

  /**
   * Start a FileChangeAccumulator for the given DirectoryClassPathEntry + PathPrefixSet pair.
   */
  public static void ensureListening(DirectoryClassPathEntry directoryClassPathEntry,
      PathPrefixSet pathPrefixSet) throws IOException {
    clearOldListeners();

    DirectoryAndPathPrefix directoryAndPathPrefix =
        new DirectoryAndPathPrefix(directoryClassPathEntry, pathPrefixSet);

    synchronized (changedFileAccumulatorsByDirectoryAndPathPrefix) {
      if (changedFileAccumulatorsByDirectoryAndPathPrefix.containsKey(directoryAndPathPrefix)) {
        return;
      }

      changedFileAccumulatorsByDirectoryAndPathPrefix.put(directoryAndPathPrefix,
          new ChangedFileAccumulator(directoryClassPathEntry.getDirectory().toPath()));
    }
  }

  /**
   * Returns a sorted copy of the list of files that have changed since the last time changed files
   * were requested.
   */
  public static List<File> getAndClearChangedFiles(DirectoryClassPathEntry directoryClassPathEntry,
      PathPrefixSet pathPrefixSet) throws ExecutionException {
    ChangedFileAccumulator changedFileAccumulator = changedFileAccumulatorsByDirectoryAndPathPrefix
        .get(new DirectoryAndPathPrefix(directoryClassPathEntry, pathPrefixSet));
    assert changedFileAccumulator
        != null : "Listening must be started before changed files can be requested.";
    return changedFileAccumulator.getAndClearChangedFiles();
  }

  @VisibleForTesting
  static int getActiveListenerCount() {
    clearOldListeners();

    synchronized (changedFileAccumulatorsByDirectoryAndPathPrefix) {
      return changedFileAccumulatorsByDirectoryAndPathPrefix.size();
    }
  }

  @VisibleForTesting
  static boolean isListening(DirectoryClassPathEntry directoryClassPathEntry,
      PathPrefixSet pathPrefixSet) {
    synchronized (changedFileAccumulatorsByDirectoryAndPathPrefix) {
      return changedFileAccumulatorsByDirectoryAndPathPrefix.containsKey(
          new DirectoryAndPathPrefix(directoryClassPathEntry, pathPrefixSet));
    }
  }

  /**
   * Finds any DirectoryClassPathEntry and PathPrefixSet pairs where at least one instance has been
   * destroyed (and thus can no longer be queried about) and shuts down the associated changed file
   * accumulation.
   */
  private static void clearOldListeners() {
    synchronized (changedFileAccumulatorsByDirectoryAndPathPrefix) {
      Iterator<Entry<DirectoryAndPathPrefix, ChangedFileAccumulator>> entriesIterator =
          changedFileAccumulatorsByDirectoryAndPathPrefix.entrySet().iterator();
      while (entriesIterator.hasNext()) {
        Entry<DirectoryAndPathPrefix, ChangedFileAccumulator> entry = entriesIterator.next();
        DirectoryAndPathPrefix directoryAndPathPrefix = entry.getKey();
        ChangedFileAccumulator fileChangeAccumulator = entry.getValue();
        if (directoryAndPathPrefix.isOld()) {
          fileChangeAccumulator.shutdown();
          entriesIterator.remove();
        }
      }
    }
  }
}
