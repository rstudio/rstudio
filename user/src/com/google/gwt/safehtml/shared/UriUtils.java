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
package com.google.gwt.safehtml.shared;

/**
 * Utility class containing static methods for validating and sanitizing URIs.
 */
public final class UriUtils {
  
  /**
   * Extracts the scheme of a URI.
   * 
   * @param uri the URI to extract the scheme from
   * @return the URI's scheme, or {@code null} if the URI does not have one
   */
  public static String extractScheme(String uri) {
    int colonPos = uri.indexOf(':');
    if (colonPos < 0) {
      return null;
    }
    String scheme = uri.substring(0, colonPos);
    if (scheme.indexOf('/') >= 0 || scheme.indexOf('#') >= 0) {
      /*
       *  The URI's prefix up to the first ':' contains other URI special
       *  chars, and won't be interpreted as a scheme.
       *  
       *  TODO(xtof): Consider basing this on URL#isValidProtocol or similar;
       *  however I'm worried that being too strict here will effectively
       *   allow dangerous schemes accepted in loosely parsing browsers.
       */
      return null;
    }
    return scheme;
  }
  
  /**
   * Determines if a {@link String} is safe to use as the value of a URI-valued
   * HTML attribute such as {@code src} or {@code href}.
   * 
   * <p>
   * In this context, a URI is safe if it can be established that using it as
   * the value of a URI-valued HTML attribute such as {@code src} or {@code
   * href} cannot result in script execution. Specifically, this method deems a
   * URI safe if it either does not have a scheme, or its scheme is one of
   * {@code http, https, ftp, mailto}.
   * 
   * @param uri the URI to validate
   * @return {@code true} if {@code uri} is safe in the above sense; {@code
   *         false} otherwise
   */
  public static boolean isSafeUri(String uri) {
    String scheme = extractScheme(uri);
    if (scheme == null) {
      return true;
    }
    /*
     * Special care is be taken with case-insensitive 'i' in the Turkish locale.
     * i -> to upper in Turkish locale -> İ
     * I -> to lower in Turkish locale -> ı
     * For this reason there are two checks for mailto: "mailto" and "MAILTO"
     * For details, see: http://www.i18nguy.com/unicode/turkish-i18n.html
     */
    String schemeLc = scheme.toLowerCase();
    return ("http".equals(schemeLc)
        || "https".equals(schemeLc)
        || "ftp".equals(schemeLc)
        || "mailto".equals(schemeLc) 
        || "MAILTO".equals(scheme.toUpperCase()));
  }

  /**
   * Sanitizes a URI.
   * 
   * <p>
   * This method returns the URI provided if it is safe to use as the the value
   * of a URI-valued HTML attribute according to {@link #isSafeUri}, or the URI
   * "{@code #}" otherwise.
   * 
   * @param uri the URI to sanitize.
   */
  public static String sanitizeUri(String uri) {
    if (isSafeUri(uri)) {
      return uri;
    } else {
      return "#";
    }
  }

  // prevent instantiation
  private UriUtils() {
  }
}
