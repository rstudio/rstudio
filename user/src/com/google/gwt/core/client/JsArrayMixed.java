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
package com.google.gwt.core.client;

/**
 * A simple wrapper around an heterogeneous native array of values.
 * 
 * This class may not be directly instantiated, and can only be returned from a
 * native method. For example,
 * 
 * <code>
 * native JsArrayMixed getNativeArray() /*-{
 *   return [
 *     { x: 0, y: 1},
 *     "apple",
 *     12345,
 *   ];
 * }-* /;
 * </code>
 */
public class JsArrayMixed extends JavaScriptObject {

  protected JsArrayMixed() {
  }

  /**
   * Gets the boolean at a given index.
   * 
   * @param index the index to be retrieved
   * @return the object at the given index, or <code>null</code> if none exists
   */
  public final native boolean getBoolean(int index) /*-{
    return Boolean(this[index]);
  }-*/;

  /**
   * Gets the double at a given index.
   * 
   * @param index the index to be retrieved
   * @return the object at the given index, or <code>null</code> if none exists
   */
  public final native double getNumber(int index) /*-{
    return Number(this[index]);
  }-*/;

  /**
   * Gets the {@link JavaScriptObject} at a given index.
   * 
   * @param index the index to be retrieved
   * @return the {@code JavaScriptObject} at the given index, or
   *         <code>null</code> if none exists
   */
  public final native <T extends JavaScriptObject> T getObject(int index) /*-{
    return this[index] != null ? Object(this[index]) : null;
  }-*/;

  /**
   * Gets the String at a given index.
   * 
   * @param index the index to be retrieved
   * @return the object at the given index, or <code>null</code> if none exists
   */
  public final native String getString(int index) /*-{
    return String(this[index]);
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
   * Pushes the given boolean onto the end of the array.
   */
  public final native void push(boolean value) /*-{
    this[this.length] = value;
  }-*/;

  /**
   * Pushes the given double onto the end of the array.
   */
  public final native void push(double value) /*-{
    this[this.length] = value;
  }-*/;

  /**
   * Pushes the given {@link JavaScriptObject} onto the end of the array.
   */
  public final native void push(JavaScriptObject value) /*-{
    this[this.length] = value;
  }-*/;

  /**
   * Pushes the given String onto the end of the array.
   */
  public final native void push(String value) /*-{
    this[this.length] = value;
  }-*/;

  /**
   * Sets the boolean value at a given index.
   * 
   * If the index is out of bounds, the value will still be set. The array's
   * length will be updated to encompass the bounds implied by the added value.
   * 
   * @param index the index to be set
   * @param value the boolean to be stored
   */
  public final native void set(int index, boolean value) /*-{
    this[index] = value;
  }-*/;

  /**
   * Sets the double value at a given index.
   * 
   * If the index is out of bounds, the value will still be set. The array's
   * length will be updated to encompass the bounds implied by the added value.
   * 
   * @param index the index to be set
   * @param value the double to be stored
   */
  public final native void set(int index, double value) /*-{
    this[index] = value;
  }-*/;

  /**
   * Sets the object value at a given index.
   * 
   * If the index is out of bounds, the value will still be set. The array's
   * length will be updated to encompass the bounds implied by the added object.
   * 
   * @param index the index to be set
   * @param value the {@link JavaScriptObject} to be stored
   */
  public final native void set(int index, JavaScriptObject value) /*-{
    this[index] = value;
  }-*/;

  /**
   * Sets the String value at a given index.
   * 
   * If the index is out of bounds, the value will still be set. The array's
   * length will be updated to encompass the bounds implied by the added String.
   * 
   * @param index the index to be set
   * @param value the String to be stored
   */
  public final native void set(int index, String value) /*-{
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
   * @return the shifted boolean
   */
  public final native boolean shiftBoolean() /*-{
    return Boolean(this.shift());
  }-*/;

  /**
   * Shifts the first value off the array.
   * 
   * @return the shifted double
   */
  public final native double shiftNumber() /*-{
    return Number(this.shift());
  }-*/;

  /**
   * Shifts the first value off the array.
   * 
   * @return the shifted {@link JavaScriptObject}
   */
  public final native <T extends JavaScriptObject> T shiftObject() /*-{
    return Object(this.shift());
  }-*/;

  /**
   * Shifts the first value off the array.
   * 
   * @return the shifted String
   */
  public final native String shiftString() /*-{
    return String(this.shift());
  }-*/;

  /**
   * Shifts a boolean onto the beginning of the array.
   * 
   * @param value the value to the stored
   */
  public final native void unshift(boolean value) /*-{
    this.unshift(value);
  }-*/;

  /**
   * Shifts a double onto the beginning of the array.
   * 
   * @param value the value to store
   */
  public final native void unshift(double value) /*-{
    this.unshift(value);
  }-*/;

  /**
   * Shifts a {@link JavaScriptObject} onto the beginning of the array.
   * 
   * @param value the value to store
   */
  public final native void unshift(JavaScriptObject value) /*-{
    this.unshift(value);
  }-*/;

  /**
   * Shifts a String onto the beginning of the array.
   * 
   * @param value the value to store
   */
  public final native void unshift(String value) /*-{
    this.unshift(value);
  }-*/;

}
