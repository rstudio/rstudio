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
package com.google.gwt.event.dom.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

import java.util.HashMap;

/**
 * Lightweight map implementation. Package protected due to non-final API.
 * 
 * @param <V> value type
 */
class PrivateMap<V> {

  /**
   * Js version of our map.
   * 
   * @param <V> value type
   */
  private static class JsMap<V> extends JavaScriptObject {

    public static PrivateMap.JsMap<?> create() {
      return JavaScriptObject.createObject().cast();
    }

    protected JsMap() {
    }

    public final native void put(int key, V value) /*-{
      this[key] = value;
    }-*/;

    public final native V unsafeGet(int key) /*-{
      return this[key];
    }-*/;

    public final native V unsafeGet(String key) /*-{
      return this[key];
    }-*/;

    public final native void unsafePut(String key, V value) /*-{
      this[key] = value;
    }-*/;
  }

  private PrivateMap.JsMap<V> map;
  private HashMap<String, V> javaMap;

  public PrivateMap() {
    if (GWT.isScript()) {
      map = JsMap.create().cast();
    } else {
      javaMap = new HashMap<String, V>();
    }
  }

  public final V get(int key) {
    if (GWT.isScript()) {
      return map.unsafeGet(key);
    } else {
      return javaMap.get(key + "");
    }
  }

  public final void put(int key, V value) {
    if (GWT.isScript()) {
      map.put(key, value);
    } else {
      javaMap.put(key + "", value);
    }
  }

  // ONLY use this for values put with safePut.
  public final V safeGet(String key) {
    return unsafeGet(":" + key);
  }

  // ONLY use this for values that will be accessed with safeGet.
  public final void safePut(String key, V value) {
    unsafePut(":" + key, value);
  }

  // ONLY use this for values put with unsafePut.
  public final V unsafeGet(String key) {
    if (GWT.isScript()) {
      return map.unsafeGet(key);
    } else {
      return javaMap.get(key);
    }
  }

  // ONLY use this for values that will be accessed with unsafeGet.
  public final void unsafePut(String key, V value) {
    if (GWT.isScript()) {
      map.unsafePut(key, value);
    } else {
      javaMap.put(key, value);
    }
  }
}
