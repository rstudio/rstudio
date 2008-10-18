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
 * Unit tests for the Javascript emulation of the Float/float autoboxed
 * fundamental type.
 */
public class FloatTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testBadStrings() {
    try {
      new Float("0.0e");
      fail("constructor");
    } catch (NumberFormatException e) {
      // Expected behavior
    }

    try {
      Float.parseFloat("0.0e");
      fail("parse");
    } catch (NumberFormatException e) {
      // Expected behavior
    }

    try {
      Float.valueOf("0x0e");
      fail("valueOf");
    } catch (NumberFormatException e) {
      // Expected behavior
    }
  }

  public void testFloatConstants() {
    assertTrue(Float.isNaN(Float.NaN));
    assertTrue(Float.isInfinite(Float.NEGATIVE_INFINITY));
    assertTrue(Float.isInfinite(Float.POSITIVE_INFINITY));
    assertTrue(Float.NEGATIVE_INFINITY < Float.POSITIVE_INFINITY);
    assertTrue(Float.MIN_VALUE < Float.MAX_VALUE);
    assertFalse(Float.NaN == Float.NaN);
    assertEquals(Float.SIZE, 32);
    // jdk1.6 assertEquals(Float.MIN_EXPONENT,
    // Math.getExponent(Float.MIN_NORMAL));
    // jdk1.6 assertEquals(Float.MAX_EXPONENT,
    // Math.getExponent(Float.MAX_VALUE));
  }

  public void testParse() {
    /*
     * Note: we must use appropriate deltas for a somewhat subtle reason.
     * Parsing a string like "1.4e-45" in JS will return the closest DOUBLE
     * rather than the closest float. The value of the parse will not be the
     * same as the value of the same string literal interpreted as a float in
     * Java.
     */
    assertEquals(0f, Float.parseFloat("0"), 0.0);
    assertEquals(-1.5f, Float.parseFloat("-1.5"), 0.0);
    assertEquals(3.0f, Float.parseFloat("3."), 0.0);
    assertEquals(0.5f, Float.parseFloat(".5"), 0.0);
    assertEquals("Can't parse MAX_VALUE", Float.MAX_VALUE,
        Float.parseFloat(String.valueOf(Float.MAX_VALUE)), 1e31);
    assertEquals("Can't parse MIN_VALUE", Float.MIN_VALUE,
        Float.parseFloat(String.valueOf(Float.MIN_VALUE)), Float.MIN_VALUE);
  }
}
