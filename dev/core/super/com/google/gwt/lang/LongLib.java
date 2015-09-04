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
package com.google.gwt.lang;

import com.google.gwt.lang.BigLongLibBase.BigLong;

/**
 * Implements a Java <code>long</code> in a way that can be translated to JavaScript.
 */
public class LongLib {

/**
   * Abstraction for long emulation. The emulation could be done using FastLong if the number is
   * small enough or BigLong if the number is big. Note that, the class here is just a place holder
   * and would normally extend JavaScriptObject if there wasn't a JVM mode.
   */
  static class LongEmul {
    SmallLong small;
    BigLong big;
  }

  /**
   * A LongEmul represented as double number.
   */
  static class SmallLong {
    double d;
  }

  /**
   * Allow standalone Java tests such as LongLibTest/LongLibJreTest to run this
   * code.
   */
  protected static boolean RUN_IN_JVM = false;

  public static LongEmul add(LongEmul a, LongEmul b) {
    if (isSmallLong(a) && isSmallLong(b)) {
      double result = asDouble(a) + asDouble(b);
      if (isSafeIntegerRange(result)) {
        return createSmallLongEmul(result);
      }
    }

    return createLongEmul(BigLongLib.add(toBigLong(a), toBigLong(b)));
  }

  public static LongEmul sub(LongEmul a, LongEmul b) {
    if (isSmallLong(a) && isSmallLong(b)) {
      double result = asDouble(a) - asDouble(b);
      if (isSafeIntegerRange(result)) {
        return createSmallLongEmul(result);
      }
    }

    return createLongEmul(BigLongLib.sub(toBigLong(a), toBigLong(b)));
  }

  public static LongEmul neg(LongEmul a) {
    // TODO: add test for max neg number
    if (isSmallLong(a)) {
      double result = 0 - asDouble(a);
      if (!Double.isNaN(result)) {
        return createSmallLongEmul(result);
      }
    }

    return createLongEmul(BigLongLib.neg(asBigLong(a)));
  }

  public static boolean gt(LongEmul a, LongEmul b) {
    return compare(a, b) > 0;
  }

  public static boolean gte(LongEmul a, LongEmul b) {
    return compare(a, b) >= 0;
  }

  public static boolean lt(LongEmul a, LongEmul b) {
    return compare(a, b) < 0;
  }

  public static boolean lte(LongEmul a, LongEmul b) {
    return compare(a, b) <= 0;
  }

  public static boolean eq(LongEmul a, LongEmul b) {
    return compare(a, b) == 0;
  }

  public static boolean neq(LongEmul a, LongEmul b) {
    return compare(a, b) != 0;
  }

  // VisibleForTesting
  static double compare(LongEmul a, LongEmul b) {
    if (isSmallLong(a) && isSmallLong(b)) {
      double result = asDouble(a) - asDouble(b);
      if (!Double.isNaN(result)) {
        return result;
      }
    }

    return BigLongLib.compare(toBigLong(a), toBigLong(b));
  }

  public static LongEmul div(LongEmul a, LongEmul b) {
    if (isSmallLong(a) && isSmallLong(b)) {
      double result = asDouble(a) / asDouble(b);
      if (isSafeIntegerRange(result)) {
        return createSmallLongEmul(truncate(result));
      }
    }

    return createLongEmul(BigLongLib.div(toBigLong(a), toBigLong(b)));
  }

  public static LongEmul mod(LongEmul a, LongEmul b) {
    if (isSmallLong(a) && isSmallLong(b)) {
      double result = asDouble(a) % asDouble(b);
      if (isSafeIntegerRange(result)) {
        return createSmallLongEmul(result);
      }
    }

    return createLongEmul(BigLongLib.mod(toBigLong(a), toBigLong(b)));
  }

  public static LongEmul mul(LongEmul a, LongEmul b) {
    if (isSmallLong(a) && isSmallLong(b)) {
      double result = asDouble(a) * asDouble(b);
      if (isSafeIntegerRange(result)) {
        return createSmallLongEmul(result);
      }
    }

    return createLongEmul(BigLongLib.mul(toBigLong(a), toBigLong(b)));
  }

  public static LongEmul not(LongEmul a) {
    return createLongEmul(BigLongLib.not(toBigLong(a)));
  }

  public static LongEmul and(LongEmul a, LongEmul b) {
    return createLongEmul(BigLongLib.and(toBigLong(a), toBigLong(b)));
  }

  public static LongEmul or(LongEmul a, LongEmul b) {
    return createLongEmul(BigLongLib.or(toBigLong(a), toBigLong(b)));
  }

  public static LongEmul xor(LongEmul a, LongEmul b) {
    return createLongEmul(BigLongLib.xor(toBigLong(a), toBigLong(b)));
  }

  public static LongEmul shl(LongEmul a, int n) {
    return createLongEmul(BigLongLib.shl(toBigLong(a), n));
  }

  public static LongEmul shr(LongEmul a, int n) {
    return createLongEmul(BigLongLib.shr(toBigLong(a), n));
  }

  public static LongEmul shru(LongEmul a, int n) {
    return createLongEmul(BigLongLib.shru(toBigLong(a), n));
  }

  public static LongEmul fromDouble(double value) {
    if (isSafeIntegerRange(value)) {
      return createSmallLongEmul(truncate(value));
    }

    return createLongEmul(BigLongLib.fromDouble(value));
  }

