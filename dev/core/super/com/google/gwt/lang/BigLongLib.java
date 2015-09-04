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

/**
 * Implements a Java <code>long</code> in a way that can be translated to
 * JavaScript and could handle numbers needs more than 44 bits.
 */
class BigLongLib extends BigLongLibBase {

  static class Const {
    static final BigLong MAX_VALUE = create(MASK, MASK, MASK_2 >> 1);
    static final BigLong MIN_VALUE = create(0, 0, SIGN_BIT_VALUE);
    static final BigLong ONE = fromInt(1);
    static final BigLong TWO = fromInt(2);
    static final BigLong ZERO = fromInt(0);
  }

  public static BigLong add(BigLong a, BigLong b) {
    int sum0 = getL(a) + getL(b);
    int sum1 = getM(a) + getM(b) + (sum0 >> BITS);
    int sum2 = getH(a) + getH(b) + (sum1 >> BITS);

    return create(sum0 & MASK, sum1 & MASK, sum2 & MASK_2);
  }

  public static BigLong and(BigLong a, BigLong b) {
    return create(getL(a) & getL(b), getM(a) & getM(b), getH(a) & getH(b));
  }

  public static double compare(BigLong a, BigLong b) {
    int signA = sign(a);
    int signB = sign(b);
    if (signA != signB) {
      return signB - signA;
    }

    int a2 = getH(a);
    int b2 = getH(b);
    if (a2 != b2) {
      return a2 - b2;
    }

    int a1 = getM(a);
    int b1 = getM(b);
    if (a1 != b1) {
      return a1 - b1;
    }

    int a0 = getL(a);
    int b0 = getL(b);
    return a0 - b0;
  }

  public static BigLong div(BigLong a, BigLong b) {
    return divMod(a, b, false);
  }

  public static BigLong fromDouble(double value) {
    if (Double.isNaN(value)) {
      return Const.ZERO;
    }
    if (value < -TWO_PWR_63_DBL) {
      return Const.MIN_VALUE;
    }
    if (value >= TWO_PWR_63_DBL) {
      return Const.MAX_VALUE;
    }

    boolean negative = false;
    if (value < 0) {
      negative = true;
      value = -value;
    }
    int a2 = 0;
    if (value >= TWO_PWR_44_DBL) {
      a2 = (int) (value / TWO_PWR_44_DBL);
      value -= a2 * TWO_PWR_44_DBL;
    }
    int a1 = 0;
    if (value >= TWO_PWR_22_DBL) {
      a1 = (int) (value / TWO_PWR_22_DBL);
      value -= a1 * TWO_PWR_22_DBL;
    }
    int a0 = (int) value;
    BigLong result = create(a0, a1, a2);
    if (negative) {
      negate(result);
    }
    return result;
  }

  public static BigLong fromInt(int value) {
    return create(value);
  }

  public static long[] getAsLongArray(long l) {
    long[] a = new long[3];
    a[0] = (int) (l & MASK);
    a[1] = (int) ((l >> BITS) & MASK);
    a[2] = (int) ((l >> BITS01) & MASK_2);
    return a;
  }

  public static BigLong mod(BigLong a, BigLong b) {
    divMod(a, b, true);
    return remainder;
  }

