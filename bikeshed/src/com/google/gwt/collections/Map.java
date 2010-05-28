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
 * A dictionary of values indexed by a set of keys.
 * 
 * Concrete implementations of this class may provide differing behavior in
 * terms of acceptable types and values allowed for its mappings. In particular,
 * the result of calling {@code containsKey} or {@code get} with {@code null}
 * keys depends on the concrete {@code Map} implementation being accessed. A
 * recommended implementation strategy when an unsupported key is received is to
 * consider such mapping as never present; therefore guaranteeing for an
 * unsupported key {@code k}, {@code get(k) == null} and {@code containsKey(k)
 * == false}.
 * 
 * @param <K> the type used to access values stored in the Map
 * @param <V> the type of values stored in the Map
 */
public abstract class Map<K, V> {

  Map() {
  }

  /**
   * Determines if a key is in the set of keys contained in the map.
   * 
   * @param key to use for testing membership
   * @return <code>true</code> if the key is contained in the map
   */
  public abstract boolean containsKey(K key);

  /**
   * Get a value indexed by a key.
   * 
   * Notice that if the Map contains {@code null} values, a returned {@code
   * null} value does not guarantee that there is no such mapping. Use {@code
   * containsKey(K)} to determine key membership.
   * 
   * @param key index to use for retrieval.
   * @return value associated to the key or <code>null</code> otherwise.
   */
  public abstract V get(K key);

  /**
   * @return <code>true</code> if the map contains no entries.
   */
  public abstract boolean isEmpty();

}
