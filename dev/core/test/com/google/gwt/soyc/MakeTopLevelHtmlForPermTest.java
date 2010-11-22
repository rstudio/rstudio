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

package com.google.gwt.soyc;

import junit.framework.TestCase;

/**
 * Test cases for {@link MakeTopLevelHtmlForPerm}.
 */
public class MakeTopLevelHtmlForPermTest extends TestCase {

  public void testGetClassSubstring() {
    assertEquals("myClass", MakeTopLevelHtmlForPerm.getClassSubstring("com.foo.myClass"));
    assertEquals("myClass", MakeTopLevelHtmlForPerm.getClassSubstring("com.foo.myClass::myMethod"));

    // We don't really expect these inputs, just testing to make sure they don't blow up
    assertEquals("Empty string", "", MakeTopLevelHtmlForPerm.getClassSubstring(""));
    assertEquals("", MakeTopLevelHtmlForPerm.getClassSubstring("::myMethod"));
    assertEquals("", MakeTopLevelHtmlForPerm.getMethodSubstring(":"));
    assertEquals("", MakeTopLevelHtmlForPerm.getMethodSubstring("::"));
    assertEquals("", MakeTopLevelHtmlForPerm.getMethodSubstring("..."));
    assertEquals("", MakeTopLevelHtmlForPerm.getMethodSubstring(".."));
    assertEquals("", MakeTopLevelHtmlForPerm.getMethodSubstring("."));
  }

  public void testGetMethodSubstring() {
    assertEquals("", MakeTopLevelHtmlForPerm.getMethodSubstring("com.foo.myClass"));
    assertEquals("myMethod", MakeTopLevelHtmlForPerm.getMethodSubstring("com.foo.myClass::myMethod"));

    // We don't really expect these inputs, just testing to make sure they don't blow up
    assertEquals("Empty string", "", MakeTopLevelHtmlForPerm.getMethodSubstring(""));
    assertEquals("myMethod", MakeTopLevelHtmlForPerm.getMethodSubstring("::myMethod"));
    assertEquals("", MakeTopLevelHtmlForPerm.getMethodSubstring("myMethod"));
    assertEquals("", MakeTopLevelHtmlForPerm.getMethodSubstring(":"));
    assertEquals("", MakeTopLevelHtmlForPerm.getMethodSubstring("::"));
  }

  public void testGetPackageSubstring() {
    assertEquals("com.foo", MakeTopLevelHtmlForPerm.getPackageSubstring("com.foo.myClass"));
    assertEquals("com.foo", MakeTopLevelHtmlForPerm.getPackageSubstring("com.foo.myClass::myMethod"));

    // We don't really expect these inputs, just testing to make sure they don't blow up
    assertEquals("Empty string", "", MakeTopLevelHtmlForPerm.getPackageSubstring(""));
    assertEquals("com.foo", MakeTopLevelHtmlForPerm.getPackageSubstring("com.foo.myClass::"));
    assertEquals("com.foo", MakeTopLevelHtmlForPerm.getPackageSubstring("com.foo.myClass:"));
    assertEquals("com", MakeTopLevelHtmlForPerm.getPackageSubstring("com"));
    assertEquals("", MakeTopLevelHtmlForPerm.getMethodSubstring("..."));
    assertEquals("", MakeTopLevelHtmlForPerm.getMethodSubstring(".."));
    assertEquals("", MakeTopLevelHtmlForPerm.getMethodSubstring("."));
  }
}
