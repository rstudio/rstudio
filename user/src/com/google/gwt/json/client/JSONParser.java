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

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;

/**
 * Parses the string representation of a JSON object into a set of
 * JSONValue-derived objects.
 * 
 * @see com.google.gwt.json.client.JSONValue
 */
public class JSONParser {

  /**
   * Given a jsonString, returns the JSONObject representation. For efficiency,
   * parsing occurs lazily as the structure is requested.
   * 
   * @param jsonString
   * @return a JSONObject that has been built by parsing the JSON string
   * @throws NullPointerException if <code>jsonString</code> is
   *           <code>null</code>
   * @throws IllegalArgumentException if <code>jsonString</code> is empty
   */
  public static JSONValue parse(String jsonString) {
    // Create a JavaScriptObject from the JSON string.
    //
    if (jsonString == null) {
      throw new NullPointerException();
    }
    if (jsonString == "") {
      throw new IllegalArgumentException("empty argument");
    }
    try {
      JavaScriptObject jsonObject = evaluate(jsonString);
      return buildValue(jsonObject);
    } catch (JavaScriptException ex) {
      throw new JSONException(ex);
    }
  }

  /**
   * Returns the {@link JSONValue} for a given {@link JavaScriptObject}.  
   * 
   * @param jsValue {@link JavaScriptObject} to build a {@link JSONValue} for, 
   *     this object cannot be a primitive JavaScript type
   * @return a {@link JSONValue} instance for the {@link JavaScriptObject}
   */
  static JSONValue buildValue(JavaScriptObject jsValue) throws JSONException {
    
    if (isNull(jsValue)) {
      return JSONNull.getInstance();
    }

    if (isArray(jsValue)) {
      return new JSONArray(jsValue);
    }

    if (isBoolean(jsValue)) {
      return JSONBoolean.getInstance(asBoolean(jsValue));
    }

    if (isString(jsValue)) {
      return new JSONString(asString(jsValue));
    }

    if (isDouble(jsValue)) {
      return new JSONNumber(asDouble(jsValue));
    }

    if (isObject(jsValue)) {
      return new JSONObject(jsValue);
    }

    /*
     * In practice we should never reach this point.  If we do, we cannot make 
     * any assumptions about the jsValue.
     */
    throw new JSONException("Unknown JavaScriptObject type");
  }

  /**
   * Returns the boolean represented by the jsValue. This method
   * assumes that {@link #isBoolean(JavaScriptObject)} returned
   * <code>true</code>.
   * 
   * @param jsValue JavaScript object to convert
   * @return the boolean represented by the jsValue
   */
  private static native boolean asBoolean(JavaScriptObject jsValue) /*-{
    return jsValue.valueOf();
  }-*/;

  /**
   * Returns the double represented by jsValue. This method assumes that
   * {@link #isDouble(JavaScriptObject)} returned <code>true</code>.
   * 
   * @param jsValue JavaScript object to convert
   * @return the double represented by the jsValue
   */
  private static native double asDouble(JavaScriptObject jsValue) /*-{
    return jsValue.valueOf();
  }-*/;

  /**
   * Returns the Javascript String as a Java String. This method assumes that
   * {@link #isString(JavaScriptObject)} returned <code>true</code>.
   * 
   * @param jsValue JavaScript object to convert
   * @return the String represented by the jsValue
   */
  private static native String asString(JavaScriptObject jsValue) /*-{
    return jsValue;
  }-*/;

  /*
   * This method converts the json string into a JavaScriptObject inside of JSNI
   * method by simply evaluating the string in JavaScript.
   */
  private static native JavaScriptObject evaluate(String jsonString) /*-{
    var x = eval('(' + jsonString + ')');
    if (typeof x == 'number' || typeof x == 'string' || typeof x == 'array' || typeof x == 'boolean') {
      x = (Object(x));
    }
    return x;
  }-*/;

  /**
   * Returns <code>true</code> if the {@link JavaScriptObject} is a wrapped
   * JavaScript Array.
   * 
   * @param jsValue JavaScript object to test
   * @return <code>true</code> if jsValue is a wrapped JavaScript Array
   */
  private static native boolean isArray(JavaScriptObject jsValue) /*-{
    return jsValue instanceof Array; 
  }-*/;

  /**
   * Returns <code>true</code> if the {@link JavaScriptObject} is a wrapped
   * JavaScript Boolean.
   * 
   * @param jsValue JavaScript object to test
   * @return <code>true</code> if jsValue is a wrapped JavaScript Boolean
   */
  private static native boolean isBoolean(JavaScriptObject jsValue) /*-{
    return jsValue instanceof Boolean; 
  }-*/;

  /**
   * Returns <code>true</code> if the {@link JavaScriptObject} is a wrapped
   * JavaScript Double.
   * 
   * @param jsValue JavaScript object to test
   * @return <code>true</code> if jsValue is a wrapped JavaScript Double
   */
  private static native boolean isDouble(JavaScriptObject jsValue) /*-{
    return jsValue instanceof Number;
  }-*/;

  /**
   * Returns <code>true</code> if the {@link JavaScriptObject} is <code>null</code>
   * or <code>undefined</code>.
   * 
   * @param jsValue JavaScript object to test
   * @return <code>true</code> if jsValue is <code>null</code> or
   *         <code>undefined</code>
   */
  private static native boolean isNull(JavaScriptObject jsValue) /*-{
    return jsValue == null;
  }-*/;

  /**
   * Returns <code>true</code> if the {@link JavaScriptObject} is a JavaScript
   * Object.
   * 
   * @param jsValue JavaScript object to test
   * @return <code>true</code> if jsValue is a JavaScript Object
   */
  private static native boolean isObject(JavaScriptObject jsValue) /*-{
    return jsValue instanceof Object;
  }-*/;

  /**
   * Returns <code>true</code> if the {@link JavaScriptObject} is a JavaScript
   * String.
   * 
   * @param jsValue JavaScript object to test
   * @return <code>true</code> if jsValue is a JavaScript String
   */
  private static native boolean isString(JavaScriptObject jsValue) /*-{
    return jsValue instanceof String;
  }-*/;

  /**
   * Not instantiable.
   */
  private JSONParser() {
  }
}