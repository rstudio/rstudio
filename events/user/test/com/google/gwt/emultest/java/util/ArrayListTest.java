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
import java.util.List;

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
  }

  @Override
  protected List makeEmptyList() {
    return new ArrayList();
  }
}
