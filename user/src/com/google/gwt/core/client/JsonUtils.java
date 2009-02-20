/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.core.client;

/**
 * Provides JSON-related utility methods.
 */
public class JsonUtils {
  @SuppressWarnings("unused")
  private static JavaScriptObject escapeTable = initEscapeTable();

  /**
   * Returns a quoted, escaped JSON String.
   */
  public static native String escapeValue(String toEscape) /*-{
    var s = toEscape.replace(/[\x00-\x1F\u2028\u2029"\\]/g, function(x) {
      return @com.google.gwt.core.client.JsonUtils::escapeChar(Ljava/lang/String;)(x);
    });
    return "\"" + s + "\"";
  }-*/;

  /*
   * TODO: Implement safeEval using a proper parser.
   */

  /**
   * Evaluates a JSON expression. This method does not validate the JSON text
   * and should only be used on JSON from trusted sources.
   * 
   * @param <T> The type of JavaScriptObject that should be returned
   * @param json The source JSON text
   * @return The evaluated object
   */
  public static native <T extends JavaScriptObject> T unsafeEval(String json) /*-{
    return eval('(' + json + ')');
  }-*/;

  @SuppressWarnings("unused")
  private static native String escapeChar(String c) /*-{
    var lookedUp = @com.google.gwt.core.client.JsonUtils::escapeTable[c.charCodeAt(0)];
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

    // Unicode line separator chars
    out[0x2028] = '\\u2028';
    out[0x2029] = '\\u2029';
    return out;
  }-*/;

  private JsonUtils() {
  }
}
