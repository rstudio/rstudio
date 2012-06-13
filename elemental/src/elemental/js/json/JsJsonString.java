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
package elemental.js.json;

import elemental.json.JsonString;
import elemental.json.JsonValue;

/**
 * Client-side 'zero overhead' JSO implementation using extension method
 * technique.
 */
final public class JsJsonString extends JsJsonValue
    implements JsonString {
  
  public static JsonString create(String string) {
    return createProd(string);
  }

  static native String getString(JsonValue value) /*-{
    return @elemental.js.json.JsJsonValue::debox(Lelemental/json/JsonValue;)(value);
  }-*/;

  /*
   * MAGIC: String cast to object interface.
   */
  private static native JsJsonString createProd(String string) /*-{
    return @elemental.js.json.JsJsonValue::box(Lelemental/json/JsonValue;)(value);
  }-*/;

  protected JsJsonString() {
  }

  public String getString() {
    return valueProd();
  }

  private native String valueProd() /*-{
    return @elemental.js.json.JsJsonValue::debox(Lelemental/json/JsonValue;)(this);
  }-*/;
}
