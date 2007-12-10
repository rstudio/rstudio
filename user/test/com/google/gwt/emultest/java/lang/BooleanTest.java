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

  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testStatics() {
    // test the "new" 1.5 statics, for now assuming "old" rest works
    assertEquals(true, Boolean.valueOf(true).booleanValue());
    assertEquals(false, Boolean.valueOf(false).booleanValue());
    assertEquals(true, Boolean.valueOf("true").booleanValue());
    assertEquals(true, Boolean.valueOf("tRuE").booleanValue());
    assertEquals(false, Boolean.valueOf(null).booleanValue());
    assertEquals(false, Boolean.valueOf("yes").booleanValue());
    assertEquals(false, Boolean.valueOf("").booleanValue());
  }
}
