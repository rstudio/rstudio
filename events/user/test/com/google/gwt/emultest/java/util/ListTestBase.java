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

  public void testListIteratorRemove() {
    // TODO(jat): implement
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
}
