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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Test various collections.
 */
public class CollectionsTest extends EmulTestBase {

  public static List<Integer> createRandomList() {
    ArrayList<Integer> l = new ArrayList<Integer>();
    l.add(new Integer(5));
    l.add(new Integer(2));
    l.add(new Integer(3));
    l.add(new Integer(1));
    l.add(new Integer(4));
    return l;
  }

  public static List<String> createSortedList() {
    ArrayList<String> l = new ArrayList<String>();
    l.add("a");
    l.add("b");
    l.add("c");
    return l;
  }

  private static Entry<String, String> dummyEntry() {
    return Collections.singletonMap("foo", "bar").entrySet().iterator().next();
  }

  /**
   * Test Collections.binarySearch(List, Object).
   * 
   * Verify the following cases: empty List odd numbers of elements even numbers
   * of elements not found value larger than all elements not found value
   * smaller than all elements
   */
  public void testBinarySearchObject() {
    List<String> a1 = new ArrayList<String>();
    int ret = Collections.binarySearch(a1, "");
    assertEquals(-1, ret);
    List<String> a2 = new ArrayList<String>(Arrays.asList(new String[] {
        "a", "g", "y"}));
    ret = Collections.binarySearch(a2, "c");
    assertEquals(-2, ret);
    ret = Collections.binarySearch(a2, "y");
    assertEquals(2, ret);
    List<String> a3 = new ArrayList<String>(Arrays.asList(new String[] {
        "b", "c", "x", "y"}));
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
   * Verify the following cases: empty List odd numbers of elements even numbers
   * of elements not found value larger than all elements not found value
   * smaller than all elements null Comparator uses natural ordering
   */
  public void testBinarySearchObjectComparator() {
    Comparator<String> inverseSort = new Comparator<String>() {
      public int compare(String o1, String o2) {
        return o2.compareTo(o1);
      }
    };
    List<String> a1 = new ArrayList<String>();
    int ret = Collections.binarySearch(a1, "", inverseSort);
    assertEquals(-1, ret);
    List<String> a2 = new ArrayList<String>(Arrays.asList(new String[] {
        "y", "g", "a"}));
    ret = Collections.binarySearch(a2, "c", inverseSort);
    assertEquals(-3, ret);
    ret = Collections.binarySearch(a2, "a", inverseSort);
    assertEquals(2, ret);
    List<String> a3 = new ArrayList<String>(Arrays.asList(new String[] {
        "y", "x", "c", "b"}));
    ret = Collections.binarySearch(a3, "a", inverseSort);
    assertEquals(-5, ret);
    ret = Collections.binarySearch(a3, "z", inverseSort);
    assertEquals(-1, ret);
    ret = Collections.binarySearch(a3, "y", inverseSort);
    assertEquals(0, ret);

    List<String> a4 = new ArrayList<String>(Arrays.asList(new String[] {
        "a", "b", "c", "d", "e"}));
    ret = Collections.binarySearch(a4, "d", null); // should not NPE
    assertEquals(3, ret);
  }

  public void testEntrySetToArrayOversized() {
    Map<String, String> delegate = new HashMap<String, String>();
    delegate.put("key", "value");
    Map<String, String> unmodifiable = Collections.unmodifiableMap(delegate);

    @SuppressWarnings("unchecked")
    Entry<String, String>[] oversizedArray = new Entry[3];
    oversizedArray[0] = dummyEntry();
    oversizedArray[1] = dummyEntry();
    oversizedArray[2] = dummyEntry();

    Entry<String, String>[] result = unmodifiable.entrySet().toArray(
        oversizedArray);
    assertSame(result, oversizedArray);
    assertEquals("key", result[0].getKey());
    assertEquals("value", result[0].getValue());
    assertNull("The element after last should be null.", result[1]);
  }

  public void testFill() {
    List<String> a = createSortedList();
    Collections.fill(a, null);
    assertEquals(new Object[a.size()], a);

    List<Integer> b = createRandomList();
    Collections.fill(b, null);
    assertEquals(new Object[b.size()], b);
  }

  public void testReverse() {
    List<String> a = createSortedList();
    Collections.reverse(a);
    Object[] x = {"c", "b", "a"};
    assertEquals(x, a);

    List<Integer> b = createRandomList();
    Collections.reverse(b);
    Collections.reverse(b);
    assertEquals(b, createRandomList());
  }

  public void testSort() {
    List<String> a = createSortedList();
    Collections.reverse(a);
    Collections.sort(a);
    assertEquals(createSortedList(), a);
  }

  public void testSortWithComparator() {
    Comparator<String> x = new Comparator<String>() {
      public int compare(String s1, String s2) {
        // sort into reverse order
        return s2.compareTo(s1);
      }
    };
    List<String> a = createSortedList();
    Collections.sort(a, x);
    Object[] expected = {"c", "b", "a"};
    assertEquals(expected, a);
  }

  public void testToArray() {
    List<Integer> testList = createRandomList();
    Integer[] testArray = new Integer[testList.size()];
    testList.toArray(testArray);
    for (int i = 0; i < testList.size(); ++i) {
      Integer val = testList.get(i);
      assertEquals(val, testArray[i]);
    }
  }
}
