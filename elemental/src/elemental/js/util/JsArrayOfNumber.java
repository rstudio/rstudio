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
import com.google.gwt.core.client.JsArrayNumber;

import elemental.util.ArrayOfNumber;
import elemental.util.CanCompareNumber;

/**
 * JavaScript native implementation of {@link ArrayOfNumber}.
 */
public final class JsArrayOfNumber extends JsArrayNumber implements ArrayOfNumber {

  /**
   * Create a new empty instance.
   */
  public static JsArrayOfNumber create() {
    return JavaScriptObject.createArray().cast();
  }

  protected JsArrayOfNumber() {
  }

  public native JsArrayOfNumber concat(ArrayOfNumber values) /*-{
    return this.concat(values);
  }-*/;

  public native void insert(int index, double value) /*-{
    this.splice(index, 0, value);
  }-*/;

  public boolean isEmpty() {
    return JsArrayOf.isEmpty(this);
  }

  public native boolean isSet(int index) /*-{
    return this[index] !== undefined;
  }-*/;

  public native double peek() /*-{
    return this[this.length - 1];
  }-*/;

  public native double pop() /*-{
    return this.pop();
  }-*/;

  public void removeByIndex(int index) {
    splice(index, 1);
  }

  public native void sort() /*-{
    this.sort();
  }-*/;

  public native void sort(CanCompareNumber comparator) /*-{
    this.sort(function(a, b) {
      return comparator.@elemental.util.CanCompareNumber::compare(DD)(a, b);
    });
  }-*/;

  public JsArrayOfNumber splice(int index, int count) {
    return JsArrayOf.splice(this, index, count);
  }
}
