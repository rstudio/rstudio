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

import static javaemul.internal.InternalPreconditions.checkArgument;
import static javaemul.internal.InternalPreconditions.checkElement;
import static javaemul.internal.InternalPreconditions.checkNotNull;
import static javaemul.internal.InternalPreconditions.checkState;

import javaemul.internal.ArrayHelper;
import javaemul.internal.annotations.SpecializeMethod;

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

      IteratorImpl() {
        findNext();
      }

      @Override
      public boolean hasNext() {
        return i < capacity();
      }

      @Override
      public E next() {
        checkElement(hasNext());
        last = i;
        findNext();
        return set[last];
      }

      @Override
      public void remove() {
        checkState(last != -1);
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
      checkNotNull(e);

      int ordinal = e.ordinal();
      if (set[ordinal] == null) {
        set[ordinal] = e;
        ++size;
        return true;
      }
      return false;
    }

    @Override
    public EnumSet<E> clone() {
      E[] clonedSet = ArrayHelper.clone(set, 0, set.length);
      return new EnumSetImpl<E>(all, clonedSet, size);
    }

    @SpecializeMethod(params = Enum.class, target = "containsEnum")
    @Override
    public boolean contains(Object o) {
      return (o instanceof Enum) && containsEnum((Enum) o);
    }

    private boolean containsEnum(Enum e) {
      return e != null && set[e.ordinal()] == e;
    }

    @Override
    public Iterator<E> iterator() {
      return new IteratorImpl();
    }

    @SpecializeMethod(params = Enum.class, target = "removeEnum")
    @Override
    public boolean remove(Object o) {
      return (o instanceof Enum) && removeEnum((Enum) o);
    }

    private boolean removeEnum(Enum e) {
      if (e != null && set[e.ordinal()] == e) {
        set[e.ordinal()] = null;
        --size;
        return true;
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
    E[] set = ArrayHelper.clone(all, 0, all.length);
    return new EnumSetImpl<E>(all, set, all.length);
  }

  public static <E extends Enum<E>> EnumSet<E> complementOf(EnumSet<E> other) {
    EnumSetImpl<E> s = (EnumSetImpl<E>) other;
    E[] all = s.all;
    E[] oldSet = s.set;
    E[] newSet = ArrayHelper.createFrom(oldSet, oldSet.length);
    for (int i = 0, c = oldSet.length; i < c; ++i) {
      if (oldSet[i] == null) {
        newSet[i] = all[i];
      }
    }
    return new EnumSetImpl<E>(all, newSet, all.length - s.size);
  }

  public static <E extends Enum<E>> EnumSet<E> copyOf(Collection<E> c) {
    if (c instanceof EnumSet) {
      return copyOf((EnumSet<E>) c);
    }

    checkArgument(!c.isEmpty(), "Collection is empty");

    Iterator<E> iterator = c.iterator();
    E first = iterator.next();
    EnumSet<E> set = of(first);
    while (iterator.hasNext()) {
      E e = iterator.next();
      set.add(e);
    }
    return set;
  }

  public static <E extends Enum<E>> EnumSet<E> copyOf(EnumSet<E> s) {
    return s.clone();
  }

  public static <E extends Enum<E>> EnumSet<E> noneOf(Class<E> elementType) {
    E[] all = elementType.getEnumConstants();
    return new EnumSetImpl<E>(all, ArrayHelper.createFrom(all, all.length), 0);
  }

  public static <E extends Enum<E>> EnumSet<E> of(E first) {
    EnumSet<E> set = noneOf(first.getDeclaringClass());
    set.add(first);
    return set;
  }

  public static <E extends Enum<E>> EnumSet<E> of(E first, E... rest) {
    EnumSet<E> set = of(first);
    Collections.addAll(set, rest);
    return set;
  }

  public static <E extends Enum<E>> EnumSet<E> range(E from, E to) {
    checkArgument(from.compareTo(to) <= 0, "%s > %s", from, to);

    E[] all = from.getDeclaringClass().getEnumConstants();
    E[] set = ArrayHelper.createFrom(all, all.length);

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
