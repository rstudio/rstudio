/*
 * Copyright 2013 Google Inc.
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Tests Java 7 features. It is super sourced so that gwt can be compiles under Java 6.
 *
 * IMPORTANT: For each test here there must exist the corresponding method in the non super sourced
 * version.
 *
 * Eventually this test will graduate and not be super sourced.
 */
public class Java7Test extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.Java7Test";
  }

  // new style class literals
// CHECKSTYLE:OFF
  int million = 1_000_000;

  int five = 0b101;
// CHECKSTYLE:ON

  public void testNewStyleLiterals() {
    assertEquals(1000000, million);
    assertEquals(5, five);
  }

  public void testSwitchOnString() {

    String s = "AA";
    int result = -1;
    switch (s) {
      case "BB":
        result = 0;
        break;
      case "CC":
      case "AA":
        result = 1;
        break;
    }
    assertEquals(1, result);
  }
}
