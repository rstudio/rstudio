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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Tests ArrayList class (and by extension, AbstractList).
 */
@SuppressWarnings("unchecked")
public class ArrayListTest extends ListTestBase {

  private static final class ArrayListWithRemoveRange extends ArrayList {
    @Override
    public void removeRange(int fromIndex, int toIndex) {
      super.removeRange(fromIndex, toIndex);
    }
  }

  public void testAbstractListUnmodifiableFailedIteratorAddIndexCorruption() {
    ListIterator<String> i = new AbstractList<String>() {
      @Override
      public int size() {
        return 0;
      }

      @Override
      public String get(int index) {
        throw new IndexOutOfBoundsException();
      }
    }.listIterator();
    try {
      i.add("bar");
      fail();
    } catch (UnsupportedOperationException expected) {
      // add() is expected to fail but shouldn't put us in a inconsistent state.
      // See: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6533203
    }
    assertFalse(i.hasPrevious());
  }

  public void testRemoveRange() {
    ArrayListWithRemoveRange l = new ArrayListWithRemoveRange();
    for (int i = 0; i < 10; i++) {
      l.add(new Integer(i));
    }
    try {
      l.removeRange(-1, 1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }

    try {
      l.removeRange(2, 1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
      // JRE
    } catch (IllegalArgumentException expected) {
      // GWT emulation
    }

    try {
      l.removeRange(2, 11);
      fail();
    } catch (IndexOutOfBoundsException expected) {
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
    l.removeRange(0, 8);

    // Tests on empty ArrayList
    assertEquals(0, l.size());
    // removeRange(0, 0) is a special case in Java; it is always a no-op
    l.removeRange(0, 0);

    try {
      // (1, 1) is not a special case and undergoes bounds checking
      l.removeRange(1, 1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }

    try {
      l.removeRange(0, 1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }

    // Tests on 1-element ArrayList
    l.add(new Integer(1));
    assertEquals(1, l.size());
    l.removeRange(0, 0); // in-bounds no-op
    l.removeRange(1, 1); // in-bounds no-op
    try {
      l.removeRange(2, 2); // out-of-bounds no-op
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }

    l.removeRange(0, 1);
    assertEquals(0, l.size());
  }

  @Override
  protected List makeEmptyList() {
    return new ArrayList();
  }
}
