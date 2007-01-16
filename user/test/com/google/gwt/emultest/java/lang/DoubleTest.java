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

import java.util.ArrayList;
import java.util.List;

/**
 * Tests <code>Double</code>.
 */
public class DoubleTest extends NumberTestCase {

  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  // Number does not have a compareTo() method.
  public void testCompareTo() {
    assertTrue(-1 >= ((Double) createNegative()).compareTo(createPositive()));
    assertTrue(1 <= ((Double) createPositive()).compareTo(createNegative()));
    assertEquals(0, ((Double) createPositive()).compareTo(createPositive()));
  }

  public void testDoubleConstants() {
    assertTrue(Double.isNaN(Double.NaN));
    assertTrue(Double.isInfinite(Double.NEGATIVE_INFINITY));
    assertTrue(Double.isInfinite(Double.POSITIVE_INFINITY));
    assertTrue(Double.NEGATIVE_INFINITY < Double.POSITIVE_INFINITY);
    assertFalse(Double.NaN == Double.NaN);
  }

  public void testParse() {
    assertEquals(positive(), createPositiveFromString().doubleValue(), DELTA);
    try {
      new Double(totallyWrongString());
      fail();
    } catch (NumberFormatException e) {
      // pass
    }
    try {
      new Double(wrongString());
      fail();
    } catch (NumberFormatException e) {
      // pass
    }
    
    // Safari thinks that 1e500 is not a number.
    // Oh, NaN is not equals to NaN, by the way.
    List infinityOrNaN = new ArrayList();
    infinityOrNaN.add("NaN");
    infinityOrNaN.add("Infinity");
    List negInfinityOrNaN = new ArrayList();
    negInfinityOrNaN.add("NaN");
    negInfinityOrNaN.add("-Infinity");
    assertTrue(infinityOrNaN.contains(new Double("1e500").toString()));
    assertTrue(negInfinityOrNaN.contains(new Double("-1e500").toString()));
    assertEquals(new Double("1e-500"), new Double("0.0"));
    assertEquals(new Double("-1e-500"), new Double("-0.0"));
  }

  protected NumberFactory createNumberFactory() {
    return new NumberFactory() {

      public Number create(double x) {
        return new Double(x);
      }

      public Number create(String x) {
        return new Double(x);
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
        return Double.valueOf(s).doubleValue();
      }

      public double valueOf(String s, int radix) {
        throw new RuntimeException("valueOf with radix not implemented.");
      }
    };
  }
}
