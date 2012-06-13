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

import elemental.util.MapFromStringToNumber;

/**
 * A JavaScript native implementation of {@link MapFromStringToNumber}.
 */
public final class JsMapFromStringToNumber extends JavaScriptObject
    implements MapFromStringToNumber {

  /**
   * Create a new empty map instance.
   */
  public static native <T> JsMapFromStringToNumber create() /*-{
    return Object.create(null);
  }-*/;

  protected JsMapFromStringToNumber() {
  }

  public native double get(String key) /*-{
    var p = @elemental.js.util.JsMapFromStringTo::propertyForKey(Ljava/lang/String;)(key);
    return this[p];
  }-*/;

  public boolean hasKey(String key) {
    return JsMapFromStringTo.hasKey(this, key);
  }

  public JsArrayOfString keys() {
    return JsMapFromStringTo.keys(this);
  }

  public native void put(String key, double value) /*-{
    var p = @elemental.js.util.JsMapFromStringTo::propertyForKey(Ljava/lang/String;)(key);
    this[p] = value;
  }-*/;

  public void remove(String key) {
    JsMapFromStringTo.remove(this, key);
  }

  public JsArrayOfNumber values() {
    return JsMapFromStringTo.values(this);
  }
}
