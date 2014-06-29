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

import com.google.gwt.core.client.JavaScriptObject;

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

  private final JavaScriptObject backingMap = JavaScriptObject.createArray();
  private int size;
  AbstractHashMap<K, V> host;

  public int size() {
    return size;
  }

  public V put(K key, V value) {
    return put(key, value, hash(key));
  }

  private native V put(K key, V value, int hashCode) /*-{
    var array = this.@InternalJsHashCodeMap::backingMap[hashCode];
    if (array) {
      for (var i = 0, c = array.length; i < c; ++i) {
        var entry = array[i];
        var entryKey = entry.@Map.Entry::getKey()();
        if (this.@InternalJsHashCodeMap::equalsBridge(*)(key, entryKey)) {
          // Found an exact match, just update the existing entry
          return entry.@Map.Entry::setValue(*)(value);
        }
      }
    } else {
      array = this.@InternalJsHashCodeMap::backingMap[hashCode] = [];
    }
    var entry = @AbstractMap.SimpleEntry::new(Ljava/lang/Object;Ljava/lang/Object;)(key, value);
    array.push(entry);
    this.@InternalJsHashCodeMap::size++;
    return null;
  }-*/;

  public V remove(Object key) {
    return remove(key, hash(key));
  }

  private native V remove(Object key, int hashCode) /*-{
    var array = this.@InternalJsHashCodeMap::backingMap[hashCode];
    if (array) {
      for (var i = 0, c = array.length; i < c; ++i) {
        var entry = array[i];
        var entryKey = entry.@Map.Entry::getKey()();
        if (this.@InternalJsHashCodeMap::equalsBridge(*)(key, entryKey)) {
          if (array.length == 1) {
            // remove the whole array
            delete this.@InternalJsHashCodeMap::backingMap[hashCode];
          } else {
            // splice out the entry we're removing
            array.splice(i, 1);
          }
          this.@InternalJsHashCodeMap::size--;
          return entry.@Map.Entry::getValue()();
        }
      }
    }
    return null;
  }-*/;

  public Map.Entry<K, V> getEntry(Object key) {
    return getEntry(key, hash(key));
  }

  private native Map.Entry<K, V> getEntry(Object key, int hashCode) /*-{
    var array = this.@InternalJsHashCodeMap::backingMap[hashCode];
    if (array) {
      for (var i = 0, c = array.length; i < c; ++i) {
        var entry = array[i];
        var entryKey = entry.@Map.Entry::getKey()();
        if (this.@InternalJsHashCodeMap::equalsBridge(*)(key, entryKey)) {
          return entry;
        }
      }
    }
    return null;
  }-*/;

  public native boolean containsValue(Object value) /*-{
    var map = this.@InternalJsHashCodeMap::backingMap;
    for (var hashCode in map) {
      // sanity check that it's really one of ours
      var hashCodeInt = parseInt(hashCode, 10);
      if (hashCode == hashCodeInt) {
        var array = map[hashCodeInt];
        for ( var i = 0, c = array.length; i < c; ++i) {
          var entry = array[i];
          var entryValue = entry.@Map.Entry::getValue()();
          if (this.@InternalJsHashCodeMap::equalsBridge(*)(value, entryValue)) {
            return true;
          }
        }
      }
    }
    return false;
  }-*/;

  public native Iterator<Entry<K, V>> entries() /*-{
    var list = this.@InternalJsHashCodeMap::newEntryList()();
    var map = this.@InternalJsHashCodeMap::backingMap;
    for (var hashCode in map) {
      // sanity check that it's really an integer
      var hashCodeInt = parseInt(hashCode, 10);
      if (hashCode == hashCodeInt) {
        var array = map[hashCodeInt];
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
        InternalJsHashCodeMap.this.remove(removed.getKey());
        return removed;
      }
    };
  }

  /**
   * Bridge method from JSNI that keeps us from having to make polymorphic calls in JSNI. By putting
   * the polymorphism in Java code, the compiler can do a better job of optimizing in most cases.
   */
  private boolean equalsBridge(Object value1, Object value2) {
    return host.equals(value1, value2);
  }

  /**
   * Returns hash code of the key as calculated by {@link AbstractMap#getHashCode(Object)} but also
   * handles null keys as well.
   */
  private int hash(Object key) {
    return key == null ? 0 : host.getHashCode(key);
  }
}
