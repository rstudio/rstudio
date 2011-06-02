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

/**
 * A request to load the code for a
 * {@link com.google.gwt.core.client.GWT#runAsync(Class, com.google.gwt.core.client.RunAsyncCallback)}
 * split point.
 */
public class RunAsyncCode implements PrefetchableResource {
  /**
   * Create an instance for the split point named with the given class. The
   * provided class must be a class literal.
   * 
   * @param splitPoint a Class literal used to name the split point
   */
  public static RunAsyncCode runAsyncCode(Class<?> splitPoint) {
    // This is a place holder for development mode.
    return forSplitPointNumber(-1);
  }

  /**
   * Not for direct use by application code. Calls to this method are created by
   * the compiler.
   */
  static RunAsyncCode forSplitPointNumber(int splitPoint) {
    return new RunAsyncCode(splitPoint);
  }

  private final int splitPoint;

  private RunAsyncCode(int splitPoint) {
    this.splitPoint = splitPoint;
  }

  public int getSplitPoint() {
    return splitPoint;
  }

  /**
   * Ask whether this code has already been loaded.
   */
  public boolean isLoaded() {
    if (!GWT.isScript()) {
      return true;
    }
    return AsyncFragmentLoader.BROWSER_LOADER.isAlreadyLoaded(splitPoint);
  }
}
