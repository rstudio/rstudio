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
package elemental.json.impl;

import elemental.json.JsonArray;
import elemental.json.JsonBoolean;
import elemental.json.JsonException;
import elemental.json.JsonFactory;
import elemental.json.JsonNull;
import elemental.json.JsonNumber;
import elemental.json.JsonObject;
import elemental.json.JsonString;
import elemental.json.JsonValue;


/**
 * Implementation of JsonFactory interface using org.json library.
 */
public class JreJsonFactory implements JsonFactory {

  public JsonString create(String string) {
    assert string != null;
    return new JreJsonString(string);
  }

  public JsonNumber create(double number) {
    return new JreJsonNumber(number);
  }

  public JsonBoolean create(boolean bool) {
    return new JreJsonBoolean(bool);
  }

  public JsonArray createArray() {
    return new JreJsonArray(this);
  }

  public JsonNull createNull() {
    return JreJsonNull.NULL_INSTANCE;
  }

  public JsonObject createObject() {
    return new JreJsonObject(this);
  }

  public <T extends JsonValue> T parse(String jsonString) throws JsonException {
    if (jsonString.startsWith("(") && jsonString.endsWith(")")) {
       // some clients send in (json) expecting an eval is required
       jsonString = jsonString.substring(1, jsonString.length() - 1);
    }
    return new JsonTokenizer(this, jsonString).nextValue();
  }
}
