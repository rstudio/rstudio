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

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * Math utility methods and constants.
 */
public final class Math {
  // The following methods are not implemented because JS doesn't provide the
  // necessary pieces:
  //   public static double ulp (double x)
  //   public static float ulp (float x)
  //   public static int getExponent (double d)
  //   public static int getExponent (float f)
  //   public static double IEEEremainder(double f1, double f2)
  //   public static double nextAfter(double start, double direction)
  //   public static float nextAfter(float start, float direction)
  //   public static double nextUp(double start) {
  //     return nextAfter(start, 1.0d);
  //   }
  //   public static float nextUp(float start) {
  //     return nextAfter(start,1.0f);
  //   }

  public static final double E = 2.7182818284590452354;
  public static final double PI = 3.14159265358979323846;

  private static final double PI_OVER_180 = PI / 180.0;
  private static final double PI_UNDER_180 = 180.0 / PI;

  public static double abs(double x) {
    // This is implemented this way so that either positive or negative zeroes
    // get converted to positive zeros.
    // See http://www.concentric.net/~Ttwang/tech/javafloat.htm for details.
    return x <= 0 ? 0.0 - x : x;
  }

  public static float abs(float x) {
    return (float) abs((double) x);
  }

  public static int abs(int x) {
    return x < 0 ? -x : x;
  }

  public static long abs(long x) {
    return x < 0 ? -x : x;
  }

  public static double acos(double x) {
    return NativeMath.acos(x);
  }

  public static double asin(double x) {
    return NativeMath.asin(x);
  }

  public static int addExact(int x, int y) {
    int r = x + y;
    // "Hacker's Delight" 2-12 Overflow if both arguments have the opposite sign of the result
    throwOverflowIf(((x ^ r) & (y ^ r)) < 0);
    return r;
  }

  public static long addExact(long x, long y) {
    long r = x + y;
    // "Hacker's Delight" 2-12 Overflow if both arguments have the opposite sign of the result
    throwOverflowIf(((x ^ r) & (y ^ r)) < 0);
    return r;
  }

  public static double atan(double x) {
    return NativeMath.atan(x);
  }

  public static double atan2(double y, double x) {
    return NativeMath.atan2(y, x);
  }

  public static double cbrt(double x) {
    return Math.pow(x, 1.0 / 3.0);
  }

  public static double ceil(double x) {
    return NativeMath.ceil(x);
  }

  public static double copySign(double magnitude, double sign) {
    return isNegative(sign) ? -Math.abs(magnitude) : Math.abs(magnitude);
  }

  private static boolean isNegative(double d) {
    return d < 0 || 1 / d < 0;
  }

  public static float copySign(float magnitude, float sign) {
    return (float) (copySign((double) magnitude, (double) sign));
  }

  public static double cos(double x) {
    return NativeMath.cos(x);
  }

  public static double cosh(double x) {
    return (Math.exp(x) + Math.exp(-x)) / 2.0;
  }

  public static int decrementExact(int x) {
    throwOverflowIf(x == Integer.MIN_VALUE);
    return x - 1;
  }

  public static long decrementExact(long x) {
    throwOverflowIf(x == Long.MIN_VALUE);
    return x - 1;
  }

  public static double exp(double x) {
    return NativeMath.exp(x);
  }

  public static double expm1(double d) {
    if (d == 0.0 || Double.isNaN(d)) {
      return d; // "a zero with same sign as argument", arg is zero, so...
    } else if (!Double.isInfinite(d)) {
      if (d < 0.0d) {
        return -1.0d;
      } else {
        return Double.POSITIVE_INFINITY;
      }
    }
    return exp(d) + 1.0d;
  }

  public static double floor(double x) {
    return NativeMath.floor(x);
  }

  public static int floorDiv(int dividend, int divisor) {
    throwDivByZeroIf(divisor == 0);
    int r = dividend / divisor;
    // if the signs are different and modulo not zero, round down
    if ((dividend ^ divisor) < 0 && (r * divisor != dividend)) {
      r--;
    }
    return r;
  }

  public static long floorDiv(long dividend, long divisor) {
    throwDivByZeroIf(divisor == 0);
    long r = dividend / divisor;
    // if the signs are different and modulo not zero, round down
    if ((dividend ^ divisor) < 0 && (r * divisor != dividend)) {
      r--;
    }
    return r;
  }

  public static int floorMod(int dividend, int divisor) {
    return dividend - floorDiv(dividend, divisor) * divisor;
  }

  public static long floorMod(long dividend, long divisor) {
    return dividend - floorDiv(dividend, divisor) * divisor;
  }

  public static double hypot(double x, double y) {
    return sqrt(x * x + y * y);
  }

  public static int incrementExact(int x) {
    throwOverflowIf(x == Integer.MAX_VALUE);
    return x + 1;
  }

  public static long incrementExact(long x) {
    throwOverflowIf(x == Long.MAX_VALUE);
    return x + 1;
  }

  public static double log(double x) {
    return NativeMath.log(x);
  }

  public static double log10(double x) {
    return NativeMath.log(x) * NativeMath.LOG10E;
  }

