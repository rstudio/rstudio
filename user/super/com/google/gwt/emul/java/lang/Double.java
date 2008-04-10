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

/**
 * Wraps a primitive <code>double</code> as an object.
 */
public final class Double extends Number implements Comparable<Double> {
  public static final int MAX_EXPONENT = 1023;  // a JDK 1.6 constant
                             // ==Math.getExponent(Double.MAX_VALUE);
  public static final double MAX_VALUE = 1.7976931348623157e+308;
  public static final int MIN_EXPONENT = -1022; // a JDK 1.6 constant
                             // ==Math.getExponent(Double.MIN_NORMAL);;
  public static final double MIN_NORMAL = 2.2250738585072014e-308;
                             // a JDK 1.6 constant
  public static final double MIN_VALUE = 4.9e-324;

  public static final double NaN = 0d / 0d;
  public static final double NEGATIVE_INFINITY = -1d / 0d;
  public static final double POSITIVE_INFINITY = 1d / 0d;
  public static final int SIZE = 64;
  static final int EXPONENT_BITSIZE = 11;
  // the extra -1 is for the sign bit
  static final int MANTISSA_BITSIZE = SIZE - EXPONENT_BITSIZE - 1;
  // the exponent is biased by one less than its midpoint, e.g. 2^11 / 2 - 1;
  static final int EXPONENT_BIAS = 1 << (EXPONENT_BITSIZE - 1) - 1;
  // the mask is all 1 bits in the exponent, e.g. 0x7ff shifted over by 52
  static final long EXPONENT_MASK = ((1L
      << EXPONENT_BITSIZE) - 1) << MANTISSA_BITSIZE;
  // place 1-bit in top position
  static final long NAN_MANTISSA = 1L << (MANTISSA_BITSIZE - 1);
  // sign bit is the MSB bit
  static final long SIGN_BIT = 0x1L << (SIZE - 1);
  // Zero represented in biased form
  static final int BIASED_ZERO_EXPONENT = EXPONENT_BIAS;
  // The maximum mantissa value, represented as a double
  static final double MAX_MANTISSA_VALUE = Math.pow(2, MANTISSA_BITSIZE);
  // The mantissa of size MANTISSA_BITSIZE with all bits set to 1_
  static final long MANTISSA_MASK = (1L << MANTISSA_BITSIZE) - 1;

  public static int compare(double x, double y) {
    if (x < y) {
      return -1;
    } else if (x > y) {
      return 1;
    } else {
      return 0;
    }
  }

  // Theory of operation: Let a double number d be represented as
  // 1.M * 2^E, where the leading bit is assumed to be 1,
  // the fractional mantissa M is multiplied 2 to the power of E.
  // We want to reliably recover M and E, and then encode them according
  // to IEEE754 (see http://en.wikipedia.org/wiki/IEEE754)
  public static long doubleToLongBits(final double d) {

    long sign = (d < 0 ? SIGN_BIT : 0);
    long exponent = 0;
    double absV = Math.abs(d);

    if (Double.isNaN(d)) {
      // IEEE754, NaN exponent bits all 1s, and mantissa is non-zero
      return EXPONENT_MASK | NAN_MANTISSA;
    }
    if (Double.isInfinite(d)) {
      // an infinite number is a number with a zero mantissa and all
      // exponent bits set to 1
      exponent = EXPONENT_MASK;
      absV = 0.0;
    } else {
      if (absV == 0.0) {
        // IEEE754, exponent is 0, mantissa is zero
        // we don't handle negative zero at the moment, it is treated as
        // positive zero
        exponent = 0L;
      } else {
        // get an approximation to the exponent
        // if d = 1.M * 2^E then
        //   log2(d) = log(1.M) + log2(2^E) = log(1.M) + E
        //   floor(log(1.M) + E) = E because log(1.M) always < 1
        // it may turn out log2(x) = log(x)/log(2) always returns the
        // the correct exponent, but this method is more defensive
        // with respect to precision to avoid off by 1 problems
        int guess = (int) Math.floor(Math.log(absV) / Math.log(2));
        // force it to MAX_EXPONENT, MAX_EXPONENT interval
        // (<= -MAX_EXPONENT = denorm/zero)
        guess = Math.max(-MAX_EXPONENT, Math.min(guess, MAX_EXPONENT));

        // Recall that d = 1.M * 2^E, so dividing by 2^E should leave
        // us with 1.M
        double exp = Math.pow(2, guess);
        absV = absV / exp;

        // while the number is still bigger than a normalized number
        // increment exponent guess
        // This might occur if there is some precision loss in determining
        // the exponent
        while (absV > 2.0) {
          guess++;
          absV /= 2.0;
        }
        // if the number is smaller than a normalized number
        // decrement exponent. If the exponent becomes zero, and we
        // fail to achieve a normalized mantissa, then this number
        // must be a denormalized value
        while (absV < 1 && guess > 0) {
          guess--;
          absV *= 2;
        }
        exponent = (guess + EXPONENT_BIAS) << MANTISSA_BITSIZE;
      }
    }
    // if denormalized
    if (exponent <= BIASED_ZERO_EXPONENT) {
      // denormalized numbers have an exponent of zero, but pretend
      // they have an exponent of 1, so since there is an implicit
      // * 2^1 for denorms, we correct by dividing by 2
      absV /= 2;
    }
    // the input value has now been stripped of its exponent
    // and is in the range [1,2), we strip off the leading decimal to normalize
    // and use the remainer as a percentage of the significand value (2^52)
    long mantissa = (long) ((absV % 1) * MAX_MANTISSA_VALUE);
    return sign | exponent | (mantissa & MANTISSA_MASK);
  }

