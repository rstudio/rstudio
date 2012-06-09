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

import java.util.ArrayList;
import java.util.HashMap;

import elemental.util.ArrayOf;
import elemental.util.ArrayOfString;
import elemental.util.MapFromStringTo;

/**
 * JRE implementation of MapFromStringTo for server and dev mode.
 */
public class JreMapFromStringTo<T> implements MapFromStringTo<T> {

  /*
   * TODO(cromwellian): this implemation uses JRE HashMap. A direct
   * implementation would be more efficient.
   */
  private HashMap<String, T> map;

  public JreMapFromStringTo() {
    map = new HashMap<String,T>();
  }

  JreMapFromStringTo(HashMap<String, T> map) {
    this.map = map;
  }


  @Override
  public T get(String key) {
    return map.get(key);
  }

  @Override
  public boolean hasKey(String key) {
    return map.containsKey(key);
  }

  @Override
  public ArrayOfString keys() {
    return new JreArrayOfString(new ArrayList<String>(map.keySet()));
  }

  @Override
  public void put(String key, T value) {
    map.put(key, value);
  }

  @Override
  public void remove(String key) {
    map.remove(key);
  }

  @Override
  public ArrayOf<T> values() {
    return new JreArrayOf<T>(new ArrayList<T>(map.values()));
  }
}
