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
import java.lang.reflect.Array;
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

  private static class IdentitySingletonSet<E> extends AbstractSet<E> implements
      Serializable {

    private final E item;

    IdentitySingletonSet(E item) {
      this.item = item;
    }

    @Override
    public boolean contains(Object o) {
      return o == item;
    }

    @Override
    public Iterator<E> iterator() {
      return new SingletonIterator<E>(item);
    }

    @Override
    public int size() {
      return 1;
    }

    @Override
    public Object[] toArray() {
      return toArray(new Object[1]);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(T[] a) {
      if (a.length < 1) {
        a = (T[]) Array.newInstance(a.getClass().getComponentType(), 1);
      }
      a[0] = (T) item;
      int i = 1;
      while (i < a.length) {
        a[i++] = null;
      }
      return a;
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

  private static final Class<?> MULTI_SET_CLASS = IdentityHashSet.class;

  private static final Class<?> SINGLETON_SET_CLASS = IdentitySingletonSet.class;

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

  public static <T> Set<T> create() {
    return Collections.emptySet();
  }

  public static <T> Set<T> create(T item) {
    return new IdentitySingletonSet<T>(item);
  }

  public static <T> Set<T> create(T... items) {
    switch (items.length) {
      case 0:
        return create();
      case 1:
        return create(items[0]);
      default:
        return new IdentityHashSet<T>(items);
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
        IdentityHashSet<T> result = new IdentityHashSet<T>();
        result.addAll(set);
        return result;
    }
  }

  public static <T> Set<T> normalizeUnmodifiable(Set<T> set) {
    if (set.size() < 2) {
      return normalize(set);
    } else {
      // TODO: implement an UnmodifiableIdentityHashSet?
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
        // IdentityHashSet -> Singleton
        if (set.remove(toRemove)) {
          return create(set.iterator().next());
        }
        return set;
      default:
        // IdentityHashSet
        set.remove(toRemove);
        return set;
    }
  }
}
