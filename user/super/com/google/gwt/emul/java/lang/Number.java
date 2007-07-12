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

/**
 * Abstract base class for numberic wrapper classes.
 */
public abstract class Number {

  /**
   *  Stores a regular expression object to verify format of float values.
   */
  protected static JavaScriptObject floatRegex;

  // CHECKSTYLE_OFF: A special need to use unusual identifiers to avoid
  // introducing name collisions.

  /**
   * @skip
   */
  protected static String[] __hexDigits = new String[] {
      "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d",
      "e", "f"};

  static {
    initNative();
  }

  private static native void initNative() /*-{
    @java.lang.Number::floatRegex = /^[+-]?\d*\.?\d*(e[+-]?\d+)?$/i;
  }-*/;

  /**
   * @skip
   *
   * This function will determine the radix that the string is expressed in
   * based on the parsing rules defined in the Javadocs for Integer.decode() and
   * invoke __parseAndValidateLong.
   */
  protected static long __decodeAndValidateLong(String s, long lowerBound,
      long upperBound) throws NumberFormatException {
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
    
    return __parseAndValidateLong(s, radix, lowerBound, upperBound);
  }

  /**
   * @skip
   *
   * This function contains common logic for parsing a String in a given
   * radix and validating the result.
   */
  protected static long __parseAndValidateLong(String s, int radix,
      long lowerBound, long upperBound) throws NumberFormatException {
  
    if (s == null) {
      throw new NumberFormatException("Unable to parse null");
    }
    int length = s.length();
    int startIndex = (length > 0) && (s.charAt(0) == '-') ? 1 : 0;
  
    for (int i = startIndex; i < length; i++) {
      if (Character.digit(s.charAt(i), radix) == -1) {
        throw new NumberFormatException("Could not parse " + s +
            " in radix " + radix);
      }
    }

    long toReturn =  __parseInt(s, radix);
    if (__isLongNaN(toReturn)) {
      throw new NumberFormatException("Unable to parse " + s);
    } else if (toReturn < lowerBound || toReturn > upperBound) {
      throw new NumberFormatException(
        "The string " + s + " exceeds the range for the requested data type");
    }
    
    return toReturn;
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

    if (__isDoubleNaN(toReturn)) {
      throw new NumberFormatException("Unable to parse " + s);
    }
    
    return toReturn;
  }

  /**
   * @skip
   */
  private static native boolean __isDoubleNaN(double x) /*-{
    return isNaN(x);
  }-*/;

  /**
   * @skip
   */
  private static native boolean __isLongNaN(long x) /*-{
    return isNaN(x);
  }-*/;

  /**
   * @skip
   *
   * Invokes the global JS function <code>parseInt()</code>.
   */
  private static native long __parseInt(String s, int radix) /*-{
    return parseInt(s, radix);
  }-*/;
  
  /**
   * @skip
   *
   * @return The floating-point representation of <code>str</code> or
   *           <code>Number.NaN</code> if the string does not match
   *           {@link floatRegex}.
   */
  private static native double __parseDouble(String str) /*-{
    if (@java.lang.Number::floatRegex.test(str)) {
      return parseFloat(str);
    } else {
      return Number.NaN;
    }
  }-*/;

  // CHECKSTYLE_ON

  /**
   *  Used by JSNI methods to report badly formatted strings.
   *  @param s the unparseable string
   *  @throws NumberFormatException every time
   */
  private static void throwNumberFormatException(String s)
      throws NumberFormatException {
    throw new NumberFormatException("Could not parse " + s);
  }

  public abstract byte byteValue();

  public abstract double doubleValue();

  public abstract float floatValue();

  public abstract int intValue();

  public abstract long longValue();

  public abstract short shortValue();
}
