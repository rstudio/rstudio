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

import java.util.Map.Entry;

/**
 * A simple wrapper around JavaScriptObject to provide {@link java.util.Map}-like semantics where
 * the key type is string.
 * <p>
 * Implementation notes:
 * <p>
 * String keys are mapped to their values via a JS associative map. String keys could collide with
 * intrinsic properties (like watch, constructor). To avoid that; {@link InternalJsStringMap}) uses
 * {@code Object.create(null)} so it doesn't inherit any properties. For legacy browsers where
 * {@code Object.create} is not available, {@link InternalJsStringMapLegacy} prepends each key with
 * a ':' while storing.
 */
class InternalJsStringMap<K, V> {

  /**
   * String map implementation for browsers that doesn't support Object.create (IE8, FF3.6).
   */
  static class InternalJsStringMapLegacy<K, V> extends InternalJsStringMap<K, V> {
    @Override
    native JavaScriptObject createMap() /*-{
      return {};
    }-*/;

    @Override
    public V get(String key) {
      return super.get(normalize(key));
    }

    @Override
    public V put(String key, V value) {
      return super.put(normalize(key), value);
    }

    @Override
    public V remove(String key) {
      return super.remove(normalize(key));
    }

    private String normalize(String key) {
      return ':' + key;
    }

    @Override
    public native boolean containsValue(Object value) /*-{
      var map = this.@InternalJsStringMap::backingMap;
      for (var key in map) {
        // only keys that start with a colon ':' count
        if (key.charCodeAt(0) == 58) {
          var entryValue = map[key];
          if (this.@InternalJsStringMap::equalsBridge(*)(value, entryValue)) {
            return true;
          }
        }
      }
      return false;
    }-*/;

    @Override
    public native Iterator<Entry<K, V>> entries() /*-{
      var list = this.@InternalJsStringMapLegacy::newEntryList()();
      for (var key in this.@InternalJsStringMap::backingMap) {
        // only keys that start with a colon ':' count
        if (key.charCodeAt(0) == 58) {
          var entry = this.@InternalJsStringMap::newMapEntry(*)(key.substring(1));
          list.@ArrayList::add(Ljava/lang/Object;)(entry);
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
          InternalJsStringMapLegacy.this.remove((String) removed.getKey());
          return removed;
        }
      };
    }
  }

  /**
   * String map implementation for browsers that includes __proto__ while iterating keys (i.e.
   * FireFox). See the Firefox bug: https://bugzilla.mozilla.org/show_bug.cgi?id=837630.
   */
  static class InternalJsStringMapWithKeysWorkaround<K, V> extends InternalJsStringMap<K, V> {
    private static final String PROTO = "__proto__";

    @Override
    public boolean containsValue(Object value) {
      V protoValue = get(PROTO);
      if (!isUndefined(protoValue) && equalsBridge(value, protoValue)) {
        return true;
      }
      return super.containsValue(value);
    }

    @Override
    protected String[] keys() {
      String[] keys = super.keys();
      if (contains(PROTO)) {
        keys[keys.length] = PROTO; // safe in webmode
      }
      return keys;
    }
  }

  private final JavaScriptObject backingMap = createMap();
  AbstractHashMap<K,V> host;

  native JavaScriptObject createMap() /*-{
    return Object.create(null);
  }-*/;

  public final boolean contains(String key) {
    return !isUndefined(get(key));
  }

  public V get(String key) {
    return at(key);
  }

  public V put(String key, V value) {
    V oldValue = at(key);
    if (isUndefined(oldValue)) {
      host.elementAdded();
    }

    set(key, toNullIfUndefined(value));

    return oldValue;
  }

  public V remove(String key) {
    V value = at(key);
    if (!isUndefined(value)) {
      delete(key);
      host.elementRemoved();
    }

    return value;
  }

  private native V at(String key) /*-{
    return this.@InternalJsStringMap::backingMap[key];
  }-*/;

  private native void set(String key, V value) /*-{
    this.@InternalJsStringMap::backingMap[key] = value;
  }-*/;

  private native void delete(String key) /*-{
    delete this.@InternalJsStringMap::backingMap[key];
  }-*/;

  public native boolean containsValue(Object value) /*-{
    var map = this.@InternalJsStringMap::backingMap;
    for (var key in map) {
      if (this.@InternalJsStringMap::equalsBridge(*)(value, map[key])) {
        return true;
      }
    }
    return false;
  }-*/;

  public Iterator<Entry<K, V>> entries() {
    final String[] keys = keys();
    return new Iterator<Map.Entry<K,V>>() {
      int i = 0, last = -1;
      @Override
      public boolean hasNext() {
        return i < keys.length;
      }
      @Override
      public Entry<K, V> next() {
        checkElement(hasNext());

        return newMapEntry(keys[last = i++]);
      }
      @Override
      public void remove() {
        checkState(last != -1);

        InternalJsStringMap.this.remove(keys[last]);
        last = -1;
      }
    };
  }

  protected native String[] keys() /*-{
    return Object.getOwnPropertyNames(this.@InternalJsStringMap::backingMap);
  }-*/;

  protected final Entry<K, V> newMapEntry(final String key) {
    return new AbstractMapEntry<K, V>() {
      @Override
      public K getKey() {
        return (K) key;
      }
      @Override
      public V getValue() {
        return get(key);
      }
      @Override
      public V setValue(V object) {
        return put(key, object);
      }
    };
  }

  /**
   * Bridge method from JSNI that keeps us from having to make polymorphic calls
   * in JSNI. By putting the polymorphism in Java code, the compiler can do a
   * better job of optimizing in most cases.
   */
  protected final boolean equalsBridge(Object value1, Object value2) {
    return host.equals(value1, value2);
  }

  private static <T> T toNullIfUndefined(T value) {
    return isUndefined(value) ? null : value;
  }

  private static native boolean isUndefined(Object value) /*-{
    return value === undefined;
  }-*/;
}
