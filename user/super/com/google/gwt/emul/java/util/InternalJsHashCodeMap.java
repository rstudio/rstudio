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
class InternalJsHashcodeMap<K, V> {

  private final JavaScriptObject backingMap = JavaScriptObject.createArray();

  public native V put(K key, V value, int hashCode, AbstractHashMap<?, ?> host) /*-{
    var array = this.@InternalJsHashcodeMap::backingMap[hashCode];
    if (array) {
      for (var i = 0, c = array.length; i < c; ++i) {
        var entry = array[i];
        var entryKey = entry.@Map.Entry::getKey()();
        if (host.@AbstractHashMap::equalsBridge(*)(key, entryKey)) {
          // Found an exact match, just update the existing entry
          return entry.@Map.Entry::setValue(*)(value);
        }
      }
    } else {
      array = this.@InternalJsHashcodeMap::backingMap[hashCode] = [];
    }
    var entry = @AbstractMap.SimpleEntry::new(Ljava/lang/Object;Ljava/lang/Object;)(key, value);
    array.push(entry);
    host.@AbstractHashMap::size++;
    return null;
  }-*/;

  public native V remove(Object key, int hashCode, AbstractHashMap<?, ?> host) /*-{
    var array = this.@InternalJsHashcodeMap::backingMap[hashCode];
    if (array) {
      for (var i = 0, c = array.length; i < c; ++i) {
        var entry = array[i];
        var entryKey = entry.@Map.Entry::getKey()();
        if (host.@AbstractHashMap::equalsBridge(*)(key, entryKey)) {
          if (array.length == 1) {
            // remove the whole array
            delete this.@InternalJsHashcodeMap::backingMap[hashCode];
          } else {
            // splice out the entry we're removing
            array.splice(i, 1);
          }
          host.@AbstractHashMap::size--;
          return entry.@Map.Entry::getValue()();
        }
      }
    }
    return null;
  }-*/;

  public native Map.Entry<K, V> getEntry(Object key, int hashCode, AbstractHashMap<?, ?> host) /*-{
    var array = this.@InternalJsHashcodeMap::backingMap[hashCode];
    if (array) {
      for (var i = 0, c = array.length; i < c; ++i) {
        var entry = array[i];
        var entryKey = entry.@Map.Entry::getKey()();
        if (host.@AbstractHashMap::equalsBridge(*)(key, entryKey)) {
          return entry;
        }
      }
    }
    return null;
  }-*/;

  public native boolean containsValue(Object value, AbstractHashMap<?, ?> host) /*-{
    var map = this.@InternalJsHashcodeMap::backingMap;
    for (var hashCode in map) {
      // sanity check that it's really one of ours
      var hashCodeInt = parseInt(hashCode, 10);
      if (hashCode == hashCodeInt) {
        var array = map[hashCodeInt];
        for ( var i = 0, c = array.length; i < c; ++i) {
          var entry = array[i];
          var entryValue = entry.@Map.Entry::getValue()();
          if (host.@AbstractHashMap::equalsBridge(*)(value, entryValue)) {
            return true;
          }
        }
      }
    }
    return false;
  }-*/;

  public native void addAllEntries(Collection<?> dest) /*-{
    var map = this.@InternalJsHashcodeMap::backingMap;
    for (var hashCode in map) {
      // sanity check that it's really an integer
      var hashCodeInt = parseInt(hashCode, 10);
      if (hashCode == hashCodeInt) {
        var array = map[hashCodeInt];
        for ( var i = 0, c = array.length; i < c; ++i) {
          dest.@Collection::add(*)(array[i]);
        }
      }
    }
  }-*/;
}