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
 * Validate NumberFormat handles Arabic numbers properly.
 */
public class NumberFormat_ar_Test extends GWTTestCase {

  /**
   * Use a module which forces the Arabic locale.
   */
  @Override
  public String getModuleName() {
    return "com.google.gwt.i18n.I18NTest_ar";
  }
  
  public void testDefault() {
    NumberFormat fmt = NumberFormat.getDecimalFormat();
    assertEquals("\u0663\u066B\u0661\u0664", fmt.format(3.14));
    assertEquals("\u0663\u066B\u0661\u0664-", fmt.format(-3.14));
  }
  
  public void testExponent() {
    NumberFormat fmt = NumberFormat.getScientificFormat();
    assertEquals("\u0663\u0627\u0633\u0660",
        fmt.format(3.14));
    assertEquals("\u0663\u0627\u0633\u0662",
        fmt.format(314.0));
    assertEquals("-\u0663\u0627\u0633\u0662",
        fmt.format(-314.0));
  }
  
  public void testForceLatin() {
    assertFalse(NumberFormat.forcedLatinDigits());
    NumberFormat.setForcedLatinDigits(true);
    assertTrue(NumberFormat.forcedLatinDigits());
    NumberFormat decLatin = NumberFormat.getDecimalFormat();
    assertEquals("1\u00A0003,14", decLatin.format(1003.14));
    assertEquals("1\u00A0003,14-", decLatin.format(-1003.14));
    NumberFormat.setForcedLatinDigits(false);
    assertFalse(NumberFormat.forcedLatinDigits());
    assertEquals("3,14", decLatin.format(3.14));
    NumberFormat decArabic = NumberFormat.getDecimalFormat();
    assertEquals("\u0663\u066B\u0661\u0664", decArabic.format(3.14));
  }
  
  public void testParse() {
    NumberFormat fmt = NumberFormat.getDecimalFormat();
    assertEquals(3.14, fmt.parse("\u0663\u066B\u0661\u0664"));
    assertEquals(-3.14, fmt.parse("\u0663\u066B\u0661\u0664-"));
    assertEquals(314.0, fmt.parse("\u0663\u0661\u0664"));
  }
}
