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

import com.google.gwt.core.client.JavaScriptObject;

/**
 * The superclass of all JSON value types.
 * 
 * @see com.google.gwt.json.client.JSONArray
 * @see com.google.gwt.json.client.JSONBoolean
 * @see com.google.gwt.json.client.JSONNumber
 * @see com.google.gwt.json.client.JSONObject
 * @see com.google.gwt.json.client.JSONString
 */
public abstract class JSONValue {
  /**
   * Not subclassable outside this package.
   */
  JSONValue() {
  }

  /**
   * Returns a non-null reference if this JSONValue is really a JSONArray.
   * 
   * @return a reference to a JSONArray if this JSONValue is a JSONArray or
   *         <code>null</code> otherwise.
   */
  public JSONArray isArray() {
    return null;
  }

  /**
   * Returns a non-null reference if this JSONValue is really a JSONBoolean.
   * 
   * @return a reference to a JSONBoolean if this JSONValue is a JSONBoolean or
   *         <code>null</code> otherwise.
   */
  public JSONBoolean isBoolean() {
    return null;
  }

  /**
   * Returns a non-null reference if this JSONValue is really a JSONNull.
   * 
   * @return a reference to a JSONNull if this JSONValue is a JSONNull or
   *         <code>null</code> otherwise.
   */
  public JSONNull isNull() {
    return null;
  }

  /**
   * Returns a non-null reference if this JSONValue is really a JSONNumber.
   * 
   * @return a reference to a JSONNumber if this JSONValue is a JSONNumber or
   *         <code>null</code> otherwise.
   */
  public JSONNumber isNumber() {
    return null;
  }

  /**
   * Returns non-null if this JSONValue is really a JSONObject.
   * 
   * @return a reference to a JSONObject if this JSONValue is a JSONObject or
   *         <code>null</code> otherwise.
   */
  public JSONObject isObject() {
    return null;
  }

  /**
   * Returns a non-null reference if this JSONValue is really a JSONString.
   * 
   * @return a reference to a JSONString if this JSONValue is a JSONString or
   *         <code>null</code> otherwise.
   */
  public JSONString isString() {
    return null;
  }

  /**
   * Returns a JSON-encoded string for this entity. Use this method to create
   * JSON strings that can be sent from the client to a server.
   */
  @Override
  public abstract String toString();

  /**
   * Internal. Returns a JS func that can unwrap this value.  Used from native
   * code.
   */
  abstract JavaScriptObject getUnwrapper();
}
