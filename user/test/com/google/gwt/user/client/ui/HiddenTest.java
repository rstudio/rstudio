// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.user.client.ui;

import com.google.gwt.junit.client.GWTTestCase;

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
      Hidden d = new Hidden(null);
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
  
  public void testConstructors(){
    Hidden e= new Hidden();
    assertEquals("", e.getName());
    Hidden e2 = new Hidden("myName");
    assertEquals("myName", e2.getName());
    Hidden e3 = new Hidden("myName", "myValue");
    assertEquals("myName", e3.getName());
    assertEquals("myValue", e3.getValue());
  }

}
