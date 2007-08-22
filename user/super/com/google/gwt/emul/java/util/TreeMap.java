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
 * Sorted map implementation, guarantees log(n) complexity for containsKey,
 * get, put, and remove.
 * 
 * @link http://java.sun.com/j2se/1.5.0/docs/api/java/util/TreeMap.html
 *
 * @param <K> key type.
 * @param <V> value type.
 */
public class TreeMap<K, V> extends AbstractMap<K, V> implements
    SortedMap<K, V>, Cloneable {

  public TreeMap() {
    this(Comparators.natural());
  }

  public TreeMap(Comparator<? super K> c) {
    // TODO(jat): implement
    throw new UnsupportedOperationException("TreeMap not implemented");
  }

  public TreeMap(Map<? extends K,? extends V> m) {
    this();
    putAll(m);
  }

  public TreeMap(SortedMap<? extends K,? extends V> m) {
    // TODO(jat): optimize
    this((Map<? extends K,? extends V>) m);
  }

  @Override
  public void clear() {
    // TODO(jat): implement
  }
  
  public Comparator<? super K> comparator() {
    // TODO(jat): implement
    return null;
  }

  @Override
  public boolean containsKey(Object key) {
    // TODO(jat): implement
    return false;
  }
  
  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    // TODO(jat): implement
    return null;
  }

  public K firstKey() {
    // TODO(jat): implement
    return null;
  }

  @Override
  public V get(Object key) {
    // TODO(jat): implement
    return null;
  }
  
  public SortedMap<K, V> headMap(K toKey) {
    // TODO(jat): implement
    return null;
  }

  public K lastKey() {
    // TODO(jat): implement
    return null;
  }

  public V put(K key, V value) {
    // TODO(jat): implement
    return null;
  }
  
  public V remove(Object key) {
    // TODO(jat): implement
    return null;
  }
  
  public int size() {
    // TODO(jat): implement
    return 0;
  }
  
  public SortedMap<K, V> subMap(K fromKey, K toKey) {
    // TODO(jat): implement
    return null;
  }

  public SortedMap<K, V> tailMap(K fromKey) {
    // TODO(jat): implement
    return null;
  }

}
