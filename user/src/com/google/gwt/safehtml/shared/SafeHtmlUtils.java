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

import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.safehtml.shared.annotations.IsSafeHtml;
import com.google.gwt.safehtml.shared.annotations.SuppressIsSafeHtmlCastCheck;

/**
 * Utility class containing static methods for escaping and sanitizing strings.
 */
public final class SafeHtmlUtils {

  private static final String HTML_ENTITY_REGEX = "[a-z]+|#[0-9]+|#x[0-9a-fA-F]+";

  /**
   * An empty String.
   */
  public static final SafeHtml EMPTY_SAFE_HTML = new SafeHtmlString("");

  private static final RegExp HTML_CHARS_RE = RegExp.compile("[&<>'\"]");
  private static final RegExp AMP_RE = RegExp.compile("&", "g");
  private static final RegExp GT_RE = RegExp.compile(">", "g");
  private static final RegExp LT_RE = RegExp.compile("<", "g");
  private static final RegExp SQUOT_RE = RegExp.compile("\'", "g");
  private static final RegExp QUOT_RE = RegExp.compile("\"", "g");

  /**
   * Returns a {@link SafeHtml} constructed from a safe string, i.e., without escaping
   * the string.
   *
   * <p>
   * <b>Important</b>: For this method to be able to honor the {@link SafeHtml}
   * contract, all uses of this method must satisfy the following constraints:
   *
   * <ol>
   *
   * <li>The argument expression must be fully determined at compile time.
   *
   * <li>The value of the argument must end in "inner HTML" context and not
   * contain incomplete HTML tags. I.e., the following is not a correct use of
   * this method, because the {@code <a>} tag is incomplete:
   *
   * <pre class="code">
   * {@code shb.appendHtmlConstant("<a href='").append(url)}</pre>
   *
   * </ol>
   *
   * <p>
   * The first constraint provides a sufficient condition that the argument (and
   * any HTML markup contained in it) originates from a trusted source. The
   * second constraint ensures the composability of {@link SafeHtml} values.
   *
   * <p>
   * When executing client-side in Development Mode, or server-side with
   * assertions enabled, the argument is HTML-parsed and validated to satisfy
   * the second constraint (the server-side check can also be enabled
   * programmatically, see
   * {@link SafeHtmlHostedModeUtils#maybeCheckCompleteHtml(String)} for
   * details). For performance reasons, this check is not performed in
   * Production Mode on the client, and with assertions disabled on the server.
   *
   * @param s the string to be wrapped as a {@link SafeHtml}
   * @return {@code s}, wrapped as a {@link SafeHtml}
   * @throws IllegalArgumentException if not running in Production Mode and
   *           {@code html} violates the second constraint
   */
  public static SafeHtml fromSafeConstant(String s) {
    SafeHtmlHostedModeUtils.maybeCheckCompleteHtml(s);
    return new SafeHtmlString(s);
  }

  /**
   * Returns a {@link SafeHtml} containing the escaped string.
   *
   * @param s the input String
   * @return a {@link SafeHtml} instance
   */
  public static SafeHtml fromString(String s) {
    return new SafeHtmlString(htmlEscape(s));
  }

  /**
   * Returns a {@link SafeHtml} constructed from a trusted string, i.e., without
   * escaping the string. No checks are performed. The calling code should be
   * carefully reviewed to ensure the argument meets the {@link SafeHtml} contract.
   *
   * @param s the input String
   * @return a {@link SafeHtml} instance
   */
  public static SafeHtml fromTrustedString(String s) {
    return new SafeHtmlString(s);
  }

  /**
   * HTML-escapes a character. HTML meta characters will be escaped as follows:
   *
   * <pre>
   * &amp; - &amp;amp;
   * &lt; - &amp;lt;
   * &gt; - &amp;gt;
   * &quot; - &amp;quot;
   * &#39; - &amp;#39;
   * </pre>
   *
   * @param c the character to be escaped
   * @return a string containing either the input character
   *     or an equivalent HTML Entity Reference
   */
  public static String htmlEscape(char c) {
    switch (c) {
      case '&':
        return "&amp;";
      case '<':
        return "&lt;";
      case '>':
        return "&gt;";
      case '"':
        return "&quot;";
      case '\'':
        return "&#39;";
      default:
        return "" + c;
    }
  }

  /**
   * HTML-escapes a string.
   *
   * <p>Note: The following variants of this function were profiled on FF40,
   * Chrome44, Safari 8 and IE11:
   * <ol>
   * <li>For each metachar, check indexOf, then use s.replace(regex, string)
   * <li>For each metachar use s.replace(regex, string)
   * <li>Manual replace each metachar by looping through characters in a loop.
   * <li>Check if any metachar is present using a regex, then use #1.
   * <li>Check if any metachar is present using a regex, then use #2.
   * <li>Check if any metachar is present using a regex, then use #3.
   * </ol>
   *
   * <p>For all browsers #4 was found to be the fastest, and is used below.
   *
   * <p>The only out-lier was firefox with #6 being the optimal option, but #6
   * performs considerably worse in all other browsers.
   *
   * @param s the string to be escaped
   * @return the input string, with all occurrences of HTML meta-characters
   *         replaced with their corresponding HTML Entity References
   */
  public static String htmlEscape(String s) {
    if (!HTML_CHARS_RE.test(s)) {
      return s;
    }
    if (s.indexOf("&") != -1) {
      s = AMP_RE.replace(s, "&amp;");
    }
    if (s.indexOf("<") != -1) {
      s = LT_RE.replace(s, "&lt;");
    }
    if (s.indexOf(">") != -1) {
      s = GT_RE.replace(s, "&gt;");
    }
    if (s.indexOf("\"") != -1) {
      s = QUOT_RE.replace(s, "&quot;");
    }
    if (s.indexOf("'") != -1) {
      s = SQUOT_RE.replace(s, "&#39;");
    }
    return s;
  }

  /**
   * HTML-escapes a string, but does not double-escape HTML-entities already
   * present in the string.
   *
   * @param text the string to be escaped
   * @return the input string, with all occurrences of HTML meta-characters
   *         replaced with their corresponding HTML Entity References, with the
   *         exception that ampersand characters are not double-escaped if they
   *         form the start of an HTML Entity Reference
   */
  @IsSafeHtml
  @SuppressIsSafeHtmlCastCheck
  public static String htmlEscapeAllowEntities(String text) {
    StringBuilder escaped = new StringBuilder();

    boolean firstSegment = true;
    for (String segment : text.split("&", -1)) {
      if (firstSegment) {
        /*
         * The first segment is never part of an entity reference, so we always
         * escape it.
         * Note that if the input starts with an ampersand, we will get an empty
         * segment before that.
         */
        firstSegment = false;
        escaped.append(htmlEscape(segment));
        continue;
      }

      int entityEnd = segment.indexOf(';');
      if (entityEnd > 0 && segment.substring(0, entityEnd).matches(HTML_ENTITY_REGEX)) {
        // Append the entity without escaping.
        escaped.append("&").append(segment.substring(0, entityEnd + 1));

        // Append the rest of the segment, escaped.
        escaped.append(htmlEscape(segment.substring(entityEnd + 1)));
      } else {
        // The segment did not start with an entity reference, so escape the
        // whole segment.
        escaped.append("&amp;").append(htmlEscape(segment));
      }
    }

    return escaped.toString();
  }

  // prevent instantiation
  private SafeHtmlUtils() {
  }
}
