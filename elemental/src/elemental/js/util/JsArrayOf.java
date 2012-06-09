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

import elemental.util.ArrayOf;
import elemental.util.CanCompare;

/**
 * JavaScript native implementation of {@link ArrayOf}.
 */
public final class JsArrayOf<T> extends JavaScriptObject implements ArrayOf<T> {
  
  /**
   * Create a new empty Array instance.
   */
  public static <T> JsArrayOf<T> create() {
    return JavaScriptObject.createArray().cast();
  }
  
  static native boolean isEmpty(JavaScriptObject array) /*-{
    return !array.length;
  }-*/;

  static native <T extends JavaScriptObject> T splice(JavaScriptObject array, int index, int count) /*-{
    return array.splice(index, count);
  }-*/;

  protected JsArrayOf() {
  }
  
  public native JsArrayOf<T> concat(ArrayOf<T> values) /*-{
    return this.concat(values);
  }-*/;
  
  public boolean contains(T value) {
    return indexOf(value) != -1;
  }

  public native T get(int index) /*-{
      return this[index];
  }-*/;

  public native int indexOf(T value) /*-{
    return this.indexOf(value);
  }-*/;

  public native void insert(int index, T value) /*-{
    this.splice(index, 0, value);
  }-*/;

  public boolean isEmpty() {
    return isEmpty(this);
  }

  /**
   * Convert each element of the array to a String and join them with a comma
   * separator. The value returned from this method may vary between browsers
   * based on how JavaScript values are converted into strings.
   */
  public String join() {
    // As per JS spec
    return join(",");
  }

  public native String join(String separator) /*-{
    return this.join(separator);
  }-*/;

  public native int length() /*-{
      return this.length;
  }-*/;

  public native T peek() /*-{
    return this[this.length - 1];
  }-*/;

  public native T pop() /*-{
    return this.pop();
  }-*/;
  
  /**
   * Pushes the given value onto the end of the array.
   */
  public native void push(T value) /*-{
    this[this.length] = value;
  }-*/;
  
  public void remove(T value) {
    final int index = indexOf(value);
    if (index != -1) {
      splice(index, 1);
    }
  }
  
  public void removeByIndex(int index) {
    splice(index, 1);
  }

  public native void set(int index, T value) /*-{
      this[index] = value;
  }-*/;

  public native void setLength(int newLength) /*-{
    this.length = newLength;
  }-*/;

  public native T shift() /*-{
    return this.shift();
  }-*/;

  public native void sort(CanCompare<T> comparator) /*-{
    this.sort(function(a, b) {
      return comparator.@elemental.util.CanCompare::compare(Ljava/lang/Object;Ljava/lang/Object;)(a, b);
    });
  }-*/;

  public JsArrayOf<T> splice(int index, int count) {
    return splice(this, index, count);
  }

  public native void unshift(T value) /*-{
    this.unshift(value);
  }-*/;
}
