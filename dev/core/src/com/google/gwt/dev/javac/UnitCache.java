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

/**
 * An interface for caching {@link CompilationUnit}s. Alternate implementations may cache only in
 * memory or both cache in memory and persist to disk.<br />
 *
 * All implementations must be reusable across multiple compiles of potentially different
 * applications within the same process. Some JUnitShell test suite execution exercises exactly this
 * path.
 */
public interface UnitCache {
  /**
   * Adds a {@link CompilationUnit} to the cache.
   */
  void add(CompilationUnit newUnit);

  /**
   * Each run of the compiler should call {@link #cleanup(TreeLogger)} when
   * finished adding units to the cache so that cache files from previous runs
   * can be purged from a persistent cache.
   */
  void cleanup(TreeLogger logger);

  /**
   * Wipe the contents of the cache.
   */
  void clear() throws UnableToCompleteException;

  /**
   * Lookup a {@link CompilationUnit} by {@link ContentId}.
   */
  CompilationUnit find(ContentId contentId);

  /**
   * Lookup a {@link CompilationUnit} by resource path. This should include any path prefix that may
   * have been stripped to reroot the resource.<br />
   *
   * Contained CompilationUnits must be keyed on this combination of path prefix and path to avoid
   * collision when reusing a UnitCache instance between compiles that do and do not SuperSource a
   * given class.
   *
   * @see CompilationUnit#getResourcePath()
   */
  CompilationUnit find(String resourcePath);

  /**
   * Remove a {@link CompilationUnit} from the cache.
   */
  void remove(CompilationUnit unit);
}
