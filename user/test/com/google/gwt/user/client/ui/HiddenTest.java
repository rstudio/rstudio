/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.user.client.ui;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * TODO: document me.
 */
public class HiddenTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testBadNames() {
    try {
      Hidden d = new Hidden("");
      fail("expected illegal argument");
    } catch (IllegalArgumentException e) {
      // Expected
    }
    try {
      Hidden d = new Hidden((String) null);
      fail("expected null pointer exception");
    } catch (NullPointerException e) {
      // Expected
    }
  }

  public void testAtributes() {
    Hidden x = new Hidden("test");
    x.setDefaultValue("myDefaultValue");
    assertEquals("myDefaultValue", x.getDefaultValue());
    x.setValue("myValue");
    assertEquals("myValue", x.getValue());
    x.setID("myID");
    assertEquals("myID", x.getID());

    x.setName("myName");
    assertEquals("myName", x.getName());
  }

  public void testConstructors() {
    Hidden e = new Hidden();
    assertEquals("", e.getName());
    Hidden e2 = new Hidden("myName");
    assertEquals("myName", e2.getName());
    Hidden e3 = new Hidden("myName", "myValue");
    assertEquals("myName", e3.getName());
    assertEquals("myValue", e3.getValue());
  }

}
