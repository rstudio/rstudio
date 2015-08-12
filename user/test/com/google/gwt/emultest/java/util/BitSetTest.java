/*
 * Copyright 2009 Google Inc.
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
import java.util.BitSet;
import java.util.HashSet;

/**
 * Tests BitSet class.
 */
public class BitSetTest extends EmulTestBase {

  /**
   * This class is used to describe numerical patterns.
   */
  private interface Pattern {
    boolean contains(int i);
  }

  // count used for looping tests
  private static final int TEST_SIZE = 509;

  // this number has to be bigger than 100
  private static final int BIG_NUMBER = (TEST_SIZE) * 10 + 101;

  private static void assertTrue(BitSet set, int index) {
    assertTrue("expected=true set[" + index + "]=false", set.get(index));
  }

  private static void assertFalse(BitSet set, int index) {
    assertFalse("expected=false set[" + index + "]=true", set.get(index));
  }

  private static BitSet createSetOfMultiples(int multiple) {
    BitSet set = new BitSet(TEST_SIZE);
    for (int i = 0; i < TEST_SIZE; i += multiple) {
      set.set(i);
    }
    return set;
  }

  // this checks to see the values given are true, and the others around are not
  private static void checkValues(BitSet set, int... values) {
    assertEquals(values.length, set.cardinality());

    int highestIndex = values[values.length - 1];
    boolean[] bits = new boolean[highestIndex + 1];
    for (int value : values) {
      bits[value] = true;
    }

    for (int index = 0; index < bits.length; index++) {
      assertEquals(bits[index], set.get(index));
    }
  }

  private static void checkEqualityTrue(BitSet setA, BitSet setB) {
    assertTrue(setA.equals(setB));
    assertTrue(setB.equals(setA));
    assertTrue(setA.equals(setA));
    assertTrue(setB.equals(setB));
  }

  private static void checkEqualityFalse(BitSet setA, BitSet setB) {
    assertFalse(setA.equals(setB));
    assertFalse(setB.equals(setA));
  }

  // Checks that the values in the given range are the only true values
  private static void checkRange(BitSet set, int fromIndex, int toIndex) {
    for (int i = fromIndex; i < toIndex; i++) {
      assertTrue(set, i);
    }
    assertEquals(toIndex - fromIndex, set.cardinality());
  }

  // Checks that the values in the given range are the only true values
  private static void checkRange(BitSet set, int fromIndex1, int toIndex1,
      int fromIndex2, int toIndex2) {
    for (int i = fromIndex1; i < toIndex1; i++) {
      assertTrue(set, i);
    }
    for (int i = fromIndex2; i < toIndex2; i++) {
      assertTrue(set, i);
    }
    assertEquals(toIndex1 - fromIndex1 + toIndex2 - fromIndex2,
        set.cardinality());
  }

  private static void checkPattern(BitSet set, Pattern pattern) {
    for (int i = 0; i < TEST_SIZE; i++) {
      boolean contained = pattern.contains(i);
      if (contained != set.get(i)) {
        fail("expected=" + contained + " set[" + i + "]=" + !contained);
      }
    }
  }

  public void testConstructor() {
    BitSet set = new BitSet();

    // test what we know to be true of a new BitSet
    assertTrue(set.isEmpty());
    assertEquals(0, set.length());
    assertEquals(0, set.cardinality());

    // test exceptions
    try {
      set = new BitSet(-1);
      fail("exception expected");
    } catch (NegativeArraySizeException expected) {
    }

    set = new BitSet(0);
    set = new BitSet(BIG_NUMBER);
  }

  public void testAnd() {
    Pattern multiplesOf6 = new Pattern() {
      @Override
      public boolean contains(int i) {
        return i % 6 == 0;
      }
    };

    // setA will contain all multiples of 2
    BitSet setA = createSetOfMultiples(2);

    // setB will contain all multiples of 3
    BitSet setB = createSetOfMultiples(3);

    // and()ing the sets should give multiples of 6
    setA.and(setB);

    // verify by checking multiples of 6
    checkPattern(setA, multiplesOf6);

    // and()ing a set to itself should do nothing
    setA.and(setA);

    // verify by checking multiples of 6
    checkPattern(setA, multiplesOf6);

    // and()ing with a set identical to itself should do nothing
    setA.and((BitSet) setA.clone());

    // verify by checking multiples of 6
    checkPattern(setA, multiplesOf6);

    // and()ing with all trues should do nothing
    BitSet trueSet = new BitSet(TEST_SIZE);
    trueSet.set(0, TEST_SIZE);
    setA.and(trueSet);

    // verify by checking multiples of 6
    checkPattern(setA, multiplesOf6);

    // and()ing with all trues in a larger set should do nothing
    trueSet.set(TEST_SIZE, TEST_SIZE * 2);
    setA.and(trueSet);

    // verify by checking multiples of 6
    checkPattern(setA, multiplesOf6);
    // there were "TEST_SIZE" extra trues, so lets verify those came out false
    for (int i = TEST_SIZE; i < TEST_SIZE * 2; i++) {
      assertFalse(setA.get(i));
    }

    // and()ing with an empty set should result in an empty set
    setA.and(new BitSet());
    assertEquals(0, setA.length());

    // these close bits should not intersect
    setB = new BitSet();
    setA.set(0);
    setB.set(1);
    setA.and(setB);
    assertTrue(setA.isEmpty());

    // these bits should not intersect
    setB = new BitSet();
    setA.set(0);
    setB.set(BIG_NUMBER);
    setA.and(setB);
    assertTrue(setA.isEmpty());
    setA.set(0);
    setB.and(setA);
    assertTrue(setB.isEmpty());

    setA = new BitSet();
    setA.set(1);
    setA.set(5);
    setB = new BitSet();
    setB.set(2);
    setA.and(setB);
    assertTrue(setA.isEmpty());

    setA = new BitSet();
    setA.set(1);
    setA.set(5);
    setB = new BitSet();
    setB.set(5);
    setA.and(setB);
    assertTrue(setA.get(5));
    assertEquals(1, setA.cardinality());

    setA = new BitSet();
    setA.set(2);
    setB = new BitSet();
    setB.set(1);
    setB.set(3);
    setA.and(setB);
    assertEquals(0, setA.cardinality());
  }

