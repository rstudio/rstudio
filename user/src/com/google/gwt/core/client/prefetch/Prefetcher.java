/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.core.client.prefetch;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.impl.AsyncFragmentLoader;

import java.util.Arrays;

/**
 * This class allows requesting the download of resources before they are
 * strictly needed. See the classes that implement {@link PrefetchableResource}.
 * Currently, the only supported resource type is {@link RunAsyncCode}.
 */
public class Prefetcher {
  /**
   * Specify which resources should be prefetched.
   */
  public static void prefetch(Iterable<? extends PrefetchableResource> resources) {
    if (!GWT.isScript()) {
      // Nothing to do in development mode
      return;
    }

    // No range checking in Production Mode means we needn't precompute the size
    int[] runAsyncSplitPoints = new int[0];
    int i = 0;
    for (PrefetchableResource resource : resources) {
      if (resource instanceof RunAsyncCode) {
        RunAsyncCode resourceRunAsync = (RunAsyncCode) resource;
        int splitPoint = resourceRunAsync.getSplitPoint();
        if (splitPoint >= 0) { // Skip placeholders, which have a -1 split point
          runAsyncSplitPoints[i++] = splitPoint;
        }
        continue;
      }

      throw new IllegalArgumentException("Unknown resource type: "
          + resource.getClass());
    }

    AsyncFragmentLoader.BROWSER_LOADER.setPrefetchQueue(runAsyncSplitPoints);
  }

  /**
   * Helper method to call {@link #prefetch(Iterable)} with a single resource.
   */
  public static void prefetch(PrefetchableResource resource) {
    prefetch(Arrays.asList(resource));
  }

  /**
   * Start prefetching.
   */
  public static void start() {
    if (!GWT.isScript()) {
      // Nothing to do in development mode
      return;
    }

    AsyncFragmentLoader.BROWSER_LOADER.startPrefetching();
  }

  /**
   * Stop prefetching.
   */
  public static void stop() {
    if (!GWT.isScript()) {
      // Nothing to do in development mode
      return;
    }

    AsyncFragmentLoader.BROWSER_LOADER.stopPrefetching();
  }
}
