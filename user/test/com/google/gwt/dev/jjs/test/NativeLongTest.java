// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.test;

import com.google.gwt.junit.client.GWTTestCase;

public class NativeLongTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testConstants() {
    assertEquals(0x5DEECE66DL, 0x5DEECE66DL);
    assertTrue(0x5DEECE66DL > 0L);
    assertTrue(0L < 0x5DEECE66DL);
    assertEquals(0xBL, 0xBL);
    assertTrue(0xBL > 0L);
    assertTrue(0L < 0xBL);
  }

  public void testLogicalAnd() {
    assertEquals(1L & 0L, 0L);
    assertEquals(1L & 3L, 1L);
  }

  public void testShift() {
    assertEquals(0x5DEECE66DL, 0x5DEECE66DL & ((1L << 48) - 1));
  }

  public void testTripleShift() {
    assertEquals(1 << 12, (1 << 60) >>> (48));
  }

  public void testXor() {
    assertTrue((255L ^ 0x5DEECE66DL) != 0);
  }

}
