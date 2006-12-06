// Copyright 2006 Google Inc. All Rights Reserved.

package com.google.gwt.http.client;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests for the URL utility class.
 */
public class URLTest extends GWTTestCase {
  
  private final String DECODED_URL = "http://www.foo \u00E9 bar.com/1_!~*'();/?@&=+$,#";
  private final String DECODED_URL_COMPONENT = "-_.!~*'():/#?@ \u00E9 ";  
  private final String ENCODED_URL = "http://www.foo%20%C3%A9%20bar.com/1_!~*'();/?@&=+$,#";
  private final String ENCODED_URL_COMPONENT = "-_.!~*'()%3A%2F%23%3F%40+%C3%A9+";
  
  public String getModuleName() {
    return "com.google.gwt.http.HttpSuite";
  }

  /**
   * Test method for {@link com.google.gwt.http.client.URL#decode(java.lang.String)}.
   */
  public void testDecode() {
    try {
      URL.decode(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException ex) {
      // expected exception was thrown
    }
    
    try {
      URL.decode("");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
      // expected exception was thrown
    }
    
    String actualURL = URL.decode(ENCODED_URL);
    assertEquals(DECODED_URL, actualURL);
  }

  /**
   * Test method for {@link com.google.gwt.http.client.URL#decodeComponent(java.lang.String)}.
   */
  public void testDecodeComponent() {
    try {
      URL.decodeComponent(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException ex) {
      // expected exception was thrown
    }
    
    try {
      URL.decodeComponent("");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
      // expected exception was thrown
    }
    
    String actualURLComponent = URL.decodeComponent(ENCODED_URL_COMPONENT);
    assertEquals(DECODED_URL_COMPONENT, actualURLComponent);
  }

  /**
   * Test method for {@link com.google.gwt.http.client.URL#encode(java.lang.String)}.
   */
  public void testEncode() {
    try {
      URL.encode(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException ex) {
      // expected exception was thrown
    }
    
    try {
      URL.encode("");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
      // expected exception was thrown
    }
    
    String actualURL = URL.encode(DECODED_URL);
    assertEquals(ENCODED_URL, actualURL);
  }

  /**
   * Test method for {@link com.google.gwt.http.client.URL#encodeComponent(java.lang.String)}.
   */
  public void testEncodeComponent() {
    try {
      URL.encodeComponent(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException ex) {
      // expected exception was thrown
    }
    
    try {
      URL.encodeComponent("");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
      // expected exception was thrown
    }
    
    String actualURLComponent = URL.encodeComponent(DECODED_URL_COMPONENT);
    assertEquals(ENCODED_URL_COMPONENT, actualURLComponent);
  }
}
