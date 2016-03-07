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
package java.lang;

import java.io.Serializable;

import javaemul.internal.JsUtils;
import javaemul.internal.NativeRegExp;
import jsinterop.annotations.JsMethod;

/**
 * Abstract base class for numeric wrapper classes.
 */
public abstract class Number implements Serializable {

  /**
   * Stores a regular expression object to verify the format of float values.
   */
  private static NativeRegExp floatRegex;

  // CHECKSTYLE_OFF: A special need to use unusual identifiers to avoid
  // introducing name collisions.

  static class __Decode {
    public final String payload;
    public final int radix;

    public __Decode(int radix, String payload) {
      this.radix = radix;
      this.payload = payload;
    }
  }

  /**
   * Use nested class to avoid clinit on outer.
   */
  static class __ParseLong {
    /**
     * The number of digits (excluding minus sign and leading zeros) to process
     * at a time.  The largest value expressible in maxDigits digits as well as
     * the factor radix^maxDigits must be strictly less than 2^31.
     */
    private static final int[] maxDigitsForRadix = {-1, -1, // unused
      30, // base 2
      19, // base 3
      15, // base 4
      13, // base 5
      11, 11, // base 6-7
      10, // base 8
      9, 9, // base 9-10
      8, 8, 8, 8, // base 11-14
      7, 7, 7, 7, 7, 7, 7, // base 15-21
      6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, // base 22-35
      5 // base 36
    };

    /**
     * A table of values radix*maxDigitsForRadix[radix].
     */
    private static final int[] maxDigitsRadixPower = new int[37];

    /**
     * The largest number of digits (excluding minus sign and leading zeros) that
     * can fit into a long for a given radix between 2 and 36, inclusive.
     */
    private static final int[] maxLengthForRadix = {-1, -1, // unused
      63, // base 2
      40, // base 3
      32, // base 4
      28, // base 5
      25, // base 6
      23, // base 7
      21, // base 8
      20, // base 9
      19, // base 10
      19, // base 11
      18, // base 12
      18, // base 13
      17, // base 14
      17, // base 15
      16, // base 16
      16, // base 17
      16, // base 18
      15, // base 19
      15, // base 20
      15, // base 21
      15, // base 22
      14, // base 23
      14, // base 24
      14, // base 25
      14, // base 26
      14, // base 27
      14, // base 28
      13, // base 29
      13, // base 30
      13, // base 31
      13, // base 32
      13, // base 33
      13, // base 34
      13, // base 35
      13  // base 36
    };

    /**
     * A table of floor(MAX_VALUE / maxDigitsRadixPower).
     */
    private static final long[] maxValueForRadix = new long[37];

    static {
      for (int i = 2; i <= 36; i++) {
        maxDigitsRadixPower[i] = (int) Math.pow(i, maxDigitsForRadix[i]);
        maxValueForRadix[i] = Long.MAX_VALUE / maxDigitsRadixPower[i];
      }
    }
  }

  @JsMethod
  private static boolean $isInstance(Object instance) {
    return "number".equals(JsUtils.typeOf(instance)) || instanceOfJavaLangNumber(instance);
  }

  private static native boolean instanceOfJavaLangNumber(Object instance) /*-{
    // Note: The instanceof Number here refers to java.lang.Number in j2cl.
    return instance instanceof Number;
  }-*/;

  /**
   * @skip
   *
   * This function will determine the radix that the string is expressed in
   * based on the parsing rules defined in the Javadocs for Integer.decode() and
   * invoke __parseAndValidateInt.
   */
  protected static int __decodeAndValidateInt(String s, int lowerBound,
      int upperBound) throws NumberFormatException {
    __Decode decode = __decodeNumberString(s);
    return __parseAndValidateInt(decode.payload, decode.radix, lowerBound,
        upperBound);
  }

  protected static __Decode __decodeNumberString(String s) {
    final boolean negative;
    if (s.startsWith("-")) {
      negative = true;
      s = s.substring(1);
    } else {
      negative = false;
      if (s.startsWith("+")) {
        s = s.substring(1);
      }
    }

    final int radix;
    if (s.startsWith("0x") || s.startsWith("0X")) {
      s = s.substring(2);
      radix = 16;
    } else if (s.startsWith("#")) {
      s = s.substring(1);
      radix = 16;
    } else if (s.startsWith("0")) {
      radix = 8;
    } else {
      radix = 10;
    }

    if (negative) {
      s = "-" + s;
    }
    return new __Decode(radix, s);
  }

  /**
   * @skip
   *
   * This function contains common logic for parsing a String as a floating-
   * point number and validating the range.
   */
  protected static double __parseAndValidateDouble(String s) throws NumberFormatException {
    if (!__isValidDouble(s)) {
      throw NumberFormatException.forInputString(s);
    }
    return parseFloat(s);
  }

  private static native double parseFloat(String str) /*-{
    return parseFloat(str);
  }-*/;

