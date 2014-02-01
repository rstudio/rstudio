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

/**
 * An interface for caching {@link CompilationUnit}s. Alternate implementations
 * may cache only in memory or both cache in memory and persist to disk.
 */
public interface UnitCache {
  /**
   * Adds a {@link CompilationUnit} to the cache.
   */
  void add(CompilationUnit newUnit);

  /**
   * Adds a new entry into the cache, but marks it as already coming from a
   * persistent archive. This means it doesn't need to be saved out to disk in a
   * persistent implementation of the cache.
   */
  void addArchivedUnit(CompilationUnit newUnit);

  /**
   * Each run of the compiler should call {@link #cleanup(TreeLogger)} when
   * finished adding units to the cache so that cache files from previous runs
   * can be purged from a persistent cache.
   */
  void cleanup(TreeLogger logger);

  /**
   * Lookup a {@link CompilationUnit} by {@link ContentId}.
   */
  CompilationUnit find(ContentId contentId);

  /**
   * Lookup a {@link CompilationUnit} by resource path. This should include any
   * path prefix that may have been was stripped to reroot the resource.
   *
   * @see CompilationUnit#getResourcePath()
   */
  CompilationUnit find(String resourcePath);

  /**
   * Remove a {@link CompilationUnit} from the cache.
   */
  void remove(CompilationUnit unit);
}
