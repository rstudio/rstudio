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
 * Implements a hash table, which maps non-null keys to non-null values.
 *
 * @deprecated use @see{java.util.HashMap} instead
 * @link http://java.sun.com/j2se/1.5.0/docs/api/java/util/Hashtable.html
 * 
 * @param <K> key type.
 * @param <V> value type.
 */
@Deprecated
public class Hashtable<K,V> extends Dictionary<K,V> implements Map<K,V>,
    Cloneable {
  /*
   * This implementation simply delegates to HashMap.
   */
  
  private HashMap<K,V> map;

  public Hashtable() {
    map = new HashMap<K,V>();
  }

  public void clear() {
    map.clear();
  }

  public boolean containsKey(Object key) {
    return map.containsKey(key);
  }

  public boolean containsValue(Object value) {
    return map.containsValue(value);
  }

  public Enumeration<V> elements() {
    return Collections.enumeration(map.values());
  }

  public Set<Map.Entry<K, V>> entrySet() {
    return map.entrySet();
  }

  public V get(Object key) {
    return map.get(key);
  }

  public boolean isEmpty() {
    return map.isEmpty();
  }

  public Enumeration<K> keys() {
    return Collections.enumeration(map.keySet());
  }

  public Set<K> keySet() {
    return map.keySet();
  }

  public V put(K key, V value) {
    return map.put(key, value);
  }

  public <OK extends K, OV extends V> void putAll(Map<OK, OV> otherMap) {
    map.putAll(otherMap);
  }

  public V remove(Object key) {
    return map.remove(key);
  }

  public int size() {
    return map.size();
  }

  public Collection<V> values() {
    return map.values();
  }

}