  public static double toDouble(LongEmul a) {
    if (isSmallLong(a)) {
      double d = asDouble(a);
      // We need to kill negative zero because that could never happen in long but our double based
      // representation may result with that.
      return d == -0.0 ? 0 : d;
    }

    return BigLongLib.toDouble(asBigLong(a));
  }

  public static LongEmul fromInt(int value) {
    return createSmallLongEmul(value);
  }

  public static int toInt(LongEmul a) {
    if (isSmallLong(a)) {
      return coerceToInt(asDouble(a));
    }

    return BigLongLib.toInt(asBigLong(a));
  }

  public static String toString(LongEmul a) {
    if (isSmallLong(a)) {
      return toString(asDouble(a));
    }

    return BigLongLib.toString(asBigLong(a));
  }

  // Called by compiler to generate constants.
  public static long[] getAsLongArray(long l) {
    if (isSafeIntegerRange(l)) {
      return new long[] {l};
    }

    return BigLongLib.getAsLongArray(l);
  }

  // TODO(goktug): Safe integer range could potentially increased update up to 53 bits.
  private static boolean isSafeIntegerRange(double value) {
    return -BigLongLibBase.TWO_PWR_44_DBL < value && value < BigLongLibBase.TWO_PWR_44_DBL;
  }

  private static double truncate(double value) {
    // Same as Math.trunc() but not available everywhere.
    return value < 0 ? Math.ceil(value) : Math.floor(value);
  }

  private static int coerceToInt(double value) {
    if (LongLib.RUN_IN_JVM) {
      return (int) (long) value;
    }
    return coerceToInt0(value);
  }

  private static native int coerceToInt0(double value)/*-{
    return value | 0;
  }-*/;

  private static String toString(double value) {
    if (LongLib.RUN_IN_JVM) {
      return String.valueOf((long) value);
    }
    return String.valueOf(value);
  }

  private static double asDouble(LongEmul value) {
    return asDouble(asSmallLong(value));
  }

  private static SmallLong asSmallLong(LongEmul value) {
    if (LongLib.RUN_IN_JVM) {
      return value.small;
    }
    return asSmallLong0(value);
  }

  private static native SmallLong asSmallLong0(LongEmul value)/*-{
    return value;
  }-*/;

  private static double asDouble(SmallLong value) {
    if (LongLib.RUN_IN_JVM) {
      return value == null ? Double.NaN : value.d;
    }
    return asDouble0(value);
  }

  private static native double asDouble0(SmallLong value)/*-{
    return value;
  }-*/;

  private static boolean isSmallLong(LongEmul value) {
    if (LongLib.RUN_IN_JVM) {
      return value.small != null;
    }
    return isSmallLong0(value);
  }

  private static native boolean isSmallLong0(LongEmul value)/*-{
    return typeof(value) === 'number';
  }-*/;

  // Visible for testing
  static BigLong asBigLong(LongEmul value) {
    if (LongLib.RUN_IN_JVM) {
      return value.big;
    }
    return asBigLong0(value);
  }

  private static native BigLong asBigLong0(LongEmul value)/*-{
    return value;
  }-*/;

  private static BigLong toBigLong(LongEmul value) {
    return isSmallLong(value) ? toBigLong(asSmallLong(value)) : asBigLong(value);
  }

  private static BigLong toBigLong(SmallLong longValue) {
    double value = asDouble(longValue);
    int a3 = 0;
    if (value < 0) {
      // Convert to a positive number that will have the exact same first 44 bits
      value += BigLongLibBase.TWO_PWR_44_DBL;
      a3 = BigLongLib.MASK_2;
    }
    int a1 = (int) (value / BigLongLibBase.TWO_PWR_22_DBL);
    int a0 = (int) (value - a1 * BigLongLibBase.TWO_PWR_22_DBL);
    return BigLongLibBase.create(a0, a1, a3);
  }

  private static LongEmul createSmallLongEmul(double value) {
    if (LongLib.RUN_IN_JVM) {
      SmallLong small = new SmallLong();
      small.d = value;
      LongEmul emul = new LongEmul();
      emul.small = small;
      return emul;
    }
    return createSmallLongEmul0(value);
  }

  private static native LongEmul createSmallLongEmul0(double value)/*-{
    return value;
  }-*/;

  private static LongEmul createLongEmul(BigLong big) {
    int a2 = BigLongLibBase.getH(big);
    if (a2 == 0) {
      return createSmallLongEmul(
          BigLongLibBase.getL(big) + BigLongLibBase.getM(big) * BigLongLibBase.TWO_PWR_22_DBL);
    }
    if (a2 == BigLongLibBase.MASK_2) {
      return createSmallLongEmul(BigLongLibBase.getL(big)
          + BigLongLibBase.getM(big) * BigLongLibBase.TWO_PWR_22_DBL
          - BigLongLib.TWO_PWR_44_DBL);
    }

    return createBigLongEmul(big);
  }

  private static LongEmul createBigLongEmul(BigLong big) {
    if (LongLib.RUN_IN_JVM) {
      LongEmul emul = new LongEmul();
      emul.big = big;
      return emul;
    }
    return createBigLongEmul0(big);
  }

  private static native LongEmul createBigLongEmul0(BigLong value)/*-{
    return value;
  }-*/;

  // VisibleForTesting
  static LongEmul copy(LongEmul value) {
    if (isSmallLong(value)) {
      return createSmallLongEmul(asDouble(value));
    } else {
      return createBigLongEmul(BigLongLibBase.create(asBigLong(value)));
    }
  }

  /**
   * Not instantiable.
   */
  private LongLib() {
  }
}
