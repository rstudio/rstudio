/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.jjs.test;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * TODO: document me.
 */
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
