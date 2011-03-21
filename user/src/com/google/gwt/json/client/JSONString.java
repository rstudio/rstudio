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
import com.google.gwt.core.client.JsonUtils;

/**
 * Represents a JSON string.
 */
public class JSONString extends JSONValue {

  /**
   * Called from {@link #getUnwrapper()}. 
   */
  private static String unwrap(JSONString value) {
    return value.value;
  }

  private String value;

  /**
   * Creates a new JSONString from the supplied String.
   * 
   * @param value a String value
   * @throws NullPointerException if <code>value</code> is <code>null</code>
   */
  public JSONString(String value) {
    if (value == null) {
      throw new NullPointerException();
    }
    this.value = value;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof JSONString)) {
      return false;
    }
    return value.equals(((JSONString) other).value);
  }

  @Override
  public int hashCode() {
    // Just use the underlying String's hashCode.
    return value.hashCode();
  }

  /**
   * Returns <code>this</code>, as this is a JSONString.
   */
  @Override
  public JSONString isString() {
    return this;
  }

  /**
   * Returns the raw Java string value of this item.
   */
  public String stringValue() {
    return value;
  }

  /**
   * Returns the JSON formatted value of this string, quoted for evaluating in a
   * JavaScript interpreter.
   */
  @Override
  public String toString() {
    return JsonUtils.escapeValue(value);
  }

  @Override
  native JavaScriptObject getUnwrapper() /*-{
    return @com.google.gwt.json.client.JSONString::unwrap(Lcom/google/gwt/json/client/JSONString;);
  }-*/;
}
