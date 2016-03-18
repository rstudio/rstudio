/*
 * Copyright 2016 Google Inc.
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
package com.google.gwt.emultest.java8.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Tests for java.util.Map Java 8 API emulation.
 */
public class MapTest extends AbstractJava8MapTest {

  @Override
  protected Map<String, String> createMap() {
    return new TestMap<>();
  }

  private static class TestMap<K, V> implements Map<K, V> {
    private final Map<K, V> container = new HashMap<>();

    @Override
    public int size() {
      return container.size();
    }

    @Override
    public boolean isEmpty() {
      return container.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
      return container.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
      return container.containsValue(value);
    }

    @Override
    public V get(Object key) {
      return container.get(key);
    }

    @Override
    public V put(K key, V value) {
      return container.put(key, value);
    }

    @Override
    public V remove(Object key) {
      return container.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
      container.putAll(m);
    }

    @Override
    public void clear() {
      container.clear();
    }

    @Override
    public Set<K> keySet() {
      return container.keySet();
    }

    @Override
    public Collection<V> values() {
      return container.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
      return container.entrySet();
    }
  }
}
