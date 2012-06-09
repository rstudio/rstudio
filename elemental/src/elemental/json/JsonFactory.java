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
package elemental.json;

/**
 * Factory interface for parsing and creating JSON objects.
 */
public interface JsonFactory {

  /**
   * Create a JsonString from a Java String.
   *
   * @param string a Java String
   * @return the parsed JsonString
   */
  JsonString create(String string);

  /**
   * Create a JsonNumber from a Java double.
   *
   * @param number a Java double
   * @return the parsed JsonNumber
   */
  JsonNumber create(double number);

  /**
   * Create a JsonBoolean from a Java boolean.
   *
   * @param bool a Java boolean
   * @return the parsed JsonBoolean
   */
  JsonBoolean create(boolean bool);

  /**
   * Create an empty JsonArray.
   *
   * @return a new JsonArray
   */
  elemental.json.JsonArray createArray();

  /**
   * Create a JsonNull.
   *
   * @return a JsonNull instance
   */
  JsonNull createNull();

  /**
   * Create an empty JsonObject.
   *
   * @return a new JsonObject
   */
  JsonObject createObject();

  /**
   * Parse a String in JSON format and return a JsonValue of the appropriate
   * type.
   *
   * @param jsonString a String in JSON format
   * @return a parsed JsonValue
   */
  <T extends JsonValue> T parse(String jsonString) throws elemental.json.JsonException;
}
