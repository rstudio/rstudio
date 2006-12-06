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

  /*
   * Given a JavaScript object that could be an Object or a primitive JavaScript
   * type, create the correct subtype of JSONValue and return it.
   */
  static JSONValue buildValue(JavaScriptObject jsValue) throws JSONException {
    if (isNull(jsValue)) {
      return JSONNull.getInstance();
    }
    if (isArray(jsValue)) {
      return new JSONArray(jsValue);
    }
    Boolean bool = asBoolean(jsValue);
    if (bool != null) {
      return JSONBoolean.getInstance(bool.booleanValue());
    }
    String str = asString(jsValue);
    if (str != null) {
      return new JSONString(str);
    }
    if (isDouble(jsValue)) {
      return new JSONNumber(asDouble(jsValue));
    }
    if (isJSONObject(jsValue)) {
      return new JSONObject(jsValue);
    }
    throw new JSONException(toJavascriptString(jsValue));
  }

  /*
   * Returns a Boolean representing the truth value of jsValue. Returns null if
   * jsValue is not a boolean value.
   */
  private static native Boolean asBoolean(JavaScriptObject jsValue) /*-{
    if (jsValue instanceof Boolean || ((typeof jsValue)=='boolean')) {
      if(jsValue==true) {
        return @java.lang.Boolean::TRUE;
      } else {
        return @java.lang.Boolean::FALSE;
      }
    }
    return null;
  }-*/;
  
  /*
   * Returns the double represented by jsValue.
   */
  private static native double asDouble(JavaScriptObject jsValue) /*-{
    return jsValue;
  }-*/;

  /*
   * converts the Javascript Object to a String. Returns null if the object is
   * not a String.
   */
  private static native String asString(JavaScriptObject jsValue) /*-{
    if (jsValue instanceof String || ((typeof jsValue)=='string')) {
      return jsValue;
    }
    return null;
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

  private static native boolean isArray(JavaScriptObject jsValue) /*-{
    return (jsValue instanceof Array); 
  }-*/;

  /*
   * Returns true if jsValue is a double.
   */
  private static native boolean isDouble(JavaScriptObject jsValue) /*-{
    return (jsValue instanceof Number || ((typeof jsValue)=='number'));
  }-*/;

  /*
   * Given a JavaScriptObject build the corresponding JSONObject representation.
   * Note it is assumed that the JavaScriptObject is a proper JSON object. This
   * is done entirely in Javascript because the Javascript code to marshal it
   * into a Java form would be about the same size.
   */
  private static native boolean isJSONObject(JavaScriptObject jsObject) /*-{
    return (jsObject instanceof Object);
  }-*/;

  private static native boolean isNull(JavaScriptObject o) /*-{
    return o == null;
  }-*/;

  /*
   * Returns the string value of jsValue. This is more informative than
   * Object.toString().
   */
  private static native String toJavascriptString(JavaScriptObject jsValue) /*-{
    return jsValue.toString();
  }-*/;

  /**
   * Not instantiable.
   */
  private JSONParser() {
  }
}