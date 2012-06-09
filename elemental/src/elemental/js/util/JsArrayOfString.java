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
import com.google.gwt.core.client.JsArrayString;

import elemental.util.ArrayOfString;
import elemental.util.CanCompareString;

/**
 * JavaScript native implementation of {@link ArrayOfString}.
 */
public final class JsArrayOfString extends JsArrayString implements ArrayOfString {
  
  /**
   * Create a new empty instance.
   */
  public static JsArrayOfString create() {
    return JavaScriptObject.createArray().cast();
  }

  protected JsArrayOfString() {
  }

  public native JsArrayOfString concat(ArrayOfString values) /*-{
    return this.concat(values);
  }-*/;

  public boolean contains(String value) {
    return indexOf(value) != -1;
  }

  public native int indexOf(String value) /*-{
    return this.indexOf(value);
  }-*/;

  public native void insert(int index, String value) /*-{
    this.splice(index, 0, value);
  }-*/;

  public boolean isEmpty() {
    return JsArrayOf.isEmpty(this);
  }

  public native String peek() /*-{
    return this[this.length - 1];
  }-*/;

  public native String pop() /*-{
    return this.pop();
  }-*/;

  public void remove(String value) {
    final int index = indexOf(value);
    if (index != -1) {
      splice(index, 1);
    }
  }

  public void removeByIndex(int index) {
    splice(index, 1);
  }

  public native void sort() /*-{
    this.sort();
  }-*/;

  public native void sort(CanCompareString comparator) /*-{
    this.sort(function(a, b) {
      return comparator.@elemental.util.CanCompareString::compare(Ljava/lang/String;Ljava/lang/String;)(a, b);
    });
  }-*/;

  public JsArrayOfString splice(int index, int count) {
    return JsArrayOf.splice(this, index, count);
  }
}
