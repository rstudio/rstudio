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

package com.google.gwt.storage.client;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Exposes the local/session {@link Storage} as a standard {@link Map
 * Map&lt;String, String&gt;}.
 *
 * <p>
 * <span style="color:red">Experimental API: This API is still under development
 * and is subject to change. </span>
 * </p>
 *
 * <p>
 * The following characteristics are associated with this Map:
 * </p>
 * <ol>
 * <li><em>Mutable</em> - All 'write' methods ({@link #put(String, String)},
 * {@link #putAll(Map)}, {@link #remove(Object)}, {@link #clear()},
 * {@link Entry#setValue(Object)}) operate as intended;</li>
 * <li><em>remove() on Iterators</em> - All remove() operations on available
 * Iterators (from {@link #keySet()}, {@link #entrySet()} and {@link #values()})
 * operate as intended;</li>
 * <li><em>No <code>null</code> values and keys</em> - The Storage doesn't
 * accept keys or values which are <code>null</code>;</li>
 * <li><em>String values and keys</em> - All keys and values in this Map are
 * String types.</li>
 * </ol>
 */
public class StorageMap extends AbstractMap<String, String> {

  /*
   * Represents a Map.Entry to a Storage item
   */
  private class StorageEntry implements Map.Entry<String, String> {
    private final String key;

    public StorageEntry(String key) {
      this.key = key;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Map.Entry)) {
        return false;
      }

      Map.Entry<?, ?> e = (Map.Entry<?, ?>) obj;
      return eq(key, e.getKey()) && eq(getValue(), e.getValue());
    }

    @Override
    public String getKey() {
      return key;
    }

    @Override
    public String getValue() {
      return get(key);
    }

    @Override
    public int hashCode() {
      return hashCode(key) ^ hashCode(getValue());
    }

    @Override
    public String setValue(String value) {
      return put(key, value);
    }

    // TODO: Use Objects.equals when GWT's source level >= 7.
    private boolean eq(Object a, Object b) {
      return (a == b) || (a != null && a.equals(b));
    }

    // TODO: Use Objects.hashCode when GWT's source level >= 7.
    private int hashCode(Object o) {
      return o != null ? o.hashCode() : 0;
    }
  }

  /*
   * Represents an Iterator over all Storage items
   */
  private class StorageEntryIterator implements Iterator<Map.Entry<String, String>> {
    private int index = -1;
    private String lastKey;

    @Override
    public boolean hasNext() {
      return index < size() - 1;
    }

    @Override
    public Map.Entry<String, String> next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      index++;
      lastKey = storage.key(index);
      return new StorageEntry(lastKey);
    }

    @Override
    public void remove() {
      if (lastKey == null) {
        throw new IllegalStateException();
      }

      storage.removeItem(lastKey);
      lastKey = null;
      index--;
    }
  }

  /*
   * Represents a Set<Map.Entry> over all Storage items
   */
  private class StorageEntrySet extends AbstractSet<Map.Entry<String, String>> {
    @Override
    public void clear() {
      StorageMap.this.clear();
    }

    @Override
    public boolean contains(Object o) {
      if (!(o instanceof Map.Entry)) {
        return false;
      }
      Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
      Object key = e.getKey();
      Object value = e.getValue();
      return key != null && value != null && value.equals(get(key));
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
      return new StorageEntryIterator();
    }

    @Override
    public boolean remove(Object o) {
      if (!contains(o)) {
        return false;
      }
      Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
      return StorageMap.this.remove(entry.getKey()) != null;
    }

    @Override
    public int size() {
      return StorageMap.this.size();
    }
  }

  private final Storage storage;

  /**
   * Creates the Map with the specified Storage as data provider.
   *
   * @param storage a local/session Storage instance obtained by either
   *          {@link Storage#getLocalStorageIfSupported()} or
   *          {@link Storage#getSessionStorageIfSupported()}.
   */
  public StorageMap(Storage storage) {
    assert storage != null : "storage cannot be null";
    this.storage = storage;
  }

  /**
   * Removes all items from the Storage.
   *
   * @see Storage#clear()
   */
  @Override
  public void clear() {
    storage.clear();
  }

  /**
   * Returns <code>true</code> if the Storage contains the specified key, <code>
   * false</code> otherwise.
   */
  @Override
  public boolean containsKey(Object key) {
    return get(key) != null;
  }

  /**
   * Returns <code>true</code> if the Storage contains the specified value,
   * <code>false</code> otherwise (or if the specified key is <code>null</code>
   * ).
   */
  @Override
  public boolean containsValue(Object value) {
    if (value == null) {
      throw new NullPointerException();
    }

    int s = size();
    for (int i = 0; i < s; i++) {
      if (value.equals(storage.getItem(storage.key(i)))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns a Set containing all entries of the Storage.
   */
  @Override
  public Set<Map.Entry<String, String>> entrySet() {
    return new StorageEntrySet();
  }

  /**
   * Returns the value associated with the specified key in the Storage.
   *
   * @param key the key identifying the value
   * @see Storage#getItem(String)
   */
  @Override
  public String get(Object key) {
    if (key == null) {
      throw new NullPointerException();
    }

    return storage.getItem(key.toString());
  }

  /**
   * Adds (or overwrites) a new key/value pair in the Storage.
   *
   * @param key the key identifying the value (not <code>null</code>)
   * @param value the value associated with the key (not <code>null</code>)
   * @see Storage#setItem(String, String)
   */
  @Override
  public String put(String key, String value) {
    if (key == null || value == null) {
      throw new NullPointerException();
    }

    String old = storage.getItem(key);
    storage.setItem(key, value);
    return old;
  }

  /**
   * Removes the key/value pair from the Storage.
   *
   * @param key the key identifying the item to remove
   * @return the value associated with the key - <code>null</code> if the key
   *         was not present in the Storage
   * @see Storage#removeItem(String)
   */
  @Override
  public String remove(Object key) {
    if (key == null) {
      throw new NullPointerException();
    }

    String k = key.toString();
    String old = storage.getItem(k);
    storage.removeItem(k);
    return old;
  }

  /**
   * Returns the number of items in the Storage.
   *
   * @return the number of items
   * @see Storage#getLength()
   */
  @Override
  public int size() {
    return storage.getLength();
  }
}
