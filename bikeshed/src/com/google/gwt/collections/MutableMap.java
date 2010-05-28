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
package com.google.gwt.collections;

/**
 * A Map whose contents may be modified.
 * 
 * The result of calling {@code put} or {@code remove} with {@code null} keys
 * and values depends on the concrete {@code MutableMap} implementation being
 * accessed. In particular, the result of calling {@code remove} or {@code put}
 * with {@code null} keys (and values) depends on the concrete {@code
 * MutableMap} implementation being accessed. A recommended implementation
 * strategy when an unsupported key is received by {@code remove} is to consider
 * such mapping as never present and return.
 * 
 * @param <K> the type used to access values stored in the Map
 * @param <V> the type of value stored in the Map
 */
public abstract class MutableMap<K, V> extends Map<K, V> {

  /**
   * Removes all entries from this map.
   */
  public abstract void clear();

  /**
   * Put the value in the map at the given key. Note: Does not return the old
   * value.
   * 
   * @param key index to the value.
   * @param value value to be stored
   */
  public abstract void put(K key, V value);

  /**
   * Deletes a key-value entry if the key is a member of the key set.
   * 
   * @param key index to the key-value
   */
  public abstract void remove(K key);

}
