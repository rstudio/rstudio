/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev;

import junit.framework.TestCase;

/**
 * Tests the methods in About
 */
public class AboutTest extends TestCase {

  public void testGwtName() {
    String result = About.getGwtName();
    assertTrue("Google Web Toolkit".equals(result));
  }

  public void testGwtSvnRev() {
    String result = About.getGwtSvnRev();
    assertFalse(result.length() == 0);
  }

  public void testGwtVersion() {
    String result = About.getGwtVersion();
    assertFalse(result.length() == 0);
    String compare = About.getGwtName() + " " + About.getGwtVersionNum();
    assertEquals(compare, result);
  }

  public void testGwtVersionNum() {
    String result = About.getGwtVersionNum();
    assertFalse(result.length() == 0);
  }

  public void testGwtVersionObject() {
    GwtVersion version = About.getGwtVersionObject();
    assertNotNull(version);
    assertFalse(version.toString().length() == 0);
  }
}
