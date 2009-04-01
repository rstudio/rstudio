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

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Utility methods for operating on memory-efficient identity sets. All sets of
 * size 0 or 1 are assumed to be immutable. All sets of size greater than 1 are
 * assumed to be mutable.
 */
public class IdentitySets {

  private static class IdentitySingletonSet<T> extends AbstractSet<T> implements
      Serializable {

    private final T item;

    IdentitySingletonSet(T item) {
      this.item = item;
    }

    public boolean contains(Object o) {
      return o == item;
    }

    public Iterator<T> iterator() {
      return new SingletonIterator<T>(item);
    }

    public int size() {
      return 1;
    }
  }

  private static final class SingletonIterator<T> implements Iterator<T> {

    /**
     * Sentinel value to mark that this iterator's single item was consumed.
     */
    private static final Object EMPTY = new Object();

    private T item;

    SingletonIterator(T item) {
      this.item = item;
    }

    public boolean hasNext() {
      return item != EMPTY;
    }

    @SuppressWarnings("unchecked")
    public T next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      T toReturn = item;
      item = (T) EMPTY;
      return toReturn;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  public static <T> Set<T> add(Set<T> set, T toAdd) {
    switch (set.size()) {
      case 0:
        // Empty -> Singleton
        return new IdentitySingletonSet<T>(toAdd);
      case 1: {
        if (set.contains(toAdd)) {
          return set;
        }
        // Singleton -> IdentityHashSet
        Set<T> result = new IdentityHashSet<T>();
        result.add(set.iterator().next());
        result.add(toAdd);
        return result;
      }
      default:
        // IdentityHashSet
        set.add(toAdd);
        return set;
    }
  }

  public static <T> Set<T> normalize(Set<T> set) {
    switch (set.size()) {
      case 0:
        return Collections.emptySet();
      case 1: {
        if (set.getClass() != IdentitySingletonSet.class) {
          return new IdentitySingletonSet<T>(set.iterator().next());
        }
        return set;
      }
      default:
        return set;
    }
  }
}
