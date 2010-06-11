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
      Double.parseDouble(".");
      fail("parse");
    } catch (NumberFormatException e) {
      // Expected behavior
    }

    try {
      Double.parseDouble(".e");
      fail("parse");
    } catch (NumberFormatException e) {
      // Expected behavior
    }

    try {
      Double.parseDouble("e5");
      fail("parse");
    } catch (NumberFormatException e) {
      // Expected behavior
    }

    try {
      Double.parseDouble(".e5");
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
  
  public void testCompare() {
    assertTrue(Double.compare(Double.NaN, Double.NaN) == 0);
    assertTrue(Double.compare(0.0, Double.NaN) < 0);
    assertTrue(Double.compare(Double.NaN, Double.POSITIVE_INFINITY) > 0);
    assertTrue(Double.compare(Double.NaN, 0.0) > 0);
    assertTrue(Double.compare(Double.POSITIVE_INFINITY, Double.NaN) < 0);
    assertTrue(Double.compare(3.0, 500.0) < 0);
    assertTrue(Double.compare(500.0, 3.0) > 0);
    assertTrue(Double.compare(500.0, 500.0) == 0);
  }
  
  public void testCompareTo() {
    Double zero = new Double(0.0);
    Double three = new Double(3.0);
    Double fiveHundred = new Double(500.0);
    Double infinity = new Double(Double.POSITIVE_INFINITY);
    Double nan = new Double(Double.NaN);
    
    assertTrue(nan.compareTo(nan) == 0);
    assertTrue(zero.compareTo(nan) < 0);
    assertTrue(nan.compareTo(infinity) > 0);
    assertTrue(nan.compareTo(zero) > 0);
    assertTrue(infinity.compareTo(nan) < 0);
    assertTrue(three.compareTo(fiveHundred) < 0);
    assertTrue(fiveHundred.compareTo(three) > 0);
    assertTrue(fiveHundred.compareTo(fiveHundred) == 0);
  }

  public void testDoubleConstants() {
    assertTrue(Double.isNaN(Double.NaN));
    assertTrue(Double.isInfinite(Double.NEGATIVE_INFINITY));
    assertTrue(Double.isInfinite(Double.POSITIVE_INFINITY));
    assertTrue(Double.NEGATIVE_INFINITY < Double.POSITIVE_INFINITY);
    assertTrue(Double.MIN_VALUE < Double.MAX_VALUE);
    assertFalse(Double.NaN == Double.NaN);
    assertEquals(64, Double.SIZE);
    // jdk1.6 assertEquals(Math.getExponent(Double.MAX_VALUE),
    // Double.MAX_EXPONENT);
    // jdk1.6 assertEquals(Math.getExponent(Double.MIN_NORMAL),
    // Double.MIN_EXPONENT);
  }

  public void testParse() {
    assertTrue(0 == Double.parseDouble("0"));
    assertTrue(100 == Double.parseDouble("1e2"));
    assertTrue(-100 == Double.parseDouble("-1e2"));
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
    
    // Test that leading and trailing whitespace is ignored
    // Test that both 'e' and 'E' may be used as the exponent delimiter
    assertTrue(2.56789e1 == Double.parseDouble("2.56789e1"));
    assertTrue(2.56789e1 == Double.parseDouble("  2.56789E+1"));
    assertTrue(2.56789e1 == Double.parseDouble("2.56789e1   "));
    assertTrue(2.56789e1 == Double.parseDouble("   2.56789E01   "));
    assertTrue(2.56789e1 == Double.parseDouble("+2.56789e1"));
    assertTrue(2.56789e1 == Double.parseDouble("  +2.56789E+01"));
    assertTrue(2.56789e1 == Double.parseDouble("+2.56789e1   "));
    assertTrue(2.56789e1 == Double.parseDouble("   +2.56789E1   "));
    assertTrue(-2.56789e1 == Double.parseDouble("-2.56789e+1"));
    assertTrue(-2.56789e1 == Double.parseDouble("  -2.56789E1"));
    assertTrue(-2.56789e1 == Double.parseDouble("-2.56789e+01   "));
    assertTrue(-2.56789e1 == Double.parseDouble("   -2.56789E1   "));
  }
}
