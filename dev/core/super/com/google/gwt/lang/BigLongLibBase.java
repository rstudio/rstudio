/*
 * Copyright 2010 Google Inc.
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

/**
 * Implements a Java <code>long</code> in a way that can be translated to
 * JavaScript.
 */
class BigLongLibBase {

  /*
   * Implementation: A LongEmul containing three values {l, m, h} (low, middle,
   * high) such that (x.l + ((long) x.m << 22) + ((long) x.h << 44)) is equal to
   * the original long integer. The constant 22 is chosen since some browsers
   * are faster when operating on integers of 24 bits or less.
   *
   * By convention, we expect and maintain that the upper bits of each word be
   * zeroed.
   *
   * Note that this class must be careful using type "long". Being the
   * implementation of the long type for Production Mode, any place it uses a
   * long is not usable in Production Mode. There is currently one such method:
   * {@link LongLib#getAsLongArray}.
   */
  static class BigLong {
    int l, m, h;
  }


  // Note that the {@link LonghLib#mul} method implicitly depends on the
  // specific value BITS == 22
  protected static final int BITS = 22;
  protected static final int BITS01 = 2 * BITS;
  protected static final int BITS2 = 64 - BITS01;
  protected static final int MASK = (1 << BITS) - 1;
  protected static final int MASK_2 = (1 << BITS2) - 1;
  protected static BigLong remainder;

  protected static final int SIGN_BIT = BITS2 - 1;
  protected static final int SIGN_BIT_VALUE = 1 << SIGN_BIT;
  protected static final double TWO_PWR_15_DBL = 0x8000;
  protected static final double TWO_PWR_16_DBL = 0x10000;
  protected static final double TWO_PWR_22_DBL = 0x400000;
  protected static final double TWO_PWR_31_DBL = TWO_PWR_16_DBL
      * TWO_PWR_15_DBL;
  protected static final double TWO_PWR_32_DBL = TWO_PWR_16_DBL
      * TWO_PWR_16_DBL;
  protected static final double TWO_PWR_44_DBL = TWO_PWR_22_DBL
      * TWO_PWR_22_DBL;
  protected static final double TWO_PWR_63_DBL = TWO_PWR_32_DBL
      * TWO_PWR_31_DBL;

  protected static BigLong create(int value) {
    int a0 = value & MASK;
    int a1 = (value >> BITS) & MASK;
    int a2 = (value < 0) ? MASK_2 : 0;

    if (LongLib.RUN_IN_JVM) {
      BigLong a = new BigLong();
      a.l = a0;
      a.m = a1;
      a.h = a2;
      return a;
    }
    return create0(a0, a1, a2);
  }

  protected static BigLong create(int a0, int a1, int a2) {
    if (LongLib.RUN_IN_JVM) {
      BigLong a = new BigLong();
      a.l = a0;
      a.m = a1;
      a.h = a2;
      return a;
    }
    return create0(a0, a1, a2);
  }

