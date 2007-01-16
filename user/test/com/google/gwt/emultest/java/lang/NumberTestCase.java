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

import com.google.gwt.junit.client.GWTTestCase;

/**
 * The base class for numeric testing.
 */
public abstract class NumberTestCase extends GWTTestCase {

  /**
   * Factory class used to generate <code> Number </code> instances.
   */
  protected interface NumberFactory {
    public Number create(double x);

    public Number create(String x);

    public long decode(String x);

    public int maxDecimalPoints();

    public int maxDigits();

    public int maxExponents();

    public int maxMinusSigns();

    public double valueOf(String s);

    public double valueOf(String s, int radix);
  }

  protected static final double DELTA = 0.00001d;
  private static final String DIGIT = "9";

  private static final String DIGIT_WITH_DECIMAL = "9.9";

  private static final String DIGIT_WITH_EXPONENT = "1e1";
  private static final String DIGIT_WITH_MINUS = "-1";

  protected NumberFactory numberFactory;

  public NumberTestCase() {
    numberFactory = createNumberFactory();
  }

  public Number createNegative() {
    return numberFactory.create(negative());
  }

  public Number createNegativeFromString() {
    return numberFactory.create(negativeString());
  }

  public Number createOverflowByte() {
    return numberFactory.create(overflowByte());
  }

  public Number createOverflowFloat() {
    return numberFactory.create(overflowFloat());
  }

  public Number createOverflowInteger() {
    return numberFactory.create(overflowInteger());
  }

  public Number createOverflowShort() {
    return numberFactory.create(overflowShort());
  }

  public Number createPositive() {
    return numberFactory.create(positive());
  }

  public Number createPositiveFromString() {
    return numberFactory.create(positiveString());
  }

  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testConstructor() {
    assertEquals(positive(), createPositive().doubleValue(), DELTA);
    assertEquals(positive(), createPositiveFromString().doubleValue(), DELTA);
    assertEquals(negative(), createNegative().doubleValue(), DELTA);
    assertEquals(negative(), createNegativeFromString().doubleValue(), DELTA);
  }

  public void testDecode() {
    // Decode only exists for Number subclasses with no fractional component.
    if (numberFactory.maxDecimalPoints() == 0) {
      assertEquals((long) positive(), numberFactory.decode(positiveString()));
      assertEquals((long) positive(), numberFactory.decode(octalString()));
      assertEquals((long) positive(), numberFactory.decode(lowerHexString()));
      assertEquals((long) negative(),
          numberFactory.decode(upperNegativeHexString()));
      assertEquals((long) positive(), numberFactory.decode(sharpString()));
      try {
        numberFactory.decode(wrongOctalString());
        fail();
      } catch (NumberFormatException e) {
        // pass
      }
      try {
        numberFactory.decode(wrongUpperHexString());
        fail();
      } catch (NumberFormatException e) {
        // pass
      }
      try {
        numberFactory.decode(wrongLowerHexString());
        fail();
      } catch (NumberFormatException e) {
        // pass
      }
      try {
        numberFactory.decode(wrongSharpString());
        fail();
      } catch (NumberFormatException e) {
        // pass
      }
      try {
        numberFactory.decode(totallyWrongString());
        fail();
      } catch (NumberFormatException e) {
        // pass
      }
      try {
        numberFactory.decode(wrongString());
        fail();
      } catch (NumberFormatException e) {
        // pass
      }
    }
  }

  public void testDecodeRepeat() {
    assertEquals("digits", numberFactory.maxDigits(),
        determineLongestAcceptable(DIGIT));
    assertEquals("decimals", numberFactory.maxDecimalPoints(),
        determineLongestAcceptable(DIGIT_WITH_DECIMAL));
    assertEquals("minus signs", numberFactory.maxMinusSigns(),
        determineLongestAcceptable(DIGIT_WITH_MINUS));
    assertEquals("minus signs", numberFactory.maxExponents(),
        determineLongestAcceptable(DIGIT_WITH_EXPONENT));
  }

  public void testEquals() {
    assertFalse(createPositive().equals(createNegative()));
    assertEquals(createPositive(), createPositive());
    assertEquals(createNegative(), createNegative());
  }

  public void testToString() {
    assertEquals(createPositive().doubleValue(), numberFactory.create(
        createPositive().toString()).doubleValue(), DELTA);
    assertEquals(createNegative().doubleValue(), numberFactory.create(
        createNegative().toString()).doubleValue(), DELTA);
  }

  public void testValueOf() {
    assertEquals("positive", positive(),
        numberFactory.valueOf(positiveString()), DELTA);
    assertEquals("negative", negative(),
        numberFactory.valueOf(negativeString()), DELTA);
    if (numberFactory.maxDecimalPoints() == 0) {
      assertEquals("positive 10", positive(), numberFactory.valueOf(
          positiveString(), 10), DELTA);
      assertEquals("hex", positive(), numberFactory.valueOf(hexString(), 16),
          DELTA);
      assertEquals("octal", positive(),
          numberFactory.valueOf(octalString(), 8), DELTA);
      assertEquals("six", positive(), numberFactory.valueOf(sixString(), 6),
          DELTA);
      assertEquals("alpha", positive(),
          numberFactory.valueOf(alphaString(), 36), DELTA);
    }
  }

  public void testXValue() {
    // We assume the positive and negative fit within the boundaries of byte.
    // Overflow is checked in LongTest.
    assertEquals("short", (short) positive(), createPositive().shortValue());
    assertEquals("long", (long) positive(), createPositive().longValue());
    assertEquals("double", positive(), createPositive().doubleValue(), 0.001);
    assertEquals("float", (float) positive(), createPositive().floatValue(),
        0.01);
    assertEquals("byte", (byte) positive(), createPositive().byteValue());
    assertEquals("integer", (int) positive(), createPositive().intValue());
  }

  protected String alphaString() {
    // 123 base 36.
    return "3f";
  }

  protected abstract NumberFactory createNumberFactory();

  protected int determineLongestAcceptable(String toRepeat) {
    int i = 0;
    try {
      while (i < 20) {
        StringBuffer x = new StringBuffer();
        for (int j = i; j >= 0; j--) {
          x.append(toRepeat);
        }
        addCheckpoint(x.toString());
        numberFactory.create(x.toString());
        i++;
      }
    } catch (NumberFormatException e) {
    }
    return i;
  }

  protected String hexString() {
    return "7b";
  }

  protected String lowerHexString() {
    return "0x7b";
  }

  protected double negative() {
    return -123;
  }

  protected String negativeString() {
    return "-123";
  }

  protected String octalString() {
    return "0173";
  }

  protected short overflowByte() {
    return 12345;
  }

  protected double overflowFloat() {
    return 1.2345e200d;
  }

  protected long overflowInteger() {
    return 123456789012345L;
  }

  protected int overflowShort() {
    return 1234512345;
  }

  protected double positive() {
    return 123;
  }

  protected String positiveString() {
    return "123";
  }

  protected String sharpString() {
    return "#7b";
  }

  protected String sixString() {
    return "323";
  }

  protected String totallyWrongString() {
    return "^&%%^$%^(*";
  }

  protected String upperNegativeHexString() {
    return "-0X7b";
  }

  protected String wrongLowerHexString() {
    return "0xaG";
  }

  protected String wrongOctalString() {
    return "088";
  }

  protected String wrongSharpString() {
    return "#ag";
  }

  protected String wrongString() {
    return "-2x";
  }

  protected String wrongUpperHexString() {
    return "0XaG";
  }

}
