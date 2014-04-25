/*
 * Copyright 2008 Google Inc.
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
 * Wraps a primitive <code>float</code> as an object.
 */
public final class Float extends Number implements Comparable<Float> {
  public static final float MAX_VALUE = 3.4028235e+38f;
  public static final float MIN_VALUE = 1.4e-45f;
  public static final int MAX_EXPONENT = 127;
  public static final int MIN_EXPONENT = -126;
  public static final float MIN_NORMAL = 1.1754943508222875E-38f;
  public static final float NaN = 0f / 0f;
  public static final float NEGATIVE_INFINITY = -1f / 0f;
  public static final float POSITIVE_INFINITY = 1f / 0f;
  public static final int SIZE = 32;
  public static final Class<Float> TYPE = float.class;

  private static final long POWER_31_INT = 2147483648L;
  private static final long POWER_32_INT = 4294967296L;

  public static int compare(float x, float y) {
    return Double.compare(x, y);
  }

  public static int floatToIntBits(float value) {
    // Return a canonical NaN
    if (isNaN(value)) {
      return 0x7fc00000;
    }

    if (value == 0.0f) {
      if (1.0 / value == NEGATIVE_INFINITY) {
        return 0x80000000; // -0.0f
      } else {
        return 0x0;
      }
    }
    boolean negative = false;
    if (value < 0.0) {
      negative = true;
      value = -value;
    }
    if (isInfinite(value)) {
      if (negative) {
        return 0xff800000;
      } else {
        return 0x7f800000;
      }
    }

    // Obtain the 64-bit representation and extract its exponent and
    // mantissa.
    long l = Double.doubleToLongBits((double) value);
    int exp = (int) (((l >> 52) & 0x7ff) - 1023);
    int mantissa = (int) ((l & 0xfffffffffffffL) >> 29);

    // If the number will be a denorm in the float representation
    // (i.e., its exponent is -127 or smaller), add a leading 1 to the
    // mantissa and shift it right to maintain an exponent of -127.
    if (exp <= -127) {
      mantissa = (0x800000 | mantissa) >> (-127 - exp + 1);
      exp = -127;
    }

    // Construct the 32-bit representation
    long bits = negative ? POWER_31_INT : 0x0L;
    bits |= (exp + 127) << 23;
    bits |= mantissa;

    return (int) bits;
  }

  /**
   * @skip Here for shared implementation with Arrays.hashCode.
   * @param f 
   * @return hash value of float (currently just truncated to int)
   */
  public static int hashCode(float f) {
    return (int) f;
  }

  public static float intBitsToFloat(int bits) {
    boolean negative = (bits & 0x80000000) != 0;
    int exp = (bits >> 23) & 0xff;
    bits &= 0x7fffff;

    if (exp == 0x0) {
      // Handle +/- 0 here, denorms below
      if (bits == 0) {
        return negative ? -0.0f : 0.0f;
      }
    } else if (exp == 0xff) {
      // Inf & NaN
      if (bits == 0) {
        return negative ? NEGATIVE_INFINITY : POSITIVE_INFINITY;
      } else {
        return NaN;
      }
    }

    if (exp == 0) {
      // Input is denormalized, renormalize by shifting left until there is a
      // leading 1
      exp = 1;
      while ((bits & 0x800000) == 0) {
        bits <<= 1;
        exp--;
      }
      bits &= 0x7fffff;
    }

    // Build the bits of a 64-bit double from the incoming bits
    long bits64 = negative ? 0x8000000000000000L : 0x0L;
    bits64 |= ((long) (exp + 896)) << 52;
    bits64 |= ((long) bits) << 29;
    return (float) Double.longBitsToDouble(bits64);
  }

  public static native boolean isInfinite(float x) /*-{
    return !isFinite(x) && !isNaN(x);
  }-*/;

  public static native boolean isNaN(float x) /*-{
    return isNaN(x);
  }-*/;

  public static float parseFloat(String s) throws NumberFormatException {
    double doubleValue = __parseAndValidateDouble(s);
    if (doubleValue > Float.MAX_VALUE) {
      return Float.POSITIVE_INFINITY;
    } else if (doubleValue < -Float.MAX_VALUE) {
      return Float.NEGATIVE_INFINITY;
    }
    return (float) doubleValue;
  }

  public static String toString(float b) {
    return String.valueOf(b);
  }

  public static Float valueOf(float f) {
    return new Float(f);
  }

  public static Float valueOf(String s) throws NumberFormatException {
    return new Float(Float.parseFloat(s));
  }

  private final transient float value;

  public Float(double value) {
    this.value = (float) value;
  }

  public Float(float value) {
    this.value = value;
  }

  public Float(String s) {
    value = parseFloat(s);
  }

  @Override
  public byte byteValue() {
    return (byte) value;
  }

  public int compareTo(Float b) {
    return compare(value, b.value);
  }

  @Override
  public double doubleValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof Float) && (((Float) o).value == value);
  }

  @Override
  public float floatValue() {
    return value;
  }

  /**
   * Performance caution: using Float objects as map keys is not recommended.
   * Using floating point values as keys is generally a bad idea due to
   * difficulty determining exact equality. In addition, there is no efficient
   * JavaScript equivalent of <code>floatToIntBits</code>. As a result, this
   * method computes a hash code by truncating the whole number portion of the
   * float, which may lead to poor performance for certain value sets if Floats
   * are used as keys in a {@link java.util.HashMap}.
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