  // Assumes BITS == 22
  public static BigLong mul(BigLong a, BigLong b) {
    // Grab 13-bit chunks
    int a0 = getL(a) & 0x1fff;
    int a1 = (getL(a) >> 13) | ((getM(a) & 0xf) << 9);
    int a2 = (getM(a) >> 4) & 0x1fff;
    int a3 = (getM(a) >> 17) | ((getH(a) & 0xff) << 5);
    int a4 = (getH(a) & 0xfff00) >> 8;

    int b0 = getL(b) & 0x1fff;
    int b1 = (getL(b) >> 13) | ((getM(b) & 0xf) << 9);
    int b2 = (getM(b) >> 4) & 0x1fff;
    int b3 = (getM(b) >> 17) | ((getH(b) & 0xff) << 5);
    int b4 = (getH(b) & 0xfff00) >> 8;

    // Compute partial products
    // Optimization: if b is small, avoid multiplying by parts that are 0
    int p0 = a0 * b0; // << 0
    int p1 = a1 * b0; // << 13
    int p2 = a2 * b0; // << 26
    int p3 = a3 * b0; // << 39
    int p4 = a4 * b0; // << 52

    if (b1 != 0) {
      p1 += a0 * b1;
      p2 += a1 * b1;
      p3 += a2 * b1;
      p4 += a3 * b1;
    }
    if (b2 != 0) {
      p2 += a0 * b2;
      p3 += a1 * b2;
      p4 += a2 * b2;
    }
    if (b3 != 0) {
      p3 += a0 * b3;
      p4 += a1 * b3;
    }
    if (b4 != 0) {
      p4 += a0 * b4;
    }

    // Accumulate into 22-bit chunks:
    // .........................................c10|...................c00|
    // |....................|..................xxxx|xxxxxxxxxxxxxxxxxxxxxx| p0
    // |....................|......................|......................|
    // |....................|...................c11|......c01.............|
    // |....................|....xxxxxxxxxxxxxxxxxx|xxxxxxxxx.............| p1
    // |....................|......................|......................|
    // |.................c22|...............c12....|......................|
    // |..........xxxxxxxxxx|xxxxxxxxxxxxxxxxxx....|......................| p2
    // |....................|......................|......................|
    // |.................c23|..c13.................|......................|
    // |xxxxxxxxxxxxxxxxxxxx|xxxxx.................|......................| p3
    // |....................|......................|......................|
    // |.........c24........|......................|......................|
    // |xxxxxxxxxxxx........|......................|......................| p4

    int c00 = p0 & 0x3fffff;
    int c01 = (p1 & 0x1ff) << 13;
    int c0 = c00 + c01;

    int c10 = p0 >> 22;
    int c11 = p1 >> 9;
    int c12 = (p2 & 0x3ffff) << 4;
    int c13 = (p3 & 0x1f) << 17;
    int c1 = c10 + c11 + c12 + c13;

    int c22 = p2 >> 18;
    int c23 = p3 >> 5;
    int c24 = (p4 & 0xfff) << 8;
    int c2 = c22 + c23 + c24;

    // Propagate high bits from c0 -> c1, c1 -> c2
    c1 += c0 >> BITS;
    c0 &= MASK;
    c2 += c1 >> BITS;
    c1 &= MASK;
    c2 &= MASK_2;

    return create(c0, c1, c2);
  }

  public static BigLong neg(BigLong a) {
    int neg0 = (~getL(a) + 1) & MASK;
    int neg1 = (~getM(a) + (neg0 == 0 ? 1 : 0)) & MASK;
    int neg2 = (~getH(a) + ((neg0 == 0 && neg1 == 0) ? 1 : 0)) & MASK_2;

    return create(neg0, neg1, neg2);
  }

  public static BigLong not(BigLong a) {
    return create((~getL(a)) & MASK, (~getM(a)) & MASK, (~getH(a)) & MASK_2);
  }

  public static BigLong or(BigLong a, BigLong b) {
    return create(getL(a) | getL(b), getM(a) | getM(b), getH(a) | getH(b));
  }

  public static BigLong shl(BigLong a, int n) {
    n &= 63;

    int res0, res1, res2;
    if (n < BITS) {
      res0 = getL(a) << n;
      res1 = (getM(a) << n) | (getL(a) >> (BITS - n));
      res2 = (getH(a) << n) | (getM(a) >> (BITS - n));
    } else if (n < BITS01) {
      res0 = 0;
      res1 = getL(a) << (n - BITS);
      res2 = (getM(a) << (n - BITS)) | (getL(a) >> (BITS01 - n));
    } else {
      res0 = 0;
      res1 = 0;
      res2 = getL(a) << (n - BITS01);
    }

    return create(res0 & MASK, res1 & MASK, res2 & MASK_2);
  }

