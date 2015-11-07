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
import com.google.gwt.thirdparty.guava.common.collect.ImmutableMap;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * Manages {@link ResourceAccumulator}s for DirectoryClassPathEntry + PathPrefixSet pairs.
 * <p>
 * ResourceAccumulators consume native resources and so require very strict lifecycle management but
 * ClassPathEntry and PathPrefixSet lifecycle management is very loose. This makes it difficult to
 * release ResourceAccumulator at the proper time. This manager class uses weak references to
 * ClassPathEntry and PathPrefixSet instances to lazily discover when ResourceAccumulator instances
 * become eligible for destruction.
 */
class ResourceAccumulatorManager {

  /**
   * A hash key that is a combination of a DirectoryClassPathEntry and PathPrefixSet which also
   * takes special care not to block the garbage collection of either.
   */
  private static class DirectoryAndPathPrefix {

    private final WeakReference<DirectoryClassPathEntry> directoryClassPathEntryRef;
    private final WeakReference<PathPrefixSet> pathPrefixSetRef;
    private int hashCode;

    public DirectoryAndPathPrefix(DirectoryClassPathEntry directoryClassPathEntry,
        PathPrefixSet pathPrefixSet) {
      this.directoryClassPathEntryRef = new WeakReference<>(directoryClassPathEntry);
      this.pathPrefixSetRef = new WeakReference<PathPrefixSet>(pathPrefixSet);
      hashCode = Objects.hash(directoryClassPathEntry, pathPrefixSet);
    }

    @Override
    public boolean equals(Object object) {
      if (object instanceof DirectoryAndPathPrefix) {
        DirectoryAndPathPrefix other = (DirectoryAndPathPrefix) object;
        return directoryClassPathEntryRef.get() == other.directoryClassPathEntryRef.get()
            && pathPrefixSetRef.get() == other.pathPrefixSetRef.get();
      }
      return false;
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    /**
     * If either the instance has been destroyed then it is no longer possible for a caller to
     * request the accumulated sources for the combination. This means the combination is
     * old and tracking can be stopped.
     */
    public boolean isOld() {
      return directoryClassPathEntryRef.get() == null || pathPrefixSetRef.get() == null;
    }
  }

  private static Map<DirectoryAndPathPrefix, ResourceAccumulator> resourceAccumulators = Maps
      .newHashMap();

  static {
    // Keep the resources fresh
    new Thread() {
      @Override
      public void run() {
        while (true) {
          try {
            refreshResources();
            Thread.sleep(10);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    }.start();
  }

  public static synchronized Map<AbstractResource, ResourceResolution> getResources(
      DirectoryClassPathEntry directoryClassPathEntry, PathPrefixSet pathPrefixSet)
      throws IOException {
    DirectoryAndPathPrefix directoryAndPathPrefix =
        new DirectoryAndPathPrefix(directoryClassPathEntry, pathPrefixSet);

    ResourceAccumulator resourceAccumulator = resourceAccumulators.get(directoryAndPathPrefix);
    if (resourceAccumulator == null) {
      Path path = directoryClassPathEntry.getDirectory().toPath();
      resourceAccumulator = new ResourceAccumulator(path, pathPrefixSet);
      resourceAccumulators.put(directoryAndPathPrefix, resourceAccumulator);
    }
    resourceAccumulator.refreshResources();
    return ImmutableMap.copyOf(resourceAccumulator.getResources());
  }

  public static synchronized void refreshResources() throws IOException {
    Iterator<Entry<DirectoryAndPathPrefix, ResourceAccumulator>> entriesIterator =
        resourceAccumulators.entrySet().iterator();
    while (entriesIterator.hasNext()) {
      Entry<DirectoryAndPathPrefix, ResourceAccumulator> entry = entriesIterator.next();
      DirectoryAndPathPrefix directoryAndPathPrefix = entry.getKey();
      ResourceAccumulator resourceAccumulator = entry.getValue();
      if (directoryAndPathPrefix.isOld()) {
        resourceAccumulator.shutdown();
        entriesIterator.remove();
      } else if (resourceAccumulator.isWatchServiceActive()) {
        resourceAccumulator.refreshResources();
      }
    }
  }

  @VisibleForTesting
  static int getActiveListenerCount() throws IOException {
    refreshResources();

    return resourceAccumulators.size();
  }

  @VisibleForTesting
  static boolean isListening(DirectoryClassPathEntry directoryClassPathEntry,
      PathPrefixSet pathPrefixSet) {
    return resourceAccumulators.containsKey(
        new DirectoryAndPathPrefix(directoryClassPathEntry, pathPrefixSet));
  }
}
