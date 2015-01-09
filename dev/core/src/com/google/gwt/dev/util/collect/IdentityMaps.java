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

import java.util.Collections;
import java.util.Map;

/**
 * Utility methods for operating on memory-efficient maps. All maps of size 0 or
 * 1 are assumed to be immutable. All maps of size greater than 1 are assumed to
 * be mutable.
 */
public class IdentityMaps {

  private static final Class<?> MULTI_MAP_CLASS = IdentityHashMap.class;

  private static final Class<?> SINGLETON_MAP_CLASS = IdentitySingletonMap.class;

  public static <K, V> Map<K, V> create() {
    return Collections.emptyMap();
  }

  public static <K, V> Map<K, V> create(K key, V value) {
    return new IdentitySingletonMap<K, V>(key, value);
  }

  public static <K, V> Map<K, V> normalize(Map<K, V> map) {
    switch (map.size()) {
      case 0:
        return create();
      case 1: {
        if (map.getClass() == SINGLETON_MAP_CLASS) {
          return map;
        }
        K key = map.keySet().iterator().next();
        return create(key, map.get(key));
      }
      default:
        if (map.getClass() == MULTI_MAP_CLASS) {
          return map;
        }
        return new IdentityHashMap<K, V>(map);
    }
  }

  public static <K, V> Map<K, V> normalizeUnmodifiable(Map<K, V> map) {
    if (map.size() < 2) {
      return normalize(map);
    } else {
      // TODO: implement an UnmodifiableIdentityHashMap?
      return Collections.unmodifiableMap(normalize(map));
    }
  }
}
