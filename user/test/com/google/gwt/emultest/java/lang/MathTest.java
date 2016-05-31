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
 * TODO: more tests
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
    assertEquals(expected, actual, 0.);
  }

  private static boolean isNegativeZero(double x) {
    return Double.doubleToLongBits(-0.0) == Double.doubleToLongBits(x);
  }

  private static double makeNegativeZero() {
    return -0.0;
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testAbs() {
    double v = Math.abs(-1.0);
    double negativeZero = makeNegativeZero();
    assertTrue(isNegativeZero(negativeZero));
    assertEquals(1.0, v);
    v = Math.abs(1.0);
    assertEquals(1.0, v);
    v = Math.abs(negativeZero);
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

  public void testCbrt() {
    double v = Math.cbrt(1000.0);
    assertEquals(10.0, v, 1e-7);
  }

  public void testCopySign() {
    double negativeZero = makeNegativeZero();

    assertEquals(3.0, Math.copySign(3.0, 2.0));
    assertEquals(3.0, Math.copySign(-3.0, 2.0));
    assertEquals(-3.0, Math.copySign(3.0, -2.0));
    assertEquals(-3.0, Math.copySign(-3.0, -2.0));

    assertEquals(2.0, Math.copySign(2.0, 0.0));
    assertEquals(2.0, Math.copySign(-2.0, 0.0));
    assertEquals(-2.0, Math.copySign(2.0, negativeZero));
    assertEquals(-2.0, Math.copySign(-2.0, negativeZero));
    assertEquals(-2.0, Math.copySign(-2.0, Double.NEGATIVE_INFINITY));
    assertEquals(2.0, Math.copySign(-2.0, Double.POSITIVE_INFINITY));
    assertEquals(2.0, Math.copySign(-2.0, Double.NaN));

    assertPositiveZero(Math.copySign(0.0, 4.0));
    assertPositiveZero(Math.copySign(negativeZero, 4.0));
    assertNegativeZero(Math.copySign(0.0, -4.0));
    assertNegativeZero(Math.copySign(negativeZero, -4.0));

    assertPositiveZero(Math.copySign(0.0, 0.0));
    assertPositiveZero(Math.copySign(negativeZero, 0.0));
    assertNegativeZero(Math.copySign(0.0, negativeZero));
    assertNegativeZero(Math.copySign(negativeZero, negativeZero));

    assertEquals(Double.POSITIVE_INFINITY, Math.copySign(Double.POSITIVE_INFINITY, 1));
    assertEquals(Double.NEGATIVE_INFINITY, Math.copySign(Double.POSITIVE_INFINITY, -1));
    assertEquals(Double.POSITIVE_INFINITY, Math.copySign(Double.NEGATIVE_INFINITY, 1));
    assertEquals(Double.NEGATIVE_INFINITY, Math.copySign(Double.NEGATIVE_INFINITY, -1));

    assertEquals(Double.NaN, Math.copySign(Double.NaN, 1), 0);
    assertEquals(Double.NaN, Math.copySign(Double.NaN, -1), 0);
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

  public void testExpm1() {
    assertNegativeZero(Math.expm1(-0.));
    assertPositiveZero(Math.expm1(0.));
    assertNaN(Math.expm1(Double.NaN));
    assertEquals(Double.POSITIVE_INFINITY, Math.expm1(Double.POSITIVE_INFINITY));
    assertEquals(-1., Math.expm1(Double.NEGATIVE_INFINITY));
    assertEquals(-0.632, Math.expm1(-1), 0.001);
    assertEquals(1.718, Math.expm1(1), 0.001);
  }

  public void testFloor() {
    double v = Math.floor(0.5);
    assertEquals(0, v, 0);
    v = Math.floor(Double.POSITIVE_INFINITY);
    assertEquals(Double.POSITIVE_INFINITY, v, 0);
    v = Math.floor(Double.NEGATIVE_INFINITY);
    assertEquals(Double.NEGATIVE_INFINITY, v, 0);
    v = Math.floor(Double.NaN);
    assertEquals(Double.NaN, v, 0);

    v = Math.floor(Double.MAX_VALUE);
    assertEquals(Double.MAX_VALUE, v, 0);
    v = Math.floor(-Double.MAX_VALUE);
    assertEquals(-Double.MAX_VALUE, v, 0);
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
    double v = Math.log(Math.E);
    assertEquals(1.0, v, 1e-15);
  }

  public void testLog10() {
    double v = Math.log10(1000.0);
    assertEquals(3.0, v, 1e-15);
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
        0, 0,
        0.5, 0,
        0.75, 1,
        1.5, 2,
        1.75, 2,
        -0, -0,
        -0.5, -0,
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
        Double.MIN_VALUE, 0,
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
    assertTrue(isNegativeZero(Math.signum(-0.)));
    assertEquals(0., Math.signum(0.), 0);
    assertEquals(-1, Math.signum(-2), 0);
    assertEquals(1, Math.signum(2), 0);
    assertEquals(-1., Math.signum(-Double.MAX_VALUE), 0);
    assertEquals(1., Math.signum(Double.MAX_VALUE), 0);
    assertEquals(-1., Math.signum(Double.NEGATIVE_INFINITY), 0);
    assertEquals(1., Math.signum(Double.POSITIVE_INFINITY), 0);
  }

  public void testSin() {
    double v = Math.sin(0.0);
    assertEquals(0.0, v, 1e-7);
    v = Math.sin(-0.0);
    assertEquals(-0.0, v, 1e-7);
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
    assertEquals(0.0, v);
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
    v = Math.sinh(-0.0);
    assertEquals(-0.0, v);
  }

  public void testTan() {
    double v = Math.tan(0.0);
    assertEquals(0.0, v, 1e-7);
    v = Math.tan(-0.0);
    assertEquals(-0.0, v, 1e-7);
    v = Math.tan(Double.NaN);
    assertNaN(v);
    v = Math.tan(Double.NEGATIVE_INFINITY);
    assertNaN(v);
    v = Math.tan(Double.POSITIVE_INFINITY);
    assertNaN(v);
  }

  public void testTanh() {
    double v = Math.tanh(0.0);
    assertEquals(0.0, v);
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
    v = Math.tanh(-0.0);
    assertEquals(-0.0, v);
  }

  public void testScalb() {
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
