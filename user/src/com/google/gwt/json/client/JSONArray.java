/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.json.client;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Represents an array of {@link com.google.gwt.json.client.JSONValue} objects.
 */
public class JSONArray extends JSONValue {

  final JavaScriptObject javascriptArray;

  final JavaScriptObject wrappedArray;

  /**
   * Creates an empty JSONArray.
   */
  public JSONArray() {
    javascriptArray = createArray();
    wrappedArray = createArray();
  }

  /**
   * Creates a new JSONArray from the supplied JavaScriptObject representing a
   * JavaScript array.
   * 
   * @param arr a JavaScript array
   */
  public JSONArray(JavaScriptObject arr) {
    javascriptArray = arr;
    wrappedArray = createArray();
  }

  /**
   * Returns the value at the specified index position.
   * 
   * @param index the index of the array item to retrieve
   * @return the value at this index, or <code>null</code> if this index is
   *         empty
   */
  public JSONValue get(int index) throws JSONException {
    if (wrappedTest(index)) {
      return wrappedGet(index);
    }
    JSONValue wrapped = null;
    if (rawTest(index)) {
      wrapped = JSONParser.buildValue(rawGet(index));
      rawSet(index, null);
    }
    wrappedSet(index, wrapped);
    return wrapped;
  }

  /**
   * Returns <code>this</code>, as this is a JSONArray.
   */
  public JSONArray isArray() {
    return this;
  }

  /**
   * Sets the specified index to the given value.
   * 
   * @param index the index to set
   * @param jsonValue the value to set
   * @return the previous value at this index, or <code>null</code> if this
   *         index was empty
   */
  public JSONValue set(int index, JSONValue jsonValue) {
    JSONValue out = get(index);
    wrappedSet(index, jsonValue);
    rawSet(index, null);
    return out;
  }

  /**
   * Returns the number of elements in this array.
   * 
   * @return size of this array
   */
  public native int size() /*-{
    return this.@com.google.gwt.json.client.JSONArray::javascriptArray.length;
  }-*/;

  /**
   * Create the JSON encoded string representation of this JSONArray instance.
   * This method may take a long time to execute if the underlying array is
   * large.
   */
  public String toString() throws JSONException {
    StringBuffer sb = new StringBuffer();
    sb.append("[");
    for (int i = 0, c = size(); i < c; i++) {
      JSONValue value = get(i);
      sb.append(value.toString());

      if (i < c - 1) {
        sb.append(",");
      }
    }
    sb.append("]");
    return sb.toString();
  }

  private native JavaScriptObject createArray() /*-{
    return [];
  }-*/;

  private native JavaScriptObject rawGet(int index) /*-{
    var x = this.@com.google.gwt.json.client.JSONArray::javascriptArray[index];
    if (typeof x == 'number' || typeof x == 'string' || typeof x == 'array' || typeof x == 'boolean') {
      x = (Object(x));
    }
    return x;
  }-*/;

  private native void rawSet(int index, JavaScriptObject jsObject) /*-{
    this.@com.google.gwt.json.client.JSONArray::javascriptArray[index] = jsObject; 
  }-*/;

  private native boolean rawTest(int index) /*-{
    var x = this.@com.google.gwt.json.client.JSONArray::javascriptArray[index];
    return x !== undefined;
  }-*/;

  private native JSONValue wrappedGet(int index) /*-{
    return this.@com.google.gwt.json.client.JSONArray::wrappedArray[index];
  }-*/;

  private native void wrappedSet(int index, JSONValue jsonValue) /*-{
    this.@com.google.gwt.json.client.JSONArray::wrappedArray[index] = jsonValue;
  }-*/;

  private native boolean wrappedTest(int index) /*-{
    var x = this.@com.google.gwt.json.client.JSONArray::wrappedArray[index];
    return x !== undefined;
  }-*/;
}
