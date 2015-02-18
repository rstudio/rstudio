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

import com.google.gwt.core.shared.GWT;
import com.google.gwt.thirdparty.guava.common.base.Preconditions;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * SafeUri utilities whose implementation differs between Development and Production Mode.
 *
 * <p>
 * This class has a super-source peer that provides the Production Mode implementation.
 *
 * <p>
 * Do not use this class - it is used for implementation only, and its methods may change in the
 * future.
 */
public class SafeUriHostedModeUtils {

  /**
   * All valid Web Addresses discrete characters, i.e. the reserved, iunreserved, href-ucschar, and
   * href-pct-form productions from RFC 3986 and RFC 3987bis, with the exception of character
   * ranges.
   *
   * @see <a href="http://tools.ietf.org/html/rfc3986#section-2">RFC 3986</a>
   * @see <a href="http://tools.ietf.org/html/draft-ietf-iri-3987bis-05#section-7.2">RFC 3987bis Web Addresses</a>
   */
  static final String HREF_DISCRETE_UCSCHAR = ":/?#[]@!$&'()*+,;=" // reserved
      + "-._~" // iunreserved
      + " <>\"{}|\\^`" // href-ucschar
      + "%"; // href-pct-form

  /**
   * Name of system property that if set, enables checks in server-side code (even if assertions are
   * disabled).
   */
  public static final String FORCE_CHECK_VALID_URI = "com.google.gwt.safehtml.ForceCheckValidUri";

  private static boolean forceCheckValidUri;

  static {
    setForceCheckValidUriFromProperty();
  }

  /**
   * Tests whether all characters in the given URI are valid Web Addresses characters.
   */
  // @VisibleForTesting
  public static boolean isValidUriCharset(String uri) {
    int len = uri.length();
    int i = 0;
    while (i < len) {
      int codePoint = uri.codePointAt(i);
      i += Character.charCount(codePoint);
      if (Character.isSupplementaryCodePoint(codePoint)) {
        continue;
      }
      if (HREF_DISCRETE_UCSCHAR.indexOf(codePoint) >= 0) {
        continue;
      }
      // iunreserved ranges
      if (('a' <= codePoint && codePoint <= 'z') || ('A' <= codePoint && codePoint <= 'Z')
          || ('0' <= codePoint && codePoint <= '9')) {
        continue;
      }
      // href-ucschar ranges
      if ((0 <= codePoint && codePoint <= 0x1F) || (0x7F <= codePoint && codePoint <= 0xD7FF)
          || (0xE000 <= codePoint && codePoint <= 0xFFFD)) {
        continue;
      }
      return false;
    }
    return true;
  }

  /**
   * Checks if the provided URI is a valid Web Address (per RFC 3987bis).
   *
   * @param uri the URL to check
   */
  public static void maybeCheckValidUri(String uri) {
    if (GWT.isClient() || forceCheckValidUri) {
      Preconditions.checkArgument(isValidUri(uri), "String is not a valid URI: %s", uri);
    } else {
      assert isValidUri(uri) : "String is not a valid URI: " + uri;
    }
  }

  /**
   * Sets a global flag that controls whether or not {@link #maybeCheckValidUri(String)} should
   * perform its check in a server-side environment.
   *
   * @param check if true, perform server-side checks.
   */
  public static void setForceCheckValidUri(boolean check) {
    forceCheckValidUri = check;
  }

  /**
   * Sets a global flag that controls whether or not {@link #maybeCheckValidUri(String)} should
   * perform its check in a server-side environment from the value of the {@value
   * FORCE_CHECK_VALID_URI} property.
   */
  // The following annotation causes javadoc to crash on Mac OS X 10.5.8,
  // using java 1.5.0_24.
  //
  // See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6442982
  //
  // @VisibleForTesting
  public static void setForceCheckValidUriFromProperty() {
    forceCheckValidUri = System.getProperty(FORCE_CHECK_VALID_URI) != null;
  }

  private static boolean isValidUri(String uri) {
    if (!isValidUriCharset(uri)) {
      return false;
    }

    // pre-process to turn href-ucschars into ucschars, and encode to URI.
    uri = UriUtils.encodeAllowEscapes(uri);
    try {
      new URI(uri);
      return true;
    } catch (URISyntaxException e) {
      return false;
    }
  }
}
