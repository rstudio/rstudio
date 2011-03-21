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

  static final JavaScriptObject typeMap = initTypeMap();

  /**
   * Evaluates a trusted JSON string and returns its JSONValue representation.
   * CAUTION! This method calls the JavaScript <code>eval()</code> function,
   * which can execute arbitrary script. DO NOT pass an untrusted string into
   * this method.
   * 
   * <p>
   * This method has been deprecated. Please call either
   * {@link #parseStrict(String)} (for inputs that strictly follow the JSON
   * specification) or {@link #parseLenient(String)}. The implementation of this
   * method calls parseLenient.
   * 
   * @param jsonString a JSON object to parse
   * @return a JSONValue that has been built by parsing the JSON string
   * @throws NullPointerException if <code>jsonString</code> is
   *           <code>null</code>
   * @throws IllegalArgumentException if <code>jsonString</code> is empty
   * 
   * @deprecated use {@link #parseStrict(String)} or
   *             {@link #parseLenient(String)}
   */
  @Deprecated
  public static JSONValue parse(String jsonString) {
    return parseLenient(jsonString);
  }

  /**
   * Evaluates a trusted JSON string and returns its JSONValue representation.
   * CAUTION! This method calls the JavaScript {@code eval()} function, which
   * can execute arbitrary script. DO NOT pass an untrusted string into this
   * method.
   * 
   * @param jsonString a JSON object to parse
   * @return a JSONValue that has been built by parsing the JSON string
   * @throws NullPointerException if <code>jsonString</code> is
   *           <code>null</code>
   * @throws IllegalArgumentException if <code>jsonString</code> is empty
   */
  public static JSONValue parseLenient(String jsonString) {
    return parse(jsonString, false);
  }

  /**
   * Evaluates a JSON string and returns its JSONValue representation. Where
   * possible, the browser's {@code JSON.parse function} is used. For older
   * browsers including IE6 and IE7 that lack a {@code JSON.parse} function, the
   * input is validated as described in RFC 4627 for safety and passed to
   * {@code eval()}.
   * 
   * @param jsonString a JSON object to parse
   * @return a JSONValue that has been built by parsing the JSON string
   * @throws NullPointerException if <code>jsonString</code> is
   *           <code>null</code>
   * @throws IllegalArgumentException if <code>jsonString</code> is empty
   */
  public static JSONValue parseStrict(String jsonString) {
    return parse(jsonString, true);
  }
  
  static void throwJSONException(String message) {
    throw new JSONException(message);
  }

  static void throwUnknownTypeException(String typeString) {
    throw new JSONException("Unexpected typeof result '" + typeString
        + "'; please report this bug to the GWT team");
  }

  /**
   * Called from {@link #initTypeMap()}.
   */
  private static JSONValue createBoolean(boolean v) {
    return JSONBoolean.getInstance(v);
  }

  /**
   * Called from {@link #initTypeMap()}.
   */
  private static JSONValue createNumber(double v) {
    return new JSONNumber(v);
  }

  /**
   * Called from {@link #initTypeMap()}. If we get here, <code>o</code> is
   * either <code>null</code> (not <code>undefined</code>) or a JavaScript
   * object.
   */
  private static native JSONValue createObject(Object o) /*-{
    if (!o) {
      return @com.google.gwt.json.client.JSONNull::getInstance()();
    }
    var v = o.valueOf ? o.valueOf() : o;
    if (v !== o) {
      // It was a primitive wrapper, unwrap it and try again.
      var func = @com.google.gwt.json.client.JSONParser::typeMap[typeof v];
      return func ? func(v) : @com.google.gwt.json.client.JSONParser::throwUnknownTypeException(Ljava/lang/String;)(typeof v);
    } else if (o instanceof Array || o instanceof $wnd.Array) {
      // Looks like an Array; wrap as JSONArray.
      // NOTE: this test can fail for objects coming from a different window,
      // but we know of no reliable tests to determine if something is an Array
      // in all cases.
      return @com.google.gwt.json.client.JSONArray::new(Lcom/google/gwt/core/client/JavaScriptObject;)(o);
    } else {
      // This is a basic JavaScript object; wrap as JSONObject.
      // Subobjects will be created on demand.
      return @com.google.gwt.json.client.JSONObject::new(Lcom/google/gwt/core/client/JavaScriptObject;)(o);
    }
  }-*/;

  /**
   * Called from {@link #initTypeMap()}.
   */
  private static JSONValue createString(String v) {
    return new JSONString(v);
  }

  /**
   * Called from {@link #initTypeMap()}. This method returns a <code>null</code>
   * pointer, representing JavaScript <code>undefined</code>.
   */
  private static JSONValue createUndefined() {
    return null;
  }

  /**
   * This method converts <code>jsonString</code> into a JSONValue.
   * In strict mode (strict == true), one of two code paths is taken:
   * 1) Call JSON.parse if available, or
   * 2) Validate the input and call eval()
   * 
   * In lenient mode (strict == false), eval() is called without validation.
   * 
   * @param strict if true, parse in strict mode. 
   */
  private static native JSONValue evaluate(String json, boolean strict) /*-{
    // Note: we cannot simply call JsonUtils.unsafeEval because it is unable
    // to return a result for inputs whose outermost type is 'string' in
    // dev mode.
    var v;
    if (strict && @com.google.gwt.core.client.JsonUtils::hasJsonParse) {
      try {
        v = JSON.parse(json);
      } catch (e) {
        return @com.google.gwt.json.client.JSONParser::throwJSONException(Ljava/lang/String;)("Error parsing JSON: " + e);
      }
    } else {
      if (strict) {
        // Validate the input according to RFC 4627.
        if (!@com.google.gwt.core.client.JsonUtils::safeToEval(Ljava/lang/String;)(json)) {
          return @com.google.gwt.json.client.JSONParser::throwJSONException(Ljava/lang/String;)("Illegal character in JSON string");
        }
      }
      json = @com.google.gwt.core.client.JsonUtils::escapeJsonForEval(Ljava/lang/String;)(json);
      try {
        v = eval('(' + json + ')');
      } catch (e) { 
        return @com.google.gwt.json.client.JSONParser::throwJSONException(Ljava/lang/String;)("Error parsing JSON: " + e);
      }
    }
    var func = @com.google.gwt.json.client.JSONParser::typeMap[typeof v];
    return func ? func(v) : @com.google.gwt.json.client.JSONParser::throwUnknownTypeException(Ljava/lang/String;)(typeof v);
  }-*/;

  private static native JavaScriptObject initTypeMap() /*-{
    return {
      "boolean": @com.google.gwt.json.client.JSONParser::createBoolean(Z),
      "number": @com.google.gwt.json.client.JSONParser::createNumber(D),
      "string": @com.google.gwt.json.client.JSONParser::createString(Ljava/lang/String;),
      "object": @com.google.gwt.json.client.JSONParser::createObject(Ljava/lang/Object;),
      "function": @com.google.gwt.json.client.JSONParser::createObject(Ljava/lang/Object;),
      "undefined": @com.google.gwt.json.client.JSONParser::createUndefined(),
    }
  }-*/;

  private static JSONValue parse(String jsonString, boolean strict) {
    if (jsonString == null) {
      throw new NullPointerException();
    }
    if (jsonString.length() == 0) {
      throw new IllegalArgumentException("empty argument");
    }
    try {
      return evaluate(jsonString, strict);
    } catch (JavaScriptException ex) {
      throw new JSONException(ex);
    }
  }

  /**
   * Not instantiable.
   */
  private JSONParser() {
  }
}