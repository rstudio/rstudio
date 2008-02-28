/*
 * Copyright 2007 Google Inc.
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

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a JSON object. A JSON object is a map of string-based keys onto a
 * set of {@link com.google.gwt.json.client.JSONValue} objects.
 */
public class JSONObject extends JSONValue {

  private static native void addAllKeysFromJavascriptObject(Set<String> s,
      JavaScriptObject javaScriptObject) /*-{
    for(var key in javaScriptObject) {
      s.@java.util.Set::add(Ljava/lang/Object;)(key);
    }
  }-*/;

  private static native boolean containsBack(JavaScriptObject backStore, String key) /*-{
    key = String(key);
    return Object.prototype.hasOwnProperty.call(backStore, key);
  }-*/;

  private static native JSONValue getFront(JavaScriptObject frontStore, String key) /*-{
    key = String(key);
    return Object.prototype.hasOwnProperty.call(frontStore, key) ? frontStore[key] : null;
  }-*/;

  private static native void putFront(JavaScriptObject frontStore, String key,
      JSONValue jsonValue) /*-{
    frontStore[String(key)] = jsonValue;
  }-*/;

  private static native Object removeBack(JavaScriptObject backStore, String key) /*-{
    key = String(key);
    var result = backStore[key];
    delete backStore[key];
    if (typeof result != 'object') {
      result = Object(result); 
    }
    return result;
  }-*/;

  private final JavaScriptObject backStore;

  private final JavaScriptObject frontStore = JavaScriptObject.createObject();

  public JSONObject() {
    backStore = JavaScriptObject.createObject();
  }

  /**
   * Creates a new JSONObject from the supplied JavaScript value.
   */
  public JSONObject(JavaScriptObject jsValue) {
    backStore = jsValue;
  }

  /**
   * Tests whether or not this JSONObject contains the specified key.
   * 
   * We use Object.hasOwnProperty here to verify that a given key is specified
   * on this object rather than a superclass (such as standard properties
   * defined on Object).
   * 
   * @param key the key to search for
   * @return <code>true</code> if the JSONObject contains the specified key
   */
  public boolean containsKey(String key) {
    return get(key) != null;
  }

  /**
   * Gets the JSONValue associated with the specified key.
   * 
   * We use Object.hasOwnProperty here to verify that a given key is specified
   * on this object rather than a superclass (such as standard properties
   * defined on Object).
   * 
   * @param key the key to search for
   * @return if found, the value associated with the specified key, or
   *         <code>null</code> otherwise
   */
  public JSONValue get(String key) {
    if (key == null) {
      return null;
    }
    JSONValue result = getFront(frontStore, key);
    if (result == null && containsBack(backStore, key)) {
      Object o = removeBack(backStore, key);
      if (o instanceof String) {
        result = new JSONString((String) o);
      } else {
        result = JSONParser.buildValue((JavaScriptObject) o);
      }
      putFront(frontStore, key, result);
    }
    return result;
  }

  /**
   * Returns <code>this</code>, as this is a JSONObject.
   */
  @Override
  public JSONObject isObject() {
    return this;
  }

  /**
   * Returns keys for which this JSONObject has associations.
   * 
   * @return array of keys for which there is a value
   */
  public Set<String> keySet() {
    Set<String> keySet = new HashSet<String>();
    addAllKeysFromJavascriptObject(keySet, frontStore);
    addAllKeysFromJavascriptObject(keySet, backStore);
    return keySet;
  }

  /**
   * Maps the specified key to the specified value in this JSONObject. If the
   * specified key already has an associated value, it is overwritten.
   * 
   * @param key the key to associate with the specified value
   * @param jsonValue the value to associate with this key
   * @return if one existed, the previous value associated with the key, or
   *         <code>null</code> otherwise
   * @throws NullPointerException if key is <code>null</code>
   */
  public JSONValue put(String key, JSONValue jsonValue) {
    if (key == null) {
      throw new NullPointerException();
    }
    JSONValue previous = get(key);
    putFront(frontStore, key, jsonValue);
    return previous;
  }

  /**
   * Determines the number of keys on this object.
   */
  public int size() {
    return keySet().size();
  }

  /**
   * Converts a JSONObject into a JSON representation that can be used to
   * communicate with a JSON service.
   * 
   * @return a JSON string representation of this JSONObject instance
   */
  @Override
  public native String toString() /*-{
    for (var key in this.@com.google.gwt.json.client.JSONObject::backStore) {
      // Wrap everything in backStore so that frontStore is canonical.
      this.@com.google.gwt.json.client.JSONObject::get(Ljava/lang/String;)(key);
    }
    var out = [];
    out.push("{");
    var first = true;
    for (var key in this.@com.google.gwt.json.client.JSONObject::frontStore) {
      if(first) {
        first = false;
      } else {
        out.push(", ");
      }
      var subObj = 
        (this.@com.google.gwt.json.client.JSONObject::frontStore[key]).
          @com.google.gwt.json.client.JSONValue::toString()();
      out.push("\"");
      out.push(key);
      out.push("\":");
      out.push(subObj);
    }
    out.push("}")
    return out.join("");
  }-*/;
}
