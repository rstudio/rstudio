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

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a JSON object. A JSON object is a map of string-based keys onto a
 * set of {@link com.google.gwt.json.client.JSONValue} objects.
 */
public class JSONObject extends JSONValue {
  private final JavaScriptObject backStore;

  private final JavaScriptObject frontStore = createBlankObject();

  public JSONObject() {
    backStore = createBlankObject();
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
   * @param key the key to search for
   * @return <code>true</code> if the JSONObject contains the specified key
   */
  public native boolean containsKey(String key) /*-{
    return this.@com.google.gwt.json.client.JSONObject::backStore[key] !== undefined
        || this.@com.google.gwt.json.client.JSONObject::frontStore[key] !== undefined;
  }-*/;

  /**
   * Gets the JSONValue associated with the specified key.
   * 
   * @param key the key to search for
   * @return if found, the value associated with the specified key, or
   *         <code>null</code> otherwise
   */
  public native JSONValue get(String key) /*-{
    if (this.@com.google.gwt.json.client.JSONObject::backStore[key] !== undefined) {
      var x=this.@com.google.gwt.json.client.JSONObject::backStore[key];
      if (typeof x == 'number' || typeof x == 'string' || typeof x == 'array' || typeof x == 'boolean') {
        x = Object(x); 
      }
      this.@com.google.gwt.json.client.JSONObject::frontStore[key]=
        // don't linebreak the next line
        @com.google.gwt.json.client.JSONParser::buildValue(Lcom/google/gwt/core/client/JavaScriptObject;)(x);
      delete this.@com.google.gwt.json.client.JSONObject::backStore[key];
    }
    var out = this.@com.google.gwt.json.client.JSONObject::frontStore[key];
    return (out == null) ? null : out;
  }-*/;

  /**
   * Returns <code>this</code>, as this is a JSONObject.
   */
  public JSONObject isObject() {
    return this;
  }

  /**
   * Returns keys for which this JSONObject has associations.
   * 
   * @return array of keys for which there is a value
   */
  public Set keySet() {
    Set keySet = new HashSet();
    addAllKeysFromJavascriptObject(keySet, frontStore);
    addAllKeysFromJavascriptObject(keySet, backStore);
    return keySet;
  }

  /**
   * Maps the specified key to the specified value in this JSONObject. If the
   * specified key already has an associated value, it is overwritten.
   * 
   * @param key the key to associate with the specified value
   * @param jsonValue the value to assoociate with this key
   * @return if one existed, the previous value associated with the key, or
   *         <code>null</code> otherwise
   */
  public native JSONValue put(String key, JSONValue jsonValue) /*-{
    var out = this.@com.google.gwt.json.client.JSONObject::get(Ljava/lang/String;)(key);
    this.@com.google.gwt.json.client.JSONObject::frontStore[key] = jsonValue;
    return out;
  }-*/;

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
  public native String toString() /*-{
    for (var x in this.@com.google.gwt.json.client.JSONObject::backStore) {
      // we are wrapping everything in backStore so that frontStore is canonical
      this.@com.google.gwt.json.client.JSONObject::get(Ljava/lang/String;)(x);
    }
    var out = [];
    out.push("{");
    var first = true;
    for (var x in this.@com.google.gwt.json.client.JSONObject::frontStore) {
      if(first) {
        first = false;
      } else {
        out.push(", ");
      }
      var subObj = 
        (this.@com.google.gwt.json.client.JSONObject::frontStore[x]).
          @com.google.gwt.json.client.JSONValue::toString()();
      out.push("\"");
      out.push(x);
      out.push("\":");
      out.push(subObj);
    }
    out.push("}")
    return out.join("");
  }-*/;

  private native void addAllKeysFromJavascriptObject(Set s,
      JavaScriptObject javaScriptObject) /*-{
    for(var key in javaScriptObject) {
     s.@java.util.Set::add(Ljava/lang/Object;)(key);
    }
  }-*/;

  private native JavaScriptObject createBlankObject() /*-{
    return {};
  }-*/;
}
