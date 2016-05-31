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

package com.google.gwt.emultest.java8.lang;

import com.google.gwt.junit.client.GWTTestCase;

import java.math.BigInteger;
import java.util.ArrayList;

/**
 * Tests for JRE emulation of java.lang.Math.
 */
public class MathTest extends GWTTestCase {

  private static final Integer[] ALL_INTEGER_CANDIDATES = getAllIntegerCandidates();
  private static final Long[] ALL_LONG_CANDIDATES = getAllLongCandidates();

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testAddExact() {
    for (int a : ALL_INTEGER_CANDIDATES) {
      for (int b : ALL_INTEGER_CANDIDATES) {
        BigInteger expectedResult = BigInteger.valueOf(a).add(BigInteger.valueOf(b));
        boolean expectedSuccess = fitsInInt(expectedResult);
        try {
          assertEquals(a + b, Math.addExact(a, b));
          assertTrue(expectedSuccess);
        } catch (ArithmeticException e) {
          assertFalse(expectedSuccess);
        }
      }
    }
  }

  public void testAddExactLongs() {
    for (long a : ALL_LONG_CANDIDATES) {
      for (long b : ALL_LONG_CANDIDATES) {
        BigInteger expectedResult = BigInteger.valueOf(a).add(BigInteger.valueOf(b));
        boolean expectedSuccess = fitsInLong(expectedResult);
        try {
          assertEquals(a + b, Math.addExact(a, b));
          assertTrue(expectedSuccess);
        } catch (ArithmeticException e) {
          assertFalse(expectedSuccess);
        }
      }
    }
  }

  public void testDecrementExact() {
    for (int a : ALL_INTEGER_CANDIDATES) {
      BigInteger expectedResult = BigInteger.valueOf(a).subtract(BigInteger.ONE);
      boolean expectedSuccess = fitsInInt(expectedResult);
      try {
        assertEquals(a - 1, Math.decrementExact(a));
        assertTrue(expectedSuccess);
      } catch (ArithmeticException e) {
        assertFalse(expectedSuccess);
      }
    }
  }

  public void testDecrementExactLong() {
    for (long a : ALL_LONG_CANDIDATES) {
      BigInteger expectedResult = BigInteger.valueOf(a).subtract(BigInteger.ONE);
      boolean expectedSuccess = fitsInLong(expectedResult);
      try {
        assertEquals(a - 1, Math.decrementExact(a));
        assertTrue(expectedSuccess);
      } catch (ArithmeticException e) {
        assertFalse(expectedSuccess);
      }
    }
  }

  public void testFloorDiv() {
    assertEquals(0, Math.floorDiv(0, 1));
    assertEquals(1, Math.floorDiv(4, 3));
    assertEquals(-2, Math.floorDiv(4, -3));
    assertEquals(-2, Math.floorDiv(-4, 3));
    assertEquals(1, Math.floorDiv(-4, -3));
    assertEquals(1, Math.floorDiv(Integer.MIN_VALUE, Integer.MIN_VALUE));
    assertEquals(1, Math.floorDiv(Integer.MAX_VALUE, Integer.MAX_VALUE));
    assertEquals(Integer.MIN_VALUE, Math.floorDiv(Integer.MIN_VALUE, 1));
    assertEquals(Integer.MAX_VALUE, Math.floorDiv(Integer.MAX_VALUE, 1));

    // special case
    assertEquals(Integer.MIN_VALUE, Math.floorDiv(Integer.MIN_VALUE, -1));

    try {
      Math.floorDiv(1, 0);
      fail();
    } catch (ArithmeticException expected) {
    }
  }

  public void testFloorDivLongs() {
    assertEquals(0L, Math.floorDiv(0L, 1L));
    assertEquals(1L, Math.floorDiv(4L, 3L));
    assertEquals(-2L, Math.floorDiv(4L, -3L));
    assertEquals(-2L, Math.floorDiv(-4L, 3L));
    assertEquals(1L, Math.floorDiv(-4L, -3L));
    assertEquals(1L, Math.floorDiv(Long.MIN_VALUE, Long.MIN_VALUE));
    assertEquals(1L, Math.floorDiv(Long.MAX_VALUE, Long.MAX_VALUE));
    assertEquals(Long.MIN_VALUE, Math.floorDiv(Long.MIN_VALUE, 1L));
    assertEquals(Long.MAX_VALUE, Math.floorDiv(Long.MAX_VALUE, 1L));

    // special case
    assertEquals(Long.MIN_VALUE, Math.floorDiv(Long.MIN_VALUE, -1));

    try {
      Math.floorDiv(1L, 0L);
      fail();
    } catch (ArithmeticException expected) {
    }
  }

