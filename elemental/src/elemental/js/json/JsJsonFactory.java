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

import elemental.json.JsonException;
import elemental.json.JsonFactory;
import elemental.json.JsonNull;
import elemental.json.JsonNumber;
import elemental.json.JsonObject;
import elemental.json.JsonString;
import elemental.json.JsonValue;

/**
 * JSNI based implementation of JsonFactory.
 */
public class JsJsonFactory implements JsonFactory {

  public JsonString create(String string) {
    return JsJsonString.create(string);
  }

  public JsonNumber create(double number) {
    return JsJsonNumber.create(number);
  }

  public elemental.json.JsonBoolean create(boolean bool) {
    return JsJsonBoolean.create(bool);
  }

  public elemental.json.JsonArray createArray() {
    return JsJsonArray.create();
  }

  public JsonNull createNull() {
    return JsJsonNull.create();
  }

  public native JsonObject createObject() /*-{
    return Object.create(null);
  }-*/;

  @SuppressWarnings({"unchecked"})
  public <T extends JsonValue> T parse(String jsonString) throws JsonException {
    try {
      return parse0(jsonString);
    } catch (Exception e) {
      throw new JsonException("Can't parse " + jsonString);
    }
  }

  private native <T extends JsonValue> T parse0(String jsonString) /*-{
    // assume Chrome, safe and non-broken JSON.parse impl
    return $wnd.JSON.parse(jsonString, function(key, value) {
      if (typeof value === 'object') {
        return value;
      }
      return Object(value);
    });
  }-*/;
}
