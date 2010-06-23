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

import org.apache.commons.collections.TestArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

/**
 * Tests List, and, by extension AbstractList. Uses inheritance to inherit all
 * of Apache's TestList and TestCollection.
 */
@SuppressWarnings("unchecked")
public abstract class ListTestBase extends TestArrayList {

  private static volatile boolean NO_OPTIMIZE_FALSE = false;

  public void testAddAll() {
    assertFalse(makeEmptyList().addAll(makeEmptyList()));
    assertTrue(makeEmptyList().addAll(makeFullList()));
    assertFalse(makeEmptyList().addAll(0, makeEmptyList()));
    assertTrue(makeEmptyList().addAll(0, makeFullList()));
    assertFalse(makeFullList().addAll(makeEmptyList()));
    assertTrue(makeFullList().addAll(makeFullList()));
    assertFalse(makeFullList().addAll(1, makeEmptyList()));
    assertTrue(makeFullList().addAll(1, makeFullList()));
  }

  public void testAddWatch() {
    List s = makeEmptyList();
    s.add("watch");
    assertEquals(s.get(0), "watch");
  }

  public void testListIteratorAddInSeveralPositions() {
    List l = makeEmptyList();
    ListIterator i = l.listIterator();
    l.add(new Integer(0));
    for (int n = 2; n < 5; n += 2) {
      l.add(new Integer(n));
    }
    i = l.listIterator();
    i.next();
    i.add(new Integer(1));
    i.next();
    i.next();
    i.previous();
    i.add(new Integer(3));
    i.next();
    i.add(new Integer(5));
    i.add(new Integer(6));
    for (int n = 0; n < 6; n++) {
      assertEquals(new Integer(n), l.get(n));
    }
  }

  public void testListIteratorCreateInvalid() {
    List l = makeEmptyList();
    l.add(new Integer(1));
    l.listIterator(0);
    try {
      l.listIterator(1);
    } catch (IndexOutOfBoundsException e) {
      // expected
    }
    try {
      l.listIterator(-1);
    } catch (IndexOutOfBoundsException e) {
      // expected
    }
  }

  public void testListIteratorHasNextHasPreviousAndIndexes() {
    List l = makeEmptyList();
    ListIterator i = l.listIterator();
    assertFalse(i.hasNext());
    assertFalse(i.hasPrevious());
    i.add(new Integer(1));
    assertEquals(1, i.nextIndex());
    assertEquals(0, i.previousIndex());
    i = l.listIterator();
    assertEquals(0, i.nextIndex());
    assertEquals(-1, i.previousIndex());
    assertTrue(i.hasNext());
    assertFalse(i.hasPrevious());
    i.next();
    assertEquals(1, i.nextIndex());
    assertEquals(0, i.previousIndex());
    assertFalse(i.hasNext());
    assertTrue(i.hasPrevious());
  }

  public void testListIteratorRemove() {
    // TODO(jat): implement
  }

  public void testListIteratorSetInSeveralPositions() {
    List l = makeEmptyList();
    for (int n = 0; n < 5; n += 2) {
      l.add(new Integer(n));
    }
    l.listIterator();
    for (int n = 0; n < 3; n++) {
      l.set(n, new Integer(n));
    }
    for (int n = 0; n < 3; n++) {
      assertEquals(new Integer(n), l.get(n));
    }
  }

  public void testRemoveAllDuplicates() {
    Collection c = makeCollection();
    c.add("a");
    c.add("a");
    Collection d = makeCollection();
    d.add("a");
    assertTrue(c.removeAll(d));
    assertEquals(0, c.size());
  }

