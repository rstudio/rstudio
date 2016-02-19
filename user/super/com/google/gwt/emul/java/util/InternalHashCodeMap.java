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

import static java.util.ConcurrentModificationDetector.structureChanged;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

import javaemul.internal.ArrayHelper;

/**
 * A simple wrapper around JavaScriptObject to provide {@link java.util.Map}-like semantics for any
 * key type.
 * <p>
 * Implementation notes:
 * <p>
 * A key's hashCode is the index in backingMap which should contain that key. Since several keys may
 * have the same hash, each value in hashCodeMap is actually an array containing all entries whose
 * keys share the same hash.
 */
class InternalHashCodeMap<K, V> implements Iterable<Entry<K, V>> {

  private final InternalJsMap<Object> backingMap = InternalJsMapFactory.newJsMap();
  private AbstractHashMap<K, V> host;
  private int size;

  public InternalHashCodeMap(AbstractHashMap<K, V> host) {
    this.host = host;
  }

  public V put(K key, V value) {
    int hashCode = hash(key);
    Entry<K, V>[] chain = getChainOrEmpty(hashCode);

    if (chain.length == 0) {
      // This is a new chain, put it to the map.
      backingMap.set(hashCode, chain);
    } else {
      // Chain already exists, perhaps key also exists.
      Entry<K, V> entry = findEntryInChain(key, chain);
      if (entry != null) {
        return entry.setValue(value);
      }
    }
    chain[chain.length] = new SimpleEntry<K, V>(key, value);
    size++;
    structureChanged(host);
    return null;
  }

  public V remove(Object key) {
    int hashCode = hash(key);
    Entry<K, V>[] chain = getChainOrEmpty(hashCode);
    for (int i = 0; i < chain.length; i++) {
      Entry<K, V> entry = chain[i];
      if (host.equals(key, entry.getKey())) {
        if (chain.length == 1) {
          ArrayHelper.setLength(chain, 0);
          // remove the whole array
          backingMap.delete(hashCode);
        } else {
          // splice out the entry we're removing
          ArrayHelper.removeFrom(chain, i, 1);
        }
        size--;
        structureChanged(host);
        return entry.getValue();
      }
    }
    return null;
  }

  public Map.Entry<K, V> getEntry(Object key) {
    return findEntryInChain(key, getChainOrEmpty(hash(key)));
  }

  private Map.Entry<K, V> findEntryInChain(Object key, Entry<K, V>[] chain) {
    for (Entry<K, V> entry : chain) {
      if (host.equals(key, entry.getKey())) {
        return entry;
      }
    }
    return null;
  }

  public int size() {
    return size;
  }

  @Override
  public Iterator<Entry<K, V>> iterator() {
    return new Iterator<Map.Entry<K,V>>() {
      final InternalJsMap.Iterator<Object> chains = backingMap.entries();
      int itemIndex = 0;
      Entry<K, V>[] chain = newEntryChain();
      Entry<K, V> lastEntry = null;

      @Override
      public boolean hasNext() {
        if (itemIndex < chain.length) {
          return true;
        }
        InternalJsMap.IteratorEntry<Object> current = chains.next();
        if (!current.done) {
          // Move to the beginning of next chain
          chain = unsafeCastToArray(current.getValue());
          itemIndex = 0;
          return true;
        }
        return false;
      }

      @Override
      public Entry<K, V> next() {
        lastEntry = chain[itemIndex++];
        return lastEntry;
      }

      @Override
      public void remove() {
        InternalHashCodeMap.this.remove(lastEntry.getKey());
        // Unless we are in a new chain, all items have shifted so our itemIndex should as well...
        if (itemIndex != 0) {
          itemIndex--;
        }
      }
    };
  }

  private Entry<K, V>[] getChainOrEmpty(int hashCode) {
    Entry<K, V>[] chain = unsafeCastToArray(backingMap.get(hashCode));
    return chain == null ? newEntryChain() : chain;
  }

  private native Entry<K, V>[] newEntryChain() /*-{
    return [];
  }-*/;

  private native Entry<K, V>[] unsafeCastToArray(Object arr) /*-{
    return arr;
  }-*/;

  /**
   * Returns hash code of the key as calculated by {@link AbstractHashMap#getHashCode(Object)} but
   * also handles null keys as well.
   */
  private int hash(Object key) {
    return key == null ? 0 : host.getHashCode(key);
  }
}
