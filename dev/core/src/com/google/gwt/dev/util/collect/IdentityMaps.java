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

  public static <K, V> Map<K, V> put(Map<K, V> map, K key, V value) {
    switch (map.size()) {
      case 0:
        // Empty -> Singleton
        return create(key, value);
      case 1: {
        // Singleton -> IdentityHashMap
        Map<K, V> result = new IdentityHashMap<K, V>();
        result.put(map.keySet().iterator().next(),
            map.values().iterator().next());
        result.put(key, value);
        return result;
      }
      default:
        // IdentityHashMap
        map.put(key, value);
        return map;
    }
  }

  public static <K, V> Map<K, V> putAll(Map<K, V> map, Map<K, V> toAdd) {
    switch (toAdd.size()) {
      case 0:
        // No-op.
        return map;
      case 1: {
        // Add one element.
        K key = toAdd.keySet().iterator().next();
        return put(map, key, toAdd.get(key));
      }
      default:
        // True list merge, result >= 2.
        switch (map.size()) {
          case 0:
            return new IdentityHashMap<K, V>(toAdd);
          case 1: {
            IdentityHashMap<K, V> result = new IdentityHashMap<K, V>();
            K key = map.keySet().iterator().next();
            result.put(key, map.get(key));
            result.putAll(toAdd);
            return result;
          }
          default:
            map.putAll(toAdd);
            return map;
        }
    }
  }

  public static <K, V> Map<K, V> remove(Map<K, V> map, K key) {
    switch (map.size()) {
      case 0:
        // Empty
        return map;
      case 1:
        // Singleton -> Empty
        if (map.containsKey(key)) {
          return create();
        }
        return map;
      case 2:
        // IdentityHashMap -> Singleton
        if (map.containsKey(key)) {
          map.remove(key);
          key = map.keySet().iterator().next();
          return create(key, map.get(key));
        }
        return map;
      default:
        // IdentityHashMap
        map.remove(key);
        return map;
    }
  }
}
