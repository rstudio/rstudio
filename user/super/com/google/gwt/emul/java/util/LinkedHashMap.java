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

/**
 * Hash table implementation of the Map interface with predictable iteration
 * order. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/LinkedHashMap.html">[Sun
 * docs]</a>
 * 
 * @param <K> key type.
 * @param <V> value type.
 */
public class LinkedHashMap<K, V> extends HashMap<K, V> implements Map<K, V> {

  /**
   * The entry we use includes next/prev pointers for a doubly-linked circular
   * list with a head node. This reduces the special cases we have to deal with
   * in the list operations.
   * 
   * Note that we duplicate the key from the underlying hash map so we can find
   * the eldest entry. The alternative would have been to modify HashMap so more
   * of the code was directly usable here, but this would have added some
   * overhead to HashMap, or to reimplement most of the HashMap code here with
   * small modifications. Paying a small storage cost only if you use
   * LinkedHashMap and minimizing code size seemed like a better tradeoff
   */
  private class ChainEntry extends MapEntryImpl<K, V> {
    private transient ChainEntry next;
    private transient ChainEntry prev;

    public ChainEntry() {
      this(null, null);
    }

    public ChainEntry(K key, V value) {
      super(key, value);
      next = prev = null;
    }

    /**
     * Add this node to the end of the chain.
     */
    public void addToEnd() {
      ChainEntry tail = head.prev;

      // Chain is valid.
      assert (head != null && tail != null);

      // This entry is not in the list.
      assert (next == null) && (prev == null);

      // Update me.
      prev = tail;
      next = head;
      tail.next = head.prev = this;
    }

    /**
     * Remove this node from any list it may be a part of.
     */
    public void remove() {
      next.prev = prev;
      prev.next = next;
      next = prev = null;
    }
  }

  private final class EntrySet extends AbstractSet<Map.Entry<K, V>> {

    private final class EntryIterator implements Iterator<Entry<K, V>> {
      // The last entry that was returned from this iterator.
      private ChainEntry last;

      // The next entry to return from this iterator.
      private ChainEntry next;

      public EntryIterator() {
        next = head.next;
      }

      public boolean hasNext() {
        return next != head;
      }

      public Map.Entry<K, V> next() {
        if (next == head) {
          throw new NoSuchElementException();
        }
        last = next;
        next = next.next;
        return last;
      }

      public void remove() {
        if (last == null) {
          throw new IllegalStateException("No current entry");
        }
        last.remove();
        map.remove(last.getKey());
        last = null;
      }
    }

    @Override
    public void clear() {
      LinkedHashMap.this.clear();
    }

    @Override
    public boolean contains(Object o) {
      if (!(o instanceof Map.Entry)) {
        return false;
      }
      Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
      Object key = entry.getKey();
      if (LinkedHashMap.this.containsKey(key)) {
        Object value = LinkedHashMap.this.get(key);
        return Utility.equalsWithNullCheck(entry.getValue(), value);
      }
      return false;
    }

    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
      return new EntryIterator();
    }

    @Override
    public int size() {
      return map.size();
    }
  }

  // True if we should use the access order (ie, for LRU caches) instead of
  // insertion order.
  private transient boolean accessOrder;

  /*
   * The head of the LRU/insert order chain, which is a doubly-linked circular
   * list. The key and value of head should never be read.
   * 
   * The most recently inserted/accessed node is at the end of the chain, ie.
   * chain.prev.
   */
  private final transient ChainEntry head = new ChainEntry();

  /*
   * The hashmap that keeps track of our entries and the chain. Note that we
   * duplicate the key here to eliminate changes to HashMap and minimize the
   * code here, at the expense of additional space.
   */
  private final transient HashMap<K, ChainEntry> map = new HashMap<K, ChainEntry>();

  {
    // Initialize the empty linked list.
    head.prev = head;
    head.next = head;
  }

  public LinkedHashMap() {
  }

  public LinkedHashMap(int ignored) {
    super(ignored);
  }

  public LinkedHashMap(int ignored, float alsoIgnored) {
    super(ignored, alsoIgnored);
  }

  public LinkedHashMap(int ignored, float alsoIgnored, boolean accessOrder) {
    super(ignored, alsoIgnored);
    this.accessOrder = accessOrder;
  }

  public LinkedHashMap(Map<? extends K, ? extends V> toBeCopied) {
    this.putAll(toBeCopied);
  }

  @Override
  public void clear() {
    map.clear();
    head.prev = head;
    head.next = head;
  }

  @Override
  public boolean containsKey(Object key) {
    return map.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    ChainEntry node = head.next;
    while (node != head) {
      if (Utility.equalsWithNullCheck(node.getValue(), value)) {
        return true;
      }
      node = node.next;
    }
    return false;
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    return new EntrySet();
  }

  @Override
  public V get(Object key) {
    ChainEntry entry = map.get(key);
    if (entry != null) {
      recordAccess(entry);
      return entry.getValue();
    }
    return null;
  }

  @Override
  public V put(K key, V value) {
    ChainEntry old = map.get(key);
    if (old == null) {
      ChainEntry newEntry = new ChainEntry(key, value);
      map.put(key, newEntry);
      newEntry.addToEnd();
      ChainEntry eldest = head.next;
      if (removeEldestEntry(eldest)) {
        eldest.remove();
        map.remove(eldest.getKey());
      }
      return null;
    } else {
      V oldValue = old.getValue();
      old.setValue(value);
      recordAccess(old);
      return oldValue;
    }
  }

  @Override
  public V remove(Object key) {
    ChainEntry entry = map.remove(key);
    if (entry != null) {
      entry.remove();
      return entry.getValue();
    }
    return null;
  }

  @Override
  public int size() {
    return map.size();
  }

  @SuppressWarnings("unused")
  protected boolean removeEldestEntry(Entry<K, V> eldest) {
    return false;
  }

  private void recordAccess(ChainEntry entry) {
    if (accessOrder) {
      // Move to the tail of the chain on access.
      entry.remove();
      entry.addToEnd();
    }
  }
}