  public void testAndNot() {
    Pattern multiplesOf2Not3 = new Pattern() {
      @Override
      public boolean contains(int i) {
        return i % 2 == 0 && i % 3 != 0;
      }
    };

    // setA will contain all multiples of 2
    BitSet setA = createSetOfMultiples(2);

    // setB will contain all multiples of 3
    BitSet setB = createSetOfMultiples(3);

    // andNot() the sets
    setA.andNot(setB);

    // verify by checking for multiples of 2 that are not multiples of 3
    checkPattern(setA, multiplesOf2Not3);

    // andNot()ing with an empty set should do nothing
    setA.andNot(new BitSet());

    // verify by checking for multiples of 2 that are not multiples of 3
    checkPattern(setA, multiplesOf2Not3);

    // andNot()ing with all trues should result in an empty set
    BitSet trueSet = new BitSet(TEST_SIZE * 2);
    trueSet.set(0, TEST_SIZE * 2);
    setA.andNot(trueSet);
    assertTrue(setA.isEmpty());

    // save setB in setA
    setA = (BitSet) setB.clone();

    // andNot()ing a set to itself should result in an empty set
    setB.andNot(setB);
    assertTrue(setB.isEmpty());

    // andNot()ing a set identical to itself should result in an empty set
    setA.andNot((BitSet) setA.clone());
    assertTrue(setA.isEmpty());

    setA = new BitSet();
    setA.set(2);
    setB = new BitSet();
    setB.set(1);
    setB.set(3);
    setA.andNot(setB);
    assertTrue(setA.get(2));
    assertEquals(1, setA.cardinality());
  }

  public void testCardinality() {
    BitSet set = new BitSet(TEST_SIZE);

    // test the empty count
    assertEquals(0, set.cardinality());

    // test a count of 1
    set.set(0);
    assertEquals(1, set.cardinality());

    // test a count of 2
    set.set(BIG_NUMBER);
    assertEquals(2, set.cardinality());

    // clear them both and test again
    set.clear(0);
    set.clear(BIG_NUMBER);
    assertEquals(0, set.cardinality());

    // test different multiples
    for (int multiple = 1; multiple < 33; multiple++) {
      set = new BitSet();
      set.set(BIG_NUMBER + multiple);
      for (int i = 1; i < 33; i++) {
        set.set(i * multiple);
        assertEquals(i + 1, set.cardinality());
      }
    }

    // test powers of 2
    set = new BitSet();
    int count = 0;
    for (int i = 1; i < TEST_SIZE; i += i) {
      set.set(i);
      count++;
    }
    assertEquals(count, set.cardinality());

    // test a long run
    set = new BitSet();
    for (int i = 0; i < TEST_SIZE; i++) {
      set.set(i);
      assertEquals(i + 1, set.cardinality());
    }
  }

  public void testClear() {
    BitSet set = new BitSet(TEST_SIZE);
    for (int i = 0; i < TEST_SIZE; i++) {
      set.set(i);
    }

    set.clear();
    assertTrue(set.isEmpty());

    set = new BitSet();
    set.set(BIG_NUMBER);
    set.clear();
    assertFalse(set.get(BIG_NUMBER));
  }

  public void testClearInt() {
    BitSet set = new BitSet();
    set.set(0);
    set.clear(0);
    assertFalse(set.get(0));
    set.set(BIG_NUMBER);
    checkValues(set, BIG_NUMBER);
    set.clear(BIG_NUMBER);
    assertFalse(set.get(BIG_NUMBER));

    set = new BitSet();
    set.set(1);
    set.set(2);
    set.set(3);
    set.set(18);
    set.set(40);
    set.clear(2);
    checkValues(set, 1, 3, 18, 40);
    set.clear(9);
    checkValues(set, 1, 3, 18, 40);
    set.clear(18);
    checkValues(set, 1, 3, 40);
    set.clear(7);
    set.clear(6);
    checkValues(set, 1, 3, 40);
    set.clear(3);
    checkValues(set, 1, 40);
    set.clear(40);
    checkValues(set, 1);
    set.clear(1);
    assertTrue(set.isEmpty());

    // test exceptions
    try {
      set.clear(-1, 2);
      fail("exception expected");
    } catch (IndexOutOfBoundsException expected) {
    }

    try {
      set.clear(3, 1);
      fail("exception expected");
    } catch (IndexOutOfBoundsException expected) {
    }

    set.clear(2, 2);
  }

