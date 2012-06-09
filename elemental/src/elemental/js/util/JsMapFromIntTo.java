/*
 * Copyright 2010 Google Inc.
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
package elemental.js.util;

import com.google.gwt.core.client.JavaScriptObject;

import elemental.util.MapFromIntTo;

/**
 * JavaScript native implementation of {@link MapFromIntTo}.
 */
public final class JsMapFromIntTo<V> extends JavaScriptObject implements MapFromIntTo<V> {
  
  /**
   * Create a new empty map instance.
   */
  public static <T> JsMapFromIntTo<T> create() {
    return JavaScriptObject.createArray().cast();
  }

  protected JsMapFromIntTo() {
  }

  public native V get(int key) /*-{
      return this[key];
  }-*/;

  final static native boolean hasKey(JavaScriptObject map, int key) /*-{
    return map[key] !== undefined;
  }-*/;

  final static native void remove(JavaScriptObject map, int key) /*-{
    delete map[key];
  }-*/;

  final static native JsArrayOfInt keys(JavaScriptObject map) /*-{
    var data = [];
    for (var p in map) {
      var key = parseInt(p);
      if (!isNaN(key)) {
        data.push(key);
      }
    }
    return data;
  }-*/;

  final public boolean hasKey(int key) {
    return hasKey(this, key);
  }

  final public JsArrayOfInt keys() {
    return keys(this);
  }

  public native void put(int key, V value) /*-{
      this[key] = value;
  }-*/;

  final public void remove(int key) {
    remove(this, key);
  }

  final public JsArrayOf<V> values() {
    return JsMapFromStringTo.values(this);
  }
}
