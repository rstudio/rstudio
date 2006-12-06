// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.emultest.java.lang;

import com.google.gwt.junit.client.GWTTestCase;

public class ObjectTest extends GWTTestCase {
  /*
   * Test method for 'java.lang.Object.hashCode()'
   */
  public void testHashCode() {
    Object obj1 = new Object();
    assertEquals(obj1.hashCode(), obj1.hashCode());
    
    Object obj2 = new Object();
    assertEquals(obj2.hashCode(), obj2.hashCode());
  }

  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }
}