  protected static BigLong divMod(BigLong a, BigLong b,
      boolean computeRemainder) {
    if (isZero(b)) {
      throw new ArithmeticException("divide by zero");
    }
    if (isZero(a)) {
      if (computeRemainder) {
        remainder = create(); // zero
      }
      return create(); // zero
    }

    // MIN_VALUE / MIN_VALUE = 1, anything other a / MIN_VALUE is 0
    if (isMinValue(b)) {
      return divModByMinValue(a, computeRemainder);
    }

    // Normalize b to abs(b), keeping track of the parity in 'negative'.
    // We can do this because we have already ensured that b != MIN_VALUE.
    boolean negative = false;
    if (isNegative(b)) {
      b = BigLongLib.neg(b);
      negative = !negative;
    }

    // If b == 2^n, bpower will be n, otherwise it will be -1
    int bpower = powerOfTwo(b);

    // True if the original value of a is negative
    boolean aIsNegative = false;
    // True if the original value of a is Long.MIN_VALUE
    boolean aIsMinValue = false;

    /*
     * Normalize a to a positive value, keeping track of the sign change in
     * 'negative' (which tracks the sign of both a and b and is used to
     * determine the sign of the quotient) and 'aIsNegative' (which is used to
     * determine the sign of the remainder).
     *
     * For all values of a except MIN_VALUE, we can just negate a and modify
     * negative and aIsNegative appropriately. When a == MIN_VALUE, negation is
     * not possible without overflowing 64 bits, so instead of computing
     * abs(MIN_VALUE) / abs(b) we compute (abs(MIN_VALUE) - 1) / abs(b). The
     * only circumstance under which these quotients differ is when b is a power
     * of two, which will divide abs(MIN_VALUE) == 2^64 exactly. In this case,
     * we can get the proper result by shifting MIN_VALUE in unsigned fashion.
     *
     * We make a single copy of a before the first operation that needs to
     * modify its value.
     */
    boolean aIsCopy = false;
    if (isMinValue(a)) {
      aIsMinValue = true;
      aIsNegative = true;
      // If b is not a power of two, treat -a as MAX_VALUE (instead of the
      // actual value (MAX_VALUE + 1)).
      if (bpower == -1) {
        a = create(BigLongLib.Const.MAX_VALUE);
        aIsCopy = true;
        negative = !negative;
      } else {
        // Signed shift of MIN_VALUE produces the right answer
        BigLong c = BigLongLib.shr(a, bpower);
        if (negative) {
          negate(c);
        }
        if (computeRemainder) {
          remainder = create(); // zero
        }
        return c;
      }
    } else if (isNegative(a)) {
      aIsNegative = true;
      a = BigLongLib.neg(a);
      aIsCopy = true;
      negative = !negative;
    }

    // Now both a and b are non-negative

    // If b is a power of two, just shift
    if (bpower != -1) {
      return divModByShift(a, bpower, negative, aIsNegative, computeRemainder);
    }

    // if a < b, the quotient is 0 and the remainder is a
    if (BigLongLib.compare(a, b) < 0) {
      if (computeRemainder) {
        if (aIsNegative) {
          remainder = BigLongLib.neg(a);
        } else {
          remainder = create(a);
        }
      }
      return create(); // zero
    }

    // Generate the quotient using bit-at-a-time long division
    return divModHelper(aIsCopy ? a : create(a), b, negative, aIsNegative,
        aIsMinValue, computeRemainder);
  }

  protected static int getH(BigLong a) {
    if (LongLib.RUN_IN_JVM) {
      return a.h;
    }
    return getHNative(a);
  }

  protected static int getL(BigLong a) {
    if (LongLib.RUN_IN_JVM) {
      return a.l;
    }
    return getLNative(a);
  }

  protected static int getM(BigLong a) {
    if (LongLib.RUN_IN_JVM) {
      return a.m;
    }
    return getMNative(a);
  }

  protected static boolean isMinValue(BigLong a) {
    return getH(a) == SIGN_BIT_VALUE && getM(a) == 0 && getL(a) == 0;
  }

  protected static boolean isNegative(BigLong a) {
    return sign(a) != 0;
  }

  protected static boolean isZero(BigLong a) {
    return getL(a) == 0 && getM(a) == 0 && getH(a) == 0;
  }

  /**
   * a = -a
   */
  protected static void negate(BigLong a) {
    int neg0 = (~getL(a) + 1) & MASK;
    int neg1 = (~getM(a) + (neg0 == 0 ? 1 : 0)) & MASK;
    int neg2 = (~getH(a) + ((neg0 == 0 && neg1 == 0) ? 1 : 0)) & MASK_2;

    if (LongLib.RUN_IN_JVM) {
      a.l = neg0;
      a.m = neg1;
      a.h = neg2;
    } else {
      setL(a, neg0);
      setM(a, neg1);
      setH(a, neg2);
    }
  }

  /**
   * @return 0 if a is >= 0, 1 if a < 0.
   */
  protected static int sign(BigLong a) {
    return getH(a) >> (BITS2 - 1);
  }

  // Assumes a is non-negative
  protected static double toDoubleHelper(BigLong a) {
    return getL(a) + (getM(a) * TWO_PWR_22_DBL) + (getH(a) * TWO_PWR_44_DBL);
  }