  public static BigLong shr(BigLong a, int n) {
    n &= 63;

    int res0, res1, res2;

    // Sign extend h(a)
    int a2 = getH(a);
    boolean negative = (a2 & SIGN_BIT_VALUE) != 0;
    if (negative) {
      a2 |= ~MASK_2;
    }

    if (n < BITS) {
      res2 = a2 >> n;
      res1 = (getM(a) >> n) | (a2 << (BITS - n));
      res0 = (getL(a) >> n) | (getM(a) << (BITS - n));
    } else if (n < BITS01) {
      res2 = negative ? MASK_2 : 0;
      res1 = a2 >> (n - BITS);
      res0 = (getM(a) >> (n - BITS)) | (a2 << (BITS01 - n));
    } else {
      res2 = negative ? MASK_2 : 0;
      res1 = negative ? MASK : 0;
      res0 = a2 >> (n - BITS01);
    }

    return create(res0 & MASK, res1 & MASK, res2 & MASK_2);
  }

  /**
   * Logical right shift. It does not preserve the sign of the input.
   */
  public static BigLong shru(BigLong a, int n) {
    n &= 63;

    int res0, res1, res2;
    int a2 = getH(a) & MASK_2;
    if (n < BITS) {
      res2 = a2 >>> n;
      res1 = (getM(a) >> n) | (a2 << (BITS - n));
      res0 = (getL(a) >> n) | (getM(a) << (BITS - n));
    } else if (n < BITS01) {
      res2 = 0;
      res1 = a2 >>> (n - BITS);
      res0 = (getM(a) >> (n - BITS)) | (getH(a) << (BITS01 - n));
    } else {
      res2 = 0;
      res1 = 0;
      res0 = a2 >>> (n - BITS01);
    }

    return create(res0 & MASK, res1 & MASK, res2 & MASK_2);
  }

  public static BigLong sub(BigLong a, BigLong b) {
    int sum0 = getL(a) - getL(b);
    int sum1 = getM(a) - getM(b) + (sum0 >> BITS);
    int sum2 = getH(a) - getH(b) + (sum1 >> BITS);

    return create(sum0 & MASK, sum1 & MASK, sum2 & MASK_2);
  }

  public static double toDouble(BigLong a) {
    if (BigLongLib.compare(a, Const.ZERO) < 0) {
      return -toDoubleHelper(BigLongLib.neg(a));
    }
    return toDoubleHelper(a);
  }

  // Assumes Integer.MIN_VALUE <= a <= Integer.MAX_VALUE
  public static int toInt(BigLong a) {
    return getL(a) | (getM(a) << BITS);
  }

  public static String toString(BigLong a) {
    if (BigLongLibBase.isZero(a)) {
      return "0";
    }

    if (BigLongLibBase.isMinValue(a)) {
      // Special-case MIN_VALUE because neg(MIN_VALUE) == MIN_VALUE
      return "-9223372036854775808";
    }

    if (BigLongLibBase.isNegative(a)) {
      return "-" + toString(neg(a));
    }

    BigLong rem = a;
    String res = "";

    while (!BigLongLibBase.isZero(rem)) {
      // Do several digits each time through the loop, so as to
      // minimize the calls to the very expensive emulated div.
      final int tenPowerZeroes = 9;
      final int tenPower = 1000000000;
      BigLong tenPowerLong = fromInt(tenPower);

      rem = divMod(rem, tenPowerLong, true);
      String digits = "" + toInt(BigLongLibBase.remainder);

      if (!BigLongLibBase.isZero(rem)) {
        int zeroesNeeded = tenPowerZeroes - digits.length();
        for (; zeroesNeeded > 0; zeroesNeeded--) {
          digits = "0" + digits;
        }
      }

      res = digits + res;
    }

    return res;
  }

  public static BigLong xor(BigLong a, BigLong b) {
    return create(getL(a) ^ getL(b), getM(a) ^ getM(b), getH(a) ^ getH(b));
  }

  /**
   * Not instantiable.
   */
  private BigLongLib() {
  }
}
