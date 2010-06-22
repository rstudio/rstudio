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
package com.google.gwt.dev.util.collect;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

class IdentitySingletonMap<K, V> implements Map<K, V>, Serializable {

  private class IdentityEntry implements Entry<K, V> {

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Entry<?, ?>)) {
        return false;
      }
      Entry<?, ?> entry = (Entry<?, ?>) o;
      return key == entry.getKey() && value == entry.getValue();
    }

    public K getKey() {
      return key;
    }

    public V getValue() {
      return value;
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(key) ^ System.identityHashCode(value);
    }

    public V setValue(V value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return key + "=" + value;
    }
  }

  /**
   * The key for the single entry. Default access to avoid synthetic accessors
   * from inner classes.
   */
  final K key;

  /**
   * The value for the single entry. Default access to avoid synthetic accessors
   * from inner classes.
   */
  final V value;

  public IdentitySingletonMap(K key, V value) {
    this.key = key;
    this.value = value;
  }

  public void clear() {
    throw new UnsupportedOperationException();
  }

  public boolean containsKey(Object k) {
    return key == k;
  }

  public boolean containsValue(Object v) {
    return value == v;
  }

  public Set<Entry<K, V>> entrySet() {
    return Sets.<Entry<K, V>> create(new IdentityEntry());
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean equals(Object o) {
    if (!(o instanceof Map)) {
      return false;
    }
    Map<K, V> other = (Map<K, V>) o;
    return entrySet().equals(other.entrySet());
  }

  public V get(Object k) {
    return (key == k) ? value : null;
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(key) ^ System.identityHashCode(value);
  }

  public boolean isEmpty() {
    return false;
  }

  public Set<K> keySet() {
    return Sets.create(key);
  }

  public V put(K key, V value) {
    throw new UnsupportedOperationException();
  }

  public void putAll(Map<? extends K, ? extends V> m) {
    throw new UnsupportedOperationException();
  }

  public V remove(Object key) {
    throw new UnsupportedOperationException();
  }

  public int size() {
    return 1;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(32 * size());
    buf.append('{');
    buf.append(key == this ? "(this Map)" : key).append('=').append(
        value == this ? "(this Map)" : value);
    buf.append('}');
    return buf.toString();
  }

  public Collection<V> values() {
    return Sets.create(value);
  }
}
