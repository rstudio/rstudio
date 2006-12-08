/*
 * Copyright 2006 Google Inc.
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
 * Implements a hash table mapping object keys onto object values.
 */
public class HashMap extends AbstractMap implements Map, Cloneable {

  private static class ImplMapEntry implements Map.Entry {
    private boolean inUse;

    private Object key;

    private Object value;

    public boolean equals(Object a) {
      if (a instanceof Map.Entry) {
        Map.Entry s = (Map.Entry) a;
        if (equalsWithNullCheck(key, s.getKey())
            && equalsWithNullCheck(value, s.getValue())) {
          return true;
        }
      }
      return false;
    }

    public Object getKey() {
      return key;
    }

    public Object getValue() {
      return value;
    }

    public int hashCode() {
      int keyHash = 0;
      int valueHash = 0;
      if (key != null) {
        keyHash = key.hashCode();
      }
      if (value != null) {
        valueHash = value.hashCode();
      }
      return keyHash ^ valueHash;
    }

    public Object setValue(Object object) {
      Object old = value;
      value = object;
      return old;
    }

    private boolean equalsWithNullCheck(Object a, Object b) {
      if (a == b) {
        return true;
      } else if (a == null) {
        return false;
      } else {
        return a.equals(b);
      }
    }
  }

  private class ImplMapEntryIterator implements Iterator {

    /**
     * Always points at the next full slot; equal to <code>entries.length</code>
     * if we're at the end.
     */
    private int i = 0;

    /**
     * Always points to the last element returned by next, or <code>-1</code>
     * if there is no last element.
     */
    private int last = -1;

    public ImplMapEntryIterator() {
      maybeAdvanceToFullSlot();
    }

    public boolean hasNext() {
      return (i < entries.length);
    }

    public Object next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      last = i++;
      maybeAdvanceToFullSlot();
      return entries[last];
    }

    public void remove() {
      if (last < 0) {
        throw new IllegalStateException();
      }
      // do the remove
      entries[last].inUse = false;
      --fullSlots;
      last = -1;
    }

