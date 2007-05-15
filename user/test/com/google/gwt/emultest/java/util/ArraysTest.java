/*
 * Copyright 2007 Google Inc.
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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Tests {@link Arrays}.
 */
public class ArraysTest extends EmulTestBase {
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  /**
   * Tests {@link Arrays#asList(Object[])}.
   */
  public void testAsList() {
    // 0
    Object[] test = {};
    List result = Arrays.asList(test);
    assertEquals(test, result);
    // n
    Object[] test2 = {new Integer(0), new Integer(1), new Integer(2)};
    List result2 = Arrays.asList(test2);
    assertEquals(test2, result2);
    // 1
    Object[] test3 = {"Hello"};
    List result3 = Arrays.asList(test3);
    assertEquals(test3, result3);
  }

  /**
   * Test Arrays.binarySearch(byte[], byte).
   * 
   * Verify the following cases:
   *   empty array
   *   odd numbers of elements
   *   even numbers of elements
   *   not found value larger than all elements
   *   not found value smaller than all elements
   *   negative values
   */
  public void testBinarySearchByte() {
    byte[] a1 = {};
    int ret = Arrays.binarySearch(a1, (byte) 0);
    assertEquals(-1, ret);
    byte[] a2 = {1, 7, 31};
    ret = Arrays.binarySearch(a2, (byte) 3);
    assertEquals(-2, ret);
    ret = Arrays.binarySearch(a2, (byte) 31);
    assertEquals(2, ret);
    byte[] a3 = {-71, 0, 35, 36};
    ret = Arrays.binarySearch(a3, (byte) 42);
    assertEquals(-5, ret);
    ret = Arrays.binarySearch(a3, (byte) -80);
    assertEquals(-1, ret);
    ret = Arrays.binarySearch(a3, (byte) -71);
    assertEquals(0, ret);    
  }

  /**
   * Test Arrays.binarySearch(char[], char).
   * 
   * Verify the following cases:
   *   empty array
   *   odd numbers of elements
   *   even numbers of elements
   *   not found value larger than all elements
   *   not found value smaller than all elements
   */
  public void testBinarySearchChar() {
    char[] a1 = {};
    int ret = Arrays.binarySearch(a1, (char) 0);
    assertEquals(-1, ret);
    char[] a2 = {1, 7, 31};
    ret = Arrays.binarySearch(a2, (char) 3);
    assertEquals(-2, ret);
    ret = Arrays.binarySearch(a2, (char) 31);
    assertEquals(2, ret);
    char[] a3 = {1, 2, 35, 36};
    ret = Arrays.binarySearch(a3, (char) 42);
    assertEquals(-5, ret);
    ret = Arrays.binarySearch(a3, (char) 0);
    assertEquals(-1, ret);
    ret = Arrays.binarySearch(a3, (char) 1);
    assertEquals(0, ret);    
  }

  /**
   * Test Arrays.binarySearch(double[], double).
   * 
   * Verify the following cases:
   *   empty array
   *   odd numbers of elements
   *   even numbers of elements
   *   not found value larger than all elements
   *   not found value smaller than all elements
   *   negative values
   */
  public void testBinarySearchDouble() {
    double[] a1 = {};
    int ret = Arrays.binarySearch(a1, 0);
    assertEquals(-1, ret);
    double[] a2 = {1, 7, 31};
    ret = Arrays.binarySearch(a2, 3);
    assertEquals(-2, ret);
    ret = Arrays.binarySearch(a2, 31);
    assertEquals(2, ret);
    double[] a3 = {-71, 0, 35, 36};
    ret = Arrays.binarySearch(a3, 42);
    assertEquals(-5, ret);
    ret = Arrays.binarySearch(a3, -80);
    assertEquals(-1, ret);
    ret = Arrays.binarySearch(a3, -71);
    assertEquals(0, ret);    
  }