  public void testSubList() {
    List<Integer> wrappedList = createListWithContent(new int[] {1, 2, 3, 4, 5});
    List<Integer> testList = wrappedList.subList(1, 4);
    assertEquals(3, testList.size());

    assertEquals(testList, Arrays.asList(2, 3, 4));
    checkListSizeAndContent(testList, 2, 3, 4);
    testList.add(1, 6);
    assertEquals(testList, Arrays.asList(2, 6, 3, 4));
    checkListSizeAndContent(testList, 2, 6, 3, 4);
    assertEquals(wrappedList, Arrays.asList(1, 2, 6, 3, 4, 5));
    checkListSizeAndContent(wrappedList, 1, 2, 6, 3, 4, 5);
    testList.remove(2);
    assertEquals(testList, Arrays.asList(2, 6, 4));
    checkListSizeAndContent(testList, 2, 6, 4);

    try {
      testList.remove(3);
      fail("Expected remove to fail");
    } catch (IndexOutOfBoundsException e) {
    }

    checkListSizeAndContent(wrappedList, 1, 2, 6, 4, 5);
    testList.set(0, 7);
    checkListSizeAndContent(testList, 7, 6, 4);
    checkListSizeAndContent(wrappedList, 1, 7, 6, 4, 5);

    try {
      wrappedList.subList(-1, 5);
      fail("expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
    }

    try {
      wrappedList.subList(0, 15);
      fail("expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
    }

    try {
      wrappedList.subList(5, 1);
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
    }

    try {
      wrappedList.subList(0, 1).add(2, 5);
      fail("expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
    }

    try {
      wrappedList.subList(0, 1).add(-1, 5);
      fail("expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
    }

    try {
      wrappedList.subList(0, 1).get(1);
      fail("expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
    }

    try {
      wrappedList.subList(0, 1).get(-1);
      fail("expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
    }

    try {
      wrappedList.subList(0, 1).set(2, 2);
      fail("expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
    }

    try {
      wrappedList.subList(0, 1).set(-1, 5);
      fail("expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
    }
  }

  /**
   * Test add() method for list returned by List<E>.subList() method.
   */
  public void testSubListAdd() {
    List<Integer> baseList = createListWithContent(new int[] {1, 2, 3, 4, 5});
    List<Integer> sublist = baseList.subList(1, 3);
    assertEquals(2, sublist.size());

    sublist.add(33);
    sublist.add(34);

    assertEquals(4, sublist.size());
    assertEquals(7, baseList.size());

    /*
     * Assert correct values are found right before and after insertion. We're
     * checking the original list (baseList) even though the changes were made
     * in the sublist. That is because modifications made to the list returned
     * by sublist method should reflect in the original list.
     */
    assertEquals(3, (int) baseList.get(2));
    assertEquals(33, (int) baseList.get(3));
    assertEquals(34, (int) baseList.get(4));
    assertEquals(4, (int) baseList.get(5));

    /*
     * Assert that it is possible to add element at the beginning of the
     * sublist.
     */
    sublist.add(0, 22);
    sublist.add(0, 21);
    checkListSizeAndContent(sublist, 21, 22, 2, 3, 33, 34);
    checkListSizeAndContent(baseList, 1, 21, 22, 2, 3, 33, 34, 4, 5);

    // check adding at the end by specifying the index
    sublist.add(6, 35);
    checkListSizeAndContent(sublist, 21, 22, 2, 3, 33, 34, 35);
    checkListSizeAndContent(baseList, 1, 21, 22, 2, 3, 33, 34, 35, 4, 5);

    /*
     * Assert adding to underlying list after the sublist view has been defined.
     * After such an action behavior of sublist is undefined.
     */
    baseList.add(9, 44);
    baseList.add(55);
    baseList.add(0, 10);
    checkListSizeAndContent(baseList, 10, 1, 21, 22, 2, 3, 33, 34, 35, 4, 44, 5, 55);
  }

  public void testSubListRemove() {
    List<Integer> baseList = createListWithContent(new int[] {1, 2, 3, 4, 5});
    List<Integer> sublist = baseList.subList(1, 3);

    sublist.remove(0);
    assertEquals(4, baseList.size());
    assertEquals(3, baseList.get(1).intValue());

    try {
      sublist.remove(1);
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException expected) {
    }
    
    assertFalse(sublist.remove(Integer.valueOf(4)));
    
    assertTrue(sublist.remove(Integer.valueOf(3)));
    
    assertEquals(0, sublist.size());
    assertEquals(3, baseList.size());
    
    sublist.add(6);
    checkListSizeAndContent(baseList, 1, 6, 4, 5);
  }

  public void testToArray() {
    List l = makeEmptyList();
    for (int i = 0; i < 10; i++) {
      l.add(new Integer(i));
    }

    {
      Object[] objArray = l.toArray();
      assertEquals(10, objArray.length);
      for (int i = 0; i < 10; i++) {
        Integer elem = (Integer) objArray[i];
        assertEquals(i, elem.intValue());
      }
      // should not cause ArrayStore
      objArray[0] = new Object();
    }

    {
      Integer[] firstArray = new Integer[13];
      firstArray[10] = new Integer(10);
      firstArray[11] = new Integer(11);
      firstArray[12] = new Integer(12);
      Integer[] intArray = (Integer[]) l.toArray(firstArray);
      assertTrue(firstArray == intArray);
      assertEquals(13, intArray.length);
      for (int i = 0; i < 13; i++) {
        if (i == 10) {
          assertNull(intArray[i]);
        } else {
          Integer elem = intArray[i];
          assertEquals(i, elem.intValue());
        }
      }
      try {
        Object[] objArray = NO_OPTIMIZE_FALSE ? new Object[1] : intArray;
        assertTrue(objArray instanceof Integer[]);
        objArray[0] = new Object();
        fail("expected ArrayStoreException");
      } catch (ArrayStoreException e) {
      }
    }

    {
      Integer[] firstArray = new Integer[0];
      Integer[] intArray = (Integer[]) l.toArray(firstArray);
      assertFalse(firstArray == intArray);
      assertEquals(10, intArray.length);
      for (int i = 0; i < 10; i++) {
        Integer elem = intArray[i];
        assertEquals(i, elem.intValue());
      }
      try {
        Object[] objArray = NO_OPTIMIZE_FALSE ? new Object[1] : intArray;
        assertTrue(objArray instanceof Integer[]);
        objArray[0] = new Object();
        fail("expected ArrayStoreException");
      } catch (ArrayStoreException e) {
      }
    }
  }

  private void checkListSizeAndContent(List<Integer> in, int... expected) {
    assertEquals(expected.length, in.size());
    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i], (int) in.get(i));
    }
  }

  private List<Integer> createListWithContent(int[] in) {
    List<Integer> results = new ArrayList<Integer>();
    for (int i = 0; i < in.length; i++) {
      results.add(in[i]);
    }
    return results;
  }
}
