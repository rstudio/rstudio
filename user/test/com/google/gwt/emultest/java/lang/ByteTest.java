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
package com.google.gwt.emultest.java.lang;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests for the JRE Byte type.
 */
public class ByteTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testConstants() {
    assertEquals(-128, Byte.MIN_VALUE);
    assertEquals(127, Byte.MAX_VALUE);
    assertEquals(8, Byte.SIZE);
  }

  public void testStatics() {
    // test the new 1.5 statics... older stuff "assumed to work"
    assertEquals(0, Byte.valueOf((byte) 0).intValue());
    assertEquals(127, Byte.valueOf((byte) 127).intValue());
    assertEquals(-128, Byte.valueOf((byte) -128).intValue());
    assertEquals(-1, Byte.valueOf((byte) 255).intValue());
  }
}
