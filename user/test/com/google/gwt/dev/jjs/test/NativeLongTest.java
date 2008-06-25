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
   * These constants are done as volatile fields so that the compiler will not
   * constant fold them. The problem is that if you write assertEquals(2L,
   * 4L/2L), the compiler will emit assertEquals(2L, 2L).
   */
  private static volatile byte BYTE_FOUR = (byte) 4;
  private static volatile char CHAR_FOUR = (char) 4;
  private static volatile int INT_FOUR = 4;
  private static volatile long LONG_100 = 100L;
  private static volatile long LONG_1234 = 0x1234123412341234L;
  private static volatile long LONG_1234_DECIMAL = 1234123412341234L;
  private static volatile long LONG_1234000012340000 = 0x1234000012340000L;
  private static volatile long LONG_5DEECE66D = 0x5DEECE66DL;
  private static volatile long LONG_B = 0xBL;
  private static volatile long LONG_DEADBEEF = 0xdeadbeefdeadbeefL;
  private static volatile long LONG_DEADBEEF12341234 = 0xdeadbeef12341234L;
  private static volatile long LONG_FFFFFFFF = 0xFFFFFFFFL;
  private static volatile long LONG_FOUR = 4L;
  private static volatile long LONG_ONE = 1L;
  private static volatile long LONG_THREE = 3L;
  private static volatile long LONG_TWO = 2L;
  private static volatile long LONG_TWO_PWR_32 = 0x100000000L;
  private static volatile long LONG_ZERO = 0L;
  private static volatile short SHORT_FOUR = (short) 4;

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testArithmetic() {
    assertEquals(-1089359682551557853L, LONG_1234 + LONG_DEADBEEF);
    assertEquals(5024439901525534073L, 2 * LONG_1234 - LONG_DEADBEEF);
    assertEquals(2476047018506819212L, LONG_1234 * LONG_DEADBEEF);
    assertEquals(-240105308887621659L, LONG_DEADBEEF / 10);
    assertEquals(-1089359682551557853L, LONG_DEADBEEF % LONG_1234);
  }

  public void testArrayInitializer() {
    long[] longs = new long[3];
    assertEquals(longs[1], 0L);
    long[][] longs2 = new long[3][3];
    assertEquals(longs2[1][1], 0L);
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

  public void testModifyingOps() {
    long l = 20;
    l += INT_FOUR;
    assertEquals(25, ++l);
    assertEquals(25, l++);
    assertEquals(26, l);
    l += BYTE_FOUR;
    assertEquals(30, l);
    l += CHAR_FOUR;
    assertEquals(34, l);
    l += SHORT_FOUR;
    assertEquals(38, l);
    l += INT_FOUR;
    assertEquals(42, l);
  }

  public void testShift() {
    assertEquals(LONG_5DEECE66D, LONG_5DEECE66D & ((LONG_ONE << 48) - 1));
    assertEquals(LONG_ONE << 12, (LONG_ONE << 60) >>> (48));

    assertTrue((LONG_ONE << 35) > (LONG_ONE << 30));
    assertEquals(0x10L, LONG_ONE << BYTE_FOUR);
    assertEquals(0x10L, LONG_ONE << CHAR_FOUR);
    assertEquals(0x10L, LONG_ONE << SHORT_FOUR);
    assertEquals(0x10L, LONG_ONE << INT_FOUR);
    assertEquals(0x10L, LONG_ONE << LONG_FOUR);

    assertEquals(1L, LONG_TWO_PWR_32 >> 32);
    assertEquals(1L, LONG_TWO_PWR_32 >>> 32);
  }

  public void testStringAppend() {
    long x = LONG_100;
    assertEquals("100 is a long", x + " is a long");
    assertEquals("a long: 100", "a long: " + x);
  }

  // Issue 1198
  public void testToHexString() {
    assertEquals("deadbeef12341234", Long.toHexString(LONG_DEADBEEF12341234));
  }

  public void testToString() {
    assertEquals("1234123412341234", "" + LONG_1234_DECIMAL);
  }
}