  public void testClearIntIntAndSetIntInt() {
    BitSet set = new BitSet();

    set.set(7);
    set.set(50);
    set.set(BIG_NUMBER);
    set.clear(0, BIG_NUMBER);
    checkValues(set, BIG_NUMBER);
    set.clear(0, BIG_NUMBER + 1);
    assertTrue(set.isEmpty());

    set.set(0, 65);
    checkRange(set, 0, 65);
    set.clear(0, 63);
    checkRange(set, 63, 65);
    set.clear(63, 65);
    assertTrue(set.isEmpty());

    set.set(0, 128);
    set.clear(0, 64);
    checkRange(set, 64, 128);
    set.clear(0, 129);
    assertTrue(set.isEmpty());

    set.set(0, 65);
    checkRange(set, 0, 65);

    set.clear(0, 16);
    checkRange(set, 16, 65);

    set.set(0, 16);
    checkRange(set, 0, 65);
    set.clear(5, 5);
    set.clear(7, 14);
    set.clear(15, 42);
    set.clear(43, 55);
    set.clear(58, 62);
    set.clear(BIG_NUMBER, BIG_NUMBER);
    set.clear(BIG_NUMBER, BIG_NUMBER + 1);
    checkValues(set, 0, 1, 2, 3, 4, 5, 6, 14, 42, 55, 56, 57, 62, 63, 64);
    set.clear(0, 65);
    assertTrue(set.isEmpty());

    set.set(0, 33);
    checkRange(set, 0, 33);
    set.clear(0, 8);
    checkRange(set, 8, 33);

    for (int i = 0; i < 33; i++) {
      // this shouldn't change anything
      set.clear(i, i);
      assertEquals(25, set.cardinality());
      // nor should this
      set.set(i, i);
      assertEquals(25, set.cardinality());
    }

    for (int i = 0; i < 65; i++) {
      set.set(0, 128);
      set.clear(i, 128 - i);
      checkRange(set, 0, i, 128 - i, 128);
    }
    set.clear(0, 128);
    assertTrue(set.isEmpty());

    set.set(7, 100);
    checkRange(set, 7, 100);

    set = new BitSet();
    set.set(BIG_NUMBER, BIG_NUMBER);
    assertEquals(0, set.cardinality());
    set.set(BIG_NUMBER, BIG_NUMBER + 1);
    checkRange(set, BIG_NUMBER, BIG_NUMBER + 1);
    set.clear(BIG_NUMBER, BIG_NUMBER);
    checkValues(set, BIG_NUMBER);
    set.clear(BIG_NUMBER, BIG_NUMBER + 1);
    assertEquals(0, set.cardinality());

    set = new BitSet();
    set.set(10, 12);
    set.clear(11, 1000);
    checkValues(set, 10);

    set = new BitSet();
    set.set(10, 12);
    set.clear(0, 10);
    set.set(BIG_NUMBER, BIG_NUMBER);
    checkValues(set, 10, 11);
    set.clear(10, 12);
    assertTrue(set.isEmpty());

    set = new BitSet();
    set.set(1, 20);
    set.clear(5, 10);
    checkRange(set, 1, 5, 10, 20);

    set = new BitSet();
    set.set(1, 10);
    set.clear(5, 15);
    checkRange(set, 1, 5);

    // test clear(int, int) exceptions
    try {
      set.clear(-1, 2);
      fail("exception expected");
    } catch (IndexOutOfBoundsException expected) {
    }

    try {
      set.clear(3, 1);
      fail("expected exception");
    } catch (IndexOutOfBoundsException expected) {
    }

    set.clear(2, 2);

    // test set(int, int) exceptions
    try {
      set.set(-1, 2);
      fail("exception expected");
    } catch (IndexOutOfBoundsException expected) {
    }

    try {
      set.set(3, 1);
      fail("exception expected");
    } catch (IndexOutOfBoundsException expected) {
    }

    set.set(2, 2);
  }

  public void testClone() {
    BitSet set = new BitSet();
    set.set(2);
    set.set(4);
    set.set(32);
    set.set(BIG_NUMBER);
    BitSet clone = (BitSet) set.clone();
    checkValues(clone, 2, 4, 32, BIG_NUMBER);
    assertTrue(set.equals(clone));
    assertEquals(4, clone.cardinality());
  }

  public void testEquals() {
    BitSet setA = new BitSet();
    BitSet setB = new BitSet();
    checkEqualityTrue(setA, setB);

    setA.set(0);
    setB.set(0);
    checkEqualityTrue(setA, setB);

    setA.set(BIG_NUMBER);
    setB.set(BIG_NUMBER);
    checkEqualityTrue(setA, setB);

    setA.clear(0);
    setB.clear(0);
    checkEqualityTrue(setA, setB);

    setA.clear(BIG_NUMBER);
    setB.clear(BIG_NUMBER);
    checkEqualityTrue(setA, setB);

    setA.set(0);
    setB.set(1);
    checkEqualityFalse(setA, setB);

    setA.set(2);
    setB.set(2);
    checkEqualityFalse(setA, setB);

    setA = new BitSet();
    setB = new BitSet();
    setA.set(Math.max(0, BIG_NUMBER - 8));
    setB.set(BIG_NUMBER + 1);
    checkEqualityFalse(setA, setB);

    BitSet set1 = new BitSet();
    set1.set(40);
    BitSet set2 = new BitSet();
    set2.set(1);
    set2.set(40);
    assertFalse(set1.equals(set2));

    setA = new BitSet();
    setA.set(2);
    setB = new BitSet();
    setB.set(1);
    setB.set(3);
    checkEqualityFalse(setA, setB);
  }

