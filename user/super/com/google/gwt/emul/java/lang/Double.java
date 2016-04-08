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

import static javaemul.internal.InternalPreconditions.checkNotNull;

import javaemul.internal.JsUtils;

import jsinterop.annotations.JsMethod;

/**
 * Wraps a primitive <code>double</code> as an object.
 */
public final class Double extends Number implements Comparable<Double> {
  public static final double MAX_VALUE = 1.7976931348623157e+308;
  public static final double MIN_VALUE = 4.9e-324;
  public static final double MIN_NORMAL = 2.2250738585072014e-308;
  public static final int MAX_EXPONENT = 1023;
                             // ==Math.getExponent(Double.MAX_VALUE);
  public static final int MIN_EXPONENT = -1022;
                             // ==Math.getExponent(Double.MIN_NORMAL);

  public static final double NaN = 0d / 0d;
  public static final double NEGATIVE_INFINITY = -1d / 0d;
  public static final double POSITIVE_INFINITY = 1d / 0d;
  public static final int SIZE = 64;
  public static final int BYTES = SIZE / Byte.SIZE;
  public static final Class<Double> TYPE = double.class;

  // 2^512, 2^-512
  private static final double POWER_512 = 1.3407807929942597E154;
  private static final double POWER_MINUS_512 = 7.458340731200207E-155;
  // 2^256, 2^-256
  private static final double POWER_256 = 1.157920892373162E77;
  private static final double POWER_MINUS_256 = 8.636168555094445E-78;
  // 2^128, 2^-128
  private static final double POWER_128 = 3.4028236692093846E38;
  private static final double POWER_MINUS_128 = 2.9387358770557188E-39;
  // 2^64, 2^-64
  private static final double POWER_64 = 18446744073709551616.0;
  private static final double POWER_MINUS_64 = 5.421010862427522E-20;
  // 2^52, 2^-52
  private static final double POWER_52 = 4503599627370496.0;
  private static final double POWER_MINUS_52 = 2.220446049250313E-16;
  // 2^32, 2^-32
  private static final double POWER_32 = 4294967296.0;
  private static final double POWER_MINUS_32 = 2.3283064365386963E-10;
  // 2^31
  private static final double POWER_31 = 2147483648.0;
  // 2^20, 2^-20
  private static final double POWER_20 = 1048576.0;
  private static final double POWER_MINUS_20 = 9.5367431640625E-7;
  // 2^16, 2^-16
  private static final double POWER_16 = 65536.0;
  private static final double POWER_MINUS_16 = 0.0000152587890625;
  // 2^8, 2^-8
  private static final double POWER_8 = 256.0;
  private static final double POWER_MINUS_8 = 0.00390625;
  // 2^4, 2^-4
  private static final double POWER_4 = 16.0;
  private static final double POWER_MINUS_4 = 0.0625;
  // 2^2, 2^-2
  private static final double POWER_2 = 4.0;
  private static final double POWER_MINUS_2 = 0.25;
  // 2^1, 2^-1
  private static final double POWER_1 = 2.0;
  private static final double POWER_MINUS_1 = 0.5;
  // 2^-1022 (smallest double non-denorm)
  private static final double POWER_MINUS_1022 = 2.2250738585072014E-308;



  private static class PowersTable {
    private static final double[] powers = {
        POWER_512, POWER_256, POWER_128, POWER_64, POWER_32, POWER_16, POWER_8,
        POWER_4, POWER_2, POWER_1
    };

    private static final double[] invPowers = {
        POWER_MINUS_512, POWER_MINUS_256, POWER_MINUS_128, POWER_MINUS_64,
        POWER_MINUS_32, POWER_MINUS_16, POWER_MINUS_8, POWER_MINUS_4, POWER_MINUS_2,
        POWER_MINUS_1
    };
  }

  public static int compare(double x, double y) {
    if (x < y) {
      return -1;
    }
    if (x > y) {
      return 1;
    }
    if (x == y) {
      return 0;
    }

    if (isNaN(x)) {
      if (isNaN(y)) {
        return 0;
      } else {
        return 1;
      }
    } else {
      return -1;
    }
  }

  public static long doubleToLongBits(double value) {
    if (isNaN(value)) {
      return 0x7ff8000000000000L;
    }

    boolean negative = false;
    if (value == 0.0) {
      if (1.0 / value == NEGATIVE_INFINITY) {
        return 0x8000000000000000L; // -0.0
      } else {
        return 0x0L;
      }
    }
    if (value < 0.0) {
      negative = true;
      value = -value;
    }
    if (isInfinite(value)) {
      if (negative) {
        return 0xfff0000000000000L;
      } else {
        return 0x7ff0000000000000L;
      }
    }

    int exp = 0;

    // Scale d by powers of 2 into the range [1.0, 2.0)
    // If the exponent would go below -1023, scale into (0.0, 1.0) instead
    if (value < 1.0) {
      int bit = 512;
      for (int i = 0; i < 10; i++, bit >>= 1) {
        if (value < PowersTable.invPowers[i] && exp - bit >= -1023) {
          value *= PowersTable.powers[i];
          exp -= bit;
        }
      }
      // Force into [1.0, 2.0) range
      if (value < 1.0 && exp - 1 >= -1023) {
        value *= 2.0;
        exp--;
      }
    } else if (value >= 2.0) {
      int bit = 512;
      for (int i = 0; i < 10; i++, bit >>= 1) {
        if (value >= PowersTable.powers[i]) {
          value *= PowersTable.invPowers[i];
          exp += bit;
        }
      }
    }

    if (exp > -1023) {
      // Remove significand of non-denormalized mantissa
      value -= 1.0;
    } else {
      // Insert 0 bit as significand of denormalized mantissa
      value *= 0.5;
    }

    // Extract high 20 bits of mantissa
    long ihi = (long) (value * POWER_20);

    // Extract low 32 bits of mantissa
    value -= ihi * POWER_MINUS_20;

    long ilo = (long) (value * POWER_52);

    // Exponent bits
    ihi |= (exp + 1023) << 20;

    // Sign bit
    if (negative) {
      ihi |= 0x80000000L;
    }

    return (ihi << 32) | ilo;
  }

