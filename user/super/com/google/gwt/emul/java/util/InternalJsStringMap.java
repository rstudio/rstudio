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
 * A simple wrapper around JavaScriptObject to provide {@link java.util.Map}-like semantics where
 * the key type is string.
 * <p>
 * Implementation notes:
 * <p>
 * String keys are mapped to their values via a JS associative map. String keys could collide with
 * intrinsic properties (like watch, constructor). To avoid that; {@link InternalJsStringMap})
 * prepends each key with a ':' while storing and {@link InternalJsStringMapModern} uses
 * {@code Object.create(null)} in the first place to avoid inheriting any properties (only available
 * in modern browsers).
 */
class InternalJsStringMap<V> {

  static class InternalJsStringMapModern<V> extends InternalJsStringMap<V> {
    @Override
    native JavaScriptObject createMap() /*-{
      return Object.create(null);
    }-*/;

    @Override
    String normalize(String key) {
      return key;
    }

    @Override
    public native boolean containsValue(Object value, AbstractHashMap<?, ?> host) /*-{
      var map = this.@InternalJsStringMap::backingMap;
      for (var key in map) {
        if (host.@AbstractHashMap::equalsBridge(*)(value, map[key])) {
          return true;
        }
      }
      return false;
    }-*/;

    @Override
    public native void addAllEntries(Collection<?> dest) /*-{
      for (var key in this.@InternalJsStringMap::backingMap) {
        var entry = this.@InternalJsStringMap::newMapEntry(*)(key);
        dest.@Collection::add(Ljava/lang/Object;)(entry);
      }
    }-*/;
  }

  private final JavaScriptObject backingMap = createMap();
  private int size;

  native JavaScriptObject createMap() /*-{
    return {};
  }-*/;

  String normalize(String key) {
    return ':' + key;
  }

  public final int size() {
    return size;
  }

  public final boolean contains(String key) {
    return !isUndefined(get(key));
  }

  public final V get(String key) {
    return at(normalize(key));
  }

  public final V put(String key, V value) {
    key = normalize(key);

    V oldValue = at(key);
    if (isUndefined(oldValue)) {
      size++;
    }

    set(key, toNullIfUndefined(value));

    return oldValue;
  }

  public final V remove(String key) {
    key = normalize(key);

    V value = at(key);
    if (!isUndefined(value)) {
      delete(key);
      size--;
    }

    return value;
  }

  private native V at(String key) /*-{
    return this.@InternalJsStringMap::backingMap[key];
  }-*/;

  private native void set(String key, V value) /*-{
    return this.@InternalJsStringMap::backingMap[key] = value;
  }-*/;

  private native void delete(String key) /*-{
    delete this.@InternalJsStringMap::backingMap[key];
  }-*/;

  public native boolean containsValue(Object value, AbstractHashMap<?, ?> host) /*-{
    var map = this.@InternalJsStringMap::backingMap;
    for (var key in map) {
      // only keys that start with a colon ':' count
      if (key.charCodeAt(0) == 58) {
        var entryValue = map[key];
        if (host.@AbstractHashMap::equalsBridge(*)(value, entryValue)) {
          return true;
        }
      }
    }
    return false;
  }-*/;

  public native void addAllEntries(Collection<?> dest) /*-{
    for (var key in this.@InternalJsStringMap::backingMap) {
      // only keys that start with a colon ':' count
      if (key.charCodeAt(0) == 58) {
        var entry = this.@InternalJsStringMap::newMapEntry(*)(key.substring(1));
        dest.@Collection::add(Ljava/lang/Object;)(entry);
      }
    }
  }-*/;

  private AbstractMapEntry<String, V> newMapEntry(final String key) {
    return new AbstractMapEntry<String, V>() {
      @Override
      public String getKey() {
        return key;
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

  private static <T> T toNullIfUndefined(T value) {
    return isUndefined(value) ? null : value;
  }

  private static native boolean isUndefined(Object value) /*-{
    return value === undefined;
  }-*/;
}
