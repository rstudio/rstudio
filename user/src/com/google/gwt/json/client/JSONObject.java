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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Represents a JSON object. A JSON object consists of a set of properties.
 */
public class JSONObject extends JSONValue {

  /**
   * Called from {@link #getUnwrapper()}. 
   */
  private static JavaScriptObject unwrap(JSONObject value) {
    return value.jsObject;
  }

  private final JavaScriptObject jsObject;

  public JSONObject() {
    this(JavaScriptObject.createObject());
  }

  /**
   * Creates a new JSONObject from the supplied JavaScript value.
   */
  public JSONObject(JavaScriptObject jsValue) {
    jsObject = jsValue;
  }

  /**
   * Tests whether or not this JSONObject contains the specified property.
   * 
   * @param key the property to search for
   * @return <code>true</code> if the JSONObject contains the specified property
   */
  public native boolean containsKey(String key) /*-{
    return key in this.@com.google.gwt.json.client.JSONObject::jsObject;
  }-*/;

  /**
   * Returns <code>true</code> if <code>other</code> is a {@link JSONObject}
   * wrapping the same underlying object.
   */
  @Override
  public boolean equals(Object other) {
    if (!(other instanceof JSONObject)) {
      return false;
    }
    return jsObject.equals(((JSONObject) other).jsObject);
  }

  /**
   * Gets the JSONValue associated with the specified property.
   * 
   * @param key the property to access
   * @return the value of the specified property, or <code>null</code> if the
   *         property does not exist
   * @throws NullPointerException if key is <code>null</code>
   */
  public JSONValue get(String key) {
    if (key == null) {
      throw new NullPointerException();
    }
    return get0(key);
  }

  /**
   * Returns the underlying JavaScript object that this object wraps.
   */
  public JavaScriptObject getJavaScriptObject() {
    return jsObject;
  }
  
  @Override
  public int hashCode() {
    return jsObject.hashCode();
  }

  /**
   * Returns <code>this</code>, as this is a JSONObject.
   */
  @Override
  public JSONObject isObject() {
    return this;
  }

  /**
   * Returns the set of properties defined on this JSONObject. The returned set
   * is immutable.
   */
  public Set<String> keySet() {
    final String[] keys = computeKeys();
    return new AbstractSet<String>() {
      @Override
      public boolean contains(Object o) {
        return (o instanceof String) && containsKey((String) o);
      }

      @Override
      public Iterator<String> iterator() {
        return Arrays.asList(keys).iterator();
      }

      @Override
      public int size() {
        return keys.length;
      }
    };
  }

  /**
   * Assign the specified property to the specified value in this JSONObject. If
   * the property already has an associated value, it is overwritten.
   * 
   * @param key the property to assign
   * @param jsonValue the value to assign
   * @return the previous value of the property, or <code>null</code> if the
   *         property did not exist
   * @throws NullPointerException if key is <code>null</code>
   */
  public JSONValue put(String key, JSONValue jsonValue) {
    if (key == null) {
      throw new NullPointerException();
    }
    JSONValue previous = get(key);
    put0(key, jsonValue);
    return previous;
  }

  /**
   * Determines the number of properties on this object.
   */
  public int size() {
    // Must always recheck due to foreign changes. :(
    return computeSize();
  }

  /**
   * Converts a JSONObject into a JSON representation that can be used to
   * communicate with a JSON service.
   * 
   * @return a JSON string representation of this JSONObject instance
   */
  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("{");
    boolean first = true;
    String[] keys = computeKeys();
    for (String key : keys) {
      if (first) {
        first = false;
      } else {
        sb.append(", ");
      }
      sb.append(JsonUtils.escapeValue(key));
      sb.append(":");
      sb.append(get(key));
    }
    sb.append("}");
    return sb.toString();
  }

  @Override
  native JavaScriptObject getUnwrapper() /*-{
    return @com.google.gwt.json.client.JSONObject::unwrap(Lcom/google/gwt/json/client/JSONObject;);
  }-*/;

  private native void addAllKeys(Collection<String> s) /*-{
    var jsObject = this.@com.google.gwt.json.client.JSONObject::jsObject;
    for (var key in jsObject) {
      if (jsObject.hasOwnProperty(key)) {
        s.@java.util.Collection::add(Ljava/lang/Object;)(key);
      }
    }
  }-*/;

  private String[] computeKeys() {
    if (GWT.isScript()) {
      return computeKeys0(new String[0]);
    } else {
      List<String> result = new ArrayList<String>();
      addAllKeys(result);
      return result.toArray(new String[result.size()]);
    }
  }

  private native String[] computeKeys0(String[] result) /*-{
    var jsObject = this.@com.google.gwt.json.client.JSONObject::jsObject;
    var i = 0;
    for (var key in jsObject) {
      if (jsObject.hasOwnProperty(key)) {
        result[i++] = key;
      }
    }
    return result;
  }-*/;

  private native int computeSize() /*-{
    var jsObject = this.@com.google.gwt.json.client.JSONObject::jsObject;
    var size = 0;
    for (var key in jsObject) {
      if (jsObject.hasOwnProperty(key)) {
        ++size;
      }
    }
    return size;
  }-*/;

  private native JSONValue get0(String key) /*-{
    var jsObject = this.@com.google.gwt.json.client.JSONObject::jsObject;
    var v;
    // In Firefox, jsObject.hasOwnProperty(key) requires a primitive string
    key = String(key);       
    if (jsObject.hasOwnProperty(key)) {
      v = jsObject[key];
    }
    var func = @com.google.gwt.json.client.JSONParser::typeMap[typeof v];
    var ret = func ? func(v) : @com.google.gwt.json.client.JSONParser::throwUnknownTypeException(Ljava/lang/String;)(typeof v);
    return ret;
  }-*/;

  private native void put0(String key, JSONValue value) /*-{
    if (value) {
      var func = value.@com.google.gwt.json.client.JSONValue::getUnwrapper()();
      this.@com.google.gwt.json.client.JSONObject::jsObject[key] = func(value);
    } else {
      delete this.@com.google.gwt.json.client.JSONObject::jsObject[key];
    }
  }-*/;
}
