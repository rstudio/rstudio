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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Arrays;

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

  /**
   * Test Collections.binarySearch(List, Object).
   *
   * Verify the following cases:
   *   empty List
   *   odd numbers of elements
   *   even numbers of elements
   *   not found value larger than all elements
   *   not found value smaller than all elements
   */
  public void testBinarySearchObject() {
    List a1 = new ArrayList();
    int ret = Collections.binarySearch(a1, "");
    assertEquals(-1, ret);
    List a2 = new ArrayList(Arrays.asList(new String[] {"a", "g", "y"}));
    ret = Collections.binarySearch(a2, "c");
    assertEquals(-2, ret);
    ret = Collections.binarySearch(a2, "y");
    assertEquals(2, ret);
    List a3 = new ArrayList(Arrays.asList(new String[]{"b", "c", "x", "y"}));
    ret = Collections.binarySearch(a3, "z");
    assertEquals(-5, ret);
    ret = Collections.binarySearch(a3, "a");
    assertEquals(-1, ret);
    ret = Collections.binarySearch(a3, "b");
    assertEquals(0, ret);
  }

  /**
   * Test Collections.binarySearch(List, Object, Comparator).
   *
   * Verify the following cases:
   *   empty List
   *   odd numbers of elements
   *   even numbers of elements
   *   not found value larger than all elements
   *   not found value smaller than all elements
   *   null Comparator uses natural ordering
   */
  public void testBinarySearchObjectComparator() {
    Comparator inverseSort = new Comparator() {
      public int compare(Object o1, Object o2) {
        return ((Comparable) o2).compareTo(o1);
      }
    };
    List a1 = new ArrayList();
    int ret = Collections.binarySearch(a1, "", inverseSort);
    assertEquals(-1, ret);
    List a2 = new ArrayList(Arrays.asList(new String[]{"y", "g", "a"}));
    ret = Collections.binarySearch(a2, "c", inverseSort);
    assertEquals(-3, ret);
    ret = Collections.binarySearch(a2, "a", inverseSort);
    assertEquals(2, ret);
    List a3 = new ArrayList(Arrays.asList(new String[]{"y", "x", "c", "b"}));
    ret = Collections.binarySearch(a3, "a", inverseSort);
    assertEquals(-5, ret);
    ret = Collections.binarySearch(a3, "z", inverseSort);
    assertEquals(-1, ret);
    ret = Collections.binarySearch(a3, "y", inverseSort);
    assertEquals(0, ret);

    List a4 = new ArrayList(Arrays.asList(
        new String[]{"a", "b", "c", "d", "e"}));
    ret = Collections.binarySearch(a4, "d", null); // should not NPE
    assertEquals(3, ret);
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

  public void testSortWithComparator() {
    Comparator x = new Comparator() {
      public int compare(Object o1, Object o2) {
        String s1 = (String) o1;
        String s2 = (String) o2;
        // sort into reverse order
        return s2.compareTo(s1);
      }
    };
    List a = createSortedList();
    Collections.sort(a, x);
    Object[] expected = {"c", "b", "a"};
    assertEquals(expected, a);
  }

}
