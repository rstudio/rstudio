/*
 * Copyright 2006 Google Inc.
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

/**
 * Tests <code>Float</code>.
 */
public class FloatTest extends NumberTestCase {

  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  // Number does not have a compareTo() method.
  public void testCompareTo() {
    assertTrue(-1 >= ((Float) createNegative()).compareTo(createPositive()));
    assertTrue(1 <= ((Float) createPositive()).compareTo(createNegative()));
    assertEquals(0, ((Float) createPositive()).compareTo(createPositive()));
  }

  public void testFloatConstants() {
    assertTrue(Float.isNaN(Float.NaN));
    assertTrue(Float.isInfinite(Float.NEGATIVE_INFINITY));
    assertTrue(Float.isInfinite(Float.POSITIVE_INFINITY));
    assertTrue(Float.NEGATIVE_INFINITY < Float.POSITIVE_INFINITY);
    assertFalse(Float.NaN == Float.NaN);
  }

  public void testParse() {
    assertEquals(positive(), createPositiveFromString().doubleValue(), DELTA);
    try {
      new Float(totallyWrongString());
      fail();
    } catch (NumberFormatException e) {
      // pass
    }
    try {
      new Float(wrongString());
      fail();
    } catch (NumberFormatException e) {
      // pass
    }
    assertEquals(new Float("1e50"), new Float("Infinity"));
    assertEquals(new Float("-1e50"), new Float("-Infinity"));
    assertEquals(new Float("1e-50"), new Float("0.0"));
    assertEquals(new Float("-1e-50"), new Float("-0.0"));
  }

  protected NumberFactory createNumberFactory() {
    return new NumberFactory() {

      public Number create(double x) {
        return new Float((float)x);
      }

      public Number create(String x) {
        return new Float(x);
      }

      public long decode(String x) {
        throw new RuntimeException("decode not implemented.");
      }

      public int maxDecimalPoints() {
        return 1;
      }

      public int maxDigits() {
        return 20;
      }

      public int maxExponents() {
        return 1;
      }

      public int maxMinusSigns() {
        return 1;
      }

      public double valueOf(String s) {
        return Float.valueOf(s).doubleValue();
      }

      public double valueOf(String s, int radix) {
        throw new RuntimeException("valueOf not implemented with radix.");
      }
    };
  }

  protected String negativeString() {
    return "-123.0";
  }

  protected String positiveString() {
    return "123.0";
  }
}
