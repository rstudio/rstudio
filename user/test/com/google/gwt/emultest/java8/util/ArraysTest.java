/*
 * Copyright 2016 Google Inc.
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
package com.google.gwt.emultest.java8.util;

import com.google.gwt.emultest.java.util.EmulTestBase;

import java.util.Arrays;

/**
 * Java 8 methods to test in java.util.Arrays.
 */
public class ArraysTest extends EmulTestBase {

  public void testParallelPrefix_double() {
    double[] array = {1};
    Arrays.parallelPrefix(array, Double::sum);
    assertTrue(Arrays.equals(new double[]{1}, array));

    array = new double[]{1, 2, 3};
    Arrays.parallelPrefix(array, Double::sum);
    assertTrue(Arrays.equals(new double[]{1, 3, 6}, array));

    array = new double[]{1, 2, 3};
    Arrays.parallelPrefix(array, 0, 0, Double::sum);
    assertTrue(Arrays.equals(new double[]{1, 2, 3}, array));

    array = new double[]{1, 2, 3, 4, 5};
    Arrays.parallelPrefix(array, 1, 4, Double::sum);
    assertTrue(Arrays.equals(new double[]{1, 2, 5, 9, 5}, array));
  }

  public void testParallelPrefix_int() {
    int[] array = {1};
    Arrays.parallelPrefix(array, Integer::sum);
    assertTrue(Arrays.equals(new int[]{1}, array));

    array = new int[]{1, 2, 3};
    Arrays.parallelPrefix(array, Integer::sum);
    assertTrue(Arrays.equals(new int[]{1, 3, 6}, array));

    array = new int[]{1, 2, 3};
    Arrays.parallelPrefix(array, 0, 0, Integer::sum);
    assertTrue(Arrays.equals(new int[]{1, 2, 3}, array));

    array = new int[]{1, 2, 3, 4, 5};
    Arrays.parallelPrefix(array, 1, 4, Integer::sum);
    assertTrue(Arrays.equals(new int[]{1, 2, 5, 9, 5}, array));
  }

  public void testParallelPrefix_long() {
    long[] array = {1};
    Arrays.parallelPrefix(array, Long::sum);
    assertTrue(Arrays.equals(new long[]{1}, array));

    array = new long[]{1, 2, 3};
    Arrays.parallelPrefix(array, Long::sum);
    assertTrue(Arrays.equals(new long[]{1, 3, 6}, array));

    array = new long[]{1, 2, 3};
    Arrays.parallelPrefix(array, 0, 0, Long::sum);
    assertTrue(Arrays.equals(new long[]{1, 2, 3}, array));

    array = new long[]{1, 2, 3, 4, 5};
    Arrays.parallelPrefix(array, 1, 4, Long::sum);
    assertTrue(Arrays.equals(new long[]{1, 2, 5, 9, 5}, array));
  }

  public void testParallelPrefix_Object() {
    String[] array = {"a"};
    Arrays.parallelPrefix(array, String::concat);
    assertEquals(new String[]{"a"}, array);

    array = new String[]{"a", "b", "c"};
    Arrays.parallelPrefix(array, String::concat);
    assertEquals(new String[]{"a", "ab", "abc"}, array);

    array = new String[]{"a", "b", "c"};
    Arrays.parallelPrefix(array, 0, 0, String::concat);
    assertEquals(new String[]{"a", "b", "c"}, array);

    array = new String[]{"a", "b", "c", "d", "e"};
    Arrays.parallelPrefix(array, 1, 4, String::concat);
    assertEquals(new String[]{"a", "b", "bc", "bcd", "e"}, array);
  }

  public void testSetAll_double() {
    double[] array = {};
    Arrays.setAll(array, i -> (double) i);
    assertTrue(Arrays.equals(new double[0], array));

    array = new double[]{0, 0, 0};
    Arrays.setAll(array, i -> (double) i + 1);
    assertTrue(Arrays.equals(new double[]{1, 2, 3}, array));
  }

  public void testSetAll_int() {
    int[] array = {};
    Arrays.setAll(array, i -> i);
    assertTrue(Arrays.equals(new int[0], array));

    array = new int[]{0, 0, 0};
    Arrays.setAll(array, i -> i + 1);
    assertTrue(Arrays.equals(new int[]{1, 2, 3}, array));
  }

  public void testSetAll_long() {
    long[] array = {};
    Arrays.setAll(array, i -> (long) i);
    assertTrue(Arrays.equals(new long[0], array));

    array = new long[]{0, 0, 0};
    Arrays.setAll(array, i -> (long) i + 1);
    assertTrue(Arrays.equals(new long[]{1, 2, 3}, array));
  }

  public void testSetAll_Object() {
    String[] array = {};
    Arrays.setAll(array, i -> "" + i);
    assertEquals(new String[0], array);

    array = new String[]{"a", "b", "c"};
    Arrays.setAll(array, i -> "" + (i + 1));
    assertEquals(new String[]{"1", "2", "3"}, array);
  }

