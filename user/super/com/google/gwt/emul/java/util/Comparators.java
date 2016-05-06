/*
 * Copyright 2007 Google Inc.
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

import static javaemul.internal.InternalPreconditions.checkNotNull;

import java.io.Serializable;

class Comparators {
  /*
   * This is a utility class that provides default Comparators. This class
   * exists so Arrays and Collections can share the natural comparator without
   * having to know internals of each other.
   *
   * This class is package protected since it is not in the JRE.
   */

  private static final Comparator<Comparable<Object>> INTERNAL_NATURAL_ORDER =
      new NaturalOrderComparator();

  private static final Comparator<Comparable<Object>> NATURAL_ORDER =
      new NaturalOrderComparator();

  private static final Comparator<Comparable<Object>> REVERSE_NATURAL_ORDER =
      new ReverseNaturalOrderComparator();

  private static final class NaturalOrderComparator
      implements Comparator<Comparable<Object>>, Serializable {

    @Override
    public int compare(Comparable<Object> a, Comparable<Object> b) {
      return checkNotNull(a).compareTo(checkNotNull(b));
    }

    @Override
    public Comparator<Comparable<Object>> reversed() {
      return REVERSE_NATURAL_ORDER;
    }
  }

  private static final class ReverseNaturalOrderComparator
      implements Comparator<Comparable<Object>>, Serializable {

    @Override
    public int compare(Comparable<Object> a, Comparable<Object> b) {
      return checkNotNull(b).compareTo(checkNotNull(a));
    }

    @Override
    public Comparator<Comparable<Object>> reversed() {
      return NATURAL_ORDER;
    }
  }

  static final class ReversedComparator<T> implements Comparator<T>, Serializable {
    private final Comparator<T> comparator;

    ReversedComparator(Comparator<T> comparator) {
      this.comparator = comparator;
    }

    @Override
    public int compare(T a, T b) {
      return comparator.compare(b, a);
    }

    @Override
    public Comparator<T> reversed() {
      return comparator;
    }
  }

  static final class NullComparator<T> implements Comparator<T>, Serializable {
    private final boolean nullFirst;
    private final Comparator<T> delegate;

    @SuppressWarnings("unchecked")
    NullComparator(boolean nullFirst, Comparator<? super T> delegate) {
      this.nullFirst = nullFirst;
      this.delegate = (Comparator<T>) delegate;
    }

    @Override
    public int compare(T a, T b) {
      if (a == null) {
        return b == null ? 0 : (nullFirst ? -1 : 1);
      }
      if (b == null) {
        return nullFirst ? 1 : -1;
      }
      return delegate == null ? 0 : delegate.compare(a, b);
    }

    @Override
    public Comparator<T> reversed() {
      return new NullComparator<>(!nullFirst, delegate == null ? null : delegate.reversed());
    }

    @Override
    public Comparator<T> thenComparing(Comparator<? super T> other) {
      return new NullComparator<>(nullFirst, delegate == null ?
          other : delegate.thenComparing(other));
    }
  }

  /**
   * Returns the natural Comparator which compares two Objects
   * according to their <i>natural ordering</i>.
   */
  @SuppressWarnings("unchecked")
  static <T> Comparator<T> naturalOrder() {
    return (Comparator<T>) NATURAL_ORDER;
  }

  /**
   * Returns reversed natural Comparator which compares two Objects
   * according to their <i>reversed natural ordering</i>.
   */
  @SuppressWarnings("unchecked")
  static <T> Comparator<T> reverseNaturalOrder() {
    return (Comparator<T>) REVERSE_NATURAL_ORDER;
  }

  /**
   * Returns the given comparator if it is non-null; natural order comparator otherwise.
   * This comparator must not be the same object as {@link Comparators#NATURAL_ORDER} comparator
   * because it's used to mask out client provided comparators in TreeMap and PriorityQueue
   * in {@link Comparators#naturalOrderToNull(Comparator)}.
   *
   * See:
   * {@link Arrays#binarySearch(Object[], Object, Comparator)}
   * {@link Arrays#binarySearch(Object[], int, int, Object, Comparator)}
   * {@link Arrays#sort(Object[], Comparator)}
   * {@link Arrays#sort(Object[], int, int, Comparator)}
   * {@link TreeMap#TreeMap(Comparator)}
   * {@link PriorityQueue#PriorityQueue(Comparator)}
   */
  @SuppressWarnings("unchecked")
  static <T> Comparator<T> nullToNaturalOrder(Comparator<T> cmp) {
    return cmp == null ? (Comparator<T>) INTERNAL_NATURAL_ORDER : cmp;
  }

  /**
   * Return null if the given comparator is natural order comparator returned by
   * {@link Comparators#nullToNaturalOrder(Comparator)}; given comparator otherwise.
   *
   * See:
   * {@link TreeMap#comparator()}
   * {@link PriorityQueue#comparator()}
   */
  static <T> Comparator<T> naturalOrderToNull(Comparator<T> cmp) {
    return cmp == INTERNAL_NATURAL_ORDER ? null : cmp;
  }

  private Comparators() { }
}
