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

import java.io.File;

/**
 * Creates a cache for {@link CompilationUnit} instances.
 */
public class UnitCacheFactory {

  /**
   * The API must be enabled explicitly for persistent caching to be live.
   */
  private static final boolean usePersistent = System.getProperties().containsKey(
      "gwt.persistentunitcache");
  private static UnitCache instance = null;

  /**
   * If the cache is enabled, instantiates the cache and begins loading units
   * into memory in a background thread. If the cache is not enabled, it clears
   * out any old cached files.
   * 
   * Only one instance of the cache is instantiated. If a previously created cache
   * exists, the previous instance is returned.
   */
  public static synchronized UnitCache get(TreeLogger logger, File cacheDir) {
    assert logger != null;
    if (instance == null) {
      if (usePersistent) {
        if (cacheDir == null) {
          String dirProp = "gwt.persistentunitcachedir";
          String propertyCacheDir = System.getProperty(dirProp);
          if (propertyCacheDir != null) {
            cacheDir = new File(propertyCacheDir);
          } else {
            throw new IllegalArgumentException("Expected " + dirProp
                + " system property to be set.");
          }
        }
        instance = new PersistentUnitCache(logger, cacheDir);
      } else {
        // Fallback - use in-memory only cache.
        instance = new MemoryUnitCache();
      }
    }
    return instance;
  }
}
