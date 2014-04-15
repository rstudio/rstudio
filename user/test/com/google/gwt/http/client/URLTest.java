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
package com.google.gwt.http.client;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests for the URL utility class.
 */
public class URLTest extends GWTTestCase {

  private final String DECODED_URL = "http://www.foo \u00E9+bar.com/1_!~*'();/?@&=+$,#";
  private final String DECODED_URL_COMPONENT = "-_.!~*'():/#?@ \u00E9+";
  private final String ENCODED_URL = "http://www.foo%20%C3%A9+bar.com/1_!~*'();/?@&=+$,#";
  private final String ENCODED_URL_COMPONENT = "-_.!~*'()%3A%2F%23%3F%40%20%C3%A9%2B";
  private final String ENCODED_URL_COMPONENT_QS = "-_.!~*'()%3A%2F%23%3F%40+%C3%A9%2B";

  @Override
  public String getModuleName() {
    return "com.google.gwt.http.HttpSuite";
  }

  /**
   * Test method for
   * {@link com.google.gwt.http.client.URL#decode(java.lang.String)}.
   */
  public void testDecode() {
    try {
      URL.decode(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException ex) {
      // expected exception was thrown
    }

    assertEquals("", URL.decode(""));
    assertEquals(" ", URL.decode(" "));

    String actualURL = URL.decode(ENCODED_URL);
    assertEquals(DECODED_URL, actualURL);
  }

  /**
   * Test method for
   * {@link com.google.gwt.http.client.URL#decodeComponent(java.lang.String)}.
   */
  @SuppressWarnings("deprecation")
  public void testDecodeComponent() {
    try {
      URL.decodeComponent(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException ex) {
      // expected exception was thrown
    }

    assertEquals("", URL.decodeComponent(""));
    assertEquals(" ", URL.decodeComponent(" "));
    assertEquals(" ", URL.decodeComponent("+"));
    assertEquals(" ", URL.decodeComponent("%20"));

    String actualURLComponent = URL.decodeComponent(ENCODED_URL_COMPONENT);
    assertEquals(DECODED_URL_COMPONENT, actualURLComponent);

    actualURLComponent = URL.decodeComponent(ENCODED_URL_COMPONENT_QS);
    assertEquals(DECODED_URL_COMPONENT, actualURLComponent);
  }

  /**
   * Test method for
   * {@link com.google.gwt.http.client.URL#decodeComponent(java.lang.String,boolean)}.
   */
  @SuppressWarnings("deprecation")
  public void testDecodeComponent2() {
    try {
      URL.decodeComponent(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException ex) {
      // expected exception was thrown
    }

    assertEquals("", URL.decodeComponent("", false));
    assertEquals("", URL.decodeComponent("", true));
    assertEquals(" ", URL.decodeComponent(" ", false));
    assertEquals(" ", URL.decodeComponent(" ", true));
    assertEquals("+", URL.decodeComponent("+", false));
    assertEquals(" ", URL.decodeComponent("+", true));
    assertEquals(" ", URL.decodeComponent("%20", false));
    assertEquals(" ", URL.decodeComponent("%20", true));

    String actualURLComponent = URL.decodeComponent(ENCODED_URL_COMPONENT, false);
    assertEquals(DECODED_URL_COMPONENT, actualURLComponent);

    actualURLComponent = URL.decodeComponent(ENCODED_URL_COMPONENT_QS, true);
    assertEquals(DECODED_URL_COMPONENT, actualURLComponent);
  }

  /**
   * Test method for
   * {@link com.google.gwt.http.client.URL#decodePathSegment(java.lang.String,boolean)}.
   */
  public void testDecodePathSegment() {
    try {
      URL.decodePathSegment(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException ex) {
      // expected exception was thrown
    }

    assertEquals("", URL.decodePathSegment(""));
    assertEquals(" ", URL.decodePathSegment(" "));
    assertEquals("+", URL.decodePathSegment("+"));
    assertEquals(" ", URL.decodePathSegment("%20"));

    String actualURLComponent = URL.decodePathSegment(ENCODED_URL_COMPONENT);
    assertEquals(DECODED_URL_COMPONENT, actualURLComponent);
  }

  /**
   * Test method for
   * {@link com.google.gwt.http.client.URL#decodeQueryString(java.lang.String)}.
   */
  public void testDecodeQueryString() {
    try {
      URL.decodeQueryString(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException ex) {
      // expected exception was thrown
    }

    try {
      // Malformed URI sequence
      URL.decodeQueryString("%E4");
      fail("Expected JavaScriptException");
    } catch (JavaScriptException ignored) {
      // expected exception was thrown
    }

    assertEquals("", URL.decodeQueryString(""));
    assertEquals(" ", URL.decodeQueryString(" "));
    assertEquals(" ", URL.decodeQueryString("+"));
    assertEquals(" ", URL.decodeQueryString("%20"));

    String actualURLComponent = URL.decodeQueryString(ENCODED_URL_COMPONENT);
    assertEquals(DECODED_URL_COMPONENT, actualURLComponent);

    actualURLComponent = URL.decodeQueryString(ENCODED_URL_COMPONENT_QS);
    assertEquals(DECODED_URL_COMPONENT, actualURLComponent);
  }

  /**
   * Test method for
   * {@link com.google.gwt.http.client.URL#encode(java.lang.String)}.
   */
  public void testEncode() {
    try {
      URL.encode(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException ex) {
      // expected exception was thrown
    }

    assertEquals("", URL.encode(""));
    assertEquals("%20",URL.encode(" "));

    String actualURL = URL.encode(DECODED_URL);
    assertEquals(ENCODED_URL, actualURL);
  }

  /**
   * Test method for
   * {@link com.google.gwt.http.client.URL#encodeComponent(java.lang.String)}.
   */
  @SuppressWarnings("deprecation")
  public void testEncodeComponent() {
    try {
      URL.encodeComponent(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException ex) {
      // expected exception was thrown
    }

    assertEquals("", URL.encodeComponent(""));
    assertEquals("+", URL.encodeComponent(" "));

    String actualURLComponent = URL.encodeComponent(DECODED_URL_COMPONENT);
    assertEquals(ENCODED_URL_COMPONENT_QS, actualURLComponent);
  }

  /**
   * Test method for
   * {@link com.google.gwt.http.client.URL#encodeComponent(java.lang.String,boolean)}.
   */
  @SuppressWarnings("deprecation")
  public void testEncodeComponent2() {
    try {
      URL.encodeComponent(null, false);
      fail("Expected NullPointerException");
    } catch (NullPointerException ex) {
      // expected exception was thrown
    }

    try {
      URL.encodeComponent(null, true);
      fail("Expected NullPointerException");
    } catch (NullPointerException ex) {
      // expected exception was thrown
    }

    assertEquals("", URL.encodeComponent("", false));
    assertEquals("", URL.encodeComponent("", true));
    assertEquals("%20", URL.encodeComponent(" ", false));
    assertEquals("+", URL.encodeComponent(" ", true));

    String actualURLComponent = URL.encodeComponent(DECODED_URL_COMPONENT, false);
    assertEquals(ENCODED_URL_COMPONENT, actualURLComponent);

    actualURLComponent = URL.encodeComponent(DECODED_URL_COMPONENT, true);
    assertEquals(ENCODED_URL_COMPONENT_QS, actualURLComponent);
  }

  /**
   * Test method for
   * {@link com.google.gwt.http.client.URL#encodePathSegment(java.lang.String,boolean)}.
   */
  public void testEncodePathSegment() {
    try {
      URL.encodePathSegment(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException ex) {
      // expected exception was thrown
    }

    assertEquals("", URL.encodePathSegment(""));
    assertEquals("%20", URL.encodePathSegment(" "));

    String actualURLComponent = URL.encodePathSegment(DECODED_URL_COMPONENT);
    assertEquals(ENCODED_URL_COMPONENT, actualURLComponent);
  }

  /**
   * Test method for
   * {@link com.google.gwt.http.client.URL#encodeQueryString(java.lang.String)}.
   */
  public void testEncodeQueryString() {
    try {
      URL.encodeQueryString(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException ex) {
      // expected exception was thrown
    }

    assertEquals("", URL.encodeQueryString(""));
    assertEquals("+", URL.encodeQueryString(" "));

    String actualURLComponent = URL.encodeQueryString(DECODED_URL_COMPONENT);
    assertEquals(ENCODED_URL_COMPONENT_QS, actualURLComponent);
  }
}
