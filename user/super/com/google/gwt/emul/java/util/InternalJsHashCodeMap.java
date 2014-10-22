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

import static com.google.gwt.core.shared.impl.InternalPreconditions.checkElement;
import static com.google.gwt.core.shared.impl.InternalPreconditions.checkState;

import com.google.gwt.core.client.JavaScriptObject;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

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
class InternalJsHashCodeMap<K, V> {

  static class InternalJsHashCodeMapLegacy<K, V> extends InternalJsHashCodeMap<K, V> {

    @Override
    native JavaScriptObject createMap() /*-{
      return {};
    }-*/;

    @Override
    public native boolean containsValue(Object value) /*-{
      var map = this.@InternalJsHashCodeMap::backingMap;
      for (var hashCode in map) {
        // sanity check that it's really an integer
        if (hashCode == parseInt(hashCode, 10)) {
          var array = map[hashCode];
          for ( var i = 0, c = array.length; i < c; ++i) {
            var entry = array[i];
            var entryValue = entry.@Map.Entry::getValue()();
            if (this.@InternalJsHashCodeMapLegacy::equalsBridge(*)(value, entryValue)) {
              return true;
            }
          }
        }
      }
      return false;
    }-*/;

    @Override
    public native Iterator<Entry<K, V>> entries() /*-{
      var list = this.@InternalJsHashCodeMapLegacy::newEntryList()();
      var map = this.@InternalJsHashCodeMap::backingMap;
      for (var hashCode in map) {
        // sanity check that it's really an integer
        if (hashCode == parseInt(hashCode, 10)) {
          var array = map[hashCode];
          for ( var i = 0, c = array.length; i < c; ++i) {
            list.@ArrayList::add(Ljava/lang/Object;)(array[i]);
          }
        }
      }
      return list.@ArrayList::iterator()();
    }-*/;

    /**
     * Returns a custom ArrayList so that we could intercept removal to forward into our map.
     */
    private ArrayList<Entry<K, V>> newEntryList() {
      return new ArrayList<Entry<K, V>>() {
        @Override
        public Entry<K, V> remove(int index) {
          Entry<K, V> removed = super.remove(index);
          InternalJsHashCodeMapLegacy.this.remove(removed.getKey());
          return removed;
        }
      };
    }

    /**
     * Bridge method from JSNI that keeps us from having to make polymorphic calls in JSNI. By
     * putting the polymorphism in Java code, the compiler can do a better job of optimizing in most
     * cases.
     */
    private boolean equalsBridge(Object value1, Object value2) {
      return host.equals(value1, value2);
    }
  }

  private final JavaScriptObject backingMap = createMap();
  AbstractHashMap<K, V> host;

  native JavaScriptObject createMap() /*-{
    return Object.create(null);
  }-*/;

  public V put(K key, V value) {
    Entry<K, V>[] chain = ensureChain(hash(key));
    for (Entry<K, V> entry : chain) {
      if (host.equals(key, entry.getKey())) {
        // Found an exact match, just update the existing entry
        return entry.setValue(value);
      }
    }
    chain[chain.length] = new SimpleEntry<K, V>(key, value);
    host.elementAdded();
    return null;
  }

  public V remove(Object key) {
    String hashCode = hash(key);
    Entry<K, V>[] chain = getChainOrEmpty(hashCode);
    for (int i = 0; i < chain.length; i++) {
      Entry<K, V> entry = chain[i];
      if (host.equals(key, entry.getKey())) {
        if (chain.length == 1) {
          // remove the whole array
          removeChain(hashCode);
        } else {
          // splice out the entry we're removing
          splice(chain, i);
        }
        host.elementRemoved();
        return entry.getValue();
      }
    }
    return null;
  }

  public Map.Entry<K, V> getEntry(Object key) {
    for (Entry<K, V> entry : getChainOrEmpty(hash(key))) {
      if (host.equals(key, entry.getKey())) {
        return entry;
      }
    }
    return null;
  }

  public boolean containsValue(Object value) {
    for (String hashCode : keys()) {
      for (Entry<K, V> entry : getChain(hashCode)) {
        if (host.equals(value, entry.getValue())) {
          return true;
        }
      }
    }
    return false;
  }

  public Iterator<Entry<K, V>> entries() {
    return new Iterator<Map.Entry<K,V>>() {
      final String[] keys = keys();
      int chainIndex = -1, itemIndex = 0;
      Entry<K, V>[] chain = new Entry[0];
      Entry<K, V>[] lastChain = null;
      Entry<K, V> lastEntry = null;

      @Override
      public boolean hasNext() {
        if (itemIndex < chain.length) {
          return true;
        }
        if (chainIndex < keys.length - 1) {
          // Move to the beginning of next chain
          chain = getChain(keys[++chainIndex]);
          itemIndex = 0;
          return true;
        }
        return false;
      }

      @Override
      public Entry<K, V> next() {
        checkElement(hasNext());

        lastChain = chain;
        lastEntry = chain[itemIndex++];
        return lastEntry;
      }

      @Override
      public void remove() {
        checkState(lastEntry != null);

        InternalJsHashCodeMap.this.remove(lastEntry.getKey());

        // If we are sill in the same chain, our itemIndex just jumped an item. We can fix that
        // by decrementing the itemIndex. However there is an exception: if there is only one
        // item, the whole chain is simply dropped not the item. If we decrement in that case, as
        // the item is not drop from the chain, we will end up returning the same item twice.
        if (chain == lastChain && chain.length != 1) {
          itemIndex--;
        }

        lastEntry = null;
      }
    };
  }

  private native String[] keys() /*-{
    return Object.getOwnPropertyNames(this.@InternalJsHashCodeMap::backingMap);
  }-*/;

  private native Entry<K, V>[] getChain(String hashCode) /*-{
    return this.@InternalJsHashCodeMap::backingMap[hashCode];
  }-*/;

  private native Entry<K, V>[] getChainOrEmpty(String hashCode) /*-{
    return this.@InternalJsHashCodeMap::backingMap[hashCode] || [];
  }-*/;

  private native Entry<K, V>[] ensureChain(String hashCode) /*-{
    var map = this.@InternalJsHashCodeMap::backingMap;
    return map[hashCode] || (map[hashCode] = []);
  }-*/;

  private native void removeChain(String hashCode) /*-{
    delete this.@InternalJsHashCodeMap::backingMap[hashCode];
  }-*/;

  /**
   * Returns hash code of the key as calculated by {@link AbstractMap#getHashCode(Object)} but also
   * handles null keys as well.
   */
  private String hash(Object key) {
    return key == null ? "0" : String.valueOf(host.getHashCode(key));
  }

  private static native void splice(Object arr, int index) /*-{
    arr.splice(index, 1);
  }-*/;
}
