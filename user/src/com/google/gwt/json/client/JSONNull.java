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
 * Represents the JSON <code>null</code> value.
 */
public class JSONNull extends JSONValue {

  private static final JSONNull instance = new JSONNull();

  /**
   * Returns the singleton null-valued JSON object.
   */
  public static JSONNull getInstance() {
    return instance;
  }

  /**
   * Called from {@link #getUnwrapper()}. 
   */
  private static JavaScriptObject unwrap() {
    return null;
  }

  /**
   * There should only be one null value.
   */
  private JSONNull() {
  }

  /**
   * Returns <code>this</code>, as this is a JSONNull.
   */
  @Override
  public JSONNull isNull() {
    return this;
  }

  /**
   * Returns "null" to allow for formatting <code>null</code> values.
   */
  @Override
  public String toString() {
    return "null";
  }

  @Override
  native JavaScriptObject getUnwrapper() /*-{
    return @com.google.gwt.json.client.JSONNull::unwrap();
  }-*/;

}
