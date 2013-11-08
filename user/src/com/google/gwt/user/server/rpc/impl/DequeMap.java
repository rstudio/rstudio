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

package com.google.gwt.user.server.rpc.impl;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;

/**
 * MultiMap with stack semantics for the values.
 * 
 * Initial insertion of a given key results in a new stack containing the value.
 * Subsequent puts for the same key add the value to the stack. Removing a key
 * removes its value from the stack.
 * 
 * @param <K> The key to use in the map
 * @param <V> The value to put in the deque
 */
public class DequeMap<K, V> {
  /**
   * The default initial capacity for the Deque objects.
   */
  private static final int DEFAULT_INITIAL_DEQUE_CAPACITY = 3;

  /**
   * The default initial capacity for the Deque objects.
   */
  private static final int DEFAULT_INITIAL_MAP_CAPACITY = 3;

  /**
   * The initial capacity for the Deque objects.
   */
  private int dequeCapacity = DEFAULT_INITIAL_DEQUE_CAPACITY;

  /**
   * The map used to hold data.
   * 
   * Note that we do not extend map because we require control over the
   * addition and removal of elements from the map.
   */
  private final HashMap<K, ArrayDeque<V>> map;

  private int size = 0;

  /**
   * Constructs an empty <tt>DequeMap</tt> with the default initial capacities
   * (3 and 3) and the default map load factor (0.75).
   */
  public DequeMap() {
    this(DEFAULT_INITIAL_MAP_CAPACITY, DEFAULT_INITIAL_DEQUE_CAPACITY);
  }

  /**
   * Constructs an empty <tt>DequeMap</tt> with the specified initial capacities
   * and the default map load factor (0.75).
   * 
   * @param initialMapCapacity the initial map capacity.
   * @param initialDequeCapacity the initial deque capacity.
   * @throws IllegalArgumentException if the initial capacity is negative.
   */
  public DequeMap(int initialMapCapacity, int initialDequeCapacity) {
    map = new HashMap<K, ArrayDeque<V>>(initialMapCapacity);
    dequeCapacity = initialDequeCapacity;
  }

  /**
   * Add a value for a key to the map.
   * 
   * If the key already exists, the value is added to the head of the queue at
   * that key. Otherwise, a new queue containing the value is created and
   * inserted into the map at the key.
   * 
   * @param key key with which the specified value is to be associated
   * @param value the value to push for the key
   */
  public void add(K key, V value) {
    ArrayDeque<V> deque = map.get(key);
    if (deque == null) {
      deque = new ArrayDeque<V>(dequeCapacity);
      deque.addFirst(value);
      map.put(key, deque);
      size++;
    } else {
      if (deque.isEmpty()) {
        size++;
      }
      deque.addFirst(value);
    }
  }

  /**
   * Get the most recent value for a key.
   * 
   * @param key key to get
   * @return the value at the head of the stack for the key, or null if the key
   *         is unknown.
   */
  public V get(K key) {
    Deque<V> deque = map.get(key);
    if (deque == null) {
      return null;
    }
    return deque.peekFirst();
  }

  /**
   * Remove the most recent value for a key.
   * 
   * @param key key to get
   * @return the value at the head of the stack for the key, or null if the key
   *         is unknown.
   */
  public V remove(K key) {
    Deque<V> deque = map.get(key);
    if (deque == null) {
      return null;
    }
    boolean wasEmpty = deque.isEmpty();
    V result = deque.pollFirst();
    if (deque.isEmpty() && !wasEmpty) {
      assert size > 0;
      size--;
    }
    return result;
  }

  /**
   * Returns true if no mappings are defined.
   */
  public boolean isEmpty() {
    return size == 0;
  }
}