  /**
   * @skip Here for shared implementation with Arrays.hashCode
   */
  public static int hashCode(double d) {
    return (int) d;
  }

  public static native boolean isInfinite(double x) /*-{
    return !isFinite(x);
  }-*/;

  public static native boolean isNaN(double x) /*-{
    return isNaN(x);
  }-*/;

  public static double longBitsToDouble(long value) {
    // exponent in MSB bits 1-11
    int exp = (int) ((value & EXPONENT_MASK) >> MANTISSA_BITSIZE);
    // unbias exponent handle denorm case
    int denorm = (exp == 0 ? 1 : 0);
    // denorm exponent becomes -1022
    exp = exp - EXPONENT_BIAS + denorm;
    // mantissa in LSB 52 bits
    long mantissa = (value & MANTISSA_MASK);
    // sign in MSB bit 0
    int sign = (value & SIGN_BIT) != 0 ? -1 : 1;
    // unbiased exponent value of EXPONENT_BIAS + 1 (e.g. 1024)
    // is equivalent to all 1 bits in biased exp (e.g. 2047)
    if (exp == EXPONENT_BIAS + 1) {
      if (mantissa != 0) {
        return Double.NaN;
      } else {
        return sign < 0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
      }
    }
    // non-denormized numbers get 1.0 added back, since our first digit is 
    // always a 1
    // mantissa is divided by 2^52, and multiplied by 2^exponent
    return sign * ((mantissa / MAX_MANTISSA_VALUE + (1 - denorm)) * Math.pow(2, exp));
  }

  public static double parseDouble(String s) throws NumberFormatException {
    return __parseAndValidateDouble(s);
  }

  public static String toString(double b) {
    return String.valueOf(b);
  }

  public static Double valueOf(double d) {
    return new Double(d);
  }

  public static Double valueOf(String s) throws NumberFormatException {
    return new Double(Double.parseDouble(s));
  }

  private final transient double value;

  public Double(double value) {
    this.value = value;
  }

  public Double(String s) {
    value = parseDouble(s);
  }

  @Override
  public byte byteValue() {
    return (byte) value;
  }

  public int compareTo(Double b) {
    if (value < b.value) {
      return -1;
    } else if (value > b.value) {
      return 1;
    } else {
      return 0;
    }
  }

  @Override
  public double doubleValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof Double) && (((Double) o).value == value);
  }

  @Override
  public float floatValue() {
    return (float) value;
  }

  /**
   * Performance caution: using Double objects as map keys is not recommended.
   * Using double values as keys is generally a bad idea due to difficulty
   * determining exact equality. In addition, there is no efficient JavaScript
   * equivalent of <code>doubleToIntBits</code>. As a result, this method
   * computes a hash code by truncating the whole number portion of the double,
   * which may lead to poor performance for certain value sets if Doubles are
   * used as keys in a {@link java.util.HashMap}.
   */
  @Override
  public int hashCode() {
    return hashCode(value);
  }

  @Override
  public int intValue() {
    return (int) value;
  }

  public boolean isInfinite() {
    return isInfinite(value);
  }

  public boolean isNaN() {
    return isNaN(value);
  }

  @Override
  public long longValue() {
    return (long) value;
  }

  @Override
  public short shortValue() {
    return (short) value;
  }

  @Override
  public String toString() {
    return toString(value);
  }

}
