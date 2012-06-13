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
import elemental.util.ArrayOfInt;
import elemental.util.MapFromIntTo;

/**
 * JRE implementation of MapFromIntTo for server and dev mode.
 */
public class JreMapFromIntTo<T> implements MapFromIntTo<T> {

  /*
   * TODO(cromwellian): this implemation uses JRE HashMap. A direct
   * implementation would be more efficient.
   */
  private HashMap<Integer, T> map;

  public JreMapFromIntTo() {
    map = new HashMap<Integer,T>();
  }

  JreMapFromIntTo(HashMap<Integer, T> map) {
    this.map = map;
  }

  @Override
  public T get(int key) {
    return map.get(key);
  }

  @Override
  public boolean hasKey(int key) {
    return map.containsKey(key);
  }

  @Override
  public ArrayOfInt keys() {
    return new JreArrayOfInt(new JreArrayOf<Integer>(new ArrayList<Integer>(map.keySet())));
  }

  @Override
  public void put(int key, T value) {
    map.put(key, value);
  }

  @Override
  public void remove(int key) {
    map.remove(key);
  }

  @Override
  public ArrayOf<T> values() {
    return new JreArrayOf<T>(new ArrayList<T>(map.values()));
  }
}