  public static double log1p(double x) {
    return Math.log(x + 1.0d);
  }

  public static double max(double x, double y) {
    return NativeMath.max(x, y);
  }

  public static float max(float x, float y) {
    return (float) NativeMath.max(x, y);
  }

  public static int max(int x, int y) {
    return x > y ? x : y;
  }

  public static long max(long x, long y) {
    return x > y ? x : y;
  }

  public static double min(double x, double y) {
    return NativeMath.min(x, y);
  }

  public static float min(float x, float y) {
    return (float) NativeMath.min(x, y);
  }

  public static int min(int x, int y) {
    return x < y ? x : y;
  }

  public static long min(long x, long y) {
    return x < y ? x : y;
  }

  public static int multiplyExact(int x, int y) {
    long r = (long) x * (long) y;
    int ir = (int) r;
    throwOverflowIf(ir != r);
    return ir;
  }

  public static long multiplyExact(long x, long y) {
    long r = x * y;
    throwOverflowIf((x == Long.MIN_VALUE && y == -1) || (y != 0 && (r / y != x)));
    return r;
  }

  public static int negateExact(int x) {
    throwOverflowIf(x == Integer.MIN_VALUE);
    return -x;
  }

  public static long negateExact(long x) {
    throwOverflowIf(x == Long.MIN_VALUE);
    return -x;
  }

  public static double pow(double x, double exp) {
    return NativeMath.pow(x, exp);
  }

  public static double random() {
    return NativeMath.random();
  }

  public static double rint(double x) {
    double mod2 = x % 2;
    if ((mod2 == -1.5) || (mod2 == 0.5)) {
      return NativeMath.floor(x);
    } else {
      return NativeMath.round(x);
    }
  }

  public static long round(double x) {
    return (long) NativeMath.round(x);
  }

  public static int round(float x) {
    double roundedValue = NativeMath.round(x);
    return unsafeCastToInt(roundedValue);
  }

  private static native int unsafeCastToInt(double d) /*-{
    return d;
  }-*/;

  public static int subtractExact(int x, int y) {
    int r = x - y;
    // "Hacker's Delight" Overflow if the arguments have different signs and
    // the sign of the result is different than the sign of x
    throwOverflowIf(((x ^ y) & (x ^ r)) < 0);
    return r;
  }

  public static long subtractExact(long x, long y) {
    long r = x - y;
    // "Hacker's Delight" Overflow if the arguments have different signs and
    // the sign of the result is different than the sign of x
    throwOverflowIf(((x ^ y) & (x ^ r)) < 0);
    return r;
  }

  public static double scalb(double d, int scaleFactor) {
    if (scaleFactor >= 31 || scaleFactor <= -31) {
      return d * Math.pow(2, scaleFactor);
    } else if (scaleFactor > 0) {
      return d * (1 << scaleFactor);
    } else if (scaleFactor == 0) {
      return d;
    } else {
      return d * 1.0d / (1 << -scaleFactor);
    }
  }

  public static float scalb(float f, int scaleFactor) {
    return (float) scalb((double) f, scaleFactor);
  }

  public static double signum(double d) {
    if (d == 0. || Double.isNaN(d)) {
      return d;
    } else {
      return d < 0 ? -1 : 1;
    }
  }

  public static float signum(float f) {
    return (float) signum((double) f);
  }

  public static double sin(double x) {
    return NativeMath.sin(x);
  }

  public static double sinh(double x) {
    return (Math.exp(x) - Math.exp(-x)) / 2.0d;
  }

  public static double sqrt(double x) {
    return NativeMath.sqrt(x);
  }

  public static double tan(double x) {
    return NativeMath.tan(x);
  }

  public static double tanh(double x) {
    if (Double.isInfinite(x)) {
      return signum(x);
    }

    double e2x = Math.exp(2.0 * x);
    return (e2x - 1) / (e2x + 1);
  }

  public static double toDegrees(double x) {
    return x * PI_UNDER_180;
  }

  public static int toIntExact(long x) {
    int ix = (int) x;
    throwOverflowIf(ix != x);
    return ix;
  }

  public static double toRadians(double x) {
    return x * PI_OVER_180;
  }

  private static void throwDivByZeroIf(boolean condition) {
    if (condition) {
      throw new ArithmeticException("div by zero");
    }
  }

  private static void throwOverflowIf(boolean condition) {
    if (condition) {
      throw new ArithmeticException("overflow");
    }
  }

  @JsType(isNative = true, name = "Math", namespace = JsPackage.GLOBAL)
  private static class NativeMath {
    public static double LOG10E;
    public static native double acos(double x);
    public static native double asin(double x);
    public static native double atan(double x);
    public static native double atan2(double y, double x);
    public static native double ceil(double x);
    public static native double cos(double x);
    public static native double exp(double x);
    public static native double floor(double x);
    public static native double log(double x);
    public static native double max(double x, double y);
    public static native double min(double x, double y);
    public static native double pow(double x, double exp);
    public static native double random();
    public static native double round(double x);
    public static native double sin(double x);
    public static native double sqrt(double x);
    public static native double tan(double x);
  }
}
