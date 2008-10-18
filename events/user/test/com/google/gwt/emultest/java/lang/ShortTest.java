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
 * Tests for the JRE Short type.
 */
public class ShortTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testConstants() {
    assertEquals(16, Short.SIZE);
    assertEquals((short) 0x7fff, Short.MAX_VALUE);
    assertEquals((short) 0x8000, Short.MIN_VALUE);
  }

  public void testReverseBytes() {
    assertEquals(0x1122, Short.reverseBytes((short) 0x2211));
    assertEquals(0, Short.reverseBytes((short) 0));
  }

  public void testStaticValueOf() {
    assertEquals(Short.MIN_VALUE, Short.valueOf(Short.MIN_VALUE).shortValue());
    assertEquals(Short.MAX_VALUE, Short.valueOf(Short.MAX_VALUE).shortValue());
  }
}
