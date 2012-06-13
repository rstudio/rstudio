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
 * Represents a Json object.
 */
public interface JsonObject extends JsonValue {

  /**
   * Return the element (uncoerced) as a JsonValue.
   */
  <T extends JsonValue> T get(String key);

  /**
   * Return the element (uncoerced) as a JsonArray. If the type is not an array,
   * this can result in runtime errors.
   */
  JsonArray getArray(String key);

  /**
   * Return the element (uncoerced) as a boolean. If the type is not a boolean,
   * this can result in runtime errors.
   */
  boolean getBoolean(String key);

  /**
   * Return the element (uncoerced) as a number. If the type is not a number, this
   * can result in runtime errors.
   */
  double getNumber(String key);

  /**
   * Return the element (uncoerced) as a JsonObject If the type is not an object,,
   * this can result in runtime errors.
   */
  JsonObject getObject(String key);

  /**
   * Return the element (uncoerced) as a String. If the type is not a String, this
   * can result in runtime errors.
   */
  String getString(String key);

  /**
   * All keys of the object.
   */
  String[] keys();

  /**
   * Set a given key to the given value.
   */
  void put(String key, JsonValue value);

  /**
   * Set a given key to the given String value.
   */
  void put(String key, String value);

  /**
   * Set a given key to the given double value.
   */
  void put(String key, double value);

  /**
   * Set a given key to the given boolean value.
   */
  void put(String key, boolean bool);

  /**
   * Test whether a given key has present.
   */
  boolean hasKey(String key);

  /**
   * Remove a given key and associated value from the object.
   * @param key
   */
  void remove(String key);
}
