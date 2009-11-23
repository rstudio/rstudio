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

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Tests {@link Arrays}.
 */
public class ArraysTest extends EmulTestBase {
  /**
   * Helper class to use in sorted objects test.
   */
  private static class TestObject {
    private static int count = 0;
    private int index;
    private int value;

    public TestObject(int value) {
      this.value = value;
      index = count++;
    }

    public int getIndex() {
      return index;
    }

    public int getValue() {
      return value;
    }

    public void setIndex(int val) {
      index = val;
    }

    @Override
    public String toString() {
      return value + "@" + index;
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  /**
   * Verifies that calling Arrays.hashCode(Object[]) with an array with
   * embedded null references works properly (and most importantly doesn't
   * throw an NPE).
   */
  public void testArraysHashCodeWithNullElements() {
    String[] a = new String[] { "foo", null, "bar", "baz" };
    Arrays.hashCode(a);
  }

  public void testArraysEqualsWithEmptyArrays() {
    assertTrue(Arrays.equals(new String[0], new String[0]));
  }

  public void testArraysEqualsWithoutNullElementsEqual() {
    assertTrue(Arrays.equals(
        new String[] { "foo" }, new String[]{ "foo" }));
  }

  public void testArraysEqualsWithoutNullElementsNotEqual() {
    assertFalse(Arrays.equals(
        new String[] { "foo" }, new String[]{ "bar" }));
  }

  public void testArraysEqualsWithNullElementsEqual() {
    assertTrue(Arrays.equals(new String[2], new String[2]));
  }

  public void testArraysEqualsWithNullElementsNotEqual() {
    assertFalse(Arrays.equals(new String[2], new String[1]));
  }

  public void testArraysEqualsWithNullAndNonNullElementsEqual() {
    assertTrue(Arrays.equals(
        new String[]{ null, "foo", null, "bar" },
        new String[]{ null, "foo", null, "bar" }));
  }

  public void testArraysEqualsWithNullAndNonNullElementsNotEqual() {
    assertFalse(Arrays.equals(
        new String[]{ null, "bar", null, "foo" },
        new String[]{ null, "foo", null, "foo" }));
  }

  /**
   * Tests {@link Arrays#asList(Object[])}.
   */
  @SuppressWarnings("unchecked")
  public void testAsList() {
    // 0
    Object[] test = {};
    List result = Arrays.asList(test);
    assertEquals(test, result);
    // n
    Object[] test2 = {0, 1, 2};
    List result2 = Arrays.asList(test2);
    assertEquals(test2, result2);
    // 1
    Object[] test3 = {"Hello"};
    List result3 = Arrays.asList(test3);
    assertEquals(test3, result3);
  }

  /**
   * Tests if changes to the list created by {@link Arrays#asList(Object[])} are
   * reflected in the original array.
   */
  @SuppressWarnings("unchecked")
  public void testAsListBacking() {
    Object[] test1 = {0, 1, 2};
    List result1 = Arrays.asList(test1);
    test1[0] = 3;
    assertEquals(test1, result1);

    Object[] test2 = {"a", "b", "c"};
    List result2 = Arrays.asList(test2);
    result2.set(2, "x");
    assertEquals(test2, result2);
  }

  /**
   * Tests {@link Arrays#asList(Object[])}.
   */
  public void testAsListIsFixed() {
    List<String> list = Arrays.asList("foo", "bar", "baz");

    try {
      list.add("bal");
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }

    try {
      list.remove(0);
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }

    try {
      list.clear();
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }

    Iterator<String> it = list.iterator();
    it.next();
    try {
      it.remove();
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }

    ListIterator<String> lit = list.listIterator();
    lit.next();
    try {
      lit.add("bal");
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }

    assertEquals(3, list.size());
  }

  /**
   * Test Arrays.binarySearch(byte[], byte).
   * 
   * <pre>
   * Verify the following cases:
   *   empty array
   *   odd numbers of elements
   *   even numbers of elements
   *   not found value larger than all elements
   *   not found value smaller than all elements
   *   negative values
   * </pre>
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
   * <pre>
   * Verify the following cases:
   *   empty array
   *   odd numbers of elements
   *   even numbers of elements
   *   not found value larger than all elements
   *   not found value smaller than all elements
   * </pre>
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
   * <pre>
   * Verify the following cases:
   *   empty array
   *   odd numbers of elements
   *   even numbers of elements
   *   not found value larger than all elements
   *   not found value smaller than all elements
   *   negative values
   * </pre>
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
   * <pre>
   * Verify the following cases:
   *   empty array
   *   odd numbers of elements
   *   even numbers of elements
   *   not found value larger than all elements
   *   not found value smaller than all elements
   *   negative values
   * </pre>
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
   * <pre>
   * Verify the following cases:
   *   empty array
   *   odd numbers of elements
   *   even numbers of elements
   *   not found value larger than all elements
   *   not found value smaller than all elements
   *   negative values
   * </pre>
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
   * <pre>
   * Verify the following cases:
   *   empty array
   *   odd numbers of elements
   *   even numbers of elements
   *   not found value larger than all elements
   *   not found value smaller than all elements
   *   negative values
   * </pre>
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
   * <pre>
   * Verify the following cases:
   *   empty array
   *   odd numbers of elements
   *   even numbers of elements
   *   not found value larger than all elements
   *   not found value smaller than all elements
   * </pre>
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
   * <pre>
   * Verify the following cases:
   *   empty array
   *   odd numbers of elements
   *   even numbers of elements
   *   not found value larger than all elements
   *   not found value smaller than all elements
   *   Comparator uses natural ordering as a default
   * </pre>
   */
  @SuppressWarnings("unchecked")
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
   * <pre>
   * Verify the following cases:
   *   empty array
   *   odd numbers of elements
   *   even numbers of elements
   *   not found value larger than all elements
   *   not found value smaller than all elements
   *   negative values
   * </pre>
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
   * Tests sorting of long primitives.
   */
  public void testLongSort() {
    long[] x = {3, 11, 2, 1, 22, 3};
    Arrays.sort(x);
    assertEquals(1, x[0]);
    assertEquals(2, x[1]);
    assertEquals(3, x[2]);
    assertEquals(3, x[3]);
    assertEquals(11, x[4]);
    assertEquals(22, x[5]);
  }

  /**
   * Verifies that values are sorted numerically rather than as strings.
   */
  public void testNumericSort() {
    Integer[] x = {3, 11, 2, 1};
    Arrays.sort(x);
    assertEquals(2, x[1].intValue());
    assertEquals(11, x[3].intValue());
  }

  /**
   * Tests sorting primitives.
   */
  public void testPrimitiveSort() {
    int[] x = {3, 11, 2, 1, 22, 3};
    Arrays.sort(x);
    assertEquals(1, x[0]);
    assertEquals(2, x[1]);
    assertEquals(3, x[2]);
    assertEquals(3, x[3]);
    assertEquals(11, x[4]);
    assertEquals(22, x[5]);
  }

  /**
   * Tests sorting a subrange of a primitive array.
   */
  public void testPrimitiveSubrangeSort() {
    int[] x = {3, 11, 2, 1, 22, 3};
    Arrays.sort(x, 1, 5);
    assertEquals(3, x[0]);
    assertEquals(1, x[1]);
    assertEquals(2, x[2]);
    assertEquals(11, x[3]);
    assertEquals(22, x[4]);
    assertEquals(3, x[5]);
  }

  /**
   * Tests simple use cases for {@link Arrays#sort(Object[])}.
   */
  public void testSimpleSort() {
    // empty array
    Object[] test = {};
    Arrays.sort(test);
    assertEquals(test.length, 0);
    // array with one element
    Integer[] test2 = {1};
    Arrays.sort(test2);
    assertEquals(1, test2[0].intValue());
    // multiple elements
    Number[] test3 = {3, 0, 2, 4, 1};
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
    Comparator<Object> t = new Comparator<Object>() {
      @SuppressWarnings("unchecked")
      public int compare(Object o1, Object o2) {
        return ((Comparable<Object>) o2).compareTo(o1);
      }
    };
    Arrays.sort(x, t);
    int hash3 = x[1].hashCode();
    assertEquals(hash, hash3);
    Object[] reverseSorted = {"c", "b", "b", "a"};
    assertEquals(x, reverseSorted);
  }

  /**
   * Verifies that equal values retain their original order. This is done by
   * trying all possible permutations of a small test array to make sure the
   * sort algorithm properly handles any ordering.
   * 
   * The current test is 6 elements, so there are 6! = 720 possible orderings to
   * test.
   */
  public void testStableSort() {
    TestObject[] origData = new TestObject[] {
        new TestObject(3), new TestObject(11), new TestObject(2),
        new TestObject(3), new TestObject(1), new TestObject(3),
        new TestObject(22)};
    int[] permutation = new int[origData.length];
    while (validPermutation(permutation, origData.length)) {
      TestObject[] permutedArray = getPermutation(origData, permutation);
      Arrays.sort(permutedArray, new Comparator<TestObject>() {
        public int compare(TestObject a, TestObject b) {
          return a.getValue() - b.getValue();
        }
      });
      for (int i = 1; i < permutedArray.length; ++i) {
        if (permutedArray[i - 1].getValue() > permutedArray[i].getValue()
            || (permutedArray[i - 1].getValue() == permutedArray[i].getValue() && permutedArray[i - 1].getIndex() > permutedArray[i].getIndex())) {
          String msg = "Permutation " + Arrays.toString(permutation) + ": "
              + Arrays.toString(permutedArray);
          permutedArray = getPermutation(origData, permutation);
          msg += " (orig: " + Arrays.toString(permutedArray) + ")";
          fail(msg);
        }
      }
      nextPermutation(permutation);
    }
  }

  /**
   * Returns a permuted array given the original array and a permutation. The
   * permutation is an array of indices which select which possible source goes
   * into the output slot of the same position. Note that previously used
   * sources are not counted, so the first permutation of a three-element array
   * [a,b,c] is [0,0,0], which maps to [a,b,c]. [1,0,0] maps to [b,a,c] since
   * the range of index[1] is from 0-1 and excludes the value b since it has
   * already been chosen. The permutation array may be shorter than the source
   * array, in which case it is choosing any m elements out of n.
   * 
   * Thus the range of index i is 0 <= permutation[i] < n-i where n is the
   * number of elements in the source array.
   * 
   * @param origData original array to permute
   * @param permutation array of indices, as described above
   * @return permuted array
   */
  private TestObject[] getPermutation(TestObject[] origData, int[] permutation) {
    TestObject[] array = new TestObject[permutation.length];
    for (int i = 0; i < permutation.length; ++i) {
      int idx = permutation[i];
      // adjust for source elements already used
      for (int j = i; j-- > 0;) {
        if (permutation[j] <= idx) {
          idx++;
        }
      }
      array[i] = origData[idx];
      // update position in output array for stability test
      array[i].setIndex(i);
    }
    return array;
  }

  /**
   * Advance the permutation to the next value. It leaves the first index set to
   * -1 if the range has been exceeded.
   * 
   * @param permutation array of indices -- see {@link #getPermutation} for
   *          details.
   */
  private void nextPermutation(int[] permutation) {
    for (int i = 0; i < permutation.length; ++i) {
      if (++permutation[i] < permutation.length - i) {
        return;
      }
      permutation[i] = 0;
    }
    permutation[0] = -1;
  }

  /**
   * Checks to see if this permutation is valid; ie, if all of the indices are
   * between 0 and n-i (see {@link #getPermutation} for details).
   * 
   * @param permutations array of indices
   * @param n length of source array.
   * @return true if the permutation is valid
   */
  private boolean validPermutation(int[] permutations, int n) {
    if (permutations[0] < 0) {
      return false;
    }
    for (int i = 0; i < permutations.length; ++i) {
      if (permutations[i] >= n - i) {
        return false;
      }
    }
    return true;
  }

}
