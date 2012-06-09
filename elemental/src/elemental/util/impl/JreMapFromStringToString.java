/*
 * Copyright 2011 Google Inc.
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
package elemental.util.impl;

import elemental.util.ArrayOfString;
import elemental.util.MapFromStringToString;

/**
 * JRE implementation of MapFromStringToString for server and dev mode.
 */
public class JreMapFromStringToString implements MapFromStringToString {

  /*
   * TODO(cromwellian): this implementation delegates, direct implementation would be more efficient.
   */
  private JreMapFromStringTo<String> map;

  public JreMapFromStringToString() {
    map = new JreMapFromStringTo<String>();
  }

  @Override
  public String get(String key) {
    return map.get(key);
  }

  @Override
  public boolean hasKey(String key) {
    return map.hasKey(key);
  }

  @Override
  public ArrayOfString keys() {
    return map.keys();
  }

  @Override
  public void put(String key, String value) {
    map.put(key, value);
  }

  @Override
  public void remove(String key) {
    map.remove(key);
  }

  @Override
  public ArrayOfString values() {
    return new JreArrayOfString(map.values());
  }
}
