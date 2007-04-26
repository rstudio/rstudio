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
  }

  public void testParse() {
    assertTrue(0 == Float.parseFloat("0"));
    assertTrue(-1.5 == Float.parseFloat("-1.5"));
    assertTrue(3.0 == Float.parseFloat("3."));
    assertTrue(0.5 == Float.parseFloat(".5"));
    assertTrue("Can't parse MAX_VALUE",
        Float.MAX_VALUE == Float.parseFloat(String.valueOf(Float.MAX_VALUE)));
    assertTrue("Can't parse MIN_VALUE",
        Float.MIN_VALUE == Float.parseFloat(String.valueOf(Float.MIN_VALUE)));
  }
}