  /**
   * Test Arrays.binarySearch(float[], float).
   * 
   * Verify the following cases:
   *   empty array
   *   odd numbers of elements
   *   even numbers of elements
   *   not found value larger than all elements
   *   not found value smaller than all elements
   *   negative values
   */
  public void testBinarySearchFloat() {
    float[] a1 = {};
    int ret = Arrays.binarySearch(a1, 0);
    assertEquals(-1, ret);
    float[] a2 = {1, 7, 31};
    ret = Arrays.binarySearch(a2, 3);
    assertEquals(-2, ret);
    ret = Arrays.binarySearch(a2, 31);
    assertEquals(2, ret);
    float[] a3 = {-71, 0, 35, 36};
    ret = Arrays.binarySearch(a3, 42);
    assertEquals(-5, ret);
    ret = Arrays.binarySearch(a3, -80);
    assertEquals(-1, ret);
    ret = Arrays.binarySearch(a3, -71);
    assertEquals(0, ret);    
  }

  /**
   * Test Arrays.binarySearch(int[], int).
   * 
   * Verify the following cases:
   *   empty array
   *   odd numbers of elements
   *   even numbers of elements
   *   not found value larger than all elements
   *   not found value smaller than all elements
   *   negative values
   */
  public void testBinarySearchInt() {
    int[] a1 = {};
    int ret = Arrays.binarySearch(a1, 0);
    assertEquals(-1, ret);
    int[] a2 = {1, 7, 31};
    ret = Arrays.binarySearch(a2, 3);
    assertEquals(-2, ret);
    ret = Arrays.binarySearch(a2, 31);
    assertEquals(2, ret);
    int[] a3 = {-71, 0, 35, 36};
    ret = Arrays.binarySearch(a3, 42);
    assertEquals(-5, ret);
    ret = Arrays.binarySearch(a3, -80);
    assertEquals(-1, ret);
    ret = Arrays.binarySearch(a3, -71);
    assertEquals(0, ret);    
  }

  /**
   * Test Arrays.binarySearch(long[], long).
   * 
   * Verify the following cases:
   *   empty array
   *   odd numbers of elements
   *   even numbers of elements
   *   not found value larger than all elements
   *   not found value smaller than all elements
   *   negative values
   */
  public void testBinarySearchLong() {
    long[] a1 = {};
    int ret = Arrays.binarySearch(a1, 0L);
    assertEquals(-1, ret);
    long[] a2 = {1, 7, 31};
    ret = Arrays.binarySearch(a2, 3L);
    assertEquals(-2, ret);
    ret = Arrays.binarySearch(a2, 31L);
    assertEquals(2, ret);
    long[] a3 = {-71, 0, 35, 36};
    ret = Arrays.binarySearch(a3, 42L);
    assertEquals(-5, ret);
    ret = Arrays.binarySearch(a3, -80L);
    assertEquals(-1, ret);
    ret = Arrays.binarySearch(a3, -71L);
    assertEquals(0, ret);    
  }

  /**
   * Test Arrays.binarySearch(Object[], Object).
   * 
   * Verify the following cases:
   *   empty array
   *   odd numbers of elements
   *   even numbers of elements
   *   not found value larger than all elements
   *   not found value smaller than all elements
   */
  public void testBinarySearchObject() {
    Object[] a1 = {};
    int ret = Arrays.binarySearch(a1, "");
    assertEquals(-1, ret);
    Object[] a2 = {"a", "g", "y"};
    ret = Arrays.binarySearch(a2, "c");
    assertEquals(-2, ret);
    ret = Arrays.binarySearch(a2, "y");
    assertEquals(2, ret);
    Object[] a3 = {"b", "c", "x", "y"};
    ret = Arrays.binarySearch(a3, "z");
    assertEquals(-5, ret);
    ret = Arrays.binarySearch(a3, "a");
    assertEquals(-1, ret);
    ret = Arrays.binarySearch(a3, "b");
    assertEquals(0, ret);    
  }

