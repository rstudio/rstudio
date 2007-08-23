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
 * Hash table implementation of the Map interface with predictable iteration
 * order. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/LinkedHashMap.html">[Sun
 * docs]</a>
 * 
 * @param <K> key type.
 * @param <V> value type.
 */
public class LinkedHashMap<K, V> extends HashMap<K, V> implements Map<K, V>,
    Cloneable {

  public LinkedHashMap() {
    this(11, 0.75f);
  }

  /**
   * @param ignored
   */
  public LinkedHashMap(int ignored) {
    this(ignored, 0.75f);
  }

  /**
   * @param ignored
   * @param alsoIgnored
   */
  public LinkedHashMap(int ignored, float alsoIgnored) {
    super(ignored, alsoIgnored);
    // TODO(jat): implement
    throw new UnsupportedOperationException("LinkedHashMap not supported");
  }

  /**
   * @param toBeCopied
   */
  public LinkedHashMap(Map<? extends K, ? extends V> toBeCopied) {
    this();
    putAll(toBeCopied);
  }

  @Override
  public void clear() {
    // TODO(jat): implement
  }

  @Override
  public Object clone() {
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

  protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
    // TODO(jat): implement
    return false;
  }

}
