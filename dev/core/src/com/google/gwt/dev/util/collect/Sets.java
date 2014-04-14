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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Utility methods for operating on memory-efficient sets. All sets of size 0 or
 * 1 are assumed to be immutable. All sets of size greater than 1 are assumed to
 * be mutable.
 */
public class Sets {

  private static final Class<?> MULTI_SET_CLASS = HashSet.class;
  private static final Class<?> SINGLETON_SET_CLASS = Collections.singleton(
      null).getClass();

  public static <T> Set<T> add(Set<T> set, T toAdd) {
    switch (set.size()) {
      case 0:
        // Empty -> Singleton
        return create(toAdd);
      case 1: {
        if (set.contains(toAdd)) {
          return set;
        }
        // Singleton -> HashSet
        Set<T> result = new HashSet<T>();
        result.add(set.iterator().next());
        result.add(toAdd);
        return result;
      }
      default:
        // HashSet
        set.add(toAdd);
        return set;
    }
  }

  public static <T> Set<T> addAll(Set<T> set, Collection<T> toAdd) {
    switch (toAdd.size()) {
      case 0:
        return set;
      case 1:
        return add(set, toAdd.iterator().next());
    }

    switch (set.size()) {
      case 0:
        // Empty -> HashSet
        return new HashSet<T>(toAdd);
      case 1: {
        // Singleton -> HashSet
        Set<T> result = new HashSet<T>();
        result.add(set.iterator().next());
        result.addAll(toAdd);
        return result;
      }
      default:
        // HashSet
        set.addAll(toAdd);
        return set;
    }
  }

  public static <T> Set<T> create() {
    return Collections.emptySet();
  }

  public static <T> Set<T> create(T item) {
    return Collections.singleton(item);
  }

  public static <T> Set<T> create(T... items) {
    switch (items.length) {
      case 0:
        return create();
      case 1:
        return create(items[0]);
      default:
        return new HashSet<T>(items);
    }
  }

  public static <T> Set<T> normalize(Set<T> set) {
    switch (set.size()) {
      case 0:
        return create();
      case 1: {
        if (set.getClass() == SINGLETON_SET_CLASS) {
          return set;
        }
        return create(set.iterator().next());
      }
      default:
        if (set.getClass() == MULTI_SET_CLASS) {
          return set;
        }
        HashSet<T> result = new HashSet<T>();
        result.addAll(set);
        return result;
    }
  }

  public static <T> Set<T> normalizeUnmodifiable(Set<T> set) {
    if (set.size() < 2) {
      return normalize(set);
    } else {
      // TODO: implement an UnmodifiableHashSet?
      return Collections.unmodifiableSet(normalize(set));
    }
  }

  public static <T> Set<T> remove(Set<T> set, T toRemove) {
    switch (set.size()) {
      case 0:
        // Empty
        return set;
      case 1:
        // Singleton -> Empty
        if (set.contains(toRemove)) {
          return create();
        }
        return set;
      case 2:
        // HashSet -> Singleton
        if (set.remove(toRemove)) {
          return create(set.iterator().next());
        }
        return set;
      default:
        // HashSet
        set.remove(toRemove);
        return set;
    }
  }
}
