/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.resources.rg;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Tests CssResourceGenerator's generation of unique CSS class names.
 */
public class CssClassNamesTestCase extends TestCase {
  static class ConstantCounter extends Counter {
    @Override
    int next() {
      return 16;
    }
  }

  private static final SortedSet<String> EMPTY_SET = new TreeSet<String>();
  private static final int NUM_CYCLES = 1000;

  public void testPrefix() {
    assertEquals("p-A", CssResourceGenerator.computeObfuscatedClassName("p-",
        new Counter(), EMPTY_SET));
  }

  public void testReservedPrefixes() {
    Counter counter = new ConstantCounter();
    SortedSet<String> hateful = new TreeSet<String>(Arrays.asList("a"));

    // Value with no prefixes
    assertEquals("AB", CssResourceGenerator.computeObfuscatedClassName("",
        counter, EMPTY_SET));

    assertEquals("CZB", CssResourceGenerator.computeObfuscatedClassName("",
        counter, hateful));

    hateful.add("c");
    assertEquals("EZZB", CssResourceGenerator.computeObfuscatedClassName("",
        counter, hateful));

    hateful.add("ezz");
    assertEquals("KVAZB", CssResourceGenerator.computeObfuscatedClassName("",
        counter, hateful));
  }

  /**
   * Quick sanity check to ensure that the initial sequence of idents is unique
   * and stable.
   */
  public void testSimple() {
    Counter counter = new Counter();
    Counter counter2 = new Counter();
    Set<String> seen = new HashSet<String>();

    for (int i = 0; i < NUM_CYCLES; i++) {
      String ident = CssResourceGenerator.computeObfuscatedClassName("",
          counter, EMPTY_SET);
      assertTrue(seen.add(ident));

      assertEquals(ident, CssResourceGenerator.computeObfuscatedClassName("",
          counter2, EMPTY_SET));
    }
    assertEquals(1000, counter.next());
  }
}
