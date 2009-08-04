/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.lang.Array;

/**
 * A {@link java.util.Set} of {@link Enum}s. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/EnumSet.html">[Sun
 * docs]</a>
 * 
 * @param <E> enumeration type
 */
public abstract class EnumSet<E extends Enum<E>> extends AbstractSet<E> {

  /**
   * Implemented via sparse array since the set size is finite. Iteration takes
   * linear time with respect to the set of the enum rather than the number of
   * items in the set.
   * 
   * Note: Implemented as a subclass instead of a concrete final EnumSet class.
   * This is because declaring an EnumSet.add(E) causes hosted mode to bind to
   * the tighter method rather than the bridge method; but the tighter method
   * isn't available in the real JRE.
   */
  static final class EnumSetImpl<E extends Enum<E>> extends EnumSet<E> {
    private class IteratorImpl implements Iterator<E> {
      /*
       * i is the index of the item that will be returned on the next call to
       * next() last is the index of the item that was returned on the previous
       * call to next(), -1 if no such item exists.
       */

      int i = -1, last = -1;

      {
        findNext();
      }

      public boolean hasNext() {
        return i < capacity();
      }

      public E next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        last = i;
        findNext();
        return set[last];
      }

      public void remove() {
        if (last < 0) {
          throw new IllegalStateException();
        }
        assert (set[last] != null);
        set[last] = null;
        --size;
        last = -1;
      }

      private void findNext() {
        ++i;
        for (int c = capacity(); i < c; ++i) {
          if (set[i] != null) {
            return;
          }
        }
      }
    }

    /**
     * All enums; reference to the class's copy; must not be modified.
     */
    private final E[] all;

    /**
     * Live enums in the set.
     */
    private E[] set;

    /**
     * Count of enums in the set.
     */
    private int size;

    /**
     * Constructs an empty set.
     */
    public EnumSetImpl(E[] all) {
      this(all, Array.createFrom(all), 0);
    }

    /**
     * Constructs a set taking ownership of the specified set. The size must
     * accurately reflect the number of non-null items in set.
     */
    public EnumSetImpl(E[] all, E[] set, int size) {
      this.all = all;
      this.set = set;
      this.size = size;
    }

    @Override
    public boolean add(E e) {
      if (e == null) {
        throw new NullPointerException();
      }
      int ordinal = e.ordinal();
      if (set[ordinal] == null) {
        set[ordinal] = e;
        ++size;
        return true;
      }
      return false;
    }

    public EnumSet<E> clone() {
      E[] clonedSet = Array.clone(set);
      return new EnumSetImpl<E>(all, clonedSet, size);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o) {
      if (o instanceof Enum) {
        Enum e = (Enum) o;
        return set[e.ordinal()] == e;
      }
      return false;
    }

    @Override
    public Iterator<E> iterator() {
      return new IteratorImpl();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean remove(Object o) {
      if (o instanceof Enum) {
        Enum e = (Enum) o;
        if (set[e.ordinal()] == e) {
          set[e.ordinal()] = null;
          --size;
          return true;
        }
      }
      return false;
    }

    @Override
    public int size() {
      return size;
    }

    @Override
    int capacity() {
      return all.length;
    }
  }

  public static <E extends Enum<E>> EnumSet<E> allOf(Class<E> elementType) {
    E[] all = elementType.getEnumConstants();
    E[] set = Array.clone(all);
    return new EnumSetImpl<E>(all, set, all.length);
  }

  public static <E extends Enum<E>> EnumSet<E> complementOf(EnumSet<E> other) {
    EnumSetImpl<E> s = (EnumSetImpl<E>) other;
    E[] all = s.all;
    E[] oldSet = s.set;
    E[] newSet = Array.createFrom(oldSet);
    for (int i = 0, c = oldSet.length; i < c; ++i) {
      if (oldSet[i] == null) {
        newSet[i] = all[i];
      }
    }
    return new EnumSetImpl<E>(all, newSet, all.length - s.size);
  }

  public static <E extends Enum<E>> EnumSet<E> copyOf(Collection<E> c) {
    if (c instanceof EnumSet) {
      return EnumSet.copyOf((EnumSet<E>) c);
    }

    Iterator<E> it = c.iterator();
    E first = it.next();
    Class<E> clazz = first.getDeclaringClass();
    EnumSet<E> set = EnumSet.noneOf(clazz);
    set.add(first);
    while (it.hasNext()) {
      set.add(it.next());
    }
    return set;
  }

  public static <E extends Enum<E>> EnumSet<E> copyOf(EnumSet<E> s) {
    return s.clone();
  }

  public static <E extends Enum<E>> EnumSet<E> noneOf(Class<E> elementType) {
    E[] all = elementType.getEnumConstants();
    return new EnumSetImpl<E>(all, Array.createFrom(all), 0);
  }

  public static <E extends Enum<E>> EnumSet<E> of(E first) {
    E[] all = first.getDeclaringClass().getEnumConstants();
    E[] set = Array.createFrom(all);
    set[first.ordinal()] = first;
    return new EnumSetImpl<E>(all, set, 1);
  }

  public static <E extends Enum<E>> EnumSet<E> of(E first, E... rest) {
    E[] all = first.getDeclaringClass().getEnumConstants();
    E[] set = Array.createFrom(all);
    set[first.ordinal()] = first;
    int size = 1;
    for (E e : rest) {
      int ordinal = e.ordinal();
      if (set[ordinal] == null) {
        set[ordinal] = e;
        ++size; // count only new elements
      }
    }
    return new EnumSetImpl<E>(all, set, size);
  }

  public static <E extends Enum<E>> EnumSet<E> range(E from, E to) {
    if (from.compareTo(to) > 0) {
      throw new IllegalArgumentException(from + " > " + to);
    }
    E[] all = from.getDeclaringClass().getEnumConstants();
    E[] set = Array.createFrom(all);

    // Inclusive
    int start = from.ordinal();
    int end = to.ordinal() + 1;
    for (int i = start; i < end; ++i) {
      set[i] = all[i];
    }
    return new EnumSetImpl<E>(all, set, end - start);
  }

  /**
   * Single implementation only.
   */
  EnumSet() {
  }

  public abstract EnumSet<E> clone();

  abstract int capacity();
}