  public void testFloorMod() {
    assertEquals(0, Math.floorMod(0, 1));
    assertEquals(1, Math.floorMod(4, 3));
    assertEquals(-2, Math.floorMod(4, -3));
    assertEquals(2, Math.floorMod(-4, 3));
    assertEquals(-1, Math.floorMod(-4, -3));
    assertEquals(0, Math.floorMod(Integer.MIN_VALUE, Integer.MIN_VALUE));
    assertEquals(0, Math.floorMod(Integer.MAX_VALUE, Integer.MAX_VALUE));
    assertEquals(0, Math.floorMod(Integer.MIN_VALUE, 1));
    assertEquals(0, Math.floorMod(Integer.MAX_VALUE, 1));

    try {
      Math.floorMod(1, 0);
      fail();
    } catch (ArithmeticException expected) {
    }
  }

  public void testFloorModLongs() {
    assertEquals(0L, Math.floorMod(0L, 1L));
    assertEquals(1L, Math.floorMod(4L, 3L));
    assertEquals(-2L, Math.floorMod(4L, -3L));
    assertEquals(2L, Math.floorMod(-4L, 3L));
    assertEquals(-1L, Math.floorMod(-4L, -3L));
    assertEquals(0L, Math.floorMod(Long.MIN_VALUE, Long.MIN_VALUE));
    assertEquals(0L, Math.floorMod(Long.MAX_VALUE, Long.MAX_VALUE));
    assertEquals(0L, Math.floorMod(Long.MIN_VALUE, 1L));
    assertEquals(0L, Math.floorMod(Long.MAX_VALUE, 1L));

    try {
      Math.floorMod(1L, 0L);
      fail();
    } catch (ArithmeticException expected) {
    }
  }

  public void testIncrementExact() {
    for (int a : ALL_INTEGER_CANDIDATES) {
      BigInteger expectedResult = BigInteger.valueOf(a).add(BigInteger.ONE);
      boolean expectedSuccess = fitsInInt(expectedResult);
      try {
        assertEquals(a + 1, Math.incrementExact(a));
        assertTrue(expectedSuccess);
      } catch (ArithmeticException e) {
        assertFalse(expectedSuccess);
      }
    }
  }

  public void testIncrementExactLong() {
    for (long a : ALL_LONG_CANDIDATES) {
      BigInteger expectedResult = BigInteger.valueOf(a).add(BigInteger.ONE);
      boolean expectedSuccess = fitsInLong(expectedResult);
      try {
        assertEquals(a + 1, Math.incrementExact(a));
        assertTrue(expectedSuccess);
      } catch (ArithmeticException e) {
        assertFalse(expectedSuccess);
      }
    }
  }

  public void testMultiplyExact() {
    for (int a : ALL_INTEGER_CANDIDATES) {
      for (int b : ALL_INTEGER_CANDIDATES) {
        BigInteger expectedResult = BigInteger.valueOf(a).multiply(BigInteger.valueOf(b));
        boolean expectedSuccess = fitsInInt(expectedResult);
        try {
          assertEquals(a * b, Math.multiplyExact(a, b));
          assertTrue(expectedSuccess);
        } catch (ArithmeticException e) {
          assertFalse(expectedSuccess);
        }
      }
    }
  }

  public void testMultiplyExactLongs() {
    for (long a : ALL_LONG_CANDIDATES) {
      for (long b : ALL_LONG_CANDIDATES) {
        BigInteger expectedResult = BigInteger.valueOf(a).multiply(BigInteger.valueOf(b));
        boolean expectedSuccess = fitsInLong(expectedResult);
        try {
          assertEquals(a * b, Math.multiplyExact(a, b));
          assertTrue(expectedSuccess);
        } catch (ArithmeticException e) {
          assertFalse(expectedSuccess);
        }
      }
    }
  }

  public void testNegateExact() {
    for (int a : ALL_INTEGER_CANDIDATES) {
      BigInteger expectedResult = BigInteger.valueOf(a).negate();
      boolean expectedSuccess = fitsInInt(expectedResult);
      try {
        assertEquals(-a, Math.negateExact(a));
        assertTrue(expectedSuccess);
      } catch (ArithmeticException e) {
        assertFalse(expectedSuccess);
      }
    }
  }