  public void testArrayStreamInt() {
    int[] six = {1, 2, 3, 4, 5, 6};

    // zero entries
    assertEquals(new int[]{}, Arrays.stream(six, 0, 0).toArray());
    assertEquals(new int[]{}, Arrays.stream(six, 1, 1).toArray());

    // single entry
    assertEquals(new int[]{1}, Arrays.stream(six, 0, 1).toArray());
    assertEquals(new int[]{2}, Arrays.stream(six, 1, 2).toArray());

    // multiple entries
    assertEquals(new int[]{1, 2, 3}, Arrays.stream(six, 0, 3).toArray());
    assertEquals(new int[]{4, 5, 6}, Arrays.stream(six, 3, 6).toArray());

    try {
      // start < 0
      Arrays.stream(six, -1, 1);
      fail("expected aioobe");
    } catch (ArrayIndexOutOfBoundsException expected) {
      // expected
    }
    try {
      // end < start
      Arrays.stream(six, 2, 1);
      fail("expected aioobe");
    } catch (ArrayIndexOutOfBoundsException expected) {
      // expected
    }
    try {
      // end > size
      Arrays.stream(six, 0, 7);
      fail("expected aioobe");
    } catch (ArrayIndexOutOfBoundsException expected) {
      // expected
    }
  }

  public void testArrayStreamLong() {
    long[] six = {1, 2, 3, 4, 5, 6};

    // zero entries
    assertEquals(new long[]{}, Arrays.stream(six, 0, 0).toArray());
    assertEquals(new long[]{}, Arrays.stream(six, 1, 1).toArray());

    // single entry
    assertEquals(new long[]{1}, Arrays.stream(six, 0, 1).toArray());
    assertEquals(new long[]{2}, Arrays.stream(six, 1, 2).toArray());

    // multiple entries
    assertEquals(new long[]{1, 2, 3}, Arrays.stream(six, 0, 3).toArray());
    assertEquals(new long[]{4, 5, 6}, Arrays.stream(six, 3, 6).toArray());

    try {
      // start < 0
      Arrays.stream(six, -1, 1);
      fail("expected aioobe");
    } catch (ArrayIndexOutOfBoundsException expected) {
      // expected
    }
    try {
      // end < start
      Arrays.stream(six, 2, 1);
      fail("expected aioobe");
    } catch (ArrayIndexOutOfBoundsException expected) {
      // expected
    }
    try {
      // end > size
      Arrays.stream(six, 0, 7);
      fail("expected aioobe");
    } catch (ArrayIndexOutOfBoundsException expected) {
      // expected
    }
  }

  public void testArrayStreamDouble() {
    double[] six = {1, 2, 3, 4, 5, 6};

    // zero entries
    assertEquals(new double[]{}, Arrays.stream(six, 0, 0).toArray());
    assertEquals(new double[]{}, Arrays.stream(six, 1, 1).toArray());

    // single entry
    assertEquals(new double[]{1}, Arrays.stream(six, 0, 1).toArray());
    assertEquals(new double[]{2}, Arrays.stream(six, 1, 2).toArray());

    // multiple entries
    assertEquals(new double[]{1, 2, 3}, Arrays.stream(six, 0, 3).toArray());
    assertEquals(new double[]{4, 5, 6}, Arrays.stream(six, 3, 6).toArray());

    try {
      // start < 0
      Arrays.stream(six, -1, 1);
      fail("expected aioobe");
    } catch (ArrayIndexOutOfBoundsException expected) {
      // expected
    }
    try {
      // end < start
      Arrays.stream(six, 2, 1);
      fail("expected aioobe");
    } catch (ArrayIndexOutOfBoundsException expected) {
      // expected
    }
    try {
      // end > size
      Arrays.stream(six, 0, 7);
      fail("expected aioobe");
    } catch (ArrayIndexOutOfBoundsException expected) {
      // expected
    }
  }

  public void testArrayStreamObject() {
    String[] six = {"1", "2", "3", "4", "5", "6"};

    // zero entries
    assertEquals(new String[]{}, Arrays.stream(six, 0, 0).toArray());
    assertEquals(new String[]{}, Arrays.stream(six, 1, 1).toArray());

    // single entry
    assertEquals(new String[]{"1"}, Arrays.stream(six, 0, 1).toArray());
    assertEquals(new String[]{"2"}, Arrays.stream(six, 1, 2).toArray());

    // multiple entries
    assertEquals(new String[]{"1", "2", "3"}, Arrays.stream(six, 0, 3).toArray());
    assertEquals(new String[]{"4", "5", "6"}, Arrays.stream(six, 3, 6).toArray());

    try {
      // start < 0
      Arrays.stream(six, -1, 1);
      fail("expected aioobe");
    } catch (ArrayIndexOutOfBoundsException expected) {
      // expected
    }
    try {
      // end < start
      Arrays.stream(six, 2, 1);
      fail("expected aioobe");
    } catch (ArrayIndexOutOfBoundsException expected) {
      // expected
    }
    try {
      // end > size
      Arrays.stream(six, 0, 7);
      fail("expected aioobe");
    } catch (ArrayIndexOutOfBoundsException expected) {
      // expected
    }
  }
}