  /**
   * Return the number of leading zeros of a long value.
   */
  // package-private for testing
  static int numberOfLeadingZeros(BigLong a) {
    int b2 = Integer.numberOfLeadingZeros(getH(a));
    if (b2 == 32) {
      int b1 = Integer.numberOfLeadingZeros(getM(a));
      if (b1 == 32) {
        return Integer.numberOfLeadingZeros(getL(a)) + 32;
      } else {
        return b1 + BITS2 - (32 - BITS);
      }
    } else {
      return b2 - (32 - BITS2);
    }
  }

  /**
   * Creates a long instance equal to 0.
   */
  private static BigLong create() {
    if (LongLib.RUN_IN_JVM) {
      return new BigLong();
    }
    return create0(0, 0, 0);
  }

  /**
   * Creates a long instance equal to a given long.
   */
  static BigLong create(BigLong a) {
    if (LongLib.RUN_IN_JVM) {
      BigLong b = new BigLong();
      b.l = getL(a);
      b.m = getM(a);
      b.h = getH(a);
      return b;
    }
    return create0(getL(a), getM(a), getH(a));
  }

  private static native BigLong create0(int l, int m, int h) /*-{
    return {l:l,m:m,h:h};
  }-*/;

  private static BigLong divModByMinValue(BigLong a, boolean computeRemainder) {
    // MIN_VALUE / MIN_VALUE == 1, remainder = 0
    // (a != MIN_VALUE) / MIN_VALUE == 0, remainder == a
    if (isMinValue(a)) {
      if (computeRemainder) {
        remainder = create(); // zero
      }
      return create(BigLongLib.Const.ONE);
    }
    if (computeRemainder) {
      remainder = create(a);
    }
    return create(); // zero
  }

  private static BigLong divModByShift(BigLong a, int bpower,
      boolean negative, boolean aIsNegative, boolean computeRemainder) {
    BigLong c = BigLongLib.shr(a, bpower);
    if (negative) {
      negate(c);
    }

    if (computeRemainder) {
      a = maskRight(a, bpower);
      if (aIsNegative) {
        remainder = BigLongLib.neg(a);
      } else {
        remainder = create(a);
      }
    }
    return c;
  }

  private static BigLong divModHelper(BigLong a, BigLong b,
      boolean negative, boolean aIsNegative, boolean aIsMinValue,
      boolean computeRemainder) {
    // Align the leading one bits of a and b by shifting b left
    int shift = numberOfLeadingZeros(b) - numberOfLeadingZeros(a);
    BigLong bshift = BigLongLib.shl(b, shift);

    BigLong quotient = create();
    while (shift >= 0) {
      boolean gte = trialSubtract(a, bshift);
      if (gte) {
        setBit(quotient, shift);
        if (isZero(a)) {
          break;
        }
      }

      toShru1(bshift);
      shift--;
    }

    if (negative) {
      negate(quotient);
    }

    if (computeRemainder) {
      if (aIsNegative) {
        remainder = BigLongLib.neg(a);
        if (aIsMinValue) {
          remainder = BigLongLib.sub(remainder, BigLongLib.Const.ONE);
        }
      } else {
        remainder = create(a);
      }
    }

    return quotient;
  }

  private static native int getHNative(BigLong a) /*-{
    return a.h;
  }-*/;

  private static native int getLNative(BigLong a) /*-{
    return a.l;
  }-*/;

  private static native int getMNative(BigLong a) /*-{
    return a.m;
  }-*/;

  /**
   * a &= ((1L << bits) - 1)
   */
  private static BigLong maskRight(BigLong a, int bits) {
    int b0, b1, b2;
    if (bits <= BITS) {
      b0 = getL(a) & ((1 << bits) - 1);
      b1 = b2 = 0;
    } else if (bits <= BITS01) {
      b0 = getL(a);
      b1 = getM(a) & ((1 << (bits - BITS)) - 1);
      b2 = 0;
    } else {
      b0 = getL(a);
      b1 = getM(a);
      b2 = getH(a) & ((1 << (bits - BITS01)) - 1);
    }

    return create(b0, b1, b2);
  }

