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

import java.io.Serializable;

/**
 * Utility methods that operate on collections. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/Collections.html">[Sun
 * docs]</a>
 */
public class Collections {

  private static final class EmptyList extends AbstractList implements
      RandomAccess, Serializable {
    @Override
    public boolean contains(Object object) {
      return false;
    }

    @Override
    public Object get(int location) {
      throw new IndexOutOfBoundsException();
    }

    @Override
    public int size() {
      return 0;
    }
  }

  private static final class EmptySet extends AbstractSet implements
      Serializable {
    @Override
    public boolean contains(Object object) {
      return false;
    }

    @Override
    public Iterator iterator() {
      return new Iterator() {
        public boolean hasNext() {
          return false;
        }

        public Object next() {
          throw new NoSuchElementException();
        }

        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }

    @Override
    public int size() {
      return 0;
    }
}

  private static final class EmptyMap extends AbstractMap implements
      Serializable {
    @Override
    public boolean containsKey(Object key) {
      return false;
    }

    @Override
    public boolean containsValue(Object value) {
      return false;
    }

    @Override
    public Set entrySet() {
      return EMPTY_SET;
    }

    @Override
    public Object get(Object key) {
      return null;
    }

    @Override
    public Set keySet() {
      return EMPTY_SET;
    }

    @Override
    public int size() {
      return 0;
    }

    @Override
    public Collection values() {
      return EMPTY_LIST;
    }
  }

  private static final class SingletonList<E> extends AbstractList<E> implements Serializable {
    private E element;

    public SingletonList(E element) {
      this.element = element;
    }

    public boolean contains(Object item) {
      return Utility.equalsWithNullCheck(element, item);
    }

    public E get(int index) {
      if (index == 0) {
        return element;
      } else {
        throw new IndexOutOfBoundsException();
      }
    }

    public int size() {
      return 1;
    }
  }

  /*
   * TODO: make the unmodifiable collections serializable.
   */

  static class UnmodifiableCollection<T> implements Collection<T> {
    protected final Collection<? extends T> coll;

    public UnmodifiableCollection(Collection<? extends T> coll) {
      this.coll = coll;
    }

    public boolean add(T o) {
      throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection<? extends T> c) {
      throw new UnsupportedOperationException();
    }

    public void clear() {
      throw new UnsupportedOperationException();
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
      return new UnmodifiableCollectionIterator<T>(coll.iterator());
    }

    public boolean remove(Object o) {
      throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    public int size() {
      return coll.size();
    }

    public Object[] toArray() {
      return coll.toArray();
    }

    public <E> E[] toArray(E[] a) {
      return coll.toArray(a);
    }

    public String toString() {
      return coll.toString();
    }
  }

  static class UnmodifiableList<T> extends UnmodifiableCollection<T> implements
      List<T> {
    private final List<? extends T> list;

    public UnmodifiableList(List<? extends T> list) {
      super(list);
      this.list = list;
    }

    public void add(int index, T element) {
      throw new UnsupportedOperationException();
    }

    public boolean addAll(int index, Collection<? extends T> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
      return list.equals(o);
    }

    public T get(int index) {
      return list.get(index);
    }

    @Override
    public int hashCode() {
      return list.hashCode();
    }

    public int indexOf(Object o) {
      return list.indexOf(o);
    }

    @Override
    public boolean isEmpty() {
      return list.isEmpty();
    }

    public int lastIndexOf(Object o) {
      return list.lastIndexOf(o);
    }

    public ListIterator<T> listIterator() {
      return listIterator(0);
    }

    public ListIterator<T> listIterator(int from) {
      return new UnmodifiableListIterator<T>(list.listIterator(from));
    }

    public T remove(int index) {
      throw new UnsupportedOperationException();
    }

    public T set(int index, T element) {
      throw new UnsupportedOperationException();
    }

    public List<T> subList(int fromIndex, int toIndex) {
      return new UnmodifiableList<T>(list.subList(fromIndex, toIndex));
    }
  }

  static class UnmodifiableMap<K, V> implements Map<K, V> {

    static class UnmodifiableEntrySet<K, V> extends
        UnmodifiableSet<Map.Entry<K, V>> {

      private static class UnmodifiableEntry<K, V> implements Map.Entry<K, V> {
        private Map.Entry<? extends K, ? extends V> entry;

        public UnmodifiableEntry(Map.Entry<? extends K, ? extends V> entry) {
          this.entry = entry;
        }

        @Override
        public boolean equals(Object o) {
          return entry.equals(o);
        }

        public K getKey() {
          return entry.getKey();
        }

        public V getValue() {
          return entry.getValue();
        }

        @Override
        public int hashCode() {
          return entry.hashCode();
        }

        public V setValue(V value) {
          throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
          return entry.toString();
        }
      }

      @SuppressWarnings("unchecked")
      public UnmodifiableEntrySet(
          Set<? extends Map.Entry<? extends K, ? extends V>> s) {
        super((Set<? extends Entry<K, V>>) s);
      }

      @Override
      public boolean contains(Object o) {
        return coll.contains(o);
      }

      @Override
      public boolean containsAll(Collection<?> o) {
        return coll.containsAll(o);
      }

      @Override
      @SuppressWarnings("unchecked")
      public Iterator<Map.Entry<K, V>> iterator() {
        final Iterator<Map.Entry<K, V>> it = (Iterator<Entry<K, V>>) coll.iterator();
        return new Iterator<Map.Entry<K, V>>() {
          public boolean hasNext() {
            return it.hasNext();
          }

          public Map.Entry<K, V> next() {
            return new UnmodifiableEntry<K, V>(it.next());
          }

          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }

      @Override
      public Object[] toArray() {
        Object[] array = super.toArray();
        wrap(array, array.length);
        return array;
      }

      @Override
      @SuppressWarnings("unchecked")
      public <T> T[] toArray(T[] a) {
        Object[] result = super.toArray(a);
        wrap(result, coll.size());
        return (T[]) result;
      }

      /**
       * Wrap an array of Map.Entries as UnmodifiableEntries.
       *
       * @param array array to wrap
       * @param size number of entries to wrap
       */
      @SuppressWarnings("unchecked")
      private void wrap(Object[] array, int size) {
        for (int i = 0; i < size; ++i) {
          array[i] = new UnmodifiableEntry<K, V>((Map.Entry<K, V>) array[i]);
        }
      }
    }

    private transient UnmodifiableSet<Map.Entry<K, V>> entrySet;
    private transient UnmodifiableSet<K> keySet;
    private final Map<? extends K, ? extends V> map;
    private transient UnmodifiableCollection<V> values;

    public UnmodifiableMap(Map<? extends K, ? extends V> map) {
      this.map = map;
    }

    public void clear() {
      throw new UnsupportedOperationException();
    }

    public boolean containsKey(Object key) {
      return map.containsKey(key);
    }

    public boolean containsValue(Object val) {
      return map.containsValue(val);
    }

    public Set<Map.Entry<K, V>> entrySet() {
      if (entrySet == null) {
        entrySet = new UnmodifiableEntrySet<K, V>(map.entrySet());
      }
      return entrySet;
    }

    @Override
    public boolean equals(Object o) {
      return map.equals(o);
    }

    public V get(Object key) {
      return map.get(key);
    }

    @Override
    public int hashCode() {
      return map.hashCode();
    }

    public boolean isEmpty() {
      return map.isEmpty();
    }

    public Set<K> keySet() {
      if (keySet == null) {
        keySet = new UnmodifiableSet<K>(map.keySet());
      }
      return keySet;
    }

    public V put(K key, V value) {
      throw new UnsupportedOperationException();
    }

    public void putAll(Map<? extends K, ? extends V> t) {
      throw new UnsupportedOperationException();
    }

    public V remove(Object key) {
      throw new UnsupportedOperationException();
    }

    public int size() {
      return map.size();
    }

    @Override
    public String toString() {
      return map.toString();
    }

    public Collection<V> values() {
      if (values == null) {
        values = new UnmodifiableCollection<V>(map.values());
      }
      return values;
    }
  }

  static class UnmodifiableRandomAccessList<T> extends UnmodifiableList<T>
      implements RandomAccess {
    public UnmodifiableRandomAccessList(List<? extends T> list) {
      super(list);
    }
  }

  static class UnmodifiableSet<T> extends UnmodifiableCollection<T> implements
      Set<T> {
    public UnmodifiableSet(Set<? extends T> set) {
      super(set);
    }

    @Override
    public boolean equals(Object o) {
      return coll.equals(o);
    }

    @Override
    public int hashCode() {
      return coll.hashCode();
    }
  }

  static class UnmodifiableSortedMap<K, V> extends UnmodifiableMap<K, V>
      implements SortedMap<K, V> {

    private SortedMap<K, ? extends V> sortedMap;

    public UnmodifiableSortedMap(SortedMap<K, ? extends V> sortedMap) {
      super(sortedMap);
      this.sortedMap = sortedMap;
    }

    public Comparator<? super K> comparator() {
      return sortedMap.comparator();
    }

    @Override
    public boolean equals(Object o) {
      return sortedMap.equals(o);
    }

    public K firstKey() {
      return sortedMap.firstKey();
    }

    @Override
    public int hashCode() {
      return sortedMap.hashCode();
    }

    public SortedMap<K, V> headMap(K toKey) {
      return new UnmodifiableSortedMap<K, V>(sortedMap.headMap(toKey));
    }

    public K lastKey() {
      return sortedMap.lastKey();
    }

    public SortedMap<K, V> subMap(K fromKey, K toKey) {
      return new UnmodifiableSortedMap<K, V>(sortedMap.subMap(fromKey, toKey));
    }

    public SortedMap<K, V> tailMap(K fromKey) {
      return new UnmodifiableSortedMap<K, V>(sortedMap.tailMap(fromKey));
    }
  }

  static class UnmodifiableSortedSet<E> extends UnmodifiableSet<E> implements
      SortedSet<E> {
    private SortedSet<E> sortedSet;

    @SuppressWarnings("unchecked")
    public UnmodifiableSortedSet(SortedSet<? extends E> sortedSet) {
      super(sortedSet);
      this.sortedSet = (SortedSet<E>) sortedSet;
    }

    public Comparator<? super E> comparator() {
      return sortedSet.comparator();
    }

    @Override
    public boolean equals(Object o) {
      return sortedSet.equals(o);
    }

    public E first() {
      return sortedSet.first();
    }

    @Override
    public int hashCode() {
      return sortedSet.hashCode();
    }

    public SortedSet<E> headSet(E toElement) {
      return new UnmodifiableSortedSet<E>(sortedSet.headSet(toElement));
    }

    public E last() {
      return sortedSet.last();
    }

    public SortedSet<E> subSet(E fromElement, E toElement) {
      return new UnmodifiableSortedSet<E>(sortedSet.subSet(fromElement,
          toElement));
    }

    public SortedSet<E> tailSet(E fromElement) {
      return new UnmodifiableSortedSet<E>(sortedSet.tailSet(fromElement));
    }
  }

  private static class UnmodifiableCollectionIterator<T> implements Iterator<T> {
    private final Iterator<? extends T> it;

    private UnmodifiableCollectionIterator(Iterator<? extends T> it) {
      this.it = it;
    }

    public boolean hasNext() {
      return it.hasNext();
    }

    public T next() {
      return it.next();
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  private static class UnmodifiableListIterator<T> extends
      UnmodifiableCollectionIterator<T> implements ListIterator<T> {
    private final ListIterator<? extends T> lit;

    private UnmodifiableListIterator(ListIterator<? extends T> lit) {
      super(lit);
      this.lit = lit;
    }

    public void add(T o) {
      throw new UnsupportedOperationException();
    }

    public boolean hasPrevious() {
      return lit.hasPrevious();
    }

    public int nextIndex() {
      return lit.nextIndex();
    }

    public T previous() {
      return lit.previous();
    }

    public int previousIndex() {
      return lit.previousIndex();
    }

    public void set(T o) {
      throw new UnsupportedOperationException();
    }
  }

  @SuppressWarnings("unchecked")
  public static final List EMPTY_LIST = new EmptyList();

  @SuppressWarnings("unchecked")
  public static final Map EMPTY_MAP = new EmptyMap();

  @SuppressWarnings("unchecked")
  public static final Set EMPTY_SET = new EmptySet();

  private static Comparator<Comparable<Object>> reverseComparator = new Comparator<Comparable<Object>>() {
    public int compare(Comparable<Object> o1, Comparable<Object> o2) {
      return o2.compareTo(o1);
    }
  };

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
      final int mid = low + ((high - low) >> 1);
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
    Collection<?> iterating = c1;
    Collection<?> testing = c2;

    // See if one of these objects possibly implements a fast contains.
    if ((c1 instanceof Set) && !(c2 instanceof Set)) {
      iterating = c2;
      testing = c1;
    }

    for (Object o : iterating) {
      if (testing.contains(o)) {
        return false;
      }
    }

    return true;
  }

  @SuppressWarnings(value = {"unchecked", "cast"})
  public static <T> List<T> emptyList() {
    return (List<T>) EMPTY_LIST;
  }

  @SuppressWarnings(value = {"unchecked", "cast"})
  public static <K, V> Map<K, V> emptyMap() {
    return (Map<K, V>) EMPTY_MAP;
  }

  @SuppressWarnings(value = {"unchecked", "cast"})
  public static <T> Set<T> emptySet() {
    return (Set<T>) EMPTY_SET;
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

  public static <T> void fill(List<? super T> list, T obj) {
    for (ListIterator<? super T> it = list.listIterator(); it.hasNext();) {
      it.next();
      it.set(obj);
    }
  }

  public static int frequency(Collection<?> c, Object o) {
    int count = 0;
    for (Object e : c) {
      if (o == null ? e == null : o.equals(e)) {
        ++count;
      }
    }
    return count;
  }

  public static <T> ArrayList<T> list(Enumeration<T> e) {
    ArrayList<T> arrayList = new ArrayList<T>();
    while (e.hasMoreElements()) {
      arrayList.add(e.nextElement());
    }
    return arrayList;
  }

  public static <T extends Object & Comparable<? super T>> T max(
      Collection<? extends T> coll) {
    return max(coll, null);
  }

  public static <T> T max(Collection<? extends T> coll,
      Comparator<? super T> comp) {

    if (comp == null) {
      comp = Comparators.natural();
    }

    Iterator<? extends T> it = coll.iterator();

    // Will throw NoSuchElementException if coll is empty.
    T max = it.next();

    while (it.hasNext()) {
      T t = it.next();
      if (comp.compare(t, max) > 0) {
        max = t;
      }
    }

    return max;
  }

  public static <T extends Object & Comparable<? super T>> T min(
      Collection<? extends T> coll) {
    return min(coll, null);
  }

  public static <T> T min(Collection<? extends T> coll,
      Comparator<? super T> comp) {
    return max(coll, reverseOrder(comp));
  }

  public static <T> List<T> nCopies(int n, T o) {
    ArrayList<T> list = new ArrayList<T>();
    for (int i = 0; i < n; ++i) {
      list.add(o);
    }
    return unmodifiableList(list);
  }

  public static <T> boolean replaceAll(List<T> list, T oldVal, T newVal) {
    boolean modified = false;
    for (ListIterator<T> it = list.listIterator(); it.hasNext();) {
      T t = it.next();
      if (t == null ? oldVal == null : t.equals(oldVal)) {
        it.set(newVal);
        modified = true;
      }
    }
    return modified;
  }

  public static <T> void reverse(List<T> l) {
    if (l instanceof RandomAccess) {
      for (int iFront = 0, iBack = l.size() - 1; iFront < iBack; ++iFront, --iBack) {
        Collections.swap(l, iFront, iBack);
      }
    } else {
      ListIterator<T> head = l.listIterator();
      ListIterator<T> tail = l.listIterator(l.size());
      while (head.nextIndex() < tail.previousIndex()) {
        T headElem = head.next();
        T tailElem = tail.previous();
        head.set(tailElem);
        tail.set(headElem);
      }
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> Comparator<T> reverseOrder() {
    return (Comparator<T>) reverseComparator;
  }

  public static <T> Comparator<T> reverseOrder(final Comparator<T> cmp) {
    if (cmp == null) {
      return reverseOrder();
    }
    return new Comparator<T>() {
      public int compare(T t1, T t2) {
        return cmp.compare(t2, t1);
      }
    };
  }

  public static <T> Set<T> singleton(T o) {
    HashSet<T> set = new HashSet<T>(1);
    set.add(o);
    return unmodifiableSet(set);
  }

  // TODO(tobyr) Is it worth creating custom singleton sets, lists, and maps?
  // More efficient at runtime, but more code bloat to download

  public static <T> List<T> singletonList(T o) {
    return new SingletonList<T>(o);
  }

  public static <K, V> Map<K, V> singletonMap(K key, V value) {
    Map<K, V> map = new HashMap<K, V>(1);
    map.put(key, value);
    return unmodifiableMap(map);
  }

  public static <T> void sort(List<T> target) {
    Object[] x = target.toArray();
    Arrays.sort(x);
    replaceContents(target, x);
  }

  @SuppressWarnings("unchecked")
  public static <T> void sort(List<T> target, Comparator<? super T> c) {
    Object[] x = target.toArray();
    Arrays.sort(x, (Comparator<Object>) c);
    replaceContents(target, x);
  }

  public static void swap(List<?> list, int i, int j) {
    swapImpl(list, i, j);
  }

  public static <T> Collection<T> unmodifiableCollection(
      final Collection<? extends T> coll) {
    return new UnmodifiableCollection<T>(coll);
  }

  public static <T> List<T> unmodifiableList(List<? extends T> list) {
    return (list instanceof RandomAccess)
        ? new UnmodifiableRandomAccessList<T>(list) : new UnmodifiableList<T>(
            list);
  }

  public static <K, V> Map<K, V> unmodifiableMap(
      final Map<? extends K, ? extends V> map) {
    return new UnmodifiableMap<K, V>(map);
  }

  public static <T> Set<T> unmodifiableSet(Set<? extends T> set) {
    return new UnmodifiableSet<T>(set);
  }

  public static <K, V> SortedMap<K, V> unmodifiableSortedMap(
      SortedMap<K, ? extends V> map) {
    return new UnmodifiableSortedMap<K, V>(map);
  }

  public static <T> SortedSet<T> unmodifiableSortedSet(
      SortedSet<? extends T> set) {
    return new UnmodifiableSortedSet<T>(set);
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
      target.set(i, (T) x[i]);
    }
  }

  private static <T> void swapImpl(List<T> list, int i, int j) {
    T t = list.get(i);
    list.set(i, list.get(j));
    list.set(j, t);
  }
}
