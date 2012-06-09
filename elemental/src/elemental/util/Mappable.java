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
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package elemental.util;

/**
 * An object which can act like a Javascript object with String keys.
 */
// TODO (cromwellian) add generic when JSO bug in gwt-dev fixed
public interface Mappable /* <T> */ {

  /**
   * Gets the value at a given key.
   *
   * @param key the key to be retrieved
   * @return the value at the given index
   */
    Object /* T */ at(String key);

  /**
   * Sets the value at a given key.
   *
   * @param key the key to assign to
   * @return the value at the given index
   */
  void setAt(String key, Object /* T */ value);
}