  public void testNegateExactLong() {
    for (long a : ALL_LONG_CANDIDATES) {
      BigInteger expectedResult = BigInteger.valueOf(a).negate();
      boolean expectedSuccess = fitsInLong(expectedResult);
      try {
        assertEquals(-a, Math.negateExact(a));
        assertTrue(expectedSuccess);
      } catch (ArithmeticException e) {
        assertFalse(expectedSuccess);
      }
    }
  }

  public void testSubtractExact() {
    for (int a : ALL_INTEGER_CANDIDATES) {
      for (int b : ALL_INTEGER_CANDIDATES) {
        BigInteger expectedResult = BigInteger.valueOf(a).subtract(BigInteger.valueOf(b));
        boolean expectedSuccess = fitsInInt(expectedResult);
        try {
          assertEquals(a - b, Math.subtractExact(a, b));
          assertTrue(expectedSuccess);
        } catch (ArithmeticException e) {
          assertFalse(expectedSuccess);
        }
      }
    }
  }

  public void testSubtractExactLongs() {
    for (long a : ALL_LONG_CANDIDATES) {
      for (long b : ALL_LONG_CANDIDATES) {
        BigInteger expectedResult = BigInteger.valueOf(a).subtract(BigInteger.valueOf(b));
        boolean expectedSuccess = fitsInLong(expectedResult);
        try {
          assertEquals(a - b, Math.subtractExact(a, b));
          assertTrue(expectedSuccess);
        } catch (ArithmeticException e) {
          assertFalse(expectedSuccess);
        }
      }
    }
  }

  public void testToIntExact() {
    final long[] longs = {0, -1, 1, Integer.MIN_VALUE, Integer.MAX_VALUE,
        Integer.MIN_VALUE - 1L, Integer.MAX_VALUE + 1L, Long.MIN_VALUE, Long.MAX_VALUE};
    for (long a : longs) {
      boolean expectedSuccess = (int) a == a;
      try {
        assertEquals((int) a, Math.toIntExact(a));
        assertTrue(expectedSuccess);
      } catch (ArithmeticException e) {
        assertFalse(expectedSuccess);
      }
    }
  }

  private static boolean fitsInInt(BigInteger big) {
    return big.bitLength() < Integer.SIZE;
  }

  private static boolean fitsInLong(BigInteger big) {
    return big.bitLength() < Long.SIZE;
  }

  private static Integer[] getAllIntegerCandidates() {
    ArrayList<Integer> candidates = new ArrayList<Integer>();
    candidates.add(0);
    candidates.add(-1);
    candidates.add(1);
    candidates.add(Integer.MAX_VALUE / 2);
    candidates.add(Integer.MAX_VALUE / 2 - 1);
    candidates.add(Integer.MAX_VALUE / 2 + 1);
    candidates.add(Integer.MIN_VALUE / 2);
    candidates.add(Integer.MIN_VALUE / 2 - 1);
    candidates.add(Integer.MIN_VALUE / 2 + 1);
    candidates.add(Integer.MAX_VALUE - 1);
    candidates.add(Integer.MAX_VALUE);
    candidates.add(Integer.MIN_VALUE + 1);
    candidates.add(Integer.MIN_VALUE);
    return candidates.toArray(new Integer[candidates.size()]);
  }

  private static Long[] getAllLongCandidates() {
    ArrayList<Long> candidates = new ArrayList<Long>();

    for (Integer x : getAllIntegerCandidates()) {
      candidates.add(x.longValue());
    }

    candidates.add(Long.MAX_VALUE / 2);
    candidates.add(Long.MAX_VALUE / 2 - 1);
    candidates.add(Long.MAX_VALUE / 2 + 1);
    candidates.add(Long.MIN_VALUE / 2);
    candidates.add(Long.MIN_VALUE / 2 - 1);
    candidates.add(Long.MIN_VALUE / 2 + 1);
    candidates.add(Integer.MAX_VALUE + 1L);
    candidates.add(Long.MAX_VALUE - 1L);
    candidates.add(Long.MAX_VALUE);
    candidates.add(Integer.MIN_VALUE - 1L);
    candidates.add(Long.MIN_VALUE + 1L);
    candidates.add(Long.MIN_VALUE);

    return candidates.toArray(new Long[candidates.size()]);
  }
}
