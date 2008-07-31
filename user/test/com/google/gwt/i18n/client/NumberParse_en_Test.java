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
public class NumberParse_en_Test extends GWTTestCase {

  /**
   * Must refer to a valid module that inherits from com.google.gwt.junit.JUnit.
   */
  @Override
  public String getModuleName() {
    return "com.google.gwt.i18n.I18NTest_en";
  }
  
  private static Number numberParse(String pattern, String toParse) {
    NumberFormat fmt = NumberFormat.getFormat(pattern);
    return new Double(fmt.parse(toParse));
  }

  public void testBasicParse() {
    Number value;
    
    value = numberParse("0.0000", "123.4579");
    assertTrue(value.doubleValue() == 123.4579);

    value = numberParse("0.0000", "+123.4579");
    assertTrue(value.doubleValue() == 123.4579);
    
    value = numberParse("0.0000", "-123.4579");
    assertTrue(value.doubleValue() == -123.4579);
    
    try {
      NumberFormat.getDecimalFormat().parse("-1-1--1");
      fail("Expecting NumberFormatException to be thrown");
    } catch (NumberFormatException e) {
    }
  }
  
  public void testExponentParse() {
    Number value;

    value = numberParse("#E0", "1.234E3");
    assertTrue(value.doubleValue() == 1.234E+3);
    
    value = numberParse("0.###E0", "1.234E3");
    assertTrue(value.doubleValue() == 1.234E+3);
    
    value = numberParse("#E0", "1.2345E4");
    assertTrue(value.doubleValue() == 12345.0);
    
    value = numberParse("0E0", "1.2345E4");
    assertTrue(value.doubleValue() == 12345.0);

    value = numberParse("0E0", "1.2345E+4");
    assertTrue(value.doubleValue() == 12345.0);
}

  public void testGroupingParse() {
    Number value;
    
    value = numberParse("#,###", "1,234,567,890");
    assertTrue(value.doubleValue() == 1234567890);
    value = numberParse("#,####", "12,3456,7890");
    assertTrue(value.doubleValue() == 1234567890);

    value = numberParse("#", "1234567890");
    assertTrue(value.doubleValue() == 1234567890);
  }
  
  public void testInfinityParse() {
    Number value;
    
    // gwt need to add those symbols first
    value = numberParse("0.0;(0.0)", "\u221E");
    assertTrue(value.doubleValue() == Double.POSITIVE_INFINITY);
    
    value = numberParse("0.0;(0.0)", "(\u221E)");
    assertTrue(value.doubleValue() == Double.NEGATIVE_INFINITY);
  }
  
  public void testNaNParse() {
    Number value;
    
    // gwt need to add those symbols first
    value = numberParse("0.0;(0.0)", "NaN");
    assertTrue(Double.isNaN(value.doubleValue()));
  }
  
  public void testPrecentParse() {
    Number value;
    
    value = numberParse("0.0;(0.0)", "123.4579%");
    assertTrue(value.doubleValue() == (123.4579 / 100));

    value = numberParse("0.0;(0.0)", "(%123.4579)");
    assertTrue(value.doubleValue() == (-123.4579 / 100));

    value = numberParse("0.0;(0.0)", "123.4579\u2030");
    assertTrue(value.doubleValue() == (123.4579 / 1000));

    value = numberParse("0.0;(0.0)", "(\u2030123.4579)");
    assertTrue(value.doubleValue() == (-123.4579 / 1000));
  }

  public void testPrefixParse() {
    Number value;
    
    value = numberParse("0.0;(0.0)", "123.4579");
    assertTrue(value.doubleValue() == 123.4579);

    value = numberParse("0.0;(0.0)", "(123.4579)");
    assertTrue(value.doubleValue() == -123.4579);
  }
}
