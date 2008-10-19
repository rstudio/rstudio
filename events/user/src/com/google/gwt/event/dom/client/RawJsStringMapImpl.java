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
 * A raw js string map implementation. public so we can avoid creating multiple
 * versions for our internal code, the API is completely unsafe with two
 * versions of get and put, so please don't use!
 * 
 * @param <ValueType> value type
 */
public class RawJsStringMapImpl<ValueType> {

  private static class KeyMap<ValueType> extends JavaScriptObject {

    public static RawJsStringMapImpl.KeyMap create() {
      return (RawJsStringMapImpl.KeyMap) JavaScriptObject.createObject();
    }

    protected KeyMap() {
    }

    public final native ValueType get(String key) /*-{
      return this[key];
    }-*/;

    public final native ValueType get(int key) /*-{
      return this[key];
    }-*/;

    public final native void put(String key, ValueType value) /*-{
      this[key] = value;
    }-*/;

    public final native void put(int key, ValueType value) /*-{
      this[key] = value;
    }-*/;
  }

  private RawJsStringMapImpl.KeyMap<ValueType> map;
  private HashMap<String, ValueType> javaMap;

  public RawJsStringMapImpl() {
    if (GWT.isScript()) {
      map = KeyMap.create();
    } else {
      javaMap = new HashMap<String, ValueType>();
    }
  }

  // Raw get, only use for values that are known not to conflict with the
  // browser's reserved keywords.
  public final ValueType get(String key) {
    if (GWT.isScript()) {
      return map.get(key);
    } else {
      return javaMap.get(key);
    }
  }

  // int get only use with int get.
  public final ValueType get(int key) {
    if (GWT.isScript()) {
      return map.get(key);
    } else {
      return javaMap.get(key + "");
    }
  }

  // Raw put, only use with int get.
  public final void put(int key, ValueType value) {
    if (GWT.isScript()) {
      map.put(key, value);
    } else {
      javaMap.put(key + "", value);
    }
  }

  // Raw put, only use for values that are known not to conflict with the
  // browser's reserved keywords.
  public final void put(String key, ValueType value) {
    if (GWT.isScript()) {
      map.put(key, value);
    } else {
      javaMap.put(key, value);
    }
  }

  // ONLY use this for values put with safePut.
  public final ValueType safeGet(String key) {
    return get(key + ":");
  }

  // ONLY use this for values that will be accessed with saveGet.
  public final void safePut(String key, ValueType value) {
    put(key + ":", value);
  }
}