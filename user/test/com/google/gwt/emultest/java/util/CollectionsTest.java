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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * TODO: document me.
 */
public class CollectionsTest extends EmulTestBase {

  public static List createSortedList() {
    ArrayList l = new ArrayList();
    l.add("a");
    l.add("b");
    l.add("c");
    return l;
  }

  public static List createRandomList() {
    ArrayList l = new ArrayList();
    l.add(new Integer(5));
    l.add(new Integer(2));
    l.add(new Integer(3));
    l.add(new Integer(1));
    l.add(new Integer(4));
    return l;
  }

  public void testReverse() {
    List a = createSortedList();
    Collections.reverse(a);
    Object[] x = {"c", "b", "a"};
    assertEquals(x, a);

    List b = createRandomList();
    Collections.reverse(b);
    Collections.reverse(b);
    assertEquals(b, createRandomList());
  }

  public void testSort() {
    List a = createSortedList();
    Collections.reverse(a);
    Collections.sort(a);
    assertEquals(createSortedList(), a);
  }

  public static void testSortWithComparator() {
    Comparator x = new Comparator() {

      public int compare(Object o1, Object o2) {
        Object[] schema = {"b", new Integer(5), "c", new Integer(4)};
        List l = Arrays.asList(schema);
        int first = l.indexOf(o1);
        int second = l.indexOf(o2);
        if (first < second) {
          return -1;
        } else if (first == second) {
          return 0;
        } else {
          return 1;
        }
      }
    };
    List a = createSortedList();
    Collections.sort(a, x);
    Object[] expected = {"b", "c", "a"};
    assertEquals(expected, a);
  }

}
