/*
 * Copyright 2006 Google Inc.
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

import com.google.gwt.core.client.GWT;

import org.apache.commons.collections.TestArrayList;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/** Tests ArrayList, and, by extension AbstractList.  Uses inheritance to 
 * inherit all of Apache's TestList and TestCollection. */
public class ArrayListTest extends TestArrayList {
  public ArrayListTest() {
  }

  public void testAddWatch() {
    ArrayList s = new ArrayList();
    s.add("watch");
    assertEquals(s.get(0), "watch");
  }

  public void testListIteratorAddInSeveralPositions() {
    ArrayList l = new ArrayList();
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
    ArrayList l = new ArrayList();
    l.add(new Integer(1));
    ListIterator i = l.listIterator(0);
    try {
      i = l.listIterator(1);
    } catch (IndexOutOfBoundsException e) {
      // expected
    }
    try {
      i = l.listIterator(-1);
    } catch (IndexOutOfBoundsException e) {
      // expected
    }    
  }
  
  public void testListIteratorHasNextHasPreviousAndIndexes() {
    List l = new ArrayList();
    ListIterator i = l.listIterator();
    assertFalse(i.hasNext());
    assertFalse(i.hasPrevious());
    i.add(new Integer(1));
    assertEquals(1,i.nextIndex());
    assertEquals(0, i.previousIndex());
    i = l.listIterator();
    assertEquals(0,i.nextIndex());
    assertEquals(-1, i.previousIndex());
    assertTrue(i.hasNext());
    assertFalse(i.hasPrevious());
    i.next();
    assertEquals(1,i.nextIndex());
    assertEquals(0, i.previousIndex());
    assertFalse(i.hasNext());
    assertTrue(i.hasPrevious());    
  }
  
  public void testListIteratorSetInSeveralPositions() {
    ArrayList l = new ArrayList();
    for (int n = 0; n < 5; n += 2) {
      l.add(new Integer(n));
    }
    ListIterator i = l.listIterator();
    for (int n = 0; n < 3; n++) {
      l.set(n, new Integer(n));
    }
    for (int n = 0; n < 3; n++) {
      assertEquals(new Integer(n), l.get(n));
    }
  }
  
  public void testRemoveRange() {
    if (GWT.isScript()) {
      ArrayList l = new ArrayList();
      for (int i = 0; i < 10; i++) {
        l.add(new Integer(i));
      }
      verifyRemoveRangeWorks(l);
    }
  }
  
  protected List makeEmptyList() {
    return new ArrayList();
  }

  private native void verifyRemoveRangeWorks(ArrayList l) /*-{
    var startIndex = l.@java.util.ArrayList::startIndex;
    var endIndex = l.@java.util.ArrayList::endIndex;
    var array = l.@java.util.ArrayList::array;
    l.@java.util.ArrayList::removeRange(II)(0,2);
    if (array[startIndex] !== undefined) {
      @junit.framework.Assert::fail(Ljava/lang/String;)("startIndex should be empty");
    }
    if (array[startIndex + 1] !== undefined) {
      @junit.framework.Assert::fail(Ljava/lang/String;)("startIndex + 1 should be empty");
    }
    if (array[startIndex + 2] === undefined) {
      @junit.framework.Assert::fail(Ljava/lang/String;)("startIndex + 2 should not be empty");
    }
    l.@java.util.ArrayList::removeRange(II)(6,8);
    if (array[endIndex - 3] === undefined) {
      @junit.framework.Assert::fail(Ljava/lang/String;)("endIndex - 3 should not be empty");
    }
    if (array[endIndex - 2] !== undefined) {
      @junit.framework.Assert::fail(Ljava/lang/String;)("endIndex - 2 should be empty");
    }
    if (array[endIndex - 1] !== undefined) {
      @junit.framework.Assert::fail(Ljava/lang/String;)("endIndex - 1 should be empty");
    }
  }-*/;
}
