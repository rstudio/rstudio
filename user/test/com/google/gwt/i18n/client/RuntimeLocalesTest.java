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
 * Test the same things as I18NTest but with a different module which
 * uses different locales.
 */
public class RuntimeLocalesTest extends GWTTestCase {
  // TODO(jat): add tests to verify runtime locales with NumberFormat etc.
  // Have to figure out how to get runtime locale set within JUnitShell.
  
  @Override
  public String getModuleName() {
    return "com.google.gwt.i18n.RuntimeLocalesTest";
  }

  public void testAvailable() {
    String[] locales = LocaleInfo.getAvailableLocaleNames();
    assertEquals(50, locales.length);
  }
}
