/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.safehtml.shared;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * GWT Unit tests for SafeHtmlString.
 */
public class GwtSafeHtmlStringTest extends GWTTestCase {

  // Test SafeHtmlString.equals()
  public void testEquals() {
    SafeHtmlString safe1 = new SafeHtmlString("stringsame");
    SafeHtmlString safe2 = new SafeHtmlString("stringsame");
    SafeHtmlString safe3 = new SafeHtmlString("stringdiff");
    assertEquals(safe1, safe2);
    assertFalse(safe1.equals(safe3));
  }
  
  // Test SafeHtmlString.hashCode()
  public void testHashCode() {
    SafeHtmlString safe1 = new SafeHtmlString("stringsame");
    SafeHtmlString safe3 = new SafeHtmlString("stringdiff");
    SafeHtmlString safe2 = new SafeHtmlString("stringsame");
    assertEquals("stringsame".hashCode(), safe1.hashCode());
    assertEquals(safe1.hashCode(), safe2.hashCode());
    assertEquals("stringdiff".hashCode(), safe3.hashCode());
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.safehtml.SafeHtmlTestsModule";
  }
}
