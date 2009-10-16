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
package com.google.gwt.core.client;

/**
 * A simple wrapper around a homogeneous native array of
 * {@link JavaScriptObject} values.
 * 
 * This class may not be directly instantiated, and can only be returned from a
 * native method. For example,
 * 
 * <code>
 * native JsArray<JavaScriptObject> getNativeArray() /*-{
 *   return [
 *     { x: 0, y: 1},
 *     { x: 2, y: 3},
 *     { x: 4, y: 5},
 *   ];
 * }-* /;
 * </code>
 * 
 * @param <T> the concrete type of object contained in this array
 */
public class JsArray<T extends JavaScriptObject> extends JavaScriptObject {

  protected JsArray() {
  }

  /**
   * Gets the object at a given index.
   * 
   * @param index the index to be retrieved
   * @return the object at the given index, or <code>null</code> if none exists
   */
  public final native T get(int index) /*-{
    return this[index];
  }-*/;

  /**
   * Convert each element of the array to a String and join them with a comma
   * separator. The value returned from this method may vary between browsers
   * based on how JavaScript values are converted into strings.
   */
  public final String join() {
    // As per JS spec
    return join(",");
  }

  /**
   * Convert each element of the array to a String and join them with a comma
   * separator. The value returned from this method may vary between browsers
   * based on how JavaScript values are converted into strings.
   */
  public final native String join(String separator) /*-{
    return this.join(separator);
  }-*/;

  /**
   * Gets the length of the array.
   * 
   * @return the array length
   */
  public final native int length() /*-{
    return this.length;
  }-*/;

  /**
   * Pushes the given value onto the end of the array.
   */
  public final native void push(T value) /*-{
    this[this.length] = value;
  }-*/;

  /**
   * Sets the object value at a given index.
   * 
   * If the index is out of bounds, the value will still be set. The array's
   * length will be updated to encompass the bounds implied by the added object.
   * 
   * @param index the index to be set
   * @param value the object to be stored
   */
  public final native void set(int index, T value) /*-{
    this[index] = value;
  }-*/;

  /**
   * Reset the length of the array.
   * 
   * @param newLength the new length of the array
   */
  public final native void setLength(int newLength) /*-{
    this.length = newLength;
  }-*/;

  /**
   * Shifts the first value off the array.
   * 
   * @return the shifted value
   */
  public final native T shift() /*-{
    return this.shift();
  }-*/;

  /**
   * Shifts a value onto the beginning of the array.
   * 
   * @param value the value to the stored
   */
  public final native void unshift(T value) /*-{
    this.unshift(value);
  }-*/;
}
