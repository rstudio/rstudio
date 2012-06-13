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

import elemental.util.MapFromStringTo;

/**
 * JavaScript native implementation of {@link MapFromStringTo}.
 */
public final class JsMapFromStringTo<V> extends JavaScriptObject implements MapFromStringTo<V> {

  /**
   * Create a new empty map instance.
   */
  public static native <T> JsMapFromStringTo<T> create() /*-{
    return Object.create(null);
  }-*/;

  static native boolean hasKey(JavaScriptObject map, String key) /*-{
    var p = @elemental.js.util.JsMapFromStringTo::propertyForKey(Ljava/lang/String;)(key);
    return map[p] !== undefined;
  }-*/;

  static native <T extends JavaScriptObject> T keys(JavaScriptObject object) /*-{
    var data = [];
    for (var item in object) {
      if (object.hasOwnProperty(item)) {
        var key = @elemental.js.util.JsMapFromStringTo::keyForProperty(Ljava/lang/String;)(item);
        data.push(key);
      }
    }
    return data;
  }-*/;

  static native void remove(JavaScriptObject map, String key) /*-{
    var p = @elemental.js.util.JsMapFromStringTo::propertyForKey(Ljava/lang/String;)(key);
    delete map[p];
  }-*/;

  static native <T extends JavaScriptObject> T values(JavaScriptObject object) /*-{
    var data = [];
    for (var item in object) {
      if (object.hasOwnProperty(item)) {
        data.push(object[item]);
      }
    }
    return data;
  }-*/;

  @SuppressWarnings("unused") // Called from JSNI.
  private static String keyForProperty(String property) {
    return property;
  }

  @SuppressWarnings("unused") // Called from JSNI.
  private static String propertyForKey(String key) {
    assert key != null : "native maps do not allow null key values.";
    return key;
  }

  protected JsMapFromStringTo() {
  }

  public native V get(String key) /*-{
    var p = @elemental.js.util.JsMapFromStringTo::propertyForKey(Ljava/lang/String;)(key);
    return this[p];
  }-*/;

  public boolean hasKey(String key) {
    return hasKey(this, key);
  }

  public JsArrayOfString keys() {
    return keys(this);
  }

  public native void put(String key, V value) /*-{
    var p = @elemental.js.util.JsMapFromStringTo::propertyForKey(Ljava/lang/String;)(key);
    this[p] = value;
  }-*/;

  public void remove(String key) {
    remove(this, key);
  }

  public JsArrayOf<V> values() {
    return values(this);
  }
}
