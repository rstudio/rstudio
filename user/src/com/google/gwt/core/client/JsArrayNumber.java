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
 * A simple wrapper around a homogeneous native array of numeric values.
 *
 * All native JavaScript numeric values are implicitly double-precision, so only
 * double values may be set and retrieved.
 *
 * This class may not be directly instantiated, and can only be returned from a
 * native method. For example,
 *
 * <code>
 * native JsArrayNumber getNativeArray() /*-{
 *   return [1.1, 2.2, 3.3];
 * }-* /;
 * </code>
 */
public class JsArrayNumber extends JavaScriptObject {
  
  protected JsArrayNumber() {
  }

  /**
   * Gets the value at a given index.
   *
   * If an undefined or non-numeric value exists at the given index, a
   * type-conversion error will occur in hosted mode and unpredictable behavior
   * may occur in web mode.
   *
   * @param index the index to be retrieved
   * @return the value at the given index
   */
  public final native double get(int index) /*-{
    return this[index];
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
   * Pushes the given number onto the end of the array.
   */
  public final native void push(double value) /*-{
    this[this.length] = value;
  }-*/;

  /**
   * Sets the value value at a given index.
   *
   * If the index is out of bounds, the value will still be set. The array's
   * length will be updated to encompass the bounds implied by the added value.
   *
   * @param index the index to be set
   * @param value the value to be stored
   */
  public final native void set(int index, double value) /*-{
    this[index] = value;
  }-*/;

  /**
   * Shifts the first value off the array.
   * @return the shifted value
   */
  public final native double shift() /*-{
    return this.shift();
  }-*/;
}
