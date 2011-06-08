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

import org.apache.commons.collections.map.AbstractReferenceMap;
import org.apache.commons.collections.map.ReferenceMap;

import java.util.Collections;
import java.util.Map;

/**
 * This cache stores {@link CompilationUnit} instances in a Map.
 * 
 * Only one unit is cached per resource path. If the contentId of the unit
 * changes, the old unit is discarded and replaced with the new unit.
 */
class MemoryUnitCache implements UnitCache {
  /**
   * Storage for a compilation unit in the map.
   */
  protected static class UnitCacheEntry {
    private final UnitOrigin origin;
    private final CompilationUnit unit;

    protected UnitCacheEntry(CompilationUnit unit, UnitOrigin source) {
      this.unit = unit;
      this.origin = source;
    }

    public UnitOrigin getOrigin() {
      return origin;
    }

    public CompilationUnit getUnit() {
      return unit;
    }
  }

  /**
   * Track how the unit was loaded. Useful in {@link PersistentUnitCache} for
   * consolidating old cache files.
   * 
   */
  protected static enum UnitOrigin {
    /**
     * Unit was loaded from an archive.
     */
    ARCHIVE,

    /**
     * Unit was loaded from persistent store.
     */
    PERSISTENT,

    /**
     * Unit was introduced by an add due to a run-time compile.
     */
    RUN_TIME;
  }

  /**
   * References to all {@link CompilationUnit} objects loaded from the
   * persistent store, and any new ones added to the store as well.
   * 
   * The key is resource path.
   */
  @SuppressWarnings("unchecked")
  protected final Map<String, UnitCacheEntry> unitMap = Collections
      .synchronizedMap(new ReferenceMap(AbstractReferenceMap.HARD, AbstractReferenceMap.SOFT));

  /**
   * References {@link CompilationUnit} objects by {@link ContentId}, which is
   * composed of the type name and a hash on the source code contents.
   */
  @SuppressWarnings("unchecked")
  protected final Map<ContentId, UnitCacheEntry> unitMapByContentId = Collections
      .synchronizedMap(new ReferenceMap(AbstractReferenceMap.HARD, AbstractReferenceMap.SOFT));

  /**
   * Adds a new entry into the cache.
   */
  @Override
  public void add(CompilationUnit newUnit) {
    add(newUnit, UnitOrigin.RUN_TIME);
  }

  /**
   * Adds a new entry into the cache, but marks it as already coming from a
   * persistent archive. This means it doesn't need to be saved out to disk.
   */
  @Override
  public void addArchivedUnit(CompilationUnit newUnit) {
    add(newUnit, UnitOrigin.ARCHIVE);
  }

  /**
   * This method is a no-op for an in-memory cache.
   */
  @Override
  public synchronized void cleanup(final TreeLogger logger) {
    // do nothing.
  }

  @Override
  public CompilationUnit find(ContentId contentId) {
    UnitCacheEntry entry = unitMapByContentId.get(contentId);
    if (entry != null) {
      return entry.getUnit();
    }
    return null;
  }

  @Override
  public CompilationUnit find(String resourcePath) {
    UnitCacheEntry entry = unitMap.get(resourcePath);
    if (entry != null) {
      return entry.getUnit();
    }
    return null;
  }

  @Override
  public void remove(CompilationUnit unit) {
    unitMap.remove(unit.getResourcePath());
    unitMapByContentId.remove(unit.getContentId());
  }

  private void add(CompilationUnit newUnit, UnitOrigin origin) {
    UnitCacheEntry newEntry = new UnitCacheEntry(newUnit, origin);
    String resourcePath = newUnit.getResourcePath();
    UnitCacheEntry oldEntry = unitMap.get(resourcePath);
    if (oldEntry != null) {
      remove(oldEntry.getUnit());
    }
    unitMap.put(resourcePath, newEntry);
    unitMapByContentId.put(newUnit.getContentId(), newEntry);
  }
}
