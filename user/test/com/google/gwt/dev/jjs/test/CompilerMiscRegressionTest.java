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
  private static float[] copy(float[] src, float[] dest) {
    System.arraycopy(src, 0, dest, 0, Math.min(src.length, dest.length));
    return dest;
  }

  /**
   * Test for issue 6638.
   */
  public void testNewArrayInlining() {
    float[] src = new float[]{1,1,1};
    float[] dest = copy(src, new float[3]);

    assertEqualContents(src, dest);
  }

  private static void assertEqualContents(float[] expected, float[] actual) {

    assertEquals("Array length mismatch", expected.length, actual.length);
    for (int i = 0; i < expected.length; i++) {
      assertEquals("Array mismatch at element " + i , expected[i], actual[i]);
    }
  }
}