    /**
     * Ensure that <code>i</code> points at a full slot; or is equal to
     * <code>entries.length</code> if we're at the end.
     */
    private void maybeAdvanceToFullSlot() {
      for (; i < entries.length; ++i) {
        if (entries[i] != null && entries[i].inUse) {
          return;
        }
      }
    }
  }

  /**
   * Number of physically empty slots in {@link #entries}.
   */
  private int emptySlots;

  /**
   * The underlying data store.
   */
  private ImplMapEntry[] entries;

  /**
   * Number of logically full slots in {@link #entries}.
   */
  private int fullSlots;

  /**
   * A number between 0 and 1, exclusive. Used to calculated the new
   * {@link #threshold} at which this map will be rehashed.
   */
  private float loadFactor;

  /**
   * Always equal to {@link #entries}.length * {@link #loadFactor}. When the
   * number of non-empty slots exceeds this number, a rehash is performed.
   */
  private int threshold;

  public HashMap() {
    this(16);
  }

  public HashMap(int initialCapacity) {
    this(initialCapacity, 0.75f);
  }

  public HashMap(int initialCapacity, float loadFactor) {
    if (initialCapacity < 0 || loadFactor <= 0) {
      throw new IllegalArgumentException(
          "initial capacity was negative or load factor was non-positive");
    }

    if (initialCapacity == 0) {
      // Rather than have to check for 0 every time we rehash, bumping 0 to 1
      // here.
      initialCapacity = 1;
    }

    /*
     * This implementation does not used linked lists in each slot. It is
     * physically impossible to store more entries than we have slots, so
     * fLoadFactor must be < 1 or we'll run out of slots.
     */
    if (loadFactor > 0.9f) {
      loadFactor = 0.9f;
    }

    this.loadFactor = loadFactor;
    realloc(initialCapacity);
  }

  public HashMap(Map m) {
    this(m.size());
    putAll(m);
  }

  public void clear() {
    fullSlots = 0;
    realloc(entries.length);
  }

  public Object clone() {
    return new HashMap(this);
  }

  public boolean containsKey(Object key) {
    int i = implFindSlot(key);
    if (i >= 0) {
      ImplMapEntry entry = entries[i];
      if (entry != null && entry.inUse) {
        return true;
      }
    }
    return false;
  }

  public boolean containsValue(Object value) {
    return super.containsValue(value);
  }

  public Set entrySet() {
    return new AbstractSet() {
      public Iterator iterator() {
        return new ImplMapEntryIterator();
      }

      public int size() {
        return fullSlots;
      }
    };
  }

  public Object get(Object key) {
    int i = implFindSlot(key);
    if (i >= 0) {
      ImplMapEntry entry = entries[i];
      if (entry != null && entry.inUse) {
        return entry.value;
      }
    }
    return null;
  }

  public int hashCode() {
    int accum = 0;
    Iterator elements = entrySet().iterator();
    while (elements.hasNext()) {
      accum += elements.next().hashCode();
    }
    return accum;
  }

  public Set keySet() {
    return super.keySet();
  }

  public Object put(Object key, Object value) {
    /*
     * If the number of non-empty slots exceeds the threshold, rehash.
     */
    if ((entries.length - emptySlots) >= threshold) {
      implRehash();
    }
    return implPutNoRehash(key, value);
  }

  public void putAll(Map m) {
    Set entrySet = m.entrySet();
    for (Iterator iter = entrySet.iterator(); iter.hasNext();) {
      Map.Entry entry = (Map.Entry) iter.next();
      put(entry.getKey(), entry.getValue());
    }
  }

  public Object remove(Object key) {
    int i = implFindSlot(key);
    if (i >= 0) {
      ImplMapEntry entry = entries[i];
      if (entry != null && entry.inUse) {
        entry.inUse = false;
        --fullSlots;
        return entry.getValue();
      }
    }
    return null;
  }

  public int size() {
    return fullSlots;
  }

  /**
   * Finds the i slot with the matching key or the first empty slot. This method
   * intentionally ignores whether or not an entry is marked "in use" because
   * the table implements "lazy deletion", meaning that every object keeps its
   * intrinsic location, whether or not it is considered still in the table or
   * not.
   * 
   * @return the i of the slot in which either the entry having the key was
   *         found or in which the entry would go; -1 if the hash table is
   *         totally full
   */
  private int implFindSlot(Object key) {
    int hashCode = (key != null ? key.hashCode() : 7919);
    hashCode = (hashCode < 0 ? -hashCode : hashCode);
    int capacity = entries.length;
    int startIndex = (hashCode % capacity);
    int slotIndex = startIndex;
    int stopIndex = capacity;
    for (int i = 0; i < 2; ++i) {
      for (; slotIndex < stopIndex; ++slotIndex) {
        Map.Entry entry = entries[slotIndex];
        if (entry == null) {
          return slotIndex;
        }

        Object testKey = entry.getKey();
        if (key == null ? testKey == null : key.equals(testKey)) {
          return slotIndex;
        }
      }

      // Wrap around
      //
      slotIndex = 0;
      stopIndex = startIndex;
    }
    // The hash table is totally full and the matching item was not found.
    //
    return -1;
  }

  /**
   * Implements 'put' with the assumption that there will definitely be room for
   * the new item.
   */
  private Object implPutNoRehash(Object key, Object value) {
    // No need to check for (i == -1) because rehash would've made the array big
    // enough to find a slot
    int i = implFindSlot(key);
    if (entries[i] != null) {
      // Just updating the value that was already there.
      // Remember that the existing entry might have been deleted.
      //
      ImplMapEntry entry = entries[i];

      Object old = null;
      if (entry.inUse) {
        old = entry.value;
      } else {
        ++fullSlots;
      }

      entry.value = value;
      entry.inUse = true;
      return old;
    } else {
      // This a brand new (key, value) pair.
      //
      ++fullSlots;
      --emptySlots;
      ImplMapEntry entry = new ImplMapEntry();
      entry.key = key;
      entry.value = value;
      entry.inUse = true;
      entries[i] = entry;
      return null;
    }
  }

  /**
   * Implements a rehash. The underlying array store may or may not be doubled.
   */
  private void implRehash() {
    // Save the old entry array.
    //
    ImplMapEntry[] oldEntries = entries;

    /*
     * Allocate a new entry array. If the actual number or full slots exceeds
     * the load factor, double the array size. Otherwise just rehash, clearing
     * out all the empty slots.
     */
    int capacity = oldEntries.length;
    if (fullSlots > threshold) {
      capacity *= 2;
    }

    realloc(capacity);

    // Now put all the in-use entries from the old array into the new array.
    //
    for (int i = 0, n = oldEntries.length; i < n; ++i) {
      ImplMapEntry oldEntry = oldEntries[i];
      if (oldEntry != null && oldEntry.inUse) {
        int slot = implFindSlot(oldEntry.key);
        // the array should be big enough to find a slot no matter what
        assert (slot >= 0);
        entries[slot] = oldEntry;
      }
    }
  }

  /**
   * Set entries to a new array of the given capacity. Automatically recomputes
   * threshold and emptySlots.
   * 
   * @param capacity
   */
  private void realloc(int capacity) {
    threshold = (int) (capacity * loadFactor);
    emptySlots = capacity - fullSlots;
    entries = new ImplMapEntry[capacity];
  }

}
