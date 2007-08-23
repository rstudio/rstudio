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

/**
 * Utility methods that operate on collections.
 * 
 * @link http://java.sun.com/j2se/1.5.0/docs/api/java/util/Collections.html
 */
public class Collections {

  /**
   * Used to implement iterators on unmodifiable lists.
   *
   * @param <E> element type.
   */
  private static class UnmodifiableListIterator<E>
      implements ListIterator<E> {

    final ListIterator<? extends E> it;

    public UnmodifiableListIterator(ListIterator<? extends E> it) {
      this.it = it;
    }

    public void add(E o) {
      throw new UnsupportedOperationException(
          "UnmodifiableListIterator: add not permitted");
    }

    public boolean hasNext() {
      return it.hasNext();
    }

    public boolean hasPrevious() {
      return it.hasPrevious();
    }

    public E next() {
      return it.next();
    }

    public int nextIndex() {
      return it.nextIndex();
    }

    public E previous() {
      return it.previous();
    }

    public int previousIndex() {
      return it.previousIndex();
    }

    public void remove() {
      throw new UnsupportedOperationException(
          "UnmodifiableListIterator: remove not permitted");
    }

    public void set(E o) {
      throw new UnsupportedOperationException(
          "UnmodifiableListIterator: set not permitted");
    }
  }
  public static final Set<?> EMPTY_SET = new HashSet<Object>();
  public static final Map<?, ?> EMPTY_MAP = new HashMap<Object, Object>();

  public static final List<?> EMPTY_LIST = new ArrayList<Object>();

  public static <T> boolean addAll(Collection<? super T> c, T... a) {
    boolean result = false;
    for (T e : a) {
      result |= c.add(e);
    }
    return result;
  }

  /**
   * Perform a binary search on a sorted List, using natural ordering.
   * 
   * <p>
   * Note: The GWT implementation differs from the JDK implementation in that it
   * does not do an iterator-based binary search for Lists that do not implement
   * RandomAccess.
   * </p>
   * 
   * @param sortedList object array to search
   * @param key value to search for
   * @return the index of an element with a matching value, or a negative number
   *         which is the index of the next larger value (or just past the end
   *         of the array if the searched value is larger than all elements in
   *         the array) minus 1 (to ensure error returns are negative)
   * @throws ClassCastException if <code>key</code> is not comparable to
   *           <code>sortedList</code>'s elements.
   */
  public static <T> int binarySearch(
      final List<? extends Comparable<? super T>> sortedList, final T key) {
    return binarySearch(sortedList, key, null);
  }

  /*
   * These methods are commented out because they cannot currently be
   * implemented in GWT. The signatures are included in case that changes.
   */
  // public static <E> Collection<E> checkedCollection(Collection<E> c, Class<E>
  // type) {
  // // FUTURE: implement
  // return null;
  // }
  //  
  // static <E> List<E> checkedList(List<E> list, Class<E> type) {
  // // FUTURE: implement
  // return null;
  // }
  //
  // public static <K,V> Map<K,V> checkedMap(Map<K,V> list, Class<K> keyType,
  // Class<V> valueType) {
  // // FUTURE: implement
  // return null;
  // }
  //
  // public static <E> Set<E> checkedSet(Set<E> list, Class<E> type) {
  // // FUTURE: implement
  // return null;
  // }
  //
  // public static <K,V> SortedMap<K,V> checkedSortedMap(SortedMap<K,V> m,
  // Class<K> keyType, Class<V> valueType) {
  // // FUTURE: implement
  // return null;
  // }
  //
  // public static <E> SortedSet<E> checkedSortedSet(SortedSet<E> list, Class<E>
  // type) {
  // // FUTURE: implement
  // return null;
  // }

