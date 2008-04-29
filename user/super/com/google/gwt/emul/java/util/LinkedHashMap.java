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
   * Note that we duplicate the key from the underlying hashmap so we can find
   * the eldest entry. The alternative would have been to modify HashMap so more
   * of the code was directly usable here, but this would have added some
   * overhead to HashMap, or to reimplement most of the HashMap code here with
   * small modifications. Paying a small storage cost only if you use
   * LinkedHashMap and minimizing code size seemed like a better tradeoff
   */
  private class ChainEntry extends MapEntryImpl<K, V> implements Serializable {
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
     * Add this node after the specified node in the chain.
     * 
     * @param node the node to insert after
     */
    public void addAfter(ChainEntry node) {
      update(node, node.next);
    }

    /**
     * Add this node before the specified node in the chain.
     * 
     * @param node the node to insert before
     */
    public void addBefore(ChainEntry node) {
      update(node.prev, node);
    }

    /**
     * Remove this node from any list it may be a part of.
     */
    public void remove() {
      next.prev = prev;
      prev.next = next;
      next = null;
      prev = null;
    }

    private void update(ChainEntry prev, ChainEntry next) {
      // This entry is not in the list.
      assert (this.next == null) && (this.prev == null);

      // Will not break the chain.
      assert (prev != null && next != null);

      // Update me.
      this.prev = prev;
      this.next = next;
      next.prev = this;
      prev.next = this;
    }
  }

  private final class EntrySet extends AbstractSet<Map.Entry<K, V>> {

    private final class EntryIterator implements Iterator<Entry<K, V>> {
      // Current entry.
      private ChainEntry current;

      // Next entry. Used to allow removal of the current node.
      private ChainEntry next;

      public EntryIterator() {
        current = head;
        next = head.next;
      }

      public boolean hasNext() {
        return next != head;
      }

      public java.util.Map.Entry<K, V> next() {
        if (next == null) {
          throw new NoSuchElementException();
        }
        current = next;
        next = next.next;
        return current;
      }

      public void remove() {
        if (current == null || current == head) {
          throw new IllegalStateException("No current entry");
        }
        current.remove();
        map.remove(current.getKey());
        current = null;
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
   * list.
   * 
   * The most recently inserted/accessed node is at the end of the chain, ie.
   * chain.prev.
   */
  private transient ChainEntry head;

  /*
   * The hashmap that keeps track of our entries and the chain. Note that we
   * duplicate the key here to eliminate changes to HashMap and minimize the
   * code here, at the expense of additional space.
   */
  private transient HashMap<K, ChainEntry> map;

  {
    // Head's value should never be accessed.
    head = new ChainEntry();
    head.prev = head;
    head.next = head;
  }

  public LinkedHashMap() {
    this(11, 0.75f, false);
  }

  public LinkedHashMap(int ignored) {
    this(ignored, 0.75f, false);
  }

  public LinkedHashMap(int ignored, float alsoIgnored) {
    this(ignored, alsoIgnored, false);
  }

  public LinkedHashMap(int ignored, float alsoIgnored, boolean accessOrder) {
    super();
    this.accessOrder = accessOrder;
    this.map = new HashMap<K, ChainEntry>(ignored, alsoIgnored);
  }

  public LinkedHashMap(Map<? extends K, ? extends V> toBeCopied) {
    this(toBeCopied.size());
    putAll(toBeCopied);
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
      if (accessOrder) {
        // Move to the tail of the chain on access if requested.
        entry.remove();
        entry.addBefore(head);
      }
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
      newEntry.addBefore(head);
      ChainEntry eldest = head.next;
      if (removeEldestEntry(eldest)) {
        eldest.remove();
        map.remove(eldest.getKey());
      }
      return null;
    } else {
      V oldValue = old.getValue();
      old.setValue(value);
      // If orders by access, move to end of execution block.
      if (accessOrder) {
        old.remove();
        old.addBefore(head);
      }
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

  protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
    return false;
  }
}
