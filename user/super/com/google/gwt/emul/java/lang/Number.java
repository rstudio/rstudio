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

import com.google.gwt.core.client.JavaScriptObject;

import java.io.Serializable;

/**
 * Abstract base class for numeric wrapper classes.
 */
public abstract class Number implements Serializable {

  /**
   * Stores a regular expression object to verify format of float values.
   */
  protected static JavaScriptObject floatRegex;

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
  static class __Digits {
    final static char[] digits = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
        'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r',
        's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
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

  /**
   * @skip
   * 
   * This function will determine the radix that the string is expressed in
   * based on the parsing rules defined in the Javadocs for Integer.decode() and
   * invoke __parseAndValidateInt.
   */
  protected static long __decodeAndValidateInt(String s, int lowerBound,
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
  protected static double __parseAndValidateDouble(String s)
      throws NumberFormatException {

    double toReturn = __parseDouble(s);

    if (__isNaN(toReturn)) {
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
  protected static int __parseAndValidateInt(String s, int radix,
      int lowerBound, int upperBound) throws NumberFormatException {

    if (s == null) {
      throw new NumberFormatException("null");
    }
    if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
      throw new NumberFormatException("radix " + radix + " out of range");
    }

    int length = s.length();
    int startIndex = (length > 0) && (s.charAt(0) == '-') ? 1 : 0;

    for (int i = startIndex; i < length; i++) {
      if (Character.digit(s.charAt(i), radix) == -1) {
        throw NumberFormatException.forInputString(s);
      }
    }

    int toReturn = __parseInt(s, radix);
    if (__isNaN(toReturn)) {
      throw NumberFormatException.forInputString(s);
    } else if (toReturn < lowerBound || toReturn > upperBound) {
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
  protected static long __parseAndValidateLong(String s, int radix)
      throws NumberFormatException {
    
    if (s == null) {
      throw new NumberFormatException("null");
    }
    if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
      throw new NumberFormatException("radix " + radix + " out of range");
    }

    int length = s.length();
    boolean negative = (length > 0) && (s.charAt(0) == '-');
    if (negative) {
      s = s.substring(1);
      length--;
    }
    if (length == 0) {
      throw NumberFormatException.forInputString(s);
    }

    // Strip leading zeros
    while (s.length() > 0 && s.charAt(0) == '0') {
      s = s.substring(1);
      length--;
    }
    
    // Immediately eject numbers that are too long -- this avoids more complex
    // overflow handling below
    if (length > __ParseLong.maxLengthForRadix[radix]) {
      throw NumberFormatException.forInputString(s);
    }
    
    // Validate the digits
    int maxNumericDigit = '0' + Math.min(radix, 10);
    int maxLowerCaseDigit = radix + 'a' - 10;
    int maxUpperCaseDigit = radix + 'A' - 10;
    for (int i = 0; i < length; i++) {
      char c = s.charAt(i);
      if (c >= '0' && c < maxNumericDigit) {
        continue;
      }
      if (c >= 'a' && c < maxLowerCaseDigit) {
        continue;
      }
      if (c >= 'A' && c < maxUpperCaseDigit) {
        continue;
      }
      throw NumberFormatException.forInputString(s);
    }

    long toReturn = 0;
    int maxDigits = __ParseLong.maxDigitsForRadix[radix];
    long radixPower = __ParseLong.maxDigitsRadixPower[radix];
    long maxValue = __ParseLong.maxValueForRadix[radix];
    
    boolean firstTime = true;
    int head = length % maxDigits;
    if (head > 0) {
      toReturn = __parseInt(s.substring(0, head), radix);
      s = s.substring(head);
      length -= head;
      firstTime = false;
    }
    
    while (length >= maxDigits) {
      head = __parseInt(s.substring(0, maxDigits), radix);
      s = s.substring(maxDigits);
      length -= maxDigits;
      if (!firstTime) {
        // Check whether multiplying by radixPower will overflow
        if (toReturn > maxValue) {
          throw new NumberFormatException(s);
        }
        toReturn *= radixPower;      
      } else {
        firstTime = false;
      }
      toReturn += head;
    }
    
    // A negative value means we overflowed Long.MAX_VALUE
    if (toReturn < 0) {
      throw NumberFormatException.forInputString(s);
    }
    
    if (negative) {
      toReturn = -toReturn;
    }
    return toReturn;
  }
  
  /**
   * @skip
   */
  private static native boolean __isNaN(double x) /*-{
    return isNaN(x);
  }-*/;

  /**
   * @skip
   * 
   * @return The floating-point representation of <code>str</code> or
   *         <code>Number.NaN</code> if the string does not match
   *         {@link #floatRegex}.
   */
  private static native double __parseDouble(String str) /*-{
    var floatRegex = @java.lang.Number::floatRegex;
    if (!floatRegex) {
      // Disallow '.' with no digits on either side
      floatRegex = @java.lang.Number::floatRegex = /^\s*[+-]?((\d+\.?\d*)|(\.\d+))([eE][+-]?\d+)?[dDfF]?\s*$/i;
    }
    if (floatRegex.test(str)) {
      return parseFloat(str);
    } else {
      return Number.NaN;
    }
  }-*/;

  /**
   * @skip
   * 
   * Invokes the global JS function <code>parseInt()</code>.
   */
  private static native int __parseInt(String s, int radix) /*-{
    return parseInt(s, radix);
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
