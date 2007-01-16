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
package java.lang;

/**
 * Abstract base class for numberic wrapper classes.
 */
public abstract class Number {

  // CHECKSTYLE_OFF: A special need to use unusual identifiers to avoid
  // introducing name collisions.

  /**
   * @skip
   */
  protected static String[] __hexDigits = new String[] {
      "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d",
      "e", "f"};

  /**
   * @skip
   */
  protected static native long __parseLongRadix(String s, int radix, 
      long min, long max) /*-{
    var digits = "0123456789abcdefghijklmnopqrstuvwxyz".substring(0,radix);
    var matchString = "-?[" + digits +"]+";
    var out = parseInt(s, radix);
    var lowerS = s.toLowerCase();
    if (!(lowerS.@java.lang.String::matches(Ljava/lang/String;)(matchString))) {
      @java.lang.Number::throwNumberFormatException(Ljava/lang/String;)(s);
    }
    if (out > max || out < min) {
      @java.lang.Number::throwNumberFormatException(Ljava/lang/String;)(s);
    }
    if (out == NaN) {
      @java.lang.Number::throwNumberFormatException(Ljava/lang/String;)(s);
    }
    return out;
    
  }-*/;

  /**
   * @skip
   */
  protected static native long __parseLongInfer(String s, long min, long max) /*-{
    // Strip the negative off to put it back after munging.
    if (s.charAt(0) == "-") {
      negative = "-";
      spos = s.substring(1);
    } else {
      negative = "";
      spos = s;
    }
    
    // Munge the result to handle specifiers.
    if (spos.substring(0,2) == "0x" || spos.substring(0,2) == "0X") {
      newString = negative + spos.substring(2);
      radix=16;
    } else if (spos.charAt(0) == "0") {
      newString = negative + spos.substring(1);
      radix=8; 
    } else if (spos.charAt(0) == "#") {
      newString = negative + spos.substring(1);
      radix=16;
    } else {
      // No munging needed.
      newString = s;
      radix = 10;
    }
    return @java.lang.Number::__parseLongRadix(Ljava/lang/String;IJJ)(newString, 
      radix, min, max);
  }-*/;

  /**
   * @skip
   */
  protected static native double __parseDouble(String s, 
      double neginf, double maxneg, double minpos, double inf) /*-{
    if (s == "NaN") {
      return NaN;
    }
    if (s == "Infinity") {
      return Infinity;
    }
    if (s == "-Infinity") {
      return -Infinity;
    }
    var out = parseFloat(s);
    if (!(s.@java.lang.String::matches(Ljava/lang/String;)("-?[0-9]+[.]?[0-9]*([eE]-?[0-9]+)?"))) {
      @java.lang.Number::throwNumberFormatException(Ljava/lang/String;)(s);
    } 
    if (out < neginf) {
      out = -Infinity;
    } else if (out > maxneg && out < 0) {
      out = -0.0;
    } else if (out > 0 && out < minpos) {
      out = 0.0;
    } else if (out > inf) {
      out = Infinity;
    } 
    return out;
  }-*/;

  // CHECKSTYLE_ON
  static void throwNumberFormatException(String message) {
    throw new NumberFormatException(message);
  }
  
  public abstract byte byteValue();

  public abstract double doubleValue();

  public abstract float floatValue();

  public abstract int intValue();

  public abstract long longValue();

  public abstract short shortValue();  
}
