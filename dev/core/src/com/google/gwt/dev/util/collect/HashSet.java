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
import java.lang.reflect.Array;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A memory-efficient hash set.
 * 
 * @param <E> the element type
 */
public class HashSet<E> extends AbstractSet<E> implements Serializable {

  private class SetIterator implements Iterator<E> {
    private int index = 0;
    private int last = -1;

    public SetIterator() {
      advanceToItem();
    }

    public boolean hasNext() {
      return index < table.length;
    }

    @SuppressWarnings("unchecked")
    public E next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      last = index;
      E toReturn = (E) unmaskNull(table[index++]);
      advanceToItem();
      return toReturn;
    }

    public void remove() {
      if (last < 0) {
        throw new IllegalStateException();
      }
      internalRemove(last);
      if (table[last] != null) {
        index = last;
      }
      last = -1;
    }

    private void advanceToItem() {
      for (; index < table.length; ++index) {
        if (table[index] != null) {
          return;
        }
      }
    }
  }

  /**
   * In the interest of memory-savings, we start with the smallest feasible
   * power-of-two table size that can hold three items without rehashing. If we
   * started with a size of 2, we'd have to expand as soon as the second item
   * was added.
   */
  private static final int INITIAL_TABLE_SIZE = 4;

  private static final Object NULL_ITEM = new Serializable() {
    Object readResolve() {
      return NULL_ITEM;
    }
  };

  static Object maskNull(Object o) {
    return (o == null) ? NULL_ITEM : o;
  }

  static Object unmaskNull(Object o) {
    return (o == NULL_ITEM) ? null : o;
  }

  /**
   * Number of objects in this set; transient due to custom serialization.
   * Default access to avoid synthetic accessors from inner classes.
   */
  transient int size = 0;

  /**
   * Backing store for all the objects; transient due to custom serialization.
   * Default access to avoid synthetic accessors from inner classes.
   */
  transient Object[] table;

  public HashSet() {
    table = new Object[INITIAL_TABLE_SIZE];
  }

  public HashSet(Collection<? extends E> c) {
    int newCapacity = INITIAL_TABLE_SIZE;
    int expectedSize = c.size();
    while (newCapacity * 3 < expectedSize * 4) {
      newCapacity <<= 1;
    }

    table = new Object[newCapacity];
    super.addAll(c);
  }

  @Override
  public boolean add(E e) {
    ensureSizeFor(size + 1);
    int index = findOrEmpty(e);
    if (table[index] == null) {
      ++size;
      table[index] = maskNull(e);
      return true;
    }
    return false;
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    ensureSizeFor(size + c.size());
    return super.addAll(c);
  }

  @Override
  public void clear() {
    table = new Object[INITIAL_TABLE_SIZE];
    size = 0;
  }

  @Override
  public boolean contains(Object o) {
    return find(o) >= 0;
  }

  @Override
  public Iterator<E> iterator() {
    return new SetIterator();
  }

  @Override
  public boolean remove(Object o) {
    int index = find(o);
    if (index < 0) {
      return false;
    }
    internalRemove(index);
    return true;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public Object[] toArray() {
    return toArray(new Object[size]);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T[] toArray(T[] a) {
    if (a.length < size) {
      a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
    }
    int index = 0;
    for (int i = 0; i < table.length; ++i) {
      Object e = table[i];
      if (e != null) {
        a[index++] = (T) unmaskNull(e);
      }
    }
    while (index < a.length) {
      a[index++] = null;
    }
    return a;
  }

  /**
   * Adapted from {@link org.apache.commons.collections.map.AbstractHashedMap}.
   */
  @SuppressWarnings("unchecked")
  protected void doReadObject(ObjectInputStream in) throws IOException,
      ClassNotFoundException {
    table = new Object[in.readInt()];
    int items = in.readInt();
    for (int i = 0; i < items; i++) {
      add((E) in.readObject());
    }
  }

  /**
   * Adapted from {@link org.apache.commons.collections.map.AbstractHashedMap}.
   */
  protected void doWriteObject(ObjectOutputStream out) throws IOException {
    out.writeInt(table.length);
    out.writeInt(size);
    for (int i = 0; i < table.length; ++i) {
      Object e = table[i];
      if (e != null) {
        out.writeObject(unmaskNull(e));
      }
    }
  }

  /**
   * Returns whether two items are equal for the purposes of this set.
   */
  protected boolean itemEquals(Object a, Object b) {
    return (a == null) ? (b == null) : a.equals(b);
  }

  /**
   * Return the hashCode for an item.
   */
  protected int itemHashCode(Object o) {
    return (o == null) ? 0 : o.hashCode();
  }

  /**
   * Works just like {@link #addAll(Collection)}, but for arrays. Used to avoid
   * having to synthesize a collection in {@link Sets}.
   */
  void addAll(E[] elements) {
    ensureSizeFor(size + elements.length);
    for (E e : elements) {
      int index = findOrEmpty(e);
      if (table[index] == null) {
        ++size;
        table[index] = maskNull(e);
      }
    }
  }

  /**
   * Removes the item at the specified index, and performs internal management
   * to make sure we don't wind up with a hole in the table. Default access to
   * avoid synthetic accessors from inner classes.
   */
  void internalRemove(int index) {
    table[index] = null;
    --size;
    plugHole(index);
  }

  /**
   * Ensures the set is large enough to contain the specified number of entries.
   */
  private void ensureSizeFor(int expectedSize) {
    if (table.length * 3 >= expectedSize * 4) {
      return;
    }

    int newCapacity = table.length << 1;
    while (newCapacity * 3 < expectedSize * 4) {
      newCapacity <<= 1;
    }

    Object[] oldTable = table;
    table = new Object[newCapacity];
    for (Object o : oldTable) {
      if (o != null) {
        int newIndex = getIndex(unmaskNull(o));
        while (table[newIndex] != null) {
          if (++newIndex == table.length) {
            newIndex = 0;
          }
        }
        table[newIndex] = o;
      }
    }
  }

  /**
   * Returns the index in the table at which a particular item resides, or -1 if
   * the item is not in the table.
   */
  private int find(Object o) {
    int index = getIndex(o);
    while (true) {
      Object existing = table[index];
      if (existing == null) {
        return -1;
      }
      if (itemEquals(o, unmaskNull(existing))) {
        return index;
      }
      if (++index == table.length) {
        index = 0;
      }
    }
  }

  /**
   * Returns the index in the table at which a particular item resides, or the
   * index of an empty slot in the table where this item should be inserted if
   * it is not already in the table.
   */
  private int findOrEmpty(Object o) {
    int index = getIndex(o);
    while (true) {
      Object existing = table[index];
      if (existing == null) {
        return index;
      }
      if (itemEquals(o, unmaskNull(existing))) {
        return index;
      }
      if (++index == table.length) {
        index = 0;
      }
    }
  }

  private int getIndex(Object o) {
    int h = itemHashCode(o);
    // Copied from Apache's AbstractHashedMap; prevents power-of-two collisions.
    h += ~(h << 9);
    h ^= (h >>> 14);
    h += (h << 4);
    h ^= (h >>> 10);
    // Power of two trick.
    return h & (table.length - 1);
  }

  /**
   * Tricky, we left a hole in the map, which we have to fill. The only way to
   * do this is to search forwards through the map shuffling back values that
   * match this index until we hit a null.
   */
  private void plugHole(int hole) {
    int index = hole + 1;
    if (index == table.length) {
      index = 0;
    }
    while (table[index] != null) {
      int targetIndex = getIndex(unmaskNull(table[index]));
      if (hole < index) {
        /*
         * "Normal" case, the index is past the hole and the "bad range" is from
         * hole (exclusive) to index (inclusive).
         */
        if (!(hole < targetIndex && targetIndex <= index)) {
          // Plug it!
          table[hole] = table[index];
          table[index] = null;
          hole = index;
        }
      } else {
        /*
         * "Wrapped" case, the index is before the hole (we've wrapped) and the
         * "good range" is from index (exclusive) to hole (inclusive).
         */
        if (index < targetIndex && targetIndex <= hole) {
          // Plug it!
          table[hole] = table[index];
          table[index] = null;
          hole = index;
        }
      }
      if (++index == table.length) {
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
