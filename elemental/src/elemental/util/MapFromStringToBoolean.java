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
package elemental.util;

/**
 * A lightweight map from {@link String} to <code>boolean</code> types.
 *
 * @see elemental.js.util.JsMapFromStringToBoolean
 */
public interface MapFromStringToBoolean {

  /**
   * Retrieves a value for the specified key.
   */
  boolean get(String key);

  /**
   * Indicates whether the map contains a value for the specified key.
   */
  boolean hasKey(String key);

  /**
   * The keys contained within this map.
   *
   * This copies the keys into a new array.
   */
  ArrayOfString keys();

  /**
   * Associates a value to the specified key.
   */
  void put(String key, boolean value);

  /**
   * Removes the value associated with the specified value, if one exists.
   */
  void remove(String key);

  /**
   * The values contained in this map.
   *
   * This copies the values into a new array.
   */
  ArrayOfBoolean values();
}
