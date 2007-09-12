/*
 * Copyright 2007 Google Inc.
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
package java.util;

import static java.util.Utility.equalsWithNullCheck;

/**
 * An {@link Map.Entry} shared by several {@link Map} implementations.
 */
class MapEntryImpl<K, V> implements Map.Entry<K, V> {

  /**
   * Helper method for constructing Map.Entry objects from JSNI code.
   */
  static <K, V> Map.Entry<K, V> create(K key, V value) {
    return new MapEntryImpl<K, V>(key, value);
  }

  private K key;

  private V value;

  /**
   * Constructor for <code>MapEntryImpl</code>.
   */
  public MapEntryImpl(K key, V value) {
    this.key = key;
    this.value = value;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof Map.Entry) {
      Map.Entry<?, ?> entry = (Map.Entry<?, ?>) other;
      if (equalsWithNullCheck(key, entry.getKey())
          && equalsWithNullCheck(value, entry.getValue())) {
        return true;
      }
    }
    return false;
  }

  public K getKey() {
    return key;
  }

  public V getValue() {
    return value;
  }

  /**
   * Calculate the hash code using Sun's specified algorithm.
   */
  @Override
  public int hashCode() {
    int keyHash = 0;
    int valueHash = 0;
    if (key != null) {
      keyHash = key.hashCode();
    }
    if (value != null) {
      valueHash = value.hashCode();
    }
    return keyHash ^ valueHash;
  }

  public V setValue(V object) {
    V old = value;
    value = object;
    return old;
  }

  @Override
  public String toString() {
    return getKey() + "=" + getValue();
  }
}
