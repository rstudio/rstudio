/*
 * Copyright 2008 Google Inc.
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

package com.google.gwt.i18n.client;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * GWT JUnit tests must extend GWTTestCase.
 */
public class NumberParse_fr_Test extends GWTTestCase {

  /**
   * Must refer to a valid module that inherits from com.google.gwt.junit.JUnit.
   */
  public String getModuleName() {
    return "com.google.gwt.i18n.I18NTest_fr";
  }

  private static Number numberParse(String pattern, String toParse) {
    NumberFormat fmt = NumberFormat.getFormat(pattern);
    return new Double(fmt.parse(toParse));
  }

  public void testBasicParse() {

    Number value;

    value = numberParse("0.0000", "123,4579");
    assertEquals(123.4579, value.doubleValue(), 0.0);

    value = numberParse("0.0000", "+123,4579");
    assertEquals(123.4579, value.doubleValue(), 0.0);

    value = numberParse("0.0000", "-123,4579");
    assertEquals(-123.4579, value.doubleValue(), 0.0);
  }

  public void testExponentParse() {

    Number value;

    value = numberParse("#E0", "1,234E3");
    assertEquals(1.234E+3, value.doubleValue(), 0.0);

    value = numberParse("0.###E0", "1,234E3");
    assertEquals(1.234E+3, value.doubleValue(), 0.0);

    value = numberParse("#E0", "1,2345E4");
    assertEquals(12345.0, value.doubleValue(), 0.0);

    value = numberParse("0E0", "1,2345E4");
    assertEquals(12345.0, value.doubleValue(), 0.0);

    value = numberParse("0E0", "1,2345E+4");
    assertEquals(12345.0, value.doubleValue(), 0.0);
  }

  public void testGroupingParse() {

    Number value;

    value = numberParse("#,###", "1\u00a0234\u00a0567\u00a0890");
    assertEquals(1234567890, value.doubleValue(), 0.0);
    value = numberParse("#,####", "12\u00a03456\u00a07890");
    assertEquals(1234567890, value.doubleValue(), 0.0);

    value = numberParse("#", "1234567890");
    assertEquals(1234567890, value.doubleValue(), 0.0);
  }

  public void testInfinityParse() {

    Number value;

    value = numberParse("0.0;(0.0)", "\u221E");
    assertEquals(Double.POSITIVE_INFINITY, value.doubleValue(), 0.0);

    value = numberParse("0.0;(0.0)", "(\u221E)");
    assertEquals(Double.NEGATIVE_INFINITY, value.doubleValue(), 0.0);
  }

  public void testPrecentParse() {

    Number value;

    value = numberParse("0.0;(0.0)", "123,4579%");
    assertEquals((123.4579 / 100), value.doubleValue(), 0.0);

    value = numberParse("0.0;(0.0)", "(%123,4579)");
    assertEquals((-123.4579 / 100), value.doubleValue(), 0.0);

    value = numberParse("0.0;(0.0)", "123,4579\u2030");
    assertEquals((123.4579 / 1000), value.doubleValue(), 0.0);

    value = numberParse("0.0;(0.0)", "(\u2030123,4579)");
    assertEquals((-123.4579 / 1000), value.doubleValue(), 0.0);
  }

  public void testPrefixParse() {

    Number value;

    value = numberParse("0.0;(0.0)", "123,4579");
    assertEquals(123.4579, value.doubleValue(), 0.0);

    value = numberParse("0.0;(0.0)", "(123,4579)");
    assertEquals(-123.4579, value.doubleValue(), 0.0);
  }

}
