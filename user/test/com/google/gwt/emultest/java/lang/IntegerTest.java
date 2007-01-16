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
 * Tests Integer operations, and parsing.  It gets more tests from 
 * <code>NumberTestCase</code>.
 */
public class IntegerTest extends NumberTestCase {
  private static final String NEGATIVE_BINARY = "11111111111111111111111110000101";
  private static final String NEGATIVE_HEX = "ffffff85";
  private static final String POSITIVE_BINARY = "1111011";
  private static final String POSITIVE_HEX = "7b";
  private static final int ZERO = 0;
  private static final String ZERO_STRING = "0";

  public void testBinaryString() {
    assertEquals(POSITIVE_BINARY, Integer.toBinaryString((int) positive()));
    assertEquals(ZERO_STRING, Integer.toBinaryString(ZERO));
    assertEquals(NEGATIVE_BINARY, Integer.toBinaryString((int) negative()));
  }

  // Number does not have a compareTo() method.
  public void testCompareTo() {
    assertTrue(-1 >= ((Integer) createNegative()).compareTo(createPositive()));
    assertTrue(1 <= ((Integer) createPositive()).compareTo(createNegative()));
    assertEquals(0, ((Integer) createPositive()).compareTo(createPositive()));
  }

  public void testHexString() {
    assertEquals(POSITIVE_HEX, Integer.toHexString((int) positive()));
    assertEquals(ZERO_STRING, Integer.toHexString(ZERO));
    assertEquals(NEGATIVE_HEX, Integer.toHexString((int) negative()));
  }

  public void testOverflow() {
    assertEquals("short overflow", (short) (overflowShort()),
        createOverflowShort().shortValue());
    assertEquals("byte overflow", (byte) overflowByte(),
        createOverflowByte().byteValue());
  }
  

  protected NumberFactory createNumberFactory() {
      return new NumberFactory() {

        public Number create(double x) {
          return new Integer((int) x);
        }

        public Number create(String x) {
          return new Integer(x);
        }

        public long decode(String x) {
          return Integer.decode(x).longValue();
        }

        public int maxDecimalPoints() {
          return 0;
        }

        public int maxDigits() {
          return 9;
        }

        public int maxExponents() {
          return 0;
        }

        public int maxMinusSigns() {
          return 1;
        }

        public double valueOf(String s) {
          return Integer.valueOf(s).doubleValue();
        }

        public double valueOf(String s, int radix) {
          return Integer.valueOf(s, radix).doubleValue();
        }
      };
    }
}
