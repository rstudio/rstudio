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

/**
 * An int multimap that cannot hold duplicate key-value pairs.
 * <p>
 * Because only int primitives are used performance and memory usage can surpass Object set
 * multimaps.
 */
public class IntHashMultimap extends IntMultimap {

  @Override
  public void put(int key, int value) {
    Object objectValues = map.get(key);
    if (objectValues != null) {
      IntArrayList listValues = (IntArrayList) objectValues;
      // Don't add duplicate values.
      if (!listValues.contains(value)) {
        listValues.add(value);
      }
    } else {
      IntArrayList listValues = new IntArrayList();
      listValues.add(value);
      map.put(key, listValues);
    }
  }
}
