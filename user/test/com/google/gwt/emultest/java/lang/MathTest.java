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

package com.google.gwt.emultest.java.lang;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests for JRE emulation of java.lang.Math.
 *
 */
public class MathTest extends GWTTestCase {

  private static void assertNegativeZero(double x) {
    assertTrue(isNegativeZero(x));
  }

  private static void assertPositiveZero(double x) {
    assertEquals(0.0, x);
    assertFalse(isNegativeZero(x));
  }

  private static void assertNaN(double x) {
    assertTrue(Double.isNaN(x));
  }

  private static void assertEquals(double expected, double actual) {
    assertEquals(expected, actual, 0.0);
  }

  private static boolean isNegativeZero(double x) {
    return Double.doubleToLongBits(-0.0) == Double.doubleToLongBits(x);
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    // Ensure -0.0 vs 0.0 behavior
    assertPositiveZero(0.0);
    assertNegativeZero(-0.0);
    assertFalse(isNegativeZero(0.0));
  }

  public void testAbs() {
    double v = Math.abs(-1.0);
    assertEquals(1.0, v);
    v = Math.abs(1.0);
    assertEquals(1.0, v);
    v = Math.abs(-0.0);
    assertPositiveZero(v);
    v = Math.abs(0.0);
    assertPositiveZero(v);
    v = Math.abs(Double.NEGATIVE_INFINITY);
    assertEquals(Double.POSITIVE_INFINITY, v);
    v = Math.abs(Double.POSITIVE_INFINITY);
    assertEquals(Double.POSITIVE_INFINITY, v);
    v = Math.abs(Double.NaN);
    assertNaN(v);
  }

  public void testAsin() {
    assertNaN(Math.asin(Double.NaN));
    assertNaN(Math.asin(1.1));
    assertNaN(Math.asin(Double.NEGATIVE_INFINITY));
    assertNaN(Math.asin(Double.POSITIVE_INFINITY));
    assertPositiveZero(Math.asin(0.0));
    assertNegativeZero(Math.asin(-0.0));

    assertEquals(0.0, Math.asin(0));
    assertEquals(1.570796326, Math.asin(1), 1e-7);
  }

  public void testAcos() {
    assertNaN(Math.acos(Double.NaN));
    assertNaN(Math.acos(1.1));
    assertNaN(Math.acos(Double.NEGATIVE_INFINITY));
    assertNaN(Math.acos(Double.POSITIVE_INFINITY));

    assertEquals(0.0, Math.acos(1));
    assertEquals(1.570796326, Math.acos(0), 1e-7);
  }

  public void testAtan() {
    assertNaN(Math.atan(Double.NaN));
    assertPositiveZero(Math.atan(0.0));
    assertNegativeZero(Math.atan(-0.0));
    assertEquals(-1.570796326, Math.atan(Double.NEGATIVE_INFINITY), 1e-7);
    assertEquals(1.570796326, Math.atan(Double.POSITIVE_INFINITY), 1e-7);
    assertEquals(0.785398163, Math.atan(1), 1e-7);
  }

  public void testAtan2() {
    assertNaN(Math.atan2(Double.NaN, 1));
    assertNaN(Math.atan2(1, Double.NaN));
    assertNaN(Math.atan2(Double.NaN, Double.NaN));
    assertPositiveZero(Math.atan2(0.0, 1.0));
    assertPositiveZero(Math.atan2(1.0, Double.POSITIVE_INFINITY));
    assertNegativeZero(Math.atan2(-0.0, 1.0));
    assertNegativeZero(Math.atan2(-1.0, Double.POSITIVE_INFINITY));
    assertEquals(Math.PI, Math.atan2(0.0, -1.0), 1e-7);
    assertEquals(Math.PI, Math.atan2(1.0, Double.NEGATIVE_INFINITY), 1e-7);
    assertEquals(-Math.PI, Math.atan2(-0.0, -1.0), 1e-7);
    assertEquals(-Math.PI, Math.atan2(-1.0, Double.NEGATIVE_INFINITY), 1e-7);
    assertEquals(Math.PI / 2, Math.atan2(1.0, 0.0), 1e-7);
    assertEquals(Math.PI / 2, Math.atan2(1.0, -0.0), 1e-7);
    assertEquals(Math.PI / 2, Math.atan2(Double.POSITIVE_INFINITY, 1.0), 1e-7);
    assertEquals(-Math.PI / 2, Math.atan2(-1.0, 0.0), 1e-7);
    assertEquals(-Math.PI / 2, Math.atan2(-1.0, -0.0), 1e-7);
    assertEquals(-Math.PI / 2, Math.atan2(Double.NEGATIVE_INFINITY, 1.0), 1e-7);
    assertEquals(Math.PI / 4, Math.atan2(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY), 1e-7);
    assertEquals(Math.PI * 3 / 4,
        Math.atan2(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY), 1e-7);
    assertEquals(-Math.PI / 4,
        Math.atan2(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), 1e-7);
    assertEquals(-3 * Math.PI / 4,
        Math.atan2(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY), 1e-7);

    assertEquals(0.463647609, Math.atan2(1, 2), 1e-7);
  }