  /**
   * Test Arrays.binarySearch(Object[], Object, Comparator).
   * 
   * Verify the following cases:
   *   empty array
   *   odd numbers of elements
   *   even numbers of elements
   *   not found value larger than all elements
   *   not found value smaller than all elements
   *   null Comparator uses natural ordering
   */
  public void testBinarySearchObjectComparator() {
    Comparator inverseSort = new Comparator() {
      public int compare(Object o1, Object o2) {
        return ((Comparable) o2).compareTo(o1);
      }
    };
    Object[] a1 = {};
    int ret = Arrays.binarySearch(a1, "", inverseSort);
    assertEquals(-1, ret);
    Object[] a2 = {"y", "g", "a"};
    ret = Arrays.binarySearch(a2, "c", inverseSort);
    assertEquals(-3, ret);
    ret = Arrays.binarySearch(a2, "a", inverseSort);
    assertEquals(2, ret);
    Object[] a3 = {"y", "x", "c", "b"};
    ret = Arrays.binarySearch(a3, "a", inverseSort);
    assertEquals(-5, ret);
    ret = Arrays.binarySearch(a3, "z", inverseSort);
    assertEquals(-1, ret);
    ret = Arrays.binarySearch(a3, "y", inverseSort);
    assertEquals(0, ret);

    Object[] a4 = {"a", "b", "c", "d", "e"};
    ret = Arrays.binarySearch(a4, "d", null); // should not NPE
    assertEquals(3, ret);
  }

  /**
   * Test Arrays.binarySearch(short[], short).
   * 
   * Verify the following cases:
   *   empty array
   *   odd numbers of elements
   *   even numbers of elements
   *   not found value larger than all elements
   *   not found value smaller than all elements
   *   negative values
   */
  public void testBinarySearchShort() {
    short[] a1 = {};
    int ret = Arrays.binarySearch(a1, (short) 0);
    assertEquals(-1, ret);
    short[] a2 = {1, 7, 31};
    ret = Arrays.binarySearch(a2, (short) 3);
    assertEquals(-2, ret);
    ret = Arrays.binarySearch(a2, (short) 31);
    assertEquals(2, ret);
    short[] a3 = {-71, 0, 35, 36};
    ret = Arrays.binarySearch(a3, (short) 42);
    assertEquals(-5, ret);
    ret = Arrays.binarySearch(a3, (short) -80);
    assertEquals(-1, ret);
    ret = Arrays.binarySearch(a3, (short) -71);
    assertEquals(0, ret);    
  }

  /**
   * Tests simple use cases for {@link Arrays#sort(Object[])}.
   */
  public void testSimpleSort() {
    // empty case
    Object[] test = {};
    Arrays.sort(test);
    assertEquals(test.length, 0);
    // single case
    Integer[] test2 = {new Integer(1)};
    Arrays.sort(test2);
    assertEquals(1, test2[0].intValue());
    // multiple case with subclassing
    Number[] test3 = {
        new Integer(3), new Integer(0), new Integer(2), new Integer(4),
        new Integer(1)};
    Arrays.sort(test3);
    for (int i = 0; i < test3.length; i++) {
      assertEquals(i, test3[i].intValue());
    }
  }

  /**
   * Tests {@link Arrays#sort(Object[], Comparator)}.
   */
  public void testSort() {
    Object[] x = {"c", "b", "b", "a"};
    int hash = x[1].hashCode();
    Arrays.sort(x);
    int hash2 = x[1].hashCode();
    assertEquals(hash, hash2);
    Object[] sorted = {"a", "b", "b", "c"};
    assertEquals(x, sorted);
    Comparator t = new Comparator() {
      public int compare(Object o1, Object o2) {
        return ((Comparable) o2).compareTo(o1);
      }
    };
    Arrays.sort(x, t);
    int hash3 = x[1].hashCode();
    assertEquals(hash, hash3);
    Object[] reverseSorted = {"c", "b", "b", "a"};
    assertEquals(x, reverseSorted);
  }
}
