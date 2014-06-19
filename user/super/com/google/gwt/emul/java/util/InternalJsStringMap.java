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
    public native V get(String key) /*-{
      return this.@InternalJsStringMap::backingMap[key];
    }-*/;

    @Override
    public native void set(String key, V value) /*-{
      this.@InternalJsStringMap::backingMap[key] = value;
    }-*/;

    @Override
    public native void remove(String key) /*-{
      delete this.@InternalJsStringMap::backingMap[key];
    }-*/;

    @Override
    public native boolean contains(String key) /*-{
      return key in this.@InternalJsStringMap::backingMap;
    }-*/;

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
    public native void addAllEntries(Collection<?> dest, AbstractHashMap<?, ?> host) /*-{
      for (var key in this.@InternalJsStringMap::backingMap) {
        var entry = host.@AbstractHashMap::newMapEntryString(*)(key);
        dest.@java.util.Collection::add(Ljava/lang/Object;)(entry);
      }
    }-*/;
  }

  private final JavaScriptObject backingMap = createMap();

  native JavaScriptObject createMap() /*-{
    return {};
  }-*/;

  public native V get(String key) /*-{
    return this.@InternalJsStringMap::backingMap[':' + key];
  }-*/;

  public native void set(String key, V value) /*-{
    this.@InternalJsStringMap::backingMap[':' + key] = value;
  }-*/;

  public native void remove(String key) /*-{
    delete this.@InternalJsStringMap::backingMap[':' + key];
  }-*/;

  public native boolean contains(String key) /*-{
    return (':' + key) in this.@InternalJsStringMap::backingMap;
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

  public native void addAllEntries(Collection<?> dest, AbstractHashMap<?, ?> host) /*-{
    for (var key in this.@InternalJsStringMap::backingMap) {
      // only keys that start with a colon ':' count
      if (key.charCodeAt(0) == 58) {
        var entry = host.@AbstractHashMap::newMapEntryString(*)(key.substring(1));
        dest.@Collection::add(Ljava/lang/Object;)(entry);
      }
    }
  }-*/;
}