  public void testFlipInt() {
    BitSet set = new BitSet();
    set.flip(0);
    assertTrue(set.get(0));
    set.flip(0);
    assertFalse(set.get(0));
    set.flip(BIG_NUMBER);
    assertTrue(set.get(BIG_NUMBER));
    set.flip(BIG_NUMBER);
    assertFalse(set.get(BIG_NUMBER));

    set = new BitSet();
    set.flip(1);
    set.flip(2);
    set.flip(3);
    set.flip(4);
    set.flip(6);
    set.flip(4);
    set.flip(6);
    set.flip(6);
    set.flip(6);
    set.flip(8);
    set.flip(10);
    set.flip(8);
    set.flip(2);
    set.flip(8);
    checkValues(set, 1, 3, 8, 10);
    set.flip(8);
    checkValues(set, 1, 3, 10);
    set.flip(3);
    checkValues(set, 1, 10);
    set.flip(10);
    set.flip(11);
    checkValues(set, 1, 11);
    set.flip(1);
    checkValues(set, 11);
    set.flip(11);
    assertTrue(set.isEmpty());

    // test exceptions
    try {
      set.flip(-1);
      fail("exception expected");
    } catch (IndexOutOfBoundsException expected) {
    }

    set.flip(BIG_NUMBER);
    set.flip(BIG_NUMBER);
  }

  public void testFlipIntInt() {
    BitSet set = new BitSet();
    set.flip(0, BIG_NUMBER);
    checkRange(set, 0, BIG_NUMBER);
    set.flip(0, BIG_NUMBER - 1);
    checkValues(set, BIG_NUMBER - 1);
    set.clear(0, BIG_NUMBER);
    assertTrue(set.isEmpty());

    set.flip(0, 33);
    set.flip(0, 8);
    checkRange(set, 8, 33);

    // current state is set.set(8,33)
    for (int i = 0; i < 33; i++) {
      // this shouldn't change anything
      set.flip(i, i);
      assertEquals(25, set.cardinality());
    }

    // current state is set.set(8,33)
    set.flip(0, 8);
    set.flip(7, 21);
    set.flip(22, 27);
    checkValues(set, 0, 1, 2, 3, 4, 5, 6, 21, 27, 28, 29, 30, 31, 32);

    set = new BitSet();
    set.flip(10, 12);
    set.flip(11, 1000);
    checkRange(set, 10, 11, 12, 1000);

    set = new BitSet();
    set.flip(10, 12);
    set.flip(0, 10);
    checkRange(set, 0, 12);
    set.flip(0, 12);
    assertTrue(set.isEmpty());

    set.flip(0, 64);
    set.flip(0, 63);
    checkValues(set, 63);
    set.flip(63, 64);
    assertTrue(set.isEmpty());

    set.flip(0, 130);
    checkRange(set, 0, 130);
    set.flip(0, 66);
    checkRange(set, 66, 130);
    set.flip(65, 131);
    checkRange(set, 65, 66, 130, 131);

    set = new BitSet();
    set.flip(1, 20);
    set.flip(5, 10);
    checkRange(set, 1, 5, 10, 20);

    set = new BitSet();
    set.flip(1, 10);
    set.flip(5, 15);
    checkRange(set, 1, 5, 10, 15);

    // test exceptions
    try {
      set.flip(-1, 2);
      fail("exception expected");
    } catch (IndexOutOfBoundsException expected) {
    }

    try {
      set.flip(3, 1);
      fail("exception expected");
    } catch (IndexOutOfBoundsException expected) {
    }

    set.flip(2, 2);
  }

  public void testGetIntAndSetInt() {
    BitSet set = new BitSet();
    set.set(0);
    assertTrue(set.get(0));
    assertFalse(set.get(1));
    assertFalse(set.get(2));
    assertFalse(set.get(100));

    set.set(BIG_NUMBER);
    assertFalse(set.get(BIG_NUMBER - 1));
    assertTrue(set.get(BIG_NUMBER));
    assertFalse(set.get(BIG_NUMBER + 1));

    set = new BitSet();
    set.set(0);
    set.set(4);
    set.set(7);
    set.set(10);
    set.set(31);
    set.set(32);
    set.set(33);
    set.set(69);
    checkValues(set, 0, 4, 7, 10, 31, 32, 33, 69);

    // test get() exceptions
    try {
      set.get(-1);
      fail("exception expected");
    } catch (IndexOutOfBoundsException expected) {
    }

    set.get(BIG_NUMBER);

    // test set() exceptions
    try {
      set.set(-1);
      fail("exception expected");
    } catch (IndexOutOfBoundsException expected) {
    }

    set.set(BIG_NUMBER);
  }

