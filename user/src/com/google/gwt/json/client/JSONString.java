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
 * Represents a JSON string.
 */
public class JSONString extends JSONValue {

  static JavaScriptObject escapeTable = initEscapeTable();

  static native String escapeChar(String c) /*-{
    var lookedUp = @com.google.gwt.json.client.JSONString::escapeTable[c.charCodeAt(0)];
    return (lookedUp == null) ? c : lookedUp;
  }-*/;

  private static native JavaScriptObject initEscapeTable() /*-{
    var out = [
      "\\u0000", "\\u0001", "\\u0002", "\\u0003", "\\u0004", "\\u0005",
      "\\u0006", "\\u0007", "\\b", "\\t", "\\n", "\\u000B",
      "\\f", "\\r", "\\u000E", "\\u000F", "\\u0010", "\\u0011",
      "\\u0012", "\\u0013", "\\u0014", "\\u0015", "\\u0016", "\\u0017",
      "\\u0018", "\\u0019", "\\u001A", "\\u001B", "\\u001C", "\\u001D",
      "\\u001E", "\\u001F"];
    out[34] = '\\"';
    out[92] = '\\\\';
    return out;
  }-*/;

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

  /**
   * Returns <code>this</code>, as this is a JSONString.
   */
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
  public String toString() {
    return escapeValue(value);
  }

  private native String escapeValue(String toEscape) /*-{
    var s = toEscape.replace(/[\x00-\x1F"\\]/g, function(x) {
      return @com.google.gwt.json.client.JSONString::escapeChar(Ljava/lang/String;)(x);
    });
    return "\"" + s + "\"";
  }-*/;
}