  public void testCbrt() {
    assertNaN(Math.cbrt(Double.NaN));
    assertEquals(Double.POSITIVE_INFINITY, Math.cbrt(Double.POSITIVE_INFINITY));
    assertEquals(Double.NEGATIVE_INFINITY, Math.cbrt(Double.NEGATIVE_INFINITY));
    assertPositiveZero(Math.cbrt(0.0));
    assertNegativeZero(Math.cbrt(-0.0));

    double v = Math.cbrt(1000.0);
    assertEquals(10.0, v, 1e-7);
  }

  public void testCeil() {
    assertNaN(Math.ceil(Double.NaN));
    assertEquals(Double.POSITIVE_INFINITY, Math.ceil(Double.POSITIVE_INFINITY));
    assertEquals(Double.NEGATIVE_INFINITY, Math.ceil(Double.NEGATIVE_INFINITY));
    assertPositiveZero(Math.ceil(0.0));
    assertNegativeZero(Math.ceil(-0.0));

    assertEquals(1.0, Math.ceil(0.5));
    assertNegativeZero(Math.ceil(-0.5));
  }

  public void testCopySign() {
    assertEquals(3.0, Math.copySign(3.0, 2.0));
    assertEquals(3.0, Math.copySign(-3.0, 2.0));
    assertEquals(-3.0, Math.copySign(3.0, -2.0));
    assertEquals(-3.0, Math.copySign(-3.0, -2.0));

    assertEquals(2.0, Math.copySign(2.0, 0.0));
    assertEquals(2.0, Math.copySign(-2.0, 0.0));
    assertEquals(-2.0, Math.copySign(2.0, -0.0));
    assertEquals(-2.0, Math.copySign(-2.0, -0.0));
    assertEquals(-2.0, Math.copySign(-2.0, Double.NEGATIVE_INFINITY));
    assertEquals(2.0, Math.copySign(-2.0, Double.POSITIVE_INFINITY));
    assertEquals(2.0, Math.copySign(-2.0, Double.NaN));

    assertPositiveZero(Math.copySign(0.0, 4.0));
    assertPositiveZero(Math.copySign(-0.0, 4.0));
    assertNegativeZero(Math.copySign(0.0, -4.0));
    assertNegativeZero(Math.copySign(-0.0, -4.0));

    assertPositiveZero(Math.copySign(0.0, 0.0));
    assertPositiveZero(Math.copySign(-0.0, 0.0));
    assertNegativeZero(Math.copySign(0.0, -0.0));
    assertNegativeZero(Math.copySign(-0.0, -0.0));

    assertEquals(Double.POSITIVE_INFINITY, Math.copySign(Double.POSITIVE_INFINITY, 1));
    assertEquals(Double.NEGATIVE_INFINITY, Math.copySign(Double.POSITIVE_INFINITY, -1));
    assertEquals(Double.POSITIVE_INFINITY, Math.copySign(Double.NEGATIVE_INFINITY, 1));
    assertEquals(Double.NEGATIVE_INFINITY, Math.copySign(Double.NEGATIVE_INFINITY, -1));

    assertNaN(Math.copySign(Double.NaN, 1));
    assertNaN(Math.copySign(Double.NaN, -1));
  }