  /**
   * @skip
   *
   * This function contains common logic for parsing a String in a given radix
   * and validating the result.
   */
  protected static int __parseAndValidateInt(String s, int radix, int lowerBound, int upperBound)
      throws NumberFormatException {
    if (s == null) {
      throw NumberFormatException.forNullInputString();
    }
    if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
      throw NumberFormatException.forRadix(radix);
    }

    int length = s.length();
    int startIndex = (length > 0) && (s.charAt(0) == '-' || s.charAt(0) == '+') ? 1 : 0;

    for (int i = startIndex; i < length; i++) {
      if (Character.digit(s.charAt(i), radix) == -1) {
        throw NumberFormatException.forInputString(s);
      }
    }

    int toReturn = JsUtils.parseInt(s, radix);
    // isTooLow is separated into its own variable to avoid a bug in BlackBerry OS 7. See
    // https://code.google.com/p/google-web-toolkit/issues/detail?id=7291.
    boolean isTooLow = toReturn < lowerBound;
    if (Double.isNaN(toReturn)) {
      throw NumberFormatException.forInputString(s);
    } else if (isTooLow || toReturn > upperBound) {
      throw NumberFormatException.forInputString(s);
    }

    return toReturn;
  }

  /**
   * @skip
   *
   * This function contains common logic for parsing a String in a given radix
   * and validating the result.
   */
  protected static long __parseAndValidateLong(String s, int radix) throws NumberFormatException {
    if (s == null) {
      throw NumberFormatException.forNullInputString();
    }
    if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
      throw NumberFormatException.forRadix(radix);
    }

    final String orig = s;

    int length = s.length();
    boolean negative = false;
    if (length > 0) {
      char c = s.charAt(0);
      if (c == '-' || c == '+') {
        s = s.substring(1);
        length--;
        negative = (c == '-');
      }
    }
    if (length == 0) {
      throw NumberFormatException.forInputString(orig);
    }

    // Strip leading zeros
    while (s.length() > 0 && s.charAt(0) == '0') {
      s = s.substring(1);
      length--;
    }

    // Immediately eject numbers that are too long -- this avoids more complex
    // overflow handling below
    if (length > __ParseLong.maxLengthForRadix[radix]) {
      throw NumberFormatException.forInputString(orig);
    }

    // Validate the digits
    for (int i = 0; i < length; i++) {
      if (Character.digit(s.charAt(i), radix) == -1) {
        throw NumberFormatException.forInputString(orig);
      }
    }

    long toReturn = 0;
    int maxDigits = __ParseLong.maxDigitsForRadix[radix];
    long radixPower = __ParseLong.maxDigitsRadixPower[radix];
    long minValue = -__ParseLong.maxValueForRadix[radix];

    boolean firstTime = true;
    int head = length % maxDigits;
    if (head > 0) {
      // accumulate negative numbers, as -Long.MAX_VALUE == Long.MIN_VALUE + 1
      // (in other words, -Long.MIN_VALUE overflows, see issue 7308)
      toReturn = - JsUtils.parseInt(s.substring(0, head), radix);
      s = s.substring(head);
      length -= head;
      firstTime = false;
    }

    while (length >= maxDigits) {
      head = JsUtils.parseInt(s.substring(0, maxDigits), radix);
      s = s.substring(maxDigits);
      length -= maxDigits;
      if (!firstTime) {
        // Check whether multiplying by radixPower will overflow
        if (toReturn < minValue) {
          throw NumberFormatException.forInputString(orig);
        }
        toReturn *= radixPower;
      } else {
        firstTime = false;
      }
      toReturn -= head;
    }

    // A positive value means we overflowed Long.MIN_VALUE
    if (toReturn > 0) {
      throw NumberFormatException.forInputString(orig);
    }

    if (!negative) {
      toReturn = -toReturn;
      // A negative value means we overflowed Long.MAX_VALUE
      if (toReturn < 0) {
        throw NumberFormatException.forInputString(orig);
      }
    }
    return toReturn;
  }

  /**
   * @skip
   *
   * @param str
   * @return {@code true} if the string matches the float format, {@code false} otherwise
   */
  private static boolean __isValidDouble(String str) {
    if (floatRegex == null) {
      floatRegex = createFloatRegex();
    }
    return floatRegex.test(str);
  }

  private static native NativeRegExp createFloatRegex() /*-{
    return /^\s*[+-]?(NaN|Infinity|((\d+\.?\d*)|(\.\d+))([eE][+-]?\d+)?[dDfF]?)\s*$/;
  }-*/;

  // CHECKSTYLE_ON

  public byte byteValue() {
    return (byte) intValue();
  }

  public abstract double doubleValue();

  public abstract float floatValue();

  public abstract int intValue();

  public abstract long longValue();

  public short shortValue() {
    return (short) intValue();
  }
}
