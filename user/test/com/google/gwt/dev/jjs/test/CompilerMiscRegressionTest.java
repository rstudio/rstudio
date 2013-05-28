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

/**
 * Tests Miscelaneous fixes.
 */
public class CompilerMiscRegressionTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  native double toNumber(String value) /*-{
    return +value;
  }-*/;

  native double addAndConvert(double v1, String v2) /*-{
    return v1 + +v2;
  }-*/;

  native double minusAndDecrement(double val) /*-{
    var lhs = val;
    return - --lhs;
  }-*/;

  /**
   * Test for issues 6373 and 3942.
   */
  public void testUnaryPlus() {
    // With the unary + operator stripped the first assertion only fails in
    // dev mode, in web mode the  comparison made by assertEquals masks
    // the error; whereas the second fails in both dev and web modes.
    assertEquals(11.0, toNumber("11"));
    assertEquals(12.0, toNumber("10") + toNumber("2"));
    assertEquals(12.0, addAndConvert(10, "2"));
    assertEquals(-10.0, minusAndDecrement(11));
  }
}