  public void testGetIntInt() {
    BitSet set = new BitSet();

    set.set(1);
    assertFalse(set.get(1, 1).get(0));
    assertTrue(set.get(1, 2).get(0));
    assertTrue(set.get(0, 2).get(1));

    set.set(32);
    set.set(50);
    set.set(BIG_NUMBER);

    BitSet subSet = set.get(0, BIG_NUMBER);
    checkValues(subSet, 1, 32, 50);

    subSet = set.get(1, BIG_NUMBER);
    checkValues(subSet, 0, 31, 49);

    subSet = set.get(2, BIG_NUMBER + 1);
    checkValues(subSet, 30, 48, BIG_NUMBER - 2);

    subSet = set.get(32, BIG_NUMBER * 2);
    checkValues(subSet, 0, 18, BIG_NUMBER - 32);
    assertEquals(3, subSet.cardinality());

    subSet = set.get(0, BIG_NUMBER + 1);
    assertEquals(set, subSet);

    set = new BitSet();
    for (int i = 8; i < 33; i++) {
      set.set(i);
    }
    for (int i = 0; i < 33; i++) {
      assertTrue(set.get(i, i).isEmpty());
    }

    // test exceptions
    try {
      set.get(-1, 2);
      fail("exception expected");
    } catch (IndexOutOfBoundsException expected) {
    }

    try {
      set.get(3, 1);
      fail("exception expected");
    } catch (IndexOutOfBoundsException expected) {
    }

    set.get(2, 2);
  }

  public void testHashCode() {
    // Note: Oracle's hashCode() implementation creates many collisions when
    // testSize is big, causing this test to fail in development mode.
    // Since this is an "unimportant test" as suggested by the original author,
    // we can just test with a small number.
    int testSize = 25;

    HashSet<Integer> hashValues = new HashSet<>();

    // count the collisions
    int collisions = 0;

    // hash an empty set
    hashValues.add(new BitSet().hashCode());

    // hash the set of {TEST_SIZE + 1}
    BitSet set = new BitSet();
    set.set(testSize + 1);
    assertTrue(hashValues.add(set.hashCode()));

    // hash the set of {0, TEST_SIZE + 1}
    set.set(0);
    assertTrue(hashValues.add(set.hashCode()));

    // hash the set of {0}
    set = new BitSet();
    set.set(0);
    assertTrue(hashValues.add(set.hashCode()));

    for (int multiple = 1; multiple < 33; multiple++) {
      set = new BitSet();
      set.set(0);

      // fill a set with multiples
      for (int i = multiple; i < testSize; i += multiple) {
        set.set(i);
        // hash the current set, except in the case of {0}
        if (i != 0) {
          if (hashValues.add(set.hashCode()) == false) {
            collisions++;
          }
          set.set(testSize + 1);
          if (hashValues.add(set.hashCode()) == false) {
            collisions++;
          }
          set.clear(testSize + 1);
        }
      }
    }

    assertEquals(0, collisions);
  }

  public void testIntersects() {
    final int prime = 37;
    for (int multiple = 1; multiple < prime; multiple++) {
      int size = prime * multiple + 1;

      // setA will contain all multiples of "multiple" up to "size"
      BitSet setA = new BitSet(size);
      for (int i = multiple; i < size; i += multiple) {
        setA.set(i);
      }

      // setB will contain all multiples of "prime" up to "size"
      BitSet setB = new BitSet();
      for (int i = prime; i < size; i += prime) {
        setB.set(i);
      }

      // the two sets should only intersect on the very last bit
      assertTrue(setA.intersects(setB));
      setA.clear(size - 1);
      assertFalse(setA.intersects(setB));

      // the inverse of a set should not intersect itself
      setB = new BitSet();
      for (int i = 0; i < prime; i++) {
        setB.set(i, !setA.get(i));
      }
      assertFalse(setA.intersects(setB));

      // a set intersects itself if it has any bits set
      assertTrue(setA.intersects(setA));
      setA = new BitSet();
      assertFalse(setA.intersects(setA));

      // an empty set doesn't intersect itself
      assertFalse(new BitSet().intersects(new BitSet()));
    }

    BitSet setA = new BitSet();
    setA.set(2);
    BitSet setB = new BitSet();
    setB.set(1);
    setB.set(3);
    assertFalse(setA.intersects(setB));
  }

  public void testIsEmpty() {
    BitSet set = new BitSet();
    assertTrue(set.isEmpty());
    set.set(0);
    assertFalse(set.isEmpty());
    set = new BitSet();
    set.set(BIG_NUMBER);
    assertFalse(set.isEmpty());
  }

  public void testLength() {
    BitSet set = new BitSet();
    assertEquals(0, set.length());

    set.set(30);
    assertEquals(31, set.length());
    set.clear(30);
    set.set(31);
    assertEquals(32, set.length());
    set.clear(31);
    set.set(100);
    set.set(BIG_NUMBER);
    assertEquals(BIG_NUMBER + 1, set.length());
    set.clear(BIG_NUMBER);
    assertEquals(101, set.length());
    set.clear(100);
    assertEquals(0, set.length());
    set.set(0);
    assertEquals(1, set.length());

    set = new BitSet();
    for (int i = 0; i < 640; i++) {
      set.set(i);
      assertEquals(i + 1, set.length());
    }
    for (int i = 0; i < 639; i++) {
      set.clear(i);
      assertEquals(640, set.length());
    }
    set.clear(639);
    assertEquals(0, set.length());
  }

