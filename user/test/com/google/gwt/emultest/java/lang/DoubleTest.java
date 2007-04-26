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
 * TODO: document me.
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
  }

  public void testParse() {
    assertTrue(0 == Double.parseDouble("0"));
    assertTrue(-1.5 == Double.parseDouble("-1.5"));
    assertTrue(3.0 == Double.parseDouble("3."));
    assertTrue(0.5 == Double.parseDouble(".5"));
    assertTrue(2.98e8 == Double.parseDouble("2.98e8"));
    assertTrue(-2.98e-8 == Double.parseDouble("-2.98e-8"));
    assertTrue(+2.98E+8 == Double.parseDouble("+2.98E+8"));
    assertTrue("Can't parse MIN_VALUE",
      Double.MIN_VALUE == Double.parseDouble(String.valueOf(Double.MIN_VALUE)));
    assertTrue("Can't parse MAX_VALUE",
      Double.MAX_VALUE == Double.parseDouble(String.valueOf(Double.MAX_VALUE)));
  }
}
