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
 * Tests the LocaleInfo class and the associated generator.
 */
public class LocaleInfoTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.i18n.I18NTest";
  }

  public void testAvailableLocales() {
    String[] locales = LocaleInfo.getAvailableLocaleNames();
    assertArrayEquals(new String[] {
        "default", "piglatin", "piglatin_UK", "piglatin_UK_WINDOWS"}, locales);
  }

  public void testCookieName() {
    String cookieName = LocaleInfo.getCurrentLocale().getLocaleCookieName();
    assertNull(cookieName);
  }

  public void testCurrentLocale() {
    String locale = LocaleInfo.getCurrentLocale().getLocaleName();
    assertEquals("piglatin_UK_WINDOWS", locale);
  }

  public void testNativeDisplayNames() {
    // en isn't in the property set for this module so should return null
    String displayName = LocaleInfo.getLocaleNativeDisplayName("en");
    assertNull(displayName);
    
    // verify piglatin is known
    displayName = LocaleInfo.getLocaleNativeDisplayName("piglatin");
    assertEquals("Igpay Atinlay", displayName);
  }

  public void testQueryParam() {
    String queryParam = LocaleInfo.getCurrentLocale().getLocaleQueryParam();
    assertEquals("locale", queryParam);
  }
  
  public void testRTL() {
    boolean isRTL = LocaleInfo.getCurrentLocale().isRTL();
    assertFalse(isRTL);
    boolean hasRTL = LocaleInfo.hasAnyRTL();
    assertFalse(hasRTL);
  }

  private void assertArrayEquals(String[] expected, String[] actual) {
    assertEquals(expected.length, actual.length);
    for (int i = 0; i < actual.length; i++) {
      assertEquals(expected[i], actual[i]);
    }
  }
}