  public void testNextClearBit() {
    BitSet set = new BitSet(10);

    assertEquals(BIG_NUMBER, set.nextClearBit(BIG_NUMBER));

    for (int i = 0; i < 10; i++) {
      assertEquals(i, set.nextClearBit(i));
    }
    for (int i = 10; i < 100; i++) {
      assertEquals(i, set.nextClearBit(i));
    }

    set.set(0, 9);
    set.set(10, 50);
    for (int i = 0; i <= 9; i++) {
      assertEquals(9, set.nextClearBit(i));
    }
    for (int i = 10; i < 50; i++) {
      assertEquals(50, set.nextClearBit(i));
    }
    for (int i = 50; i < 100; i++) {
      assertEquals(i, set.nextClearBit(i));
    }

    set = new BitSet();
    set.set(61);
    for (int i = 0; i < 60; i++) {
      set.set(i);
      assertEquals(i + 1, set.nextClearBit(0));
    }
    set.clear(61);
    for (int i = 60; i < 100; i++) {
      set.set(i);
      assertEquals(i + 1, set.nextClearBit(0));
    }
    assertEquals(BIG_NUMBER, set.nextClearBit(BIG_NUMBER));

    try {
      set.nextClearBit(-1);
      fail("exception expected");
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  public void testNextSetBit() {
    BitSet set = new BitSet();

    assertEquals(-1, set.nextSetBit(0));
    assertEquals(-1, set.nextSetBit(BIG_NUMBER));

    set.set(0);
    set.set(1);
    set.set(3);
    set.set(31);
    set.set(32);
    set.set(33);
    assertEquals(0, set.nextSetBit(0));
    assertEquals(1, set.nextSetBit(1));
    assertEquals(3, set.nextSetBit(2));
    assertEquals(3, set.nextSetBit(3));
    assertEquals(31, set.nextSetBit(4));
    assertEquals(31, set.nextSetBit(31));
    assertEquals(32, set.nextSetBit(32));
    assertEquals(33, set.nextSetBit(33));
    assertEquals(-1, set.nextSetBit(34));

    set = new BitSet();
    set.set(BIG_NUMBER);
    assertEquals(BIG_NUMBER, set.nextSetBit(0));
    assertEquals(BIG_NUMBER, set.nextSetBit(BIG_NUMBER));
    assertEquals(-1, set.nextSetBit(BIG_NUMBER + 1));

    for (int i = 0; i < TEST_SIZE; i++) {
      set.set(BIG_NUMBER + i);
    }
    set.set(TEST_SIZE / 2);
    assertEquals(TEST_SIZE / 2, set.nextSetBit(0));

    try {
      set.nextSetBit(-1);
      fail("exception expected");
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  public void testPreviousClearBit() {
    BitSet set = new BitSet();

    assertEquals(-1, set.previousClearBit(-1));
    assertEquals(0, set.previousClearBit(0));
    assertEquals(100, set.previousClearBit(100));

    set.set(0, 9);
    set.set(10, 50);
    for (int i = 100; i >= 50; i--) {
      assertEquals(i, set.previousClearBit(i));
    }
    for (int i = 49; i >= 9; i--) {
      assertEquals(9, set.previousClearBit(i));
    }
    for (int i = 8; i >= 0; i--) {
      assertEquals(-1, set.previousClearBit(i));
    }

    try {
      set.previousClearBit(-100);
      fail("exception expected");
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  public void testPreviousSetBit() {
    BitSet set = new BitSet();

    assertEquals(-1, set.previousSetBit(-1));
    assertEquals(-1, set.previousSetBit(0));
    assertEquals(-1, set.previousSetBit(100));

    set.set(0, 9);
    set.set(10, 50);
    for (int i = 100; i >= 50; i--) {
      assertEquals(49, set.previousSetBit(i));
    }
    for (int i = 49; i >= 10; i--) {
      assertEquals(i, set.previousSetBit(i));
    }
    assertEquals(8, set.previousSetBit(9));
    for (int i = 8; i >= 0; i--) {
      assertEquals(i, set.previousSetBit(i));
    }

    try {
      set.previousSetBit(-100);
      fail("exception expected");
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  public void testOr() {
    Pattern multiplesOf2And5 = new Pattern() {
      @Override
      public boolean contains(int i) {
        return i % 2 == 0 || i % 5 == 0;
      }
    };

    // setA will contain all multiples of 2
    BitSet setA = createSetOfMultiples(2);

    // setB will contain all multiples of 5
    BitSet setB = createSetOfMultiples(5);

    // or() the two sets to get both multiples of 2 and 5
    setA.or(setB);

    // verify multiples of 2 and 5
    checkPattern(setA, multiplesOf2And5);

    // or()ing a set to itself should do nothing
    setA.or(setA);

    // verify multiples of 2 and 5
    checkPattern(setA, multiplesOf2And5);

    // or()ing with set identical to itself should do nothing
    setA.or((BitSet) setA.clone());

    // verify multiples of 2 and 5
    checkPattern(setA, multiplesOf2And5);

    // or()ing with an empty set (all falses) should do nothing
    setA.or(new BitSet());

    // verify multiples of 2 and 5
    checkPattern(setA, multiplesOf2And5);

    // or()ing with all trues should result in all trues
    BitSet trueSet = new BitSet(TEST_SIZE * 2);
    trueSet.set(0, TEST_SIZE * 2);
    setA.or(trueSet);
    assertEquals(TEST_SIZE * 2, setA.cardinality());

    setA = new BitSet();
    setA.set(2);
    setB = new BitSet();
    setB.set(1);
    setB.set(3);
    setA.or(setB);
    checkRange(setA, 1, 4);
  }

  public void testSetIntBoolean() {
    BitSet set = new BitSet();
    set.set(0, true);
    assertTrue(set.get(0));
    set.set(0, false);
    assertFalse(set.get(0));
    set.set(BIG_NUMBER, true);
    assertTrue(set.get(BIG_NUMBER));
    set.set(BIG_NUMBER, false);
    assertFalse(set.get(BIG_NUMBER));

    set = new BitSet();
    set.set(1, true);
    set.set(2, true);
    set.set(3, true);
    set.set(4, true);
    set.set(6, true);
    set.set(4, false);
    set.set(6, true);
    set.set(6, false);
    set.set(6, false);
    set.set(8, true);
    set.set(10, true);
    set.set(8, false);
    set.set(2, false);
    set.set(8, true);
    checkValues(set, 1, 3, 8, 10);
    set.set(8, false);
    checkValues(set, 1, 3, 10);
    set.set(3, false);
    checkValues(set, 1, 10);
    set.set(10, false);
    set.set(11, true);
    checkValues(set, 1, 11);
    set.set(1, false);
    checkValues(set, 11);
    set.set(11, false);
    assertTrue(set.isEmpty());
  }

  public void testSetIntIntBoolean() {
    BitSet set = new BitSet();
    set.set(0, BIG_NUMBER, true);
    assertEquals(BIG_NUMBER, set.cardinality());
    set.set(0, BIG_NUMBER - 1, false);
    assertEquals(1, set.cardinality());
    set.set(0, BIG_NUMBER, false);
    assertTrue(set.isEmpty());

    set.set(0, 32, true);
    checkRange(set, 0, 32);
    set.set(0, 8, false);
    checkRange(set, 8, 32);
    set.set(0, 8, true);
    checkRange(set, 0, 32);
    set.set(7, 21, false);
    checkValues(set, 0, 1, 2, 3, 4, 5, 6, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31);
    set.set(22, 27, false);
    checkValues(set, 0, 1, 2, 3, 4, 5, 6, 21, 27, 28, 29, 30, 31);

    set = new BitSet();
    set.set(11, 1000, true);
    set.set(10, 12, false);
    checkRange(set, 12, 1000);
    assertEquals(988, set.cardinality());

    set = new BitSet();
    set.set(10, 12, true);
    set.set(0, 10, true);
    checkRange(set, 0, 12);
    set.set(0, 12, false);
    assertTrue(set.isEmpty());

    set = new BitSet();
    set.set(1, 20, true);
    set.set(5, 10, false);
    checkRange(set, 1, 5, 10, 20);

    set = new BitSet();
    set.set(1, 10, true);
    set.set(5, 10, false);
    set.set(10, 15, true);
    checkRange(set, 1, 5, 10, 15);

    // test exceptions
    try {
      set.set(-1, 2, true);
      fail("exception expected");
    } catch (IndexOutOfBoundsException expected) {
    }

    try {
      set.set(3, 1, true);
      fail("exception expected");
    } catch (IndexOutOfBoundsException expected) {
    }

    set.set(2, 2, true);

    try {
      set.set(-1, 2, false);
      fail("exception expected");
    } catch (IndexOutOfBoundsException expected) {
    }

    try {
      set.set(3, 1, false);
      fail("exception expected");
    } catch (IndexOutOfBoundsException expected) {
    }

    set.set(2, 2, false);
  }

  public void testSize() {
    // this is an unimportant test

    BitSet set = new BitSet(7);
    assertTrue(set.size() >= 7);
    set = new BitSet(BIG_NUMBER);
    assertTrue(set.size() >= BIG_NUMBER);
  }

  public void testToString() {
    BitSet set = new BitSet();
    assertEquals("{}", set.toString());

    set.set(32);
    assertEquals("{32}", set.toString());

    set.set(BIG_NUMBER);
    assertEquals("{32, " + BIG_NUMBER + "}", set.toString());

    set.set(1);
    assertEquals("{1, 32, " + BIG_NUMBER + "}", set.toString());

    set.set(2);
    assertEquals("{1, 2, 32, " + BIG_NUMBER + "}", set.toString());
  }

  public void testXor() {
    Pattern exclusiveMultiples = new Pattern() {
      @Override
      public boolean contains(int i) {
        return (i % 2 == 0) ^ (i % 3 == 0);
      }
    };

    // setA will contain all multiples of 2
    BitSet setA = createSetOfMultiples(2);

    // setB will contain all multiples of 3
    BitSet setB = createSetOfMultiples(3);

    // xor()ing the sets should give exclusive multiples of 2 and 3
    setA.xor(setB);

    // verify by checking for exclusive multiples of 2 and 3
    checkPattern(setA, exclusiveMultiples);

    // xor()ing a set to an empty set should do nothing
    setA.xor(new BitSet());

    // verify by checking for exclusive multiples of 2 and 3
    checkPattern(setA, exclusiveMultiples);

    // xor()ing a set with all trues should flip each bit
    BitSet trueSet = new BitSet(TEST_SIZE * 2);
    trueSet.set(0, TEST_SIZE * 2);
    setA.xor(trueSet);

    // verify by checking for !(exclusive multiples of 2 and 3)
    checkPattern(setA, new Pattern() {
      @Override
      public boolean contains(int i) {
        return !((i % 2 == 0) ^ (i % 3 == 0));
      }
    });
    // there were "TEST_SIZE" extra trues, so verify those came out as true
    for (int i = TEST_SIZE; i < TEST_SIZE * 2; i++) {
      assertTrue(setA.get(i));
    }

    // xor()ing a set to itself should result in an empty set
    setA.xor(setA);
    assertTrue(setA.isEmpty());

    // xor()ing a set identical to itself should result in an empty set
    setB.xor((BitSet) setB.clone());
    assertTrue(setB.isEmpty());

    setA = new BitSet();
    setA.set(2);
    setB = new BitSet();
    setB.set(1);
    setB.set(3);
    setA.xor(setB);
    checkRange(setA, 1, 4);
  }

  public void testToByteArray() {
    BitSet set = new BitSet();
    assertEquals(0, set.toByteArray().length);

    int[] bits = {7, 8, 9, 10, 11, 12, 13, 14, 15, 24, 25, 26, 27, 28, 32, 33, 34, 35, 36, 37, 38,
        47, 49, 51, 52, 53, 54, 55};
    for (int bit : bits) {
      set.set(bit);
    }
    byte[] expected = {(byte) -128, (byte) -1, (byte) 0, (byte) 31,
        (byte) 127, (byte) 128, (byte) 250};
    assertTrue(Arrays.equals(expected, set.toByteArray()));

    set.clear();
    assertEquals(0, set.toByteArray().length);
  }

  public void testToLongArray() {
    BitSet set = new BitSet();
    assertEquals(0, set.toLongArray().length);

    int[] bits = {63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82,
        83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103,
        104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121,
        122, 123, 124, 125, 126, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140,
        141, 142, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158,
        159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176,
        177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 287, 288, 289,
        290, 291, 292, 293, 294, 295, 296, 297, 298, 299, 300, 301, 302, 303, 304, 305, 306, 307,
        308, 309, 310, 311, 312, 313, 314, 315, 316, 317, 318, 319, 320, 321, 322, 323, 324, 325,
        326, 327, 328, 329, 330, 331, 332, 333, 334, 335, 336, 337, 338, 339, 340, 341, 342, 343,
        344, 345, 346, 347, 348, 349, 350, 384, 385, 386, 387, 388, 389, 390, 391, 392, 393, 394,
        395, 396, 397, 398, 399, 400, 401, 402, 403, 404, 405, 406, 407, 408, 409, 410, 411, 412,
        413, 414, 450, 453, 454, 514, 515, 516, 519, 520, 521, 522, 523, 524, 525, 526, 527, 528,
        529, 530, 531, 532, 533, 534, 535, 536, 537, 538, 539, 540, 541, 542, 543, 544, 545, 546,
        547, 548, 549, 550, 551, 552, 553, 554, 555, 556, 557, 558, 559, 560, 561, 562, 563, 564,
        565, 566, 567, 568, 569, 570, 571, 572, 573, 574, 575, 584, 640, 641, 642, 643, 644, 645,
        646, 711};
    for (int bit : bits) {
      set.set(bit);
    }
    long[] expected = {Long.MIN_VALUE, Long.MAX_VALUE, -1, 0, Integer.MIN_VALUE,
        Integer.MAX_VALUE, 0x7fffffff, 100, -100, 256, 127, 128};
    assertTrue(Arrays.equals(expected, set.toLongArray()));

    set.clear();
    assertEquals(0, set.toLongArray().length);
  }

  public void testValueOfBytes() {
    BitSet set = BitSet.valueOf(new byte[0]);
    assertTrue(set.isEmpty());

    set = BitSet.valueOf(new byte[]{(byte) -128, (byte) -1, (byte) 0, (byte) 31,
        (byte) 127, (byte) 128, (byte) 250});
    assertEquals("{7, 8, 9, 10, 11, 12, 13, 14, 15, 24, 25, 26, 27, 28, 32, 33, 34, 35, 36, 37, 38, " +
        "47, 49, 51, 52, 53, 54, 55}", set.toString());
  }

  public void testValueOfLongs() {
    BitSet set = BitSet.valueOf(new long[0]);
    assertTrue(set.isEmpty());

    set = BitSet.valueOf(new long[]{Long.MIN_VALUE, Long.MAX_VALUE, -1, 0, Integer.MIN_VALUE,
        Integer.MAX_VALUE, 0x7fffffff, 100, -100, 256, 127, 128});
    assertEquals("{63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, " +
        "83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, " +
        "104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, " +
        "122, 123, 124, 125, 126, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, " +
        "141, 142, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, " +
        "159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, " +
        "177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 287, 288, 289, " +
        "290, 291, 292, 293, 294, 295, 296, 297, 298, 299, 300, 301, 302, 303, 304, 305, 306, 307, " +
        "308, 309, 310, 311, 312, 313, 314, 315, 316, 317, 318, 319, 320, 321, 322, 323, 324, 325, " +
        "326, 327, 328, 329, 330, 331, 332, 333, 334, 335, 336, 337, 338, 339, 340, 341, 342, 343, " +
        "344, 345, 346, 347, 348, 349, 350, 384, 385, 386, 387, 388, 389, 390, 391, 392, 393, 394, " +
        "395, 396, 397, 398, 399, 400, 401, 402, 403, 404, 405, 406, 407, 408, 409, 410, 411, 412, " +
        "413, 414, 450, 453, 454, 514, 515, 516, 519, 520, 521, 522, 523, 524, 525, 526, 527, 528, " +
        "529, 530, 531, 532, 533, 534, 535, 536, 537, 538, 539, 540, 541, 542, 543, 544, 545, 546, " +
        "547, 548, 549, 550, 551, 552, 553, 554, 555, 556, 557, 558, 559, 560, 561, 562, 563, 564, " +
        "565, 566, 567, 568, 569, 570, 571, 572, 573, 574, 575, 584, 640, 641, 642, 643, 644, 645, " +
        "646, 711}", set.toString());
  }
}
