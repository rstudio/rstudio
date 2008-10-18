/*
 * Copyright 2007 Google Inc.
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

import org.apache.commons.collections.TestComparator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * TODO: document me.
 */
public class ComparatorTest extends TestComparator {
  public Comparator makeComparator() {
    return new DummyComparator();
  }

  public List getComparableObjectsOrdered() {
    List l = new ArrayList();
    l.add("x");
    l.add("y");
    l.add("z");
    return l;
  }
}

/**
 * List comparator for testing.
 */
class DummyComparator implements Comparator {
  /**
   * Compares returns reverse hash order.
   */
  public int compare(Object arg0, Object arg1) {
    int a = arg0.hashCode();
    int b = arg1.hashCode();
    int accum;
    if (a == b) {
      accum = 0;
    } else if (a > b) {
      accum = 1;
    } else {
      accum = -1;
    }
    return accum;
  }
}