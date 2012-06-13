/*
 * Copyright 2010 Google Inc.
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
package elemental.js.util;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * A static API to the browser's JSON object.
 * TODO(knorton) : Remove this when generated DOM bindings are submitted. 
 */
public class Json {
  /**
   * Parse a string containing JSON into a {@link JavaScriptObject}.
   *
   * @param <T> the overlay type to expect from the parse
   * @param jsonAsString
   * @return a JavaScript object constructed from the parse
   */
  public native static <T extends JavaScriptObject> T parse(String jsonAsString) /*-{
    return JSON.parse(jsonAsString);
  }-*/;

  /**
   * Convert a {@link JavaScriptObject} into a string representation.
   *
   * @param json a JavaScript object to be converted to a string
   * @return JSON in string representation
   */
  public native static String stringify(JavaScriptObject json) /*-{
    return JSON.stringify(json);
  }-*/;

  private Json() {
  }
}