  /**
   * Perform a binary search on a sorted List, using a user-specified comparison
   * function.
   * 
   * <p>
   * Note: The GWT implementation differs from the JDK implementation in that it
   * does not do an iterator-based binary search for Lists that do not implement
   * RandomAccess.
   * </p>
   * 
   * @param sortedList List to search
   * @param key value to search for
   * @param comparator comparision function, <code>null</code> indicates
   *          <i>natural ordering</i> should be used.
   * @return the index of an element with a matching value, or a negative number
   *         which is the index of the next larger value (or just past the end
   *         of the array if the searched value is larger than all elements in
   *         the array) minus 1 (to ensure error returns are negative)
   * @throws ClassCastException if <code>key</code> and
   *           <code>sortedList</code>'s elements cannot be compared by
   *           <code>comparator</code>.
   */
  public static <T> int binarySearch(final List<? extends T> sortedList,
      final T key, Comparator<? super T> comparator) {
    /*
     * TODO: This doesn't implement the "iterator-based binary search" described
     * in the JDK docs for non-RandomAccess Lists. Until GWT provides a
     * LinkedList, this shouldn't be an issue.
     */
    if (comparator == null) {
      comparator = Comparators.natural();
    }
    int low = 0;
    int high = sortedList.size() - 1;

    while (low <= high) {
      final int mid = low + ((high - low) / 2);
      final T midVal = sortedList.get(mid);
      final int compareResult = comparator.compare(midVal, key);

      if (compareResult < 0) {
        low = mid + 1;
      } else if (compareResult > 0) {
        high = mid - 1;
      } else {
        // key found
        return mid;
      }
    }
    // key not found.
    return -low - 1;
  }

  public static <T> void copy(List<? super T> dest, List<? extends T> src) {
    // TODO(jat): optimize
    dest.addAll(src);
  }

  public static boolean disjoint(Collection<?> c1, Collection<?> c2) {
    // TODO(jat): implement
    throw new UnsupportedOperationException("disjoint not implemented");
  }

  @SuppressWarnings("unchecked")
  public static <T> List<T> emptyList() {
    return (List<T>) EMPTY_LIST; // suppress unchecked warning
  }

  @SuppressWarnings("unchecked")
  public static <K, V> Map<K, V> emptyMap() {
    return (Map<K, V>) EMPTY_MAP; // suppress unchecked warning
  }

  @SuppressWarnings("unchecked")
  public static <T> Set<T> emptySet() {
    return (Set<T>) EMPTY_SET; // suppress unchecked warning
  }

  public static <T> Enumeration<T> enumeration(Collection<T> c) {
    final Iterator<T> it = c.iterator();
    return new Enumeration<T>() {
      public boolean hasMoreElements() {
        return it.hasNext();
      }

      public T nextElement() {
        return it.next();
      }
    };
  }

  public static <T> void reverse(List<T> l) {
    int lastPos = l.size() - 1;
    for (int i = 0; i < l.size() / 2; i++) {
      T element = l.get(i);
      int swapPos = lastPos - i;
      assert (swapPos > i);
      T swappedWith = l.get(swapPos);
      l.set(i, swappedWith);
      l.set(swapPos, element);
    }
  }

  public static <T> void sort(List<T> target) {
    Object[] x = target.toArray();
    Arrays.sort(x);
    replaceContents(target, x);
  }

  public static <T> void sort(List<T> target, Comparator<? super T> c) {
    Object[] x = target.toArray();
    Arrays.unsafeSort(x, c);
    replaceContents(target, x);
  }

  public static <T> Collection<T> unmodifiableCollection(
      final Collection<? extends T> coll) {
    return new Collection<T>() {

      public boolean add(T o) {
        throw new UnsupportedOperationException(
            "unmodifiableCollection: add not permitted");
      }

      public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException(
            "unmodifiableCollection: addAll not permitted");
      }

      public void clear() {
        throw new UnsupportedOperationException(
           "unmodifiableCollection: clear not permitted");
      }

      public boolean contains(Object o) {
        return coll.contains(o);
      }

      public boolean containsAll(Collection<?> c) {
        return coll.containsAll(c);
      }

      public boolean isEmpty() {
        return coll.isEmpty();
      }

      public Iterator<T> iterator() {
        final Iterator<? extends T> it = coll.iterator();
        return new Iterator<T>() {

          public boolean hasNext() {
            return it.hasNext();
          }

          public T next() {
            return it.next();
          }

          public void remove() {
            throw new UnsupportedOperationException(
              "unmodifiableCollection.iterator: remove not permitted");
          }
        };
      }

      public boolean remove(Object o) {
        throw new UnsupportedOperationException(
            "unmodifiableCollection: remove not permitted");
      }

      public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException(
            "unmodifiableCollection: removeAll not permitted");
      }

