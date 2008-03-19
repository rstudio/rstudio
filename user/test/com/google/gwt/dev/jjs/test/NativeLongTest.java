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
package com.google.gwt.dev.jjs.test;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Test direct uses of longs. Mostly this tests that LongLib is in fact being
 * invoked in various cases. The behavior of LongLib itself is tested in
 * LongLibTest.
 */
public class NativeLongTest extends GWTTestCase {
  /*
   * These silly looking constants are made into public fields so that the
   * compiler will not constant fold them. The problem is that if you write
   * assertEquals(2L, 4L/2L), the compiler will emit assertEquals(2L, 2L).
   */
  private static long LONG_1234 = 0x1234123412341234L;
  private static long LONG_1234_DECIMAL = 1234123412341234L;
  private static long LONG_1234000012340000 = 0x1234000012340000L;
  private static long LONG_5DEECE66D = 0x5DEECE66DL;
  private static long LONG_B = 0xBL;
  private static long LONG_DEADBEEF = 0xdeadbeefdeadbeefL;
  private static long LONG_DEADBEEF12341234 = 0xdeadbeef12341234L;
  private static long LONG_FFFFFFFF = 0xFFFFFFFFL;
  private static long LONG_ONE = 1L;
  private static long LONG_THREE = 3L;
  private static long LONG_TWO = 2L;
  private static long LONG_TWO_PWR_32 = 0x100000000L;
  private static long LONG_ZERO = 0L;

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  @Override
  public void setUp() {
    pretendToChangeTheConstants();
  }

  public void testArithmetic() {
    assertEquals(-1089359682551557853L, LONG_1234 + LONG_DEADBEEF);
    assertEquals(5024439901525534073L, 2 * LONG_1234 - LONG_DEADBEEF);
    assertEquals(2476047018506819212L, LONG_1234 * LONG_DEADBEEF);
    assertEquals(-240105308887621659L, LONG_DEADBEEF / 10);
    assertEquals(-1089359682551557853L, LONG_DEADBEEF % LONG_1234);
  }

  public void testCasts() {
    assertEquals(0x12341234, (int) LONG_1234);
    assertEquals(0x1234, (short) LONG_1234);
  }

  public void testConstants() {
    assertEquals(LONG_5DEECE66D, LONG_5DEECE66D);
    assertTrue(LONG_5DEECE66D > 0L);
    assertTrue(0L < LONG_5DEECE66D);
    assertEquals(LONG_B, LONG_B);
    assertTrue(LONG_B > 0L);
    assertTrue(0L < LONG_B);
  }

  public void testFor64Bits() {
    long x = LONG_1234;
    pretendToChangeTheConstants();
    long y = LONG_1234 + LONG_ONE;
    long z = y - x;
    // with longs implemented as doubles, z will be 0 instead of 1
    assertEquals(1L, z);
  }

  public void testImplicitCastFromLong() {
    double d = LONG_ONE;
    d += LONG_TWO;
    assertEquals(0.0, 3L, d);
    assertTrue(3L == d);

    float f = LONG_ONE;
    f += LONG_TWO;
    assertEquals(0.0, 3L, f);
    assertTrue(3L == f);
  }

  public void testImplicitCastToLong() {
    long l = 10;
    l += 5;
    assertEquals(15, l);
    assertTrue(15 == l);
  }

  public void testModifyingOps() {
    long l = 20;
    l += 10;
    assertEquals(31, ++l);
    assertEquals(31, l++);
    assertEquals(32, l);
  }

  public void testLogicalAnd() {
    assertEquals(LONG_1234, LONG_1234 & -LONG_ONE);
    assertEquals(0x12341234L, LONG_1234 & LONG_FFFFFFFF);
    assertEquals(0L, LONG_ONE & LONG_ZERO);
    assertEquals(1L, LONG_ONE & LONG_THREE);
  }

  public void testLogicalOr() {
    assertEquals(-1L, LONG_1234 | -LONG_ONE);
    assertEquals(0x12341234FFFFFFFFL, LONG_1234 | LONG_FFFFFFFF);
    assertEquals(1L, LONG_ONE | LONG_ZERO);
    assertEquals(3L, LONG_ONE | LONG_THREE);
  }

  public void testLogicalXor() {
    assertTrue((255L ^ LONG_5DEECE66D) != 0);

    assertEquals(0L, LONG_1234 ^ LONG_1234);
    assertEquals(0x0000123400001234L, LONG_1234 ^ LONG_1234000012340000);
    assertEquals(1L, LONG_ONE ^ LONG_ZERO);
    assertEquals(2L, LONG_ONE ^ LONG_THREE);
  }

  public void testShift() {
    assertEquals(LONG_5DEECE66D, LONG_5DEECE66D & ((LONG_ONE << 48) - 1));
    assertEquals(LONG_ONE << 12, (LONG_ONE << 60) >>> (48));

    assertTrue((LONG_ONE << 35) > (LONG_ONE << 30));

    assertEquals(1L, LONG_TWO_PWR_32 >> 32);
    assertEquals(1L, LONG_TWO_PWR_32 >>> 32);
  }

  // Issue 1198
  public void testToHexString() {
    assertEquals("deadbeef12341234", Long.toHexString(LONG_DEADBEEF12341234));
  }

  public void testToString() {
    assertEquals("1234123412341234", "" + LONG_1234_DECIMAL);
  }

  /**
   * This method tries to trick the compiler into thinking the global constants
   * are not fixed.
   */
  private void pretendToChangeTheConstants() {
    int i = 10;
    while (i > 1) {
      i /= 2;
    }
    if (i == 1) {
      return;
    }

    // not reached
    ++LONG_1234;
    ++LONG_1234_DECIMAL;
    ++LONG_1234000012340000;
    ++LONG_5DEECE66D;
    ++LONG_B;
    ++LONG_DEADBEEF;
    ++LONG_DEADBEEF12341234;
    ++LONG_FFFFFFFF;
    ++LONG_ONE;
    ++LONG_THREE;
    ++LONG_TWO;
    ++LONG_TWO_PWR_32;
    ++LONG_ZERO;
  }
}
