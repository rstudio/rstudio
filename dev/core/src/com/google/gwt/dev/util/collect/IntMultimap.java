/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.util.collect;

import cern.colt.list.IntArrayList;
import cern.colt.map.OpenIntObjectHashMap;

import java.io.Serializable;

/**
 * A collection that maps individual int keys to multiple int values.
 * <p>
 * Because only int primitives are used performance and memory usage can surpass Object multimaps.
 */
public class IntMultimap implements Serializable {

  protected OpenIntObjectHashMap map = new OpenIntObjectHashMap();

  public void clear() {
    map.clear();
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof IntMultimap) {
      IntMultimap that = (IntMultimap) object;

      if (map.size() != that.map.size()) {
        return false;
      }

      // Colt's OpenIntObjectHashMap.equals() is broken, so check the contents manually.
      IntArrayList keys = map.keys();
      for (int i = 0; i < keys.size(); i++) {
        int key = keys.get(i);
        if (!get(key).equals(that.get(key))) {
          return false;
        }
      }

      return true;
    }
    return false;
  }

  public IntArrayList get(int key) {
    return (IntArrayList) map.get(key);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;

    IntArrayList keys = map.keys();
    for (int i = 0; i < keys.size(); i++) {
      int key = keys.get(i);
      result = prime * result + key;

      IntArrayList values = get(key);
      if (values == null) {
        continue;
      }
      for (int j = 0; j < values.size(); j++) {
        int value = values.get(j);
        result = prime * result + value;
      }
    }

    return result;
  }

  public IntArrayList keys() {
    return map.keys();
  }

  public void put(int key, int value) {
    Object objectValues = map.get(key);
    if (objectValues != null) {
      IntArrayList listValues = (IntArrayList) objectValues;
      listValues.add(value);
    } else {
      IntArrayList listValues = new IntArrayList();
      listValues.add(value);
      map.put(key, listValues);
    }
  }

  public void putAll(IntMultimap thatMap) {
    IntArrayList keys = thatMap.map.keys();
    for (int key : keys.elements()) {
      IntArrayList values = (IntArrayList) thatMap.map.get(key);
      map.put(key, values.copy());
    }
  }

  public IntArrayList remove(int key) {
    IntArrayList values = get(key);
    map.removeKey(key);
    return values;
  }

  public boolean remove(int key, int value) {
    IntArrayList values = get(key);
    if (values != null) {
      int valueIndex = values.indexOf(value);
      if (valueIndex != -1) {
        values.remove(valueIndex);
        return true;
      }
    }
    return false;
  }
}
