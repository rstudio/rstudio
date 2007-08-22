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

/**
 * Map using reference equality on keys.
 * 
 * @link http://java.sun.com/j2se/1.5.0/docs/api/java/util/IdentityHashMap.html
 * 
 * @param <K> key type
 * @param <V> value type
 */
public class IdentityHashMap<K, V> extends AbstractMap<K, V> implements
    Map<K, V>, Cloneable {

  public IdentityHashMap() {
    this(10);
  }

  public IdentityHashMap(int expectedMaxSize) {
    // TODO(jat): implement
    throw new UnsupportedOperationException("IdentityHashMap not implemented");
  }

  public IdentityHashMap(Map<? extends K, ? extends V> map) {
    this(map.size());
    putAll(map);
  }

  @Override
  public Set<java.util.Map.Entry<K, V>> entrySet() {
    // TODO(jat): implement
    return null;
  }

  @Override
  public V get(Object key) {
    // TODO(jat): implement
    return null;
  }

  @Override
  public V put(K key, V value) {
    // TODO(jat): implement
    return null;
  }

  @Override
  public V remove(Object key) {
    // TODO(jat): implement
    return null;
  }

}