  /**
   * Return the exact log base 2 of a, or -1 if a is not a power of two:
   *
   * <pre>
   * if (x == 2^n) {
   *   return n;
   * } else {
   *   return -1;
   * }
   * </pre>
   */
  private static int powerOfTwo(BigLong a) {
    // Power of two or 0
    int l = getL(a);
    if ((l & (l - 1)) != 0) {
      return -1;
    }
    int m = getM(a);
    if ((m & (m - 1)) != 0) {
      return -1;
    }
    int h = getH(a);
    if ((h & (h - 1)) != 0) {
      return -1;
    }
    if (h == 0 && m == 0 && l == 0) {
      return -1;
    }
    if (h == 0 && m == 0 && l != 0) {
      return Integer.numberOfTrailingZeros(l);
    }
    if (h == 0 && m != 0 && l == 0) {
      return Integer.numberOfTrailingZeros(m) + BITS;
    }
    if (h != 0 && m == 0 && l == 0) {
      return Integer.numberOfTrailingZeros(h) + BITS01;
    }

    return -1;
  }

  private static void setBit(BigLong a, int bit) {
    if (LongLib.RUN_IN_JVM) {
      if (bit < BITS) {
        a.l |= 0x1 << bit;
      } else if (bit < BITS01) {
        a.m |= 0x1 << (bit - BITS);
      } else {
        a.h |= 0x1 << (bit - BITS01);
      }
    } else {
      if (bit < BITS) {
        setBitL(a, bit);
      } else if (bit < BITS01) {
        setBitM(a, bit - BITS);
      } else {
        setBitH(a, bit - BITS01);
      }
    }
  }

  private static native void setBitH(BigLong a, int bit) /*-{
    a.h |= 1 << bit;
  }-*/;

  private static native void setBitL(BigLong a, int bit) /*-{
    a.l |= 1 << bit;
  }-*/;

  private static native void setBitM(BigLong a, int bit) /*-{
    a.m |= 1 << bit;
  }-*/;

  private static native void setH(BigLong a, int x) /*-{
    a.h = x;
  }-*/;

  private static native void setL(BigLong a, int x) /*-{
    a.l = x;
  }-*/;

  private static native void setM(BigLong a, int x) /*-{
    a.m = x;
  }-*/;

  /**
   * a >>= 1. Assumes a >= 0.
   */
  private static void toShru1(BigLong a) {
    int a1 = getM(a);
    int a2 = getH(a);
    int a0 = getL(a);

    if (LongLib.RUN_IN_JVM) {
      a.h = a2 >>> 1;
      a.m = (a1 >>> 1) | ((a2 & 0x1) << (BITS - 1));
      a.l = (a0 >>> 1) | ((a1 & 0x1) << (BITS - 1));
    } else {
      setH(a, a2 >>> 1);
      setM(a, (a1 >>> 1) | ((a2 & 0x1) << (BITS - 1)));
      setL(a, (a0 >>> 1) | ((a1 & 0x1) << (BITS - 1)));
    }
  }

  /**
   * Attempt to subtract b from a if a >= b:
   *
   * <pre>
   * if (a >= b) {
   *   a -= b;
   *   return true;
   * } else {
   *   return false;
   * }
   * </pre>
   */
  private static boolean trialSubtract(BigLong a, BigLong b) {
    // Early exit
    int sum2 = getH(a) - getH(b);
    if (sum2 < 0) {
      return false;
    }

    int sum0 = getL(a) - getL(b);
    int sum1 = getM(a) - getM(b) + (sum0 >> BITS);
    sum2 += (sum1 >> BITS);

    if (sum2 < 0) {
      return false;
    }

    if (LongLib.RUN_IN_JVM) {
      a.l = sum0 & MASK;
      a.m = sum1 & MASK;
      a.h = sum2 & MASK_2;
    } else {
      setL(a, sum0 & MASK);
      setM(a, sum1 & MASK);
      setH(a, sum2 & MASK_2);
    }

    return true;
  }

  /**
   * Not instantiable outside this package.
   */
  BigLongLibBase() {
  }
}
