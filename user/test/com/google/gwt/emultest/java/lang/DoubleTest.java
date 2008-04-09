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

package com.google.gwt.emultest.java.lang;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Unit tests for the emulated-in-Javascript Double/double autoboxed types.
 */
public class DoubleTest extends GWTTestCase {

  // Some actual results from JDK1.6 VM doubleToLongBits calls    
  private static final long NAN_LONG_VALUE = 0x7ff8000000000000L;
  private static final long POSINF_LONG_VALUE = 0x7ff0000000000000L;
  private static final long NEGINF_LONG_VALUE = 0xfff0000000000000L;
  private static final long MAXD_LONG_VALUE = 0x7fefffffffffffffL;
  private static final long MIND_LONG_VALUE = 0x1L;
  private static final long MINNORM_LONG_VALUE = 0x10000000000000L;
  private static final double TEST1_DOUBLE_VALUE = 2.3e27;
  private static final long TEST1_LONG_VALUE = 0x459dba0fc757e49cL;
  private static final long NEGTEST1_LONG_VALUE = 0xc59dba0fc757e49cL;

  // TODO(fabbott): this constants are from the JDK 1.6 Double, so we can't rely on them
  // when we build on 1.5! But when we *do* support 1.6, this def'n should go away
  public static final double MIN_NORMAL = 2.2250738585072014e-308;
  

  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testBadStrings() {
    try {
      new Double("0.0e");
      fail("constructor");
    } catch (NumberFormatException e) {
      // Expected behavior
    }

    try {
      Double.parseDouble("0.0e");
      fail("parse");
    } catch (NumberFormatException e) {
      // Expected behavior
    }

    try {
      Double.valueOf("0x0e");
      fail("valueOf");
    } catch (NumberFormatException e) {
      // Expected behavior
    }
  }

  public void testDoubleConstants() {
    assertTrue(Double.isNaN(Double.NaN));
    assertTrue(Double.isInfinite(Double.NEGATIVE_INFINITY));
    assertTrue(Double.isInfinite(Double.POSITIVE_INFINITY));
    assertTrue(Double.NEGATIVE_INFINITY < Double.POSITIVE_INFINITY);
    assertTrue(Double.MIN_VALUE < Double.MAX_VALUE);
    assertFalse(Double.NaN == Double.NaN);
    assertEquals(64, Double.SIZE);
    // jdk1.6  assertEquals(Math.getExponent(Double.MAX_VALUE), Double.MAX_EXPONENT);
    // jdk1.6  assertEquals(Math.getExponent(Double.MIN_NORMAL), Double.MIN_EXPONENT);
  }

  public void testDoubleToLongBits() {
    assertEquals(Double.doubleToLongBits(Double.NaN), NAN_LONG_VALUE);
    assertEquals(Double.doubleToLongBits(Double.POSITIVE_INFINITY), POSINF_LONG_VALUE);
    assertEquals(Double.doubleToLongBits(Double.NEGATIVE_INFINITY), NEGINF_LONG_VALUE);
    assertEquals(Double.doubleToLongBits(Double.MAX_VALUE), MAXD_LONG_VALUE);
    assertEquals(Double.doubleToLongBits(Double.MIN_VALUE), MIND_LONG_VALUE);
    assertEquals(Double.doubleToLongBits(Double.MAX_VALUE), MAXD_LONG_VALUE);
    assertEquals(Double.doubleToLongBits(Double.MIN_VALUE), MIND_LONG_VALUE);
    assertEquals(Double.doubleToLongBits(TEST1_DOUBLE_VALUE), TEST1_LONG_VALUE);
    assertEquals(Double.doubleToLongBits(-TEST1_DOUBLE_VALUE), NEGTEST1_LONG_VALUE);
    // TODO(fabbott): swap back to Double.MIN_NORMAL when we use jdk 1.6
    assertEquals(Double.doubleToLongBits(MIN_NORMAL), MINNORM_LONG_VALUE);
  }
  
  public void testLongBitsToDouble() {
    assertTrue(Double.isNaN(Double.longBitsToDouble(NAN_LONG_VALUE)));
    assertTrue(Double.POSITIVE_INFINITY == Double.longBitsToDouble(POSINF_LONG_VALUE));
    assertTrue(Double.NEGATIVE_INFINITY == Double.longBitsToDouble(NEGINF_LONG_VALUE));
    assertTrue(Double.MAX_VALUE == Double.longBitsToDouble(MAXD_LONG_VALUE));
    assertTrue(Double.MIN_VALUE == Double.longBitsToDouble(MIND_LONG_VALUE));
    assertTrue(TEST1_DOUBLE_VALUE == Double.longBitsToDouble(TEST1_LONG_VALUE));
    assertTrue(-TEST1_DOUBLE_VALUE == Double.longBitsToDouble(NEGTEST1_LONG_VALUE));
    // TODO(fabbott): swap back to Double.MIN_NORMAL when we use jdk 1.6
    assertTrue(MIN_NORMAL == Double.longBitsToDouble(MINNORM_LONG_VALUE));
  }

  public void testParse() {
    assertTrue(0 == Double.parseDouble("0"));
    assertTrue(-1.5 == Double.parseDouble("-1.5"));
    assertTrue(3.0 == Double.parseDouble("3."));
    assertTrue(0.5 == Double.parseDouble(".5"));
    assertTrue(2.98e8 == Double.parseDouble("2.98e8"));
    assertTrue(-2.98e-8 == Double.parseDouble("-2.98e-8"));
    assertTrue(+2.98E+8 == Double.parseDouble("+2.98E+8"));
    assertTrue(
        "Can't parse MIN_VALUE",
        Double.MIN_VALUE == Double.parseDouble(String.valueOf(Double.MIN_VALUE)));
    assertTrue(
        "Can't parse MAX_VALUE",
        Double.MAX_VALUE == Double.parseDouble(String.valueOf(Double.MAX_VALUE)));
  }
}
