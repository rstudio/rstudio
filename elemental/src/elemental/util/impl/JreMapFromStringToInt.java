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

import elemental.util.ArrayOfInt;
import elemental.util.ArrayOfString;
import elemental.util.MapFromStringToInt;

/**
 * JRE implementation of MapFromStringToInt for server and dev mode.
 */
public class JreMapFromStringToInt implements MapFromStringToInt {

  /*
   * TODO(cromwellian): this implementation delegates, direct implementation would be more efficient.
   */
  private JreMapFromStringTo<Integer> map;

  public JreMapFromStringToInt() {
    map = new JreMapFromStringTo<Integer>();
  }

  @Override
  public int get(String key) {
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
  public void put(String key, int value) {
    map.put(key, value);
  }

  @Override
  public void remove(String key) {
    map.remove(key);
  }

  @Override
  public ArrayOfInt values() {
    return new JreArrayOfInt(map.values());
  }
}