  public static int hashCode(double d) {
    return (int) d;
  }

  public static boolean isFinite(double x) {
    return JsUtils.isFinite(x);
  }

  public static boolean isInfinite(double x) {
    return !isNaN(x) && !isFinite(x);
  }

  public static boolean isNaN(double x) {
    return JsUtils.isNaN(x);
  }

  public static double longBitsToDouble(long bits) {
    long ihi = (long) (bits >> 32);
    long ilo = (long) (bits & 0xffffffffL);
    if (ihi < 0) {
      ihi += 0x100000000L;
    }
    if (ilo < 0) {
      ilo += 0x100000000L;
    }

    boolean negative = (ihi & 0x80000000) != 0;
    int exp = (int) ((ihi >> 20) & 0x7ff);
    ihi &= 0xfffff; // remove sign bit and exponent

    if (exp == 0x0) {
      double d = (ihi * POWER_MINUS_20) + (ilo * POWER_MINUS_52);
      d *= POWER_MINUS_1022;
      return negative ? (d == 0.0 ? -0.0 : -d) : d;
    } else if (exp == 0x7ff) {
      if (ihi == 0 && ilo == 0) {
        return negative ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
      } else {
        return Double.NaN;
      }
    }

    // Normalize exponent
    exp -= 1023;

    double d = 1.0 + (ihi * POWER_MINUS_20) + (ilo * POWER_MINUS_52);
    if (exp > 0) {
      int bit = 512;
      for (int i = 0; i < 10; i++, bit >>= 1) {
        if (exp >= bit) {
          d *= PowersTable.powers[i];
          exp -= bit;
        }
      }
    } else if (exp < 0) {
      while (exp < 0) {
        int bit = 512;
        for (int i = 0; i < 10; i++, bit >>= 1) {
          if (exp <= -bit) {
            d *= PowersTable.invPowers[i];
            exp += bit;
          }
        }
      }
    }
    return negative ? -d : d;
  }

  public static double max(double a, double b) {
    return Math.max(a, b);
  }

  public static double min(double a, double b) {
    return Math.min(a, b);
  }

  public static double parseDouble(String s) throws NumberFormatException {
    return __parseAndValidateDouble(s);
  }

  public static double sum(double a, double b) {
    return a + b;
  }

  public static String toString(double b) {
    return String.valueOf(b);
  }

  public static Double valueOf(double d) {
    return new Double(d);
  }

  public static Double valueOf(String s) throws NumberFormatException {
    return new Double(s);
  }

  public Double(double value) {
    /*
     * Call to $createDouble(value) must be here so that the method is referenced and not
     * pruned before new Double(value) is replaced by $createDouble(value) by
     * RewriteConstructorCallsForUnboxedTypes.
     */
    $createDouble(value);
  }

  public Double(String s) {
    /*
     * Call to $createDouble(value) must be here so that the method is referenced and not
     * pruned before new Double(value) is replaced by $createDouble(value) by
     * RewriteConstructorCallsForUnboxedTypes.
     */
    $createDouble(s);
  }

  @Override
  public byte byteValue() {
    return (byte) doubleValue();
  }

  @Override
  public int compareTo(Double b) {
    return compare(doubleValue(), b.doubleValue());
  }

  @Override
  public double doubleValue() {
    return unsafeCast(checkNotNull(this));
  }

  private static native double unsafeCast(Object instance) /*-{
    return instance;
  }-*/;

  @Override
  public boolean equals(Object o) {
    return checkNotNull(this) == o;
  }

  @Override
  public float floatValue() {
    return (float) doubleValue();
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
    return hashCode(doubleValue());
  }

  @Override
  public int intValue() {
    return (int) doubleValue();
  }

  public boolean isInfinite() {
    return isInfinite(doubleValue());
  }

  public boolean isNaN() {
    return isNaN(doubleValue());
  }

  @Override
  public long longValue() {
    return (long) doubleValue();
  }

  @Override
  public short shortValue() {
    return (short) doubleValue();
  }

  @Override
  public String toString() {
    return toString(doubleValue());
  }

  // CHECKSTYLE_OFF: Utility Methods for unboxed Double.
  @JsMethod(name = "$create__double")
  private static Double $createDouble(double x) {
    return createNative(x);
  }

  @JsMethod(name = "$create__java_lang_String")
  private static Double $createDouble(String s) {
    return createNative(Double.parseDouble(s));
  }

  private static native Double createNative(double x) /*-{
    return x;
  }-*/;

  @JsMethod
  private static boolean $isInstance(Object instance) {
    return "number".equals(JsUtils.typeOf(instance));
  }
  //CHECKSTYLE_ON: End utility methods
}
