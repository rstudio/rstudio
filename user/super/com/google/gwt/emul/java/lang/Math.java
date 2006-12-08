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
 * Math utility methods and constants.
 */
public final class Math {

  public static final double E = 2.7182818284590452354;
  public static final double PI = 3.14159265358979323846;

  private static final double PI_OVER_180 = PI / 180.0;
  private static final double PI_UNDER_180 = 180.0 / PI;

  public static double abs(double x) {
    return x < 0 ? -x : x;
  }

  public static float abs(float x) {
    return x < 0 ? -x : x;
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

  public static native double ceil(double x) /*-{
    return Math.ceil(x);
  }-*/;

  public static native double cos(double x) /*-{
    return Math.cos(x);
  }-*/;

  public static native double exp(double x) /*-{
    return Math.exp(x);
  }-*/;

  public static native double floor(double x) /*-{
    return Math.floor(x);
  }-*/;

  public static native double log(double x) /*-{
    return Math.log(x);
  }-*/;

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

  public static native long round(double x) /*-{
    return Math.round(x);
  }-*/;

  public static native int round(float x) /*-{
    return Math.round(x);
  }-*/;

  public static native double sin(double x) /*-{
    return Math.sin(x);
  }-*/;

  public static native double sqrt(double x) /*-{
    return Math.sqrt(x);
  }-*/;

  public static native double tan(double x) /*-{
    return Math.tan(x);
  }-*/;

  public static double toDegrees(double x) {
    return x * PI_UNDER_180;
  }

  public static double toRadians(double x) {
    return x * PI_OVER_180;
  }
}
