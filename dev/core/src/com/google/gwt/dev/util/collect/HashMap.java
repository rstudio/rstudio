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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A memory-efficient hash map.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class HashMap<K, V> implements Map<K, V>, Serializable {

  /**
   * In the interest of memory-savings, we start with the smallest feasible
   * power-of-two table size that can hold three items without rehashing. If we
   * started with a size of 2, we'd have to expand as soon as the second item
   * was added.
   */
  private static final int INITIAL_TABLE_SIZE = 4;

  private abstract class BaseIterator<E> implements Iterator<E> {
    private Object[] coModCheckKeys = keys;
    private int index = 0;
    private int last = -1;

    @Override
    public boolean hasNext() {
      if (coModCheckKeys != keys) {
        throw new ConcurrentModificationException();
      }
      advanceToItem();
      return index < keys.length;
    }

    @Override
    public E next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      last = index;
      return iteratorItem(index++);
    }

    @Override
    public void remove() {
      if (last < 0) {
        throw new IllegalStateException();
      }
      if (coModCheckKeys != keys) {
        throw new ConcurrentModificationException();
      }
      internalRemove(last);
      if (keys[last] != null) {
        // Hole was plugged.
        index = last;
      }
      last = -1;
    }

    protected abstract E iteratorItem(int index);

    private void advanceToItem() {
      for (; index < keys.length; ++index) {
        if (keys[index] != null) {
          return;
        }
      }
    }
  }

  private class EntryIterator extends BaseIterator<Entry<K, V>> {
    @Override
    protected Entry<K, V> iteratorItem(int index) {
      return new HashEntry(index);
    }
  }

  private class EntrySet extends AbstractSet<Entry<K, V>> {
    @Override
    public boolean add(Entry<K, V> entry) {
      boolean result = !HashMap.this.containsKey(entry.getKey());
      HashMap.this.put(entry.getKey(), entry.getValue());
      return result;
    }

    @Override
    public boolean addAll(Collection<? extends Entry<K, V>> c) {
      HashMap.this.resizeForJoin(c.size());
      return super.addAll(c);
    }

    @Override
    public void clear() {
      HashMap.this.clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
      if (!(o instanceof Entry)) {
        return false;
      }
      Entry<K, V> entry = (Entry<K, V>) o;
      V value = HashMap.this.get(entry.getKey());
      return HashMap.this.valueEquals(value, entry.getValue());
    }

    @Override
    public int hashCode() {
      return HashMap.this.hashCode();
    }

    @Override
    public Iterator<java.util.Map.Entry<K, V>> iterator() {
      return new EntryIterator();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean remove(Object o) {
      if (!(o instanceof Entry)) {
        return false;
      }
      Entry<K, V> entry = (Entry<K, V>) o;
      int index = findKey(entry.getKey());
      if (index >= 0 && valueEquals(values[index], entry.getValue())) {
        internalRemove(index);
        return true;
      }
      return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      boolean didRemove = false;
      for (Object o : c) {
        didRemove |= remove(o);
      }
      return didRemove;
    }

    @Override
    public int size() {
      return HashMap.this.size;
    }
  }

  private class HashEntry implements Entry<K, V> {
    private final int index;

    public HashEntry(int index) {
      this.index = index;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object o) {
      if (!(o instanceof Entry)) {
        return false;
      }
      Entry<K, V> entry = (Entry<K, V>) o;
      return keyEquals(getKey(), entry.getKey())
          && valueEquals(getValue(), entry.getValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public K getKey() {
      return (K) unmaskNullKey(keys[index]);
    }

    @Override
    @SuppressWarnings("unchecked")
    public V getValue() {
      return (V) values[index];
    }

    @Override
    public int hashCode() {
      return keyHashCode(getKey()) ^ valueHashCode(getValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public V setValue(V value) {
      V previous = (V) values[index];
      values[index] = value;
      return previous;
    }

    @Override
    public String toString() {
      return getKey() + "=" + getValue();
    }
  }

  private class KeyIterator extends BaseIterator<K> {
    @SuppressWarnings("unchecked")
    @Override
    protected K iteratorItem(int index) {
      return (K) unmaskNullKey(keys[index]);
    }
  }

  private class KeySet extends AbstractSet<K> {
    @Override
    public void clear() {
      HashMap.this.clear();
    }

    @Override
    public boolean contains(Object o) {
      return HashMap.this.containsKey(o);
    }

    @Override
    public int hashCode() {
      int result = 0;
      for (int i = 0; i < keys.length; ++i) {
        Object key = keys[i];
        if (key != null) {
          result += keyHashCode(unmaskNullKey(key));
        }
      }
      return result;
    }

    @Override
    public Iterator<K> iterator() {
      return new KeyIterator();
    }

    @Override
    public boolean remove(Object o) {
      int index = findKey(o);
      if (index >= 0) {
        internalRemove(index);
        return true;
      }
      return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      boolean didRemove = false;
      for (Object o : c) {
        didRemove |= remove(o);
      }
      return didRemove;
    }

    @Override
    public int size() {
      return HashMap.this.size;
    }
  }

  private class ValueIterator extends BaseIterator<V> {
    @SuppressWarnings("unchecked")
    @Override
    protected V iteratorItem(int index) {
      return (V) values[index];
    }
  }

  private class Values extends AbstractCollection<V> {
    @Override
    public void clear() {
      HashMap.this.clear();
    }

    @Override
    public boolean contains(Object o) {
      return HashMap.this.containsValue(o);
    }

    @Override
    public int hashCode() {
      int result = 0;
      for (int i = 0; i < keys.length; ++i) {
        if (keys[i] != null) {
          result += valueHashCode(values[i]);
        }
      }
      return result;
    }

    @Override
    public Iterator<V> iterator() {
      return new ValueIterator();
    }

    @Override
    public boolean remove(Object o) {
      if (o == null) {
        for (int i = 0; i < keys.length; ++i) {
          if (keys[i] != null && values[i] == null) {
            internalRemove(i);
            return true;
          }
        }
      } else {
        for (int i = 0; i < keys.length; ++i) {
          if (valueEquals(values[i], o)) {
            internalRemove(i);
            return true;
          }
        }
      }
      return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      boolean didRemove = false;
      for (Object o : c) {
        didRemove |= remove(o);
      }
      return didRemove;
    }

    @Override
    public int size() {
      return HashMap.this.size;
    }
  }

  private static final Object NULL_KEY = new Serializable() {
    Object readResolve() {
      return NULL_KEY;
    }
  };

  static Object maskNullKey(Object k) {
    return (k == null) ? NULL_KEY : k;
  }

  static Object unmaskNullKey(Object k) {
    return (k == NULL_KEY) ? null : k;
  }

  /**
   * Backing store for all the keys; transient due to custom serialization.
   * Default access to avoid synthetic accessors from inner classes.
   */
  transient Object[] keys;

  /**
   * Number of pairs in this set; transient due to custom serialization. Default
   * access to avoid synthetic accessors from inner classes.
   */
  transient int size = 0;

  /**
   * Backing store for all the values; transient due to custom serialization.
   * Default access to avoid synthetic accessors from inner classes.
   */
  transient Object[] values;

  /**
   * Whether the map should be sorted when being serialized. If true, the
   * key must a Comparable.
   */
  final boolean stableWriteObject;


  public HashMap() {
    this(false);
  }

  public HashMap(boolean stableWriteObject) {
    this.stableWriteObject = stableWriteObject;
    initTable(INITIAL_TABLE_SIZE);
  }

  public HashMap(Map<? extends K, ? extends V> m) {
    this.stableWriteObject = false;
    int newCapacity = INITIAL_TABLE_SIZE;
    int expectedSize = m.size();
    while (newCapacity * 3 < expectedSize * 4) {
      newCapacity <<= 1;
    }

    initTable(newCapacity);
    putAll(m);
  }

  @Override
  public void clear() {
    initTable(INITIAL_TABLE_SIZE);
    size = 0;
  }

  @Override
  public boolean containsKey(Object key) {
    return findKey(key) >= 0;
  }

  @Override
  public boolean containsValue(Object value) {
    if (value == null) {
      for (int i = 0; i < keys.length; ++i) {
        if (keys[i] != null && values[i] == null) {
          return true;
        }
      }
    } else {
      for (Object existing : values) {
        if (valueEquals(existing, value)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return new EntrySet();
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean equals(Object o) {
    if (!(o instanceof Map)) {
      return false;
    }
    Map<K, V> other = (Map<K, V>) o;
    return entrySet().equals(other.entrySet());
  }

  @Override
  @SuppressWarnings("unchecked")
  public V get(Object key) {
    int index = findKey(key);
    return (index < 0) ? null : (V) values[index];
  }

  @Override
  public int hashCode() {
    int result = 0;
    for (int i = 0; i < keys.length; ++i) {
      Object key = keys[i];
      if (key != null) {
        result += keyHashCode(unmaskNullKey(key)) ^ valueHashCode(values[i]);
      }
    }
    return result;
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public Set<K> keySet() {
    return new KeySet();
  }

  @Override
  @SuppressWarnings("unchecked")
  public V put(K key, V value) {
    int index = findKeyOrEmpty(key);
    if (keys[index] == null) {
      // Not in the map, may need to grow.
      if (ensureSizeFor(++size)) {
        // If we had to grow the table, must recompute the index.
        index = findKeyOrEmpty(key);
      }
      keys[index] = maskNullKey(key);
      values[index] = value;
      return null;
    } else {
      // In the map, set a new value;
      Object previousValue = values[index];
      values[index] = value;
      return (V) previousValue;
    }
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    resizeForJoin(m.size());
    for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public V remove(Object key) {
    int index = findKey(key);
    if (index < 0) {
      return null;
    }
    Object previousValue = values[index];
    internalRemove(index);
    return (V) previousValue;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public String toString() {
    if (size == 0) {
      return "{}";
    }
    StringBuilder buf = new StringBuilder(32 * size());
    buf.append('{');

    boolean needComma = false;
    for (int i = 0; i < keys.length; ++i) {
      Object key = keys[i];
      if (key != null) {
        if (needComma) {
          buf.append(',').append(' ');
        }
        key = unmaskNullKey(key);
        Object value = values[i];
        buf.append(key == this ? "(this Map)" : key).append('=').append(
            value == this ? "(this Map)" : value);
        needComma = true;
      }
    }
    buf.append('}');
    return buf.toString();
  }

  @Override
  public Collection<V> values() {
    return new Values();
  }

  /**
   * Adapted from {@link org.apache.commons.collections.map.AbstractHashedMap}.
   */
  @SuppressWarnings("unchecked")
  protected void doReadObject(ObjectInputStream in) throws IOException,
      ClassNotFoundException {
    int capacity = in.readInt();
    initTable(capacity);
    int items = in.readInt();
    for (int i = 0; i < items; i++) {
      Object key = in.readObject();
      Object value = in.readObject();
      put((K) key, (V) value);
    }
  }

  /**
   * Adapted from {@link org.apache.commons.collections.map.AbstractHashedMap}.
   */
  protected void doWriteObject(ObjectOutputStream out) throws IOException {
    out.writeInt(keys.length);
    out.writeInt(size);

    if (stableWriteObject) {
      final Integer[] idx = new Integer[keys.length];
      for (int i = 0; i < keys.length; i++) {
        idx[i] = i;
      }
      Arrays.sort(idx, new Comparator<Integer>() {
          @SuppressWarnings({"rawtypes", "unchecked"})
          @Override public int compare(final Integer o1, final Integer o2) {
              Comparable c1 = (Comparable)(keys[o1]);
              Comparable c2 = (Comparable)(keys[o2]);
              if (c1 == null) {
                // null < anything else
                return (c2 == null) ? 0 : 1;
              }
              if (c2 == null) {
                return -1;
              }
              return c1.compareTo(c2);
          }
      });

      for (int i = 0; i < keys.length; ++i) {
        int current = idx[i];
        Object key = keys[current];
        if (key != null) {
          out.writeObject(unmaskNullKey(key));
          out.writeObject(values[current]);
        }
      }
    } else {
      for (int i = 0; i < keys.length; ++i) {
        Object key = keys[i];
        if (key != null) {
          out.writeObject(unmaskNullKey(key));
          out.writeObject(values[i]);
        }
      }
    }
  }

  /**
   * Returns whether two keys are equal for the purposes of this set.
   */
  protected boolean keyEquals(Object a, Object b) {
    return (a == null) ? (b == null) : a.equals(b);
  }

  /**
   * Returns the hashCode for a key.
   */
  protected int keyHashCode(Object k) {
    return (k == null) ? 0 : k.hashCode();
  }

  /**
   * Returns whether two values are equal for the purposes of this set.
   */
  protected boolean valueEquals(Object a, Object b) {
    return (a == null) ? (b == null) : a.equals(b);
  }

  /**
   * Returns the hashCode for a value.
   */
  protected int valueHashCode(Object v) {
    return (v == null) ? 0 : v.hashCode();
  }

  /**
   * Ensures the map is large enough to contain the specified number of entries.
   * Default access to avoid synthetic accessors from inner classes.
   */
  boolean ensureSizeFor(int expectedSize) {
    if (keys.length * 3 >= expectedSize * 4) {
      return false;
    }

    int newCapacity = keys.length << 1;
    while (newCapacity * 3 < expectedSize * 4) {
      newCapacity <<= 1;
    }

    Object[] oldKeys = keys;
    Object[] oldValues = values;
    initTable(newCapacity);
    for (int i = 0; i < oldKeys.length; ++i) {
      Object k = oldKeys[i];
      if (k != null) {
        int newIndex = getKeyIndex(unmaskNullKey(k));
        while (keys[newIndex] != null) {
          if (++newIndex == keys.length) {
            newIndex = 0;
          }
        }
        keys[newIndex] = k;
        values[newIndex] = oldValues[i];
      }
    }
    return true;
  }

  /**
   * Returns the index in the key table at which a particular key resides, or -1
   * if the key is not in the table. Default access to avoid synthetic accessors
   * from inner classes.
   */
  int findKey(Object k) {
    int index = getKeyIndex(k);
    while (true) {
      Object existing = keys[index];
      if (existing == null) {
        return -1;
      }
      if (keyEquals(k, unmaskNullKey(existing))) {
        return index;
      }
      if (++index == keys.length) {
        index = 0;
      }
    }
  }

  /**
   * Returns the index in the key table at which a particular key resides, or
   * the index of an empty slot in the table where this key should be inserted
   * if it is not already in the table. Default access to avoid synthetic
   * accessors from inner classes.
   */
  int findKeyOrEmpty(Object k) {
    int index = getKeyIndex(k);
    while (true) {
      Object existing = keys[index];
      if (existing == null) {
        return index;
      }
      if (keyEquals(k, unmaskNullKey(existing))) {
        return index;
      }
      if (++index == keys.length) {
        index = 0;
      }
    }
  }

  /**
   * Removes the entry at the specified index, and performs internal management
   * to make sure we don't wind up with a hole in the table. Default access to
   * avoid synthetic accessors from inner classes.
   */
  void internalRemove(int index) {
    keys[index] = null;
    values[index] = null;
    --size;
    plugHole(index);
  }

  /**
   * Resizes this map to accommodate the minimum size required to join this map
   * with another map. This is an optimization to prevent multiple resizes
   * during the join operation. Naively, it would seem like we should resize to
   * hold {@code (size + otherSize)}. However, the incoming map might have
   * duplicates with this map; it might even be all duplicates. The correct
   * behavior when the incoming map is all duplicates is NOT to resize, and
   * therefore not to invalidate any iterators.
   * <p>
   * In practice, this strategy results in a worst-case of two resizes. In the
   * worst case, where {@code size} and {@code otherSize} are roughly equal and
   * the sets are completely disjoint, we might do 1 initial rehash and then 1
   * additional rehash down the road. But this is an edge case that requires
   * getting unlucky on both boundaries. Most of the time, we do either 1
   * initial rehash or 1 down the road, because doubling the capacity generally
   * allows this map to absorb an equally-sized disjoint map.
   */
  boolean resizeForJoin(int sizeOther) {
    return ensureSizeFor(Math.max(size, sizeOther));
  }

  private int getKeyIndex(Object k) {
    int h = keyHashCode(k);
    // Copied from Apache's AbstractHashedMap; prevents power-of-two collisions.
    h += ~(h << 9);
    h ^= (h >>> 14);
    h += (h << 4);
    h ^= (h >>> 10);
    // Power of two trick.
    return h & (keys.length - 1);
  }

  private void initTable(int capacity) {
    keys = new Object[capacity];
    values = new Object[capacity];
  }

  /**
   * Tricky, we left a hole in the map, which we have to fill. The only way to
   * do this is to search forwards through the map shuffling back values that
   * match this index until we hit a null.
   */
  private void plugHole(int hole) {
    int index = hole + 1;
    if (index == keys.length) {
      index = 0;
    }
    while (keys[index] != null) {
      int targetIndex = getKeyIndex(unmaskNullKey(keys[index]));
      if (hole < index) {
        /*
         * "Normal" case, the index is past the hole and the "bad range" is from
         * hole (exclusive) to index (inclusive).
         */
        if (!(hole < targetIndex && targetIndex <= index)) {
          // Plug it!
          keys[hole] = keys[index];
          values[hole] = values[index];
          keys[index] = null;
          values[index] = null;
          hole = index;
        }
      } else {
        /*
         * "Wrapped" case, the index is before the hole (we've wrapped) and the
         * "good range" is from index (exclusive) to hole (inclusive).
         */
        if (index < targetIndex && targetIndex <= hole) {
          // Plug it!
          keys[hole] = keys[index];
          values[hole] = values[index];
          keys[index] = null;
          values[index] = null;
          hole = index;
        }
      }
      if (++index == keys.length) {
        index = 0;
      }
    }
  }

  private void readObject(ObjectInputStream in) throws IOException,
      ClassNotFoundException {
    in.defaultReadObject();
    doReadObject(in);
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
    doWriteObject(out);
  }
}
