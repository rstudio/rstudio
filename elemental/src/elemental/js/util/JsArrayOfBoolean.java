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
import com.google.gwt.core.client.JsArrayBoolean;

import elemental.util.ArrayOfBoolean;

/**
 * JavaScript native implementation of {@link ArrayOfBoolean}.
 */
public final class JsArrayOfBoolean extends JsArrayBoolean implements ArrayOfBoolean {

  /**
   * Create a new empty instance.
   */
  public static JsArrayOfBoolean create() {
    return JavaScriptObject.createArray().cast();
  }

  protected JsArrayOfBoolean() {
  }

  public native JsArrayOfBoolean concat(ArrayOfBoolean values) /*-{
    return this.concat(values);
  }-*/;

  public boolean contains(boolean value) {
    return indexOf(value) != -1;
  }

  public native int indexOf(boolean value) /*-{
    return this.indexOf(value);
  }-*/;

  public native void insert(int index, boolean value) /*-{
    this.splice(index, 0, value);
  }-*/;

  public boolean isEmpty() {
    return JsArrayOf.isEmpty(this);
  }

  public native boolean isSet(int index) /*-{
    return this[index] !== undefined;
  }-*/;

  public native boolean peek() /*-{
    return this[this.length - 1];
  }-*/;

  public native boolean pop() /*-{
    return this.pop();
  }-*/;

  public void remove(boolean value) {
    final int index = indexOf(value);
    if (index != -1) {
      splice(index, 1);
    }
  }

  public void removeByIndex(int index) {
    splice(index, 1);
  }

  public JsArrayOfBoolean splice(int index, int count) {
    return JsArrayOf.splice(this, index, count);
  }
}
