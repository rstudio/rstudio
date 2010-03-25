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
      floatRegex = @java.lang.Number::floatRegex = /^\s*[+-]?((\d+\.?\d*)|(\.\d+))([eE][+-]?\d+)?\s*$/i;
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
