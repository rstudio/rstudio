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

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Test LinkedList class.
 */
@SuppressWarnings("unchecked")
public class LinkedListTest extends ListTestBase {

  private static final class LinkedListWithRemoveRange extends LinkedList {
    @Override
    public void removeRange(int fromIndex, int toIndex) {
      super.removeRange(fromIndex, toIndex);
    }
  }

  public void testAddFirst() {
    // TODO(jat): implement
  }

  public void testAddLast() {
    // TODO(jat): implement
  }

  public void testElement() {
    // TODO(jat): implement
  }

  public void testGetFirst() {
    // TODO(jat): implement
  }

  public void testGetLast() {
    // TODO(jat): implement
  }

  public void testOffer() {
    // TODO(jat): implement
  }

  public void testPeek() {
    // TODO(jat): implement
  }

  public void testPoll() {
    // TODO(jat): implement
  }

  public void testRemove() {
    // TODO(jat): implement
  }

  public void testRemoveFirst() {
    // TODO(jat): implement
  }

  public void testRemoveLast() {
    // TODO(jat): implement
  }

  public void testRemoveRange() {
    LinkedListWithRemoveRange l = new LinkedListWithRemoveRange();
    for (int i = 0; i < 10; i++) {
      l.add(new Integer(i));
    }
    try {
      l.removeRange(-1, 1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }

    try {
      l.removeRange(2, 11);
      fail();
    } catch (NoSuchElementException expected) {
    }

    assertEquals(2, l.size());
    for (int i = 0; i < 2; i++) {
      Integer elem = (Integer) l.get(i);
      assertEquals(i, elem.intValue());
    }

    for (int i = 2; i < 10; i++) {
      l.add(new Integer(i));
    }

    l.removeRange(3, 5);
    assertEquals(8, l.size());
    for (int i = 0; i < 3; i++) {
      Integer elem = (Integer) l.get(i);
      assertEquals(i, elem.intValue());
    }
    for (int i = 3; i < 8; i++) {
      Integer elem = (Integer) l.get(i);
      assertEquals(i + 2, elem.intValue());
    }
  }

  @Override
  protected List makeEmptyList() {
    return new LinkedList();
  }
}
