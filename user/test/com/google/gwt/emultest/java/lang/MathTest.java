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
    assertEquals(0.0, v);
    assertFalse(isNegativeZero(v));
    v = Math.abs(0.0);
    assertEquals(0.0, v);
    v = Math.abs(Double.NEGATIVE_INFINITY);
    assertEquals(Double.POSITIVE_INFINITY, v);
    v = Math.abs(Double.POSITIVE_INFINITY);
    assertEquals(Double.POSITIVE_INFINITY, v);
    v = Math.abs(Double.NaN);
    assertTrue(Double.isNaN(v));
  }

  public void testCbrt() {
    double v = Math.cbrt(1000.0);
    assertEquals(10.0, v, 1e-7);
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
    assertTrue(Double.isNaN(v));
    v = Math.cos(Double.NEGATIVE_INFINITY);
    assertTrue(Double.isNaN(v));
    v = Math.cos(Double.POSITIVE_INFINITY);
    assertTrue(Double.isNaN(v));
  }

  public void testCosh() {
    double v = Math.cosh(0.0);
    assertEquals(1.0, v, 1e-7);
    v = Math.cosh(1.0);
    assertEquals(1.5430806348, v, 1e-7);
    v = Math.cosh(-1.0);
    assertEquals(1.5430806348, v, 1e-7);
    v = Math.cosh(Double.NaN);
    assertTrue(Double.isNaN(v));
    v = Math.cosh(Double.NEGATIVE_INFINITY);
    assertEquals(Double.POSITIVE_INFINITY, v);
    v = Math.cosh(Double.POSITIVE_INFINITY);
    assertEquals(Double.POSITIVE_INFINITY, v);
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
    assertTrue(Double.isNaN(Math.max(Double.NaN, 1d)));
    assertTrue(Double.isNaN(Math.max(1d, Double.NaN)));
    assertTrue(Double.isNaN(Math.max(Double.NaN, Double.POSITIVE_INFINITY)));
    assertTrue(Double.isNaN(Math.max(Double.POSITIVE_INFINITY, Double.NaN)));
    assertTrue(Double.isNaN(Math.max(Double.NaN, Double.NEGATIVE_INFINITY)));
    assertTrue(Double.isNaN(Math.max(Double.NEGATIVE_INFINITY, Double.NaN)));

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
    assertTrue(Double.isNaN(Math.min(Double.NaN, 1d)));
    assertTrue(Double.isNaN(Math.min(1d, Double.NaN)));
    assertTrue(Double.isNaN(Math.min(Double.NaN, Double.POSITIVE_INFINITY)));
    assertTrue(Double.isNaN(Math.min(Double.POSITIVE_INFINITY, Double.NaN)));
    assertTrue(Double.isNaN(Math.min(Double.NaN, Double.NEGATIVE_INFINITY)));
    assertTrue(Double.isNaN(Math.min(Double.NEGATIVE_INFINITY, Double.NaN)));

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
    assertTrue(Double.isNaN(v));
    v = Math.sin(Double.NEGATIVE_INFINITY);
    assertTrue(Double.isNaN(v));
    v = Math.sin(Double.POSITIVE_INFINITY);
    assertTrue(Double.isNaN(v));
  }

  public void testSinh() {
    double v = Math.sinh(0.0);
    assertEquals(0.0, v);
    v = Math.sinh(1.0);
    assertEquals(1.175201193, v, 1e-7);
    v = Math.sinh(-1.0);
    assertEquals(-1.175201193, v, 1e-7);
    v = Math.sinh(Double.NaN);
    assertTrue(Double.isNaN(v));
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
    assertTrue(Double.isNaN(v));
    v = Math.tan(Double.NEGATIVE_INFINITY);
    assertTrue(Double.isNaN(v));
    v = Math.tan(Double.POSITIVE_INFINITY);
    assertTrue(Double.isNaN(v));
  }

  public void testTanh() {
    double v = Math.tanh(0.0);
    assertEquals(0.0, v);
    v = Math.tanh(1.0);
    assertEquals(0.761594155, v, 1e-7);
    v = Math.tanh(-1.0);
    assertEquals(-0.761594155, v, 1e-7);
    v = Math.tanh(Double.NaN);
    assertTrue(Double.isNaN(v));
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
