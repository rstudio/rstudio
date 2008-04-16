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
 * Tests for the JRE Boolean type.
 */
public class BooleanTest extends GWTTestCase {

  static volatile boolean bfalse = false;
  static volatile boolean btrue = true;

  static volatile String false1 = "t";
  static volatile String false2 = "1";
  static volatile String false3 = "false";
  static volatile String false4 = null;
  static volatile String true1 = "true";
  static volatile String true2 = "TRUE";
  static volatile String true3 = "TrUe";

  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testCtor() {
    assertTrue(new Boolean(btrue));
    assertTrue(new Boolean(true1));
    assertTrue(new Boolean(true2));
    assertTrue(new Boolean(true3));
    assertFalse(new Boolean(bfalse));
    assertFalse(new Boolean(false1));
    assertFalse(new Boolean(false2));
    assertFalse(new Boolean(false3));
    assertFalse(new Boolean(false4));
  }

  public void testParseBoolean() {
    assertTrue(Boolean.parseBoolean(true1));
    assertTrue(Boolean.parseBoolean(true2));
    assertTrue(Boolean.parseBoolean(true3));
    assertFalse(Boolean.parseBoolean(false1));
    assertFalse(Boolean.parseBoolean(false2));
    assertFalse(Boolean.parseBoolean(false3));
    assertFalse(Boolean.parseBoolean(false4));
  }

  public void testValueOf() {
    assertTrue(Boolean.valueOf(btrue));
    assertTrue(Boolean.valueOf(true1));
    assertTrue(Boolean.valueOf(true2));
    assertTrue(Boolean.valueOf(true3));
    assertFalse(Boolean.valueOf(bfalse));
    assertFalse(Boolean.valueOf(false1));
    assertFalse(Boolean.valueOf(false2));
    assertFalse(Boolean.valueOf(false3));
    assertFalse(Boolean.valueOf(false4));
  }

}
