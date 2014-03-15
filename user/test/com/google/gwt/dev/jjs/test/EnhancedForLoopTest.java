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
package com.google.gwt.dev.jjs.test;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests the new JDK 1.5 enhanced for loop.
 */
public class EnhancedForLoopTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testArray() {
    String[] items = new String[] {"1", "2", "3", "4", "5"};
    List<String> out = new ArrayList<String>();
    for (String i : items) {
      out.add(i);
    }
    assertTrue(out.equals(Arrays.asList(items)));
  }

  public void testBoxing() {
    List<Integer> items = Arrays.asList(1, 2, 3, 4, 5);
    List<Integer> out = new ArrayList<Integer>();
    for (int i : items) {
      out.add(i);
    }
    assertTrue(out.equals(items));

    // Tests auto-unboxing.
    List<Long> itemsL = Arrays.asList(1L, 2L, 3L, 4L, 5L);
    List<Long> outL = new ArrayList<Long>();
    for (long l : items) {
      outL.add(l);
    }
    assertTrue(outL.equals(itemsL));

    int[] unboxedItems = new int[] {1, 2, 3, 4, 5};
    out.clear();

    for (Integer i : unboxedItems) {
      out.add(i);
    }

    // Tests auto-boxing.
    for (int i = 0; i < 5; ++i) {
      assertTrue(out.get(i).intValue() == unboxedItems[i]);
    }
  }

  public void testIterable() {
    Iterable<Integer> it = Arrays.asList(1, 2, 3, 4, 5);
    List<Integer> out = new ArrayList<Integer>();
    for (Integer i : it) {
      out.add(i);
    }
    assertTrue(it.equals(out));
  }

  public void testList() {
    List<String> items = Arrays.asList("1", "2", "3", "4", "5");
    List<String> out = new ArrayList<String>();
    for (String i : items) {
      out.add(i);
    }
    assertTrue(out.equals(items));
  }
}
