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

import java.io.File;

/**
 * Lazily creates a singleton cache for {@link CompilationUnit} instances.
 */
public class UnitCacheSingleton {

  public static final String GWT_PERSISTENTUNITCACHE = "gwt.persistentunitcache";
  private static final String GWT_PERSISTENTUNITCACHEDIR = "gwt.persistentunitcachedir";

  /**
   * The API must be enabled explicitly for persistent caching to be live.
   */
  private static final String configPropertyValue = System.getProperty(GWT_PERSISTENTUNITCACHE,
      "true");
  private static final boolean usePersistent = configPropertyValue.length() == 0
      || Boolean.parseBoolean(configPropertyValue);
  private static UnitCache instance = null;

  /**
   * If a cache exists, asks it to clear its contents.
   */
  public static synchronized void clearCache() throws UnableToCompleteException {
    if (instance == null) {
      return;
    }
    instance.clear();
  }

  /**
   * If the cache is enabled, instantiates the cache and begins loading units
   * into memory in a background thread. If the cache is not enabled, it clears
   * out any old cached files.
   * <p>
   * Only one instance of the cache is instantiated. If a previously created
   * cache exists, the previous instance is returned.
   * <p>
   * The specified cache dir parameter is optional.
   */
  public static synchronized UnitCache get(TreeLogger logger, File specifiedCacheDir) {
    return get(logger, specifiedCacheDir, null);
  }

  /**
   * If the cache is enabled, instantiates the cache and begins loading units
   * into memory in a background thread. If the cache is not enabled, it clears
   * out any old cached files.
   * <p>
   * Only one instance of the cache is instantiated. If a previously created
   * cache exists, the previous instance is returned.
   * <p>
   * Both specified and fallback cache dir parameters are optional.
   */
  public static synchronized UnitCache get(TreeLogger logger, File specifiedCacheDir,
      File fallbackCacheDir) {
    assert logger != null;
    if (instance == null) {
      String propertyCachePath = System.getProperty(GWT_PERSISTENTUNITCACHEDIR);
      File propertyCacheDir = propertyCachePath != null ? new File(propertyCachePath) : null;

      if (usePersistent) {
        File actualCacheDir = null;

        // Pick the highest priority cache dir that is available.
        if (specifiedCacheDir != null) {
          actualCacheDir = specifiedCacheDir;
        } else if (propertyCacheDir != null) {
          actualCacheDir = propertyCacheDir;
        } else if (fallbackCacheDir != null) {
          actualCacheDir = fallbackCacheDir;
        } else {
          logger.log(TreeLogger.TRACE, "Persistent caching disabled - no directory specified.\n"
              + "To enable persistent unit caching use -Dgwt.persistentunitcachedir=<dir>");
        }

        if (actualCacheDir != null) {
          try {
            return instance = new PersistentUnitCache(logger, actualCacheDir);
          } catch (UnableToCompleteException ignored) {
          }
        }
      }
      // Fallback - use in-memory only cache.
      instance = new MemoryUnitCache();
    }
    return instance;
  }
}