  public void testCos() {
    double v = Math.cos(0.0);
    assertEquals(1.0, v, 1e-7);
    v = Math.cos(-0.0);
    assertEquals(1.0, v, 1e-7);
    v = Math.cos(Math.PI * .5);
    assertEquals(0.0, v, 1e-7);
    v = Math.cos(Math.PI);
    assertEquals(-1.0, v, 1e-7);
    v = Math.cos(Math.PI * 1.5);
    assertEquals(0.0, v, 1e-7);
    v = Math.cos(Double.NaN);
    assertNaN(v);
    v = Math.cos(Double.NEGATIVE_INFINITY);
    assertNaN(v);
    v = Math.cos(Double.POSITIVE_INFINITY);
    assertNaN(v);
  }

  public void testCosh() {
    double v = Math.cosh(0.0);
    assertEquals(1.0, v, 1e-7);
    v = Math.cosh(1.0);
    assertEquals(1.5430806348, v, 1e-7);
    v = Math.cosh(-1.0);
    assertEquals(1.5430806348, v, 1e-7);
    v = Math.cosh(Double.NaN);
    assertNaN(v);
    v = Math.cosh(Double.NEGATIVE_INFINITY);
    assertEquals(Double.POSITIVE_INFINITY, v);
    v = Math.cosh(Double.POSITIVE_INFINITY);
    assertEquals(Double.POSITIVE_INFINITY, v);
  }

  public void testExp() {
    assertNaN(Math.exp(Double.NaN));
    assertEquals(Double.POSITIVE_INFINITY, Math.exp(Double.POSITIVE_INFINITY));
    assertPositiveZero(Math.exp(Double.NEGATIVE_INFINITY));
    assertEquals(1, Math.exp(0));
    assertEquals(2.718281, Math.exp(1), 0.000001);
  }

  public void testExpm1() {
    assertNegativeZero(Math.expm1(-0.0));
    assertPositiveZero(Math.expm1(0.0));
    assertNaN(Math.expm1(Double.NaN));
    assertEquals(Double.POSITIVE_INFINITY, Math.expm1(Double.POSITIVE_INFINITY));
    assertEquals(-1.0, Math.expm1(Double.NEGATIVE_INFINITY));
    assertEquals(-0.632, Math.expm1(-1), 0.001);
    assertEquals(1.718, Math.expm1(1), 0.001);
  }

  public void testFloor() {
    double v = Math.floor(0.5);
    assertEquals(0, v, 0);
    v = Math.floor(Double.POSITIVE_INFINITY);
    assertEquals(Double.POSITIVE_INFINITY, v);
    v = Math.floor(Double.NEGATIVE_INFINITY);
    assertEquals(Double.NEGATIVE_INFINITY, v);
    v = Math.floor(Double.NaN);
    assertNaN(v);
    assertPositiveZero(Math.floor(0.0));
    assertNegativeZero(Math.floor(-0.0));

    v = Math.floor(Double.MAX_VALUE);
    assertEquals(Double.MAX_VALUE, v, 0);
    v = Math.floor(-Double.MAX_VALUE);
    assertEquals(-Double.MAX_VALUE, v, 0);
  }