      public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException(
            "unmodifiableCollection: retainAll not permitted");
      }

      public int size() {
        return coll.size();
      }

      public Object[] toArray() {
        return coll.toArray();
      }

      public <OT> OT[] toArray(OT[] a) {
        return coll.toArray(a);
      }
    };
  };

  public static <T> List<T> unmodifiableList(final List<? extends T> list) {
    return new List<T>() {
      public void add(int index, T element) {
        throw new UnsupportedOperationException(
            "unmodifiableList: add not permitted");
      }

      public boolean add(T o) {
        throw new UnsupportedOperationException(
            "unmodifiableList: add not permitted");
      }

      public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException(
            "unmodifiableList: addAll not permitted");
      }

      public boolean addAll(int index, Collection<? extends T> c) {
        throw new UnsupportedOperationException(
            "unmodifiableList: addAll not permitted");
      }

      public void clear() {
        throw new UnsupportedOperationException(
            "unmodifiableList: clear not permitted");
      }

      public boolean contains(Object o) {
        return list.contains(o);
      }

      public boolean containsAll(Collection<?> c) {
        return list.containsAll(c);
      }

      public T get(int index) {
        return list.get(index);
      }

      public int indexOf(Object o) {
        return list.indexOf(o);
      }

      public boolean isEmpty() {
        return list.isEmpty();
      }

      public Iterator<T> iterator() {
        return listIterator();
      }

      public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
      }

      public ListIterator<T> listIterator() {
        return new UnmodifiableListIterator<T>(list.listIterator());
      }

      public ListIterator<T> listIterator(int from) {
        return new UnmodifiableListIterator<T>(list.listIterator(from));
      }

      public T remove(int index) {
        throw new UnsupportedOperationException(
            "unmodifiableList: remove not permitted");
      }

      public boolean remove(Object o) {
        throw new UnsupportedOperationException(
            "unmodifiableList: remove not permitted");
      }

      public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException(
            "unmodifiableList: removeAll not permitted");
      }

      public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException(
            "unmodifiableList: retainAll not permitted");
      }

      public T set(int index, T element) {
        throw new UnsupportedOperationException(
            "unmodifiableList: set not permitted");
      }

      public int size() {
        return list.size();
      }

      public List<T> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException(
            "unmodifiableList: subList not permitted");
      }

      public Object[] toArray() {
        return list.toArray();
      }

      public <OT> OT[] toArray(OT[] array) {
        return list.toArray(array);
      }
    };
  }

  public static <K, V> Map<K, V> unmodifiableMap(
      Map<? extends K, ? extends V> map) {
    throw new UnsupportedOperationException("unmodifiableMap not implemented");
  }

  public static <T> Set<T> unmodifiableSet(Set<? extends T> set) {
    throw new UnsupportedOperationException("unmodifiableSet not implemented");
  }

  public static <K, V> SortedMap<K, V> unmodifiableSortedMap(
      SortedMap<? extends K, ? extends V> map) {
    throw new UnsupportedOperationException(
        "unmodifiableSortedMap not implemented");
  }

  public static <T> SortedSet<T> unmodifiableSortedSet(
      SortedSet<? extends T> set) {
    throw new UnsupportedOperationException(
        "unmodifiableSortedSet not implemented");
  }

  /**
   * Replace contents of a list from an array.
   *
   * @param <T> element type
   * @param target list to replace contents from an array
   * @param x an Object array which can contain only T instances
   */
  @SuppressWarnings("unchecked")
  private static <T> void replaceContents(List<T> target, Object[] x) {
    int size = target.size();
    assert (x.length == size);
    for (int i = 0; i < size; i++) {
      target.set(i, (T) x[i]); // suppress unchecked
    }
  }

}
