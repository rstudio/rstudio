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

  public static native double acos(double x) /*-{
    return Math.acos(x);
  }-*/;

  public static native double asin(double x) /*-{
    return Math.asin(x);
  }-*/;

  public static native double atan(double x) /*-{
    return Math.atan(x);
  }-*/;

  public static native double atan2(double y, double x) /*-{
    return Math.atan2(y, x);
  }-*/;

  public static double cbrt(double x) {
    return Math.pow(x, 1.0 / 3.0);
  }

  public static native double ceil(double x) /*-{
    return Math.ceil(x);
  }-*/;

  public static double copySign(double magnitude, double sign) {
    if (sign < 0) {
      return (magnitude < 0) ? magnitude : -magnitude;
    } else {
      return (magnitude > 0) ? magnitude : -magnitude;
    }
  }

  public static float copySign(float magnitude, float sign) {
    return (float) (copySign((double) magnitude, (double) sign));
  }

  public static native double cos(double x) /*-{
    return Math.cos(x);
  }-*/;

  public static native double cosh(double x) /*-{
    return (Math.exp(x) + Math.exp(-x)) / 2.0;
  }-*/;

  public static native double exp(double x) /*-{
    return Math.exp(x);
  }-*/;

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

  public static native double floor(double x) /*-{
    return Math.floor(x);
  }-*/;

  public static double hypot(double x, double y) {
    return sqrt(x * x + y * y);
  }

  public static native double log(double x) /*-{
    return Math.log(x);
  }-*/;

  public static native double log10(double x) /*-{
    return Math.log(x) * Math.LOG10E;
  }-*/;

  public static double log1p(double x) {
    return Math.log(x + 1.0d);
  }

  public static double max(double x, double y) {
    return x > y ? x : y;
  }

  public static float max(float x, float y) {
    return x > y ? x : y;
  }

  public static int max(int x, int y) {
    return x > y ? x : y;
  }

  public static long max(long x, long y) {
    return x > y ? x : y;
  }

  public static double min(double x, double y) {
    return x < y ? x : y;
  }

  public static float min(float x, float y) {
    return x < y ? x : y;
  }

  public static int min(int x, int y) {
    return x < y ? x : y;
  }

  public static long min(long x, long y) {
    return x < y ? x : y;
  }

  public static native double pow(double x, double exp) /*-{
    return Math.pow(x, exp);
  }-*/;

  public static native double random() /*-{
    return Math.random();
  }-*/;

  public static double rint(double d) {
    if (Double.isNaN(d)) {
      return d;
    } else if (Double.isInfinite(d)) {
      return d;
    } else if (d == 0.0d) {
      return d;
    } else {
      return round(d);
    }
  }

  public static long round(double x) {
    return (long) round0(x);
  }

  public static native int round(float x) /*-{
    return Math.round(x);
  }-*/;

  public static double scalb(double d, int scaleFactor) {
    if (scaleFactor > 0) {
      return d * (1 << scaleFactor);
    } else if (scaleFactor == 0) {
      return d;
    } else {
      return d * 1.0d / (1 << -scaleFactor);
    }
  }

  public static float scalb(float f, int scaleFactor) {
    if (scaleFactor > 0) {
      return f * (1 << scaleFactor);
    } else if (scaleFactor == 0) {
      return f;
    } else {
      return f * 1.0f / (1 << -scaleFactor);
    }
  }

  public static double signum(double d) {
    if (d > 0.0d) {
      return 1.0d;
    } else if (d < 0.0d) {
      return -1.0d;
    } else {
      return 0.0d;
    }
  }

  public static float signum(float f) {
    if (f > 0.0f) {
      return 1.0f;
    } else if (f < 0.0f) {
      return -1.0f;
    } else {
      return 0.0f;
    }
  }

  public static native double sin(double x) /*-{
    return Math.sin(x);
  }-*/;

  public static native double sinh(double x) /*-{
    return (Math.exp(x) - Math.exp(-x)) / 2.0;
  }-*/;

  public static native double sqrt(double x) /*-{
    return Math.sqrt(x);
  }-*/;

  public static native double tan(double x) /*-{
    return Math.tan(x);
  }-*/;

  public static native double tanh(double x) /*-{
    if (x == Infinity) {
      return 1.0;
    }
    var e2x = Math.exp(2.0 * x);
    return (e2x - 1) / (e2x + 1);
  }-*/;

  public static double toDegrees(double x) {
    return x * PI_UNDER_180;
  }

  public static double toRadians(double x) {
    return x * PI_OVER_180;
  }

  private static native double round0(double x) /*-{
    return Math.round(x);
  }-*/;
}