  public void testHypot() {
    assertEquals(Double.POSITIVE_INFINITY,
        Math.hypot(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
    assertEquals(Double.POSITIVE_INFINITY,
        Math.hypot(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY));
    assertEquals(Double.POSITIVE_INFINITY,
        Math.hypot(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
    assertEquals(Double.POSITIVE_INFINITY,
        Math.hypot(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
    assertEquals(Double.POSITIVE_INFINITY,
        Math.hypot(0, Double.POSITIVE_INFINITY));
    assertEquals(Double.POSITIVE_INFINITY,
        Math.hypot(0, Double.NEGATIVE_INFINITY));
    assertEquals(Double.POSITIVE_INFINITY,
        Math.hypot(Double.POSITIVE_INFINITY, 0));
    assertEquals(Double.POSITIVE_INFINITY,
        Math.hypot(Double.NEGATIVE_INFINITY, 0));
    assertEquals(Double.POSITIVE_INFINITY,
        Math.hypot(Double.NaN, Double.POSITIVE_INFINITY));
    assertEquals(Double.POSITIVE_INFINITY,
        Math.hypot(Double.NaN, Double.NEGATIVE_INFINITY));
    assertEquals(Double.POSITIVE_INFINITY,
        Math.hypot(Double.POSITIVE_INFINITY, Double.NaN));
    assertEquals(Double.POSITIVE_INFINITY,
        Math.hypot(Double.NEGATIVE_INFINITY, Double.NaN));
    assertNaN(Math.hypot(Double.NaN, 0));
    assertNaN(Math.hypot(0, Double.NaN));

    assertEquals(1.414213562, Math.hypot(1, 1), 1e-7);
    assertEquals(5, Math.hypot(3, 4));
  }

  public void testMax() {
    assertEquals(2d, Math.max(1d, 2d));
    assertEquals(2d, Math.max(2d, 1d));
    assertEquals(0d, Math.max(-0d, 0d));
    assertEquals(0d, Math.max(0d, -0d));
    assertEquals(1d, Math.max(-1d, 1d));
    assertEquals(1d, Math.max(1d, -1d));
    assertEquals(-1d, Math.max(-1d, -2d));
    assertEquals(-1d, Math.max(-2d, -1d));
    assertNaN(Math.max(Double.NaN, 1d));
    assertNaN(Math.max(1d, Double.NaN));
    assertNaN(Math.max(Double.NaN, Double.POSITIVE_INFINITY));
    assertNaN(Math.max(Double.POSITIVE_INFINITY, Double.NaN));
    assertNaN(Math.max(Double.NaN, Double.NEGATIVE_INFINITY));
    assertNaN(Math.max(Double.NEGATIVE_INFINITY, Double.NaN));

    assertEquals(2f, Math.max(1f, 2f));
    assertEquals(2f, Math.max(2f, 1f));
    assertEquals(0f, Math.max(-0f, 0f));
    assertEquals(0f, Math.max(0f, -0f));
    assertEquals(1f, Math.max(-1f, 1f));
    assertEquals(1f, Math.max(1f, -1f));
    assertEquals(-1f, Math.max(-1f, -2f));
    assertEquals(-1f, Math.max(-2f, -1f));
    assertTrue(Float.isNaN(Math.max(Float.NaN, 1f)));
    assertTrue(Float.isNaN(Math.max(1f, Float.NaN)));
    assertTrue(Float.isNaN(Math.max(Float.NaN, Float.POSITIVE_INFINITY)));
    assertTrue(Float.isNaN(Math.max(Float.POSITIVE_INFINITY, Float.NaN)));
    assertTrue(Float.isNaN(Math.max(Float.NaN, Float.NEGATIVE_INFINITY)));
    assertTrue(Float.isNaN(Math.max(Float.NEGATIVE_INFINITY, Float.NaN)));
  }

  public void testMin() {
    assertEquals(1d, Math.min(1d, 2d));
    assertEquals(1d, Math.min(2d, 1d));
    assertEquals(-0d, Math.min(-0d, 0d));
    assertEquals(-0d, Math.min(0d, -0d));
    assertEquals(-1d, Math.min(-1d, 1d));
    assertEquals(-1d, Math.min(1d, -1d));
    assertEquals(-2d, Math.min(-1d, -2d));
    assertEquals(-2d, Math.min(-2d, -1d));
    assertNaN(Math.min(Double.NaN, 1d));
    assertNaN(Math.min(1d, Double.NaN));
    assertNaN(Math.min(Double.NaN, Double.POSITIVE_INFINITY));
    assertNaN(Math.min(Double.POSITIVE_INFINITY, Double.NaN));
    assertNaN(Math.min(Double.NaN, Double.NEGATIVE_INFINITY));
    assertNaN(Math.min(Double.NEGATIVE_INFINITY, Double.NaN));

    assertEquals(1f, Math.min(1f, 2f));
    assertEquals(1f, Math.min(2f, 1f));
    assertEquals(-0f, Math.min(-0f, 0f));
    assertEquals(-0f, Math.min(0f, -0f));
    assertEquals(-1f, Math.min(-1f, 1f));
    assertEquals(-1f, Math.min(1f, -1f));
    assertEquals(-2f, Math.min(-1f, -2f));
    assertEquals(-2f, Math.min(-2f, -1f));
    assertTrue(Float.isNaN(Math.min(Float.NaN, 1f)));
    assertTrue(Float.isNaN(Math.min(1f, Float.NaN)));
    assertTrue(Float.isNaN(Math.min(Float.NaN, Float.POSITIVE_INFINITY)));
    assertTrue(Float.isNaN(Math.min(Float.POSITIVE_INFINITY, Float.NaN)));
    assertTrue(Float.isNaN(Math.min(Float.NaN, Float.NEGATIVE_INFINITY)));
    assertTrue(Float.isNaN(Math.min(Float.NEGATIVE_INFINITY, Float.NaN)));
  }

  public void testLog() {
    assertNaN(Math.log(Double.NaN));
    assertNaN(Math.log(Double.NEGATIVE_INFINITY));
    assertNaN(Math.log(-1));
    assertEquals(Double.POSITIVE_INFINITY, Math.log(Double.POSITIVE_INFINITY));
    assertEquals(Double.NEGATIVE_INFINITY, Math.log(0.0));
    assertEquals(Double.NEGATIVE_INFINITY, Math.log(-0.0));

    double v = Math.log(Math.E);
    assertEquals(1.0, v, 1e-15);
  }

  public void testLog10() {
    assertNaN(Math.log10(Double.NaN));
    assertNaN(Math.log10(Double.NEGATIVE_INFINITY));
    assertNaN(Math.log10(-1));
    assertEquals(Double.POSITIVE_INFINITY, Math.log10(Double.POSITIVE_INFINITY));
    assertEquals(Double.NEGATIVE_INFINITY, Math.log10(0.0));
    assertEquals(Double.NEGATIVE_INFINITY, Math.log10(-0.0));

    double v = Math.log10(1000.0);
    assertEquals(3.0, v, 1e-15);
  }

  public void testLog1p() {
    assertNaN(Math.log1p(Double.NaN));
    assertNaN(Math.log1p(-2));
    assertNaN(Math.log1p(Double.NEGATIVE_INFINITY));
    assertEquals(Double.POSITIVE_INFINITY, Math.log1p(Double.POSITIVE_INFINITY));
    assertEquals(Double.NEGATIVE_INFINITY, Math.log1p(-1));
    assertPositiveZero(Math.log1p(0.0));
    assertNegativeZero(Math.log1p(-0.0));

    assertEquals(-0.693147180, Math.log1p(-0.5), 1e-7);
    assertEquals(1.313261687, Math.log1p(Math.E), 1e-7);
  }

  public void testPow() {
    assertEquals(1, Math.pow(2, 0.0));
    assertEquals(1, Math.pow(2, -0.0));
    assertEquals(2, Math.pow(2, 1));
    assertEquals(-2, Math.pow(-2, 1));
    assertNaN(Math.pow(1, Double.NaN));
    assertNaN(Math.pow(Double.NaN, Double.NaN));
    assertNaN(Math.pow(Double.NaN, 1));
    assertEquals(1, Math.pow(Double.NaN, 0.0));
    assertEquals(1, Math.pow(Double.NaN, -0.0));
    assertEquals(Double.POSITIVE_INFINITY, Math.pow(1.1, Double.POSITIVE_INFINITY));
    assertEquals(Double.POSITIVE_INFINITY, Math.pow(-1.1, Double.POSITIVE_INFINITY));
    assertEquals(Double.POSITIVE_INFINITY, Math.pow(0.9, Double.NEGATIVE_INFINITY));
    assertEquals(Double.POSITIVE_INFINITY, Math.pow(-0.9, Double.NEGATIVE_INFINITY));
    assertPositiveZero(Math.pow(1.1, Double.NEGATIVE_INFINITY));
    assertPositiveZero(Math.pow(-1.1, Double.NEGATIVE_INFINITY));
    assertPositiveZero(Math.pow(0.9, Double.POSITIVE_INFINITY));
    assertPositiveZero(Math.pow(-0.9, Double.POSITIVE_INFINITY));
    assertNaN(Math.pow(1, Double.POSITIVE_INFINITY));
    assertNaN(Math.pow(-1, Double.POSITIVE_INFINITY));
    assertNaN(Math.pow(1, Double.NEGATIVE_INFINITY));
    assertNaN(Math.pow(-1, Double.NEGATIVE_INFINITY));
    assertPositiveZero(Math.pow(0.0, 1));
    assertPositiveZero(Math.pow(Double.POSITIVE_INFINITY, -1));
    assertEquals(Double.POSITIVE_INFINITY, Math.pow(0.0, -1));
    assertEquals(Double.POSITIVE_INFINITY, Math.pow(Double.POSITIVE_INFINITY, 1));
    assertPositiveZero(Math.pow(-0.0, 2));
    assertPositiveZero(Math.pow(Double.NEGATIVE_INFINITY, -2));
    assertNegativeZero(Math.pow(-0.0, 1));
    assertNegativeZero(Math.pow(Double.NEGATIVE_INFINITY, -1));
    assertEquals(Double.POSITIVE_INFINITY, Math.pow(-0.0, -2));
    assertEquals(Double.POSITIVE_INFINITY, Math.pow(Double.NEGATIVE_INFINITY, 2));
    assertEquals(Double.NEGATIVE_INFINITY, Math.pow(-0.0, -1));
    assertEquals(Double.NEGATIVE_INFINITY, Math.pow(Double.NEGATIVE_INFINITY, 1));

    assertEquals(9, Math.pow(3, 2));
  }

  public void testRound_float() {
    assertEquals(1, Math.round(0.5f));
    assertEquals(Integer.MAX_VALUE, Math.round(Float.POSITIVE_INFINITY));
    assertEquals(Integer.MIN_VALUE, Math.round(Float.NEGATIVE_INFINITY));
    assertEquals(0, Math.round(Float.NaN));
  }

  public void testRound() {
    long v = Math.round(0.5);
    assertEquals(1L, v);
    v = Math.round(Double.POSITIVE_INFINITY);
    assertEquals(Long.MAX_VALUE, v);
    v = Math.round(Double.NEGATIVE_INFINITY);
    assertEquals(Long.MIN_VALUE, v);
    v = Math.round(Double.NaN);
    assertEquals(0L, v);

    v = Math.round(Double.MAX_VALUE);
    assertEquals(Long.MAX_VALUE, v);
    v = Math.round(-Double.MAX_VALUE);
    assertEquals(Long.MIN_VALUE, v);
  }

  public void testRint() {
    final double twoTo52 = 1L << 52;
    // format: value to be round and expected value
    final double[] testValues = {
        0.0, 0.0,
        0.5, 0.0,
        0.75, 1,
        1.5, 2,
        1.75, 2,
        -0.0, -0.0,
        -0.5, -0.0,
        -1.25, -1,
        -1.5, -2,
        -2.5, -2,
        twoTo52, twoTo52,
        twoTo52 - 0.25, twoTo52,
        twoTo52 + 0.25, twoTo52,
        twoTo52 + 0.5, twoTo52,
        twoTo52 - 0.5, twoTo52,
        twoTo52 + 0.75, twoTo52 + 1,
        twoTo52 - 0.75, twoTo52 - 1,
        -twoTo52, -twoTo52,
        -twoTo52 + 0.25, -twoTo52,
        -twoTo52 - 0.25, -twoTo52,
        -twoTo52 + 0.5, -twoTo52,
        -twoTo52 - 0.5, -twoTo52,
        -twoTo52 + 0.75, -twoTo52 + 1,
        -twoTo52 - 0.75, -twoTo52 - 1,
        Double.MIN_VALUE, 0.0,
        Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
        Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
        Double.NaN, Double.NaN,
        Double.MAX_VALUE, Double.MAX_VALUE,
        -Double.MAX_VALUE, -Double.MAX_VALUE,
    };
    for (int i = 0; i < testValues.length;) {
      double v = testValues[i++];
      double expected = testValues[i++];
      double actual = Math.rint(v);
      assertEquals("value: " + v + ", expected: " + expected + ", actual: " + actual,
          expected, actual, 0);
    }
  }

  public void testSignum() {
    assertNaN(Math.signum(Double.NaN));
    assertNegativeZero(Math.signum(-0.0));
    assertPositiveZero(Math.signum(0.0));
    assertEquals(-1, Math.signum(-2));
    assertEquals(1, Math.signum(2));
    assertEquals(-1.0, Math.signum(-Double.MAX_VALUE));
    assertEquals(1.0, Math.signum(Double.MAX_VALUE));
    assertEquals(-1.0, Math.signum(Double.NEGATIVE_INFINITY));
    assertEquals(1.0, Math.signum(Double.POSITIVE_INFINITY));
  }

  public void testSin() {
    double v = Math.sin(0.0);
    assertPositiveZero(v);
    v = Math.sin(-0.0);
    assertNegativeZero(v);
    v = Math.sin(Math.PI * .5);
    assertEquals(1.0, v, 1e-7);
    v = Math.sin(Math.PI);
    assertEquals(0.0, v, 1e-7);
    v = Math.sin(Math.PI * 1.5);
    assertEquals(-1.0, v, 1e-7);
    v = Math.sin(Double.NaN);
    assertNaN(v);
    v = Math.sin(Double.NEGATIVE_INFINITY);
    assertNaN(v);
    v = Math.sin(Double.POSITIVE_INFINITY);
    assertNaN(v);
  }

  public void testSinh() {
    double v = Math.sinh(0.0);
    assertPositiveZero(v);
    v = Math.sinh(-0.0);
    assertNegativeZero(v);
    v = Math.sinh(1.0);
    assertEquals(1.175201193, v, 1e-7);
    v = Math.sinh(-1.0);
    assertEquals(-1.175201193, v, 1e-7);
    v = Math.sinh(Double.NaN);
    assertNaN(v);
    v = Math.sinh(Double.NEGATIVE_INFINITY);
    assertEquals(Double.NEGATIVE_INFINITY, v);
    v = Math.sinh(Double.POSITIVE_INFINITY);
    assertEquals(Double.POSITIVE_INFINITY, v);
  }

  public void testSqrt() {
    assertNaN(Math.sqrt(Double.NaN));
    assertNaN(Math.sqrt(Double.NEGATIVE_INFINITY));
    assertNaN(Math.sqrt(-1));
    assertEquals(Double.POSITIVE_INFINITY, Math.sqrt(Double.POSITIVE_INFINITY));
    assertPositiveZero(Math.sqrt(0.0));
    assertNegativeZero(Math.sqrt(-0.0));

    assertEquals(1.732050807, Math.sqrt(3), 1e-7);
  }

  public void testTan() {
    double v = Math.tan(0.0);
    assertPositiveZero(v);
    v = Math.tan(-0.0);
    assertNegativeZero(v);
    v = Math.tan(Double.NaN);
    assertNaN(v);
    v = Math.tan(Double.NEGATIVE_INFINITY);
    assertNaN(v);
    v = Math.tan(Double.POSITIVE_INFINITY);
    assertNaN(v);
  }

  public void testTanh() {
    double v = Math.tanh(0.0);
    assertPositiveZero(v);
    v = Math.tanh(-0.0);
    assertNegativeZero(v);
    v = Math.tanh(1.0);
    assertEquals(0.761594155, v, 1e-7);
    v = Math.tanh(-1.0);
    assertEquals(-0.761594155, v, 1e-7);
    v = Math.tanh(Double.NaN);
    assertNaN(v);
    v = Math.tanh(Double.NEGATIVE_INFINITY);
    assertEquals(-1.0, v, 1e-7);
    v = Math.tanh(Double.POSITIVE_INFINITY);
    assertEquals(1.0, v, 1e-7);
  }

  public void testScalb() {
    for (int scaleFactor = -32; scaleFactor <= 32; scaleFactor++) {
      assertNaN(Math.scalb(Double.NaN, scaleFactor));
      assertEquals(Double.POSITIVE_INFINITY, Math.scalb(Double.POSITIVE_INFINITY, scaleFactor));
      assertEquals(Double.NEGATIVE_INFINITY, Math.scalb(Double.NEGATIVE_INFINITY, scaleFactor));
      assertPositiveZero(Math.scalb(0.0, scaleFactor));
      assertNegativeZero(Math.scalb(-0.0, scaleFactor));
    }

    assertEquals(40.0d, Math.scalb(5d, 3));
    assertEquals(40.0f, Math.scalb(5f, 3));

    assertEquals(64.0d, Math.scalb(64d, 0));
    assertEquals(64.0f, Math.scalb(64f, 0));

    // Cases in which we can't use integer shift (|scaleFactor| >= 31):

    assertEquals(2147483648.0d, Math.scalb(1d, 31));
    assertEquals(4294967296.0d, Math.scalb(1d, 32));
    assertEquals(2.3283064e-10d, Math.scalb(1d, -32), 1e-7d);

    assertEquals(2147483648.0f, Math.scalb(1f, 31));
    assertEquals(4294967296.0f, Math.scalb(1f, 32));
    assertEquals(2.3283064e-10f, Math.scalb(1f, -32), 1e-7f);
  }
}
