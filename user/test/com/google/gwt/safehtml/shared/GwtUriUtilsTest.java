/*
 * Copyright 2011 Google Inc.
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Unit tests for {@link UriUtils}.
 */
public class GwtUriUtilsTest extends GWTTestCase {

  private static final String JAVASCRIPT_URL = "javascript:alert('BOOM!');";
  private static final String MAILTO_URL = "mailto:foo@example.com";
  private static final String CONSTANT_URL =
    "http://gwt.google.com/samples/Showcase/Showcase.html?locale=fr#!CwCheckBox";
  private static final String EMPTY_GIF_DATA_URL =
    "data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==";

  public void testEncode_noEscape() {
    StringBuilder sb = new StringBuilder(UriUtils.DONT_NEED_ENCODING);
    final int upcaseOffset = 'A' - 'a';
    for (char c = 'a'; c <= 'z'; c++) {
      sb.append(c).append((char) (c + upcaseOffset));
    }
    for (char c = '0'; c <= '9'; c++) {
      sb.append(c);
    }
    final String expected = sb.toString();

    assertEquals(expected, UriUtils.encode(expected));
  }

  public void testEncode_percent() {
    assertEquals("foo%25bar", UriUtils.encode("foo%bar"));
  }

  public void testEncode_percentAndOthers() {
    assertEquals("fo%20o%25b%0Aa%22r", UriUtils.encode("fo o%b\na\"r"));
  }

  public void testEncode_withEscapes1() {
    assertEquals("foo%bar", UriUtils.encodeAllowEscapes("foo%bar"));
  }

  public void testEncode_withEscapes2() {
    assertEquals("foo%25bar", UriUtils.encodeAllowEscapes("foo%25bar"));
  }

  public void testEncode_withEscapes3() {
    assertEquals("foo%E2%82%ACbar", UriUtils.encodeAllowEscapes("foo\u20ACbar"));
  }

  public void testEncode_withEscapes4() {
    assertEquals("foo%E2%82%ACbar", UriUtils.encodeAllowEscapes("foo%E2%82%ACbar"));
  }

  public void testEncode_withEscapesIncompleteEscapes() {
    assertEquals("foob%25ar%25a", UriUtils.encodeAllowEscapes("foob%ar%a"));
  }

  public void testEncode_withEscapesInvalidEscapes() {
    assertEquals("f%25ooba%25r", UriUtils.encodeAllowEscapes("f%ooba%r"));
  }

  public void testFromTrustedString() {
    assertEquals(CONSTANT_URL, UriUtils.fromTrustedString(CONSTANT_URL).asString());
    assertEquals(MAILTO_URL, UriUtils.fromTrustedString(MAILTO_URL).asString());
    assertEquals(EMPTY_GIF_DATA_URL, UriUtils.fromTrustedString(EMPTY_GIF_DATA_URL).asString());
    assertEquals(JAVASCRIPT_URL, UriUtils.fromTrustedString(JAVASCRIPT_URL).asString());
    if (GWT.isClient()) {
      assertEquals(GWT.getModuleBaseURL(),
          UriUtils.fromTrustedString(GWT.getModuleBaseURL()).asString());
      assertEquals(GWT.getHostPageBaseURL(),
          UriUtils.fromTrustedString(GWT.getHostPageBaseURL()).asString());
    }
  }

  public void testFromTrustedString_withInvalidUrl() {
    if (GWT.isProdMode()) {
      // fromTrustedString does not parse/validate its argument in prod mode.
      // Hence we short-circuit this test in prod mode.
      return;
    }
    try {
      SafeUri u = UriUtils.fromTrustedString("a\uD800b");
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  @SuppressWarnings("deprecation")
  public void testUnsafeCastFromUntrustedString() {
    assertEquals(CONSTANT_URL, UriUtils.unsafeCastFromUntrustedString(CONSTANT_URL).asString());
    assertEquals(MAILTO_URL, UriUtils.unsafeCastFromUntrustedString(MAILTO_URL).asString());
    assertEquals(EMPTY_GIF_DATA_URL, UriUtils.unsafeCastFromUntrustedString(EMPTY_GIF_DATA_URL)
        .asString());
    assertEquals(JAVASCRIPT_URL, UriUtils.unsafeCastFromUntrustedString(JAVASCRIPT_URL).asString());
    assertEquals("a\uD800b", UriUtils.unsafeCastFromUntrustedString("a\uD800b").asString());
    if (GWT.isClient()) {
      assertEquals(GWT.getModuleBaseURL(), UriUtils.unsafeCastFromUntrustedString(
          GWT.getModuleBaseURL()).asString());
      assertEquals(GWT.getHostPageBaseURL(), UriUtils.unsafeCastFromUntrustedString(
          GWT.getHostPageBaseURL()).asString());
    }
  }

  public void testFromString() {
    assertEquals(CONSTANT_URL, UriUtils.fromString(CONSTANT_URL).asString());
    assertEquals(MAILTO_URL, UriUtils.fromString(MAILTO_URL).asString());
    assertEquals(UriUtils.sanitizeUri(EMPTY_GIF_DATA_URL),
        UriUtils.fromString(EMPTY_GIF_DATA_URL).asString());
    assertEquals(UriUtils.sanitizeUri(JAVASCRIPT_URL),
        UriUtils.fromString(JAVASCRIPT_URL).asString());
    if (GWT.isClient()) {
      assertEquals(GWT.getModuleBaseURL(),
          UriUtils.fromString(GWT.getModuleBaseURL()).asString());
      assertEquals(GWT.getHostPageBaseURL(),
          UriUtils.fromString(GWT.getHostPageBaseURL()).asString());
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.safehtml.SafeHtmlTestsModule";
  }
}
