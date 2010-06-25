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

  private static native boolean isNegativeZero(double x) /*-{
    var v = 1 / x;
    return v == Number.NEGATIVE_INFINITY;
  }-*/;
  
  private static native double makeNegativeZero() /*-{
    return 1 / Number.NEGATIVE_INFINITY;
  }-*/;
  
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
}
