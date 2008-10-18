/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.emultest.java.util;

import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;

/**
 * Tests <code>TreeMap</code> with a <code>Comparator</code>.
 */
public class TreeMapStringStringWithComparatorTest extends
    TreeMapStringStringTest {
  @Override
  protected SortedMap<String, String> createSortedMap() {
    setComparator(new Comparator<String>() {
      public int compare(String o1, String o2) {
        if (o1 == null) {
          return o2 == null ? 0 : -1;
        }
        if (o2 == null) {
          return 1;
        }
        return o1.compareTo(o2);
      }
    });
    return super.createSortedMap();
  }

  @Override
  protected Map<String, String> makeEmptyMap() {
    return createSortedMap();
  }

  @Override
  public boolean useNullKey() {
    return true;
  }
}
