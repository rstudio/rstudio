/*
 * Copyright 2014 Google Inc.
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
 * Skeletal implementation of a NavigableMap.
 */
abstract class AbstractNavigableMap<K, V> extends AbstractMap<K, V> implements NavigableMap<K, V> {

  class DescendingMap extends AbstractNavigableMap<K, V> {
    @Override
    public void clear() {
      ascendingMap().clear();
    }

    @Override
    public Comparator<? super K> comparator() {
      return Collections.reverseOrder(ascendingMap().comparator());
    }

    @Override
    public NavigableMap<K, V> descendingMap() {
      return ascendingMap();
    }

    @Override
    public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
      return ascendingMap().tailMap(toKey, inclusive).descendingMap();
    }

    @Override
    public V put(K key, V value) {
      return ascendingMap().put(key, value);
    }

    @Override
    public V remove(Object key) {
      return ascendingMap().remove(key);
    }

    @Override
    public int size() {
      return ascendingMap().size();
    }

    @Override
    public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive,
        K toKey, boolean toInclusive) {
      return ascendingMap().subMap(toKey, toInclusive, fromKey, fromInclusive).descendingMap();
    }

    @Override
    public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
      return ascendingMap().headMap(fromKey, inclusive).descendingMap();
    }

    AbstractNavigableMap<K, V> ascendingMap() {
      return AbstractNavigableMap.this;
    }

    @Override
    Iterator<Entry<K, V>> descendingEntryIterator() {
      return ascendingMap().entryIterator();
    }

    @Override
    Iterator<Entry<K, V>> entryIterator() {
      return ascendingMap().descendingEntryIterator();
    }

    @Override
    Entry<K, V> getEntry(K key) {
      return ascendingMap().getEntry(key);
    }

    @Override
    Entry<K, V> getFirstEntry() {
      return ascendingMap().getLastEntry();
    }

    @Override
    Entry<K, V> getLastEntry() {
      return ascendingMap().getFirstEntry();
    }

    @Override
    Entry<K, V> getCeilingEntry(K key) {
      return ascendingMap().getFloorEntry(key);
    }

    @Override
    Entry<K, V> getFloorEntry(K key) {
      return ascendingMap().getCeilingEntry(key);
    }

    @Override
    Entry<K, V> getHigherEntry(K key) {
      return ascendingMap().getLowerEntry(key);
    }

    @Override
    Entry<K, V> getLowerEntry(K key) {
      return ascendingMap().getHigherEntry(key);
    }

    @Override
    boolean removeEntry(Entry<K, V> entry) {
      return ascendingMap().removeEntry(entry);
    }
  }

  class EntrySet extends AbstractSet<Entry<K, V>> {
    @Override
    public boolean contains(Object o) {
      return (o instanceof Entry) && containsEntry((Entry<?, ?>) o);
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
      return entryIterator();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean remove(Object o) {
      if (o instanceof Entry) {
        Entry<K, V> entry = (Entry<K, V>) o;
        return removeEntry(entry);
      }
      return false;
    }

    @Override
    public int size() {
      return AbstractNavigableMap.this.size();
    }
  }

  private static final class NavigableKeySet<K, V> extends AbstractSet<K>
      implements NavigableSet<K> {

    private final NavigableMap<K, V> map;

    NavigableKeySet(NavigableMap<K, V> map) {
      this.map = map;
    }

    @Override
    public K ceiling(K k) {
      return map.ceilingKey(k);
    }

    @Override
    public void clear() {
      map.clear();
    }

    @Override
    public Comparator<? super K> comparator() {
      return map.comparator();
    }

    @Override
    public boolean contains(Object o) {
      return map.containsKey(o);
    }

    @Override
    public Iterator<K> descendingIterator() {
      return descendingSet().iterator();
    }

    @Override
    public NavigableSet<K> descendingSet() {
      return map.descendingMap().navigableKeySet();
    }

    @Override
    public K first() {
      return map.firstKey();
    }

    @Override
    public K floor(K k) {
      return map.floorKey(k);
    }

    @Override
    public SortedSet<K> headSet(K toElement) {
      return headSet(toElement, false);
    }

    @Override
    public NavigableSet<K> headSet(K toElement, boolean inclusive) {
      return map.headMap(toElement, inclusive).navigableKeySet();
    }

    @Override
    public K higher(K k) {
      return map.higherKey(k);
    }

    @Override
    public Iterator<K> iterator() {
      final Iterator<Entry<K, V>> entryIterator = map.entrySet().iterator();
      return new Iterator<K>() {
        @Override
        public boolean hasNext() {
          return entryIterator.hasNext();
        }

        @Override
        public K next() {
          Entry<K, V> entry = entryIterator.next();
          return entry.getKey();
        }

        @Override
        public void remove() {
          entryIterator.remove();
        }
      };
    }

    @Override
    public K last() {
      return map.lastKey();
    }

    @Override
    public K lower(K k) {
      return map.lowerKey(k);
    }

    @Override
    public K pollFirst() {
      return getEntryKeyOrNull(map.pollFirstEntry());
    }

    @Override
    public K pollLast() {
      return getEntryKeyOrNull(map.pollLastEntry());
    }

    @Override
    public boolean remove(Object o) {
      if (map.containsKey(o)) {
        map.remove(o);
        return true;
      }
      return false;
    }

    @Override
    public int size() {
      return map.size();
    }

    @Override
    public NavigableSet<K> subSet(K fromElement, boolean fromInclusive,
        K toElement, boolean toInclusive) {
      return map.subMap(fromElement, fromInclusive, toElement, toInclusive).navigableKeySet();
    }

    @Override
    public SortedSet<K> subSet(K fromElement, K toElement) {
      return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<K> tailSet(K fromElement) {
      return tailSet(fromElement, true);
    }

    @Override
    public NavigableSet<K> tailSet(K fromElement, boolean inclusive) {
      return map.tailMap(fromElement, inclusive).navigableKeySet();
    }
  }

  private static <K, V> Entry<K, V> copyOf(Entry<K, V> entry) {
    return entry == null ? null : new SimpleImmutableEntry<K, V>(entry);
  }

  private static <K, V> K getKeyOrNSE(Entry<K, V> entry) {
    if (entry == null) {
      throw new NoSuchElementException();
    }
    return entry.getKey();
  }

  @Override
  public Entry<K, V> ceilingEntry(K key) {
    return copyOf(getCeilingEntry(key));
  }

  @Override
  public K ceilingKey(K key) {
    return getEntryKeyOrNull(getCeilingEntry(key));
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean containsKey(Object k) {
    K key = (K) k;
    return getEntry(key) != null;
  }

  @Override
  public NavigableSet<K> descendingKeySet() {
    return descendingMap().navigableKeySet();
  }

  @Override
  public NavigableMap<K, V> descendingMap() {
    return new DescendingMap();
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return new EntrySet();
  }

  @Override
  public Entry<K, V> firstEntry() {
    return copyOf(getFirstEntry());
  }

  @Override
  public K firstKey() {
    return getKeyOrNSE(getFirstEntry());
  }

  @Override
  public Entry<K, V> floorEntry(K key) {
    return copyOf(getFloorEntry(key));
  }

  @Override
  public K floorKey(K key) {
    return getEntryKeyOrNull(getFloorEntry(key));
  }

  @SuppressWarnings("unchecked")
  @Override
  public V get(Object k) {
    K key = (K) k;
    return getEntryValueOrNull(getEntry(key));
  }

  @Override
  public SortedMap<K, V> headMap(K toKey) {
    return headMap(toKey, false);
  }

  @Override
  public Entry<K, V> higherEntry(K key) {
    return copyOf(getHigherEntry(key));
  }

  @Override
  public K higherKey(K key) {
    return getEntryKeyOrNull(getHigherEntry(key));
  }

  @Override
  public Set<K> keySet() {
    return navigableKeySet();
  }

  @Override
  public Entry<K, V> lastEntry() {
    return copyOf(getLastEntry());
  }

  @Override
  public K lastKey() {
    return getKeyOrNSE(getLastEntry());
  }

  @Override
  public Entry<K, V> lowerEntry(K key) {
    return copyOf(getLowerEntry(key));
  }

  @Override
  public K lowerKey(K key) {
    return getEntryKeyOrNull(getLowerEntry(key));
  }

  @Override
  public NavigableSet<K> navigableKeySet() {
    return new NavigableKeySet<K, V>(this);
  }

  @Override
  public Entry<K, V> pollFirstEntry() {
    return pollEntry(getFirstEntry());
  }

  @Override
  public Entry<K, V> pollLastEntry() {
    return pollEntry(getLastEntry());
  }

  @Override
  public SortedMap<K, V> subMap(K fromKey, K toKey) {
    return subMap(fromKey, true, toKey, false);
  }

  @Override
  public SortedMap<K, V> tailMap(K fromKey) {
    return tailMap(fromKey, true);
  }

  @SuppressWarnings("unchecked")
  @Override
  boolean containsEntry(Entry<?, ?> entry) {
    K key = (K) entry.getKey();
    Entry<K, V> lookupEntry = getEntry(key);
    return lookupEntry != null && Objects.equals(lookupEntry.getValue(), entry.getValue());
  }

  /**
   * Returns an iterator over the entries in this map in descending order.
   */
  abstract Iterator<Entry<K, V>> descendingEntryIterator();

  /**
   * Returns an iterator over the entries in this map in ascending order.
   */
  abstract Iterator<Entry<K, V>> entryIterator();

  /**
   * Returns the entry corresponding to the specified key. If no such entry exists returns
   * {@code null}.
   */
  abstract Entry<K, V> getEntry(K key);

  /**
   * Returns the first entry or {@code null} if map is empty.
   */
  abstract Entry<K, V> getFirstEntry();

  /**
   * Returns the last entry or {@code null} if map is empty.
   */
  abstract Entry<K, V> getLastEntry();

  /**
   * Gets the entry corresponding to the specified key or the entry for the least key greater than
   * the specified key. If no such entry exists returns {@code null}.
   */
  abstract Entry<K, V> getCeilingEntry(K key);

  /**
   * Gets the entry corresponding to the specified key or the entry for the greatest key less than
   * the specified key. If no such entry exists returns {@code null}.
   */
  abstract Entry<K, V> getFloorEntry(K key);

  /**
   * Gets the entry for the least key greater than the specified key. If no such entry exists
   * returns {@code null}.
   */
  abstract Entry<K, V> getHigherEntry(K key);

  /**
   * Returns the entry for the greatest key less than the specified key. If no such entry exists
   * returns {@code null}.
   */
  abstract Entry<K, V> getLowerEntry(K key);

  /**
   * Remove an entry from the tree, returning whether it was found.
   */
  abstract boolean removeEntry(Entry<K, V> entry);

  private Entry<K, V> pollEntry(Entry<K, V> entry) {
    if (entry != null) {
      removeEntry(entry);
    }
    return copyOf(entry);
  }
}
