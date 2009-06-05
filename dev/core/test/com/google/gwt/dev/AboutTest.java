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

  @SuppressWarnings("deprecation")
  public void testDepreciatedConstants() {
    assertEquals("GWT_NAME", About.getGwtName(), About.GWT_NAME); 
    assertEquals("GWT_VERSION", About.getGwtVersion(), About.GWT_VERSION);
    assertEquals("GWT_VERSION_NUM", About.getGwtVersionNum(), About.GWT_VERSION_NUM);
    assertEquals("GWT_SVNREV", About.getGwtSvnRev(), About.GWT_SVNREV);
  }
  
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
    
  }
  
  public void testGwtVersionNum() {
    String result = About.getGwtVersionNum();
    assertFalse(result.length() == 0);
  }
  
  public void testParseGwtVersionString() {
    int[] result;
    result = About.parseGwtVersionString("0.0.0");
    assertEquals("result.length", 3, result.length);
    assertEquals("0.0.0 - 0", 0, result[0]);
    assertEquals("0.0.0 - 1", 0, result[1]);
    assertEquals("0.0.0 - 2", 0, result[2]);
    
    result = About.parseGwtVersionString(null);
    assertEquals("result.length", 3, result.length);
    assertEquals("null - 0", 0, result[0]);
    assertEquals("null - 1", 0, result[1]);
    assertEquals("null - 2", 0, result[2]);    
    
    result = About.parseGwtVersionString("1.5.4");
    assertEquals("result.length", 3, result.length);
    assertEquals("1.5.4 - 0", 1, result[0]);
    assertEquals("1.5.4 - 1", 5, result[1]);
    assertEquals("1.5.4 - 2", 4, result[2]);
    
    result = About.parseGwtVersionString("prefix1.5.4");
    assertEquals("prefix1.5.4 - 0", 1, result[0]);
    assertEquals("prefix1.5.4 - 1", 5, result[1]);
    assertEquals("prefix1.5.4 - 2", 4, result[2]);
    
    result = About.parseGwtVersionString("1.5.4-suffix0");
    assertEquals("result.length", 3, result.length);
    assertEquals("1.5.4-suffix0 - 0", 1, result[0]);
    assertEquals("1.5.4-suffix0 - 1", 5, result[1]);
    assertEquals("1.5.4-suffix0 - 2", 4, result[2]);
    
    result = About.parseGwtVersionString("1.5.4-patch0");
    assertEquals("result.length", 3, result.length);
    assertEquals("1.5.4-patch0 - 0", 1, result[0]);
    assertEquals("1.5.4-patch0 - 1", 5, result[1]);
    assertEquals("1.5.4-patch0 - 2", 4, result[2]);

    try {
      result = About.parseGwtVersionString("1.5.4.2");
      fail("Should have thrown exception parsing 1.5.4.2. Got "
          + result[0] +", " + result[1] + ", " + result[2]);
    } catch (NumberFormatException ex) {
      // OK
    }        
    
    try {
      result = About.parseGwtVersionString("1.5baloney");
      fail("Should have thrown exception parsing 1.5baloney Got "
          + result[0] +", " + result[1] + ", " + result[2]);
    } catch (NumberFormatException ex) {
      // OK
    }            
    
    try {
      result = About.parseGwtVersionString("1");
      fail("Should have thrown exception parsing 1 Got "
          + result[0] +", " + result[1] + ", " + result[2]);
    } catch (NumberFormatException ex) {
      // OK
    }
    
    try {
      result = About.parseGwtVersionString("1.5");
      fail("Should have thrown exception parsing 1.5 Got "
          + result[0] +", " + result[1] + ", " + result[2]);
    } catch (NumberFormatException ex) {
      // OK
    }
  }
}
