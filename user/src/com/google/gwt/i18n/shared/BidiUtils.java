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
package com.google.gwt.i18n.shared;

import com.google.gwt.i18n.client.HasDirection.Direction;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.regexp.shared.SplitResult;

/**
 * Utility functions for performing common Bidi tests on strings.
 */
public class BidiUtils {

  /**
   * A practical pattern to identify strong LTR characters. This pattern is not
   * completely correct according to the Unicode standard. It is simplified
   * for performance and small code size.
   * <p>
   * This is volatile to prevent the compiler from inlining this constant in
   * various references below.
   */
  private static volatile String LTR_CHARS =
    "A-Za-z\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u02B8\u0300-\u0590\u0800-\u1FFF" +
    "\u2C00-\uFB1C\uFDFE-\uFE6F\uFEFD-\uFFFF";

  /**
   * A practical pattern to identify strong RTL characters. This pattern is not
   * completely correct according to the Unicode standard. It is simplified for
   * performance and small code size.
   * <p>
   * This is volatile to prevent the compiler from inlining this constant in
   * various references below.
   */
  private static volatile String RTL_CHARS =
      "\u0591-\u07FF\uFB1D-\uFDFD\uFE70-\uFEFC";
  
  /**
   * Regular expression to check if the first strongly directional character in
   * a string is LTR.
   */
  private static final RegExp FIRST_STRONG_IS_LTR_RE =
      RegExp.compile("^[^" + RTL_CHARS + "]*[" + LTR_CHARS + ']');

  /**
   * Regular expression to check if the first strongly directional character in
   * a string is RTL.
   */
  private static final RegExp FIRST_STRONG_IS_RTL_RE =
      RegExp.compile("^[^" + LTR_CHARS + "]*[" + RTL_CHARS + ']');

  /**
   * Regular expression to check if a string contains any LTR characters.
   */
  private static final RegExp HAS_ANY_LTR_RE =
      RegExp.compile("[" + LTR_CHARS + ']');

  /**
   * Regular expression to check if a string contains any RTL characters.
   */
  private static final RegExp HAS_ANY_RTL_RE =
      RegExp.compile("[" + RTL_CHARS + ']');

  /**
   * Regular expression to check if a string contains any numerals. Used to
   * differentiate between completely neutral strings and those containing
   * numbers, which are weakly LTR.
   */
  private static final RegExp HAS_NUMERALS_RE = RegExp.compile("\\d");

  /**
   * Simplified regular expression for an HTML tag (opening or closing) or an
   * HTML escape. We might want to skip over such expressions when estimating
   * the text directionality.
   */
  private static final RegExp SKIP_HTML_RE =
      RegExp.compile("<[^>]*>|&[^;]+;", "g");

  /**
   * An instance of BidiUtils, to be returned by {@link #get()}.
   */
  private static final BidiUtils INSTANCE = new BidiUtils();

  /**
   * Regular expression to check if a string looks like something that must
   * always be LTR even in RTL text, e.g. a URL. When estimating the
   * directionality of text containing these, we treat these as weakly LTR, like
   * numbers.
   */
  private static final RegExp IS_REQUIRED_LTR_RE = RegExp.compile("^http://.*");

  /**
   * Regular expressions to check if the last strongly-directional character in
   * a piece of text is LTR.
   */
  private static final RegExp LAST_STRONG_IS_LTR_RE =
      RegExp.compile("[" + LTR_CHARS + "][^" + RTL_CHARS + "]*$");

  /**
   * Regular expressions to check if the last strongly-directional character in
   * a piece of text is RTL.
   */
  private static final RegExp LAST_STRONG_IS_RTL_RE =
      RegExp.compile("[" + RTL_CHARS + "][^" + LTR_CHARS + "]*$");

  /**
   * This constant defines the threshold of RTL directionality.
   */
  private static final float RTL_DETECTION_THRESHOLD = 0.40f;

  /**
   * Regular expression to split a string into "words" for directionality
   * estimation based on relative word counts.
   */
  private static final RegExp WORD_SEPARATOR_RE = RegExp.compile("\\s+");

  /**
   * Get an instance of BidiUtils.
   * @return An instance of BidiUtils
   */
  public static BidiUtils get() {
    return INSTANCE;
  }

  /**
   * Not instantiable.
   */
  private BidiUtils() {
  }

  /**
   * Like {@link #endsWithLtr(String, boolean)}, but assumes {@code str} is not
   * HTML / HTML-escaped.
   */
  public boolean endsWithLtr(String str) {
    return LAST_STRONG_IS_LTR_RE.test(str);
  }

  /**
   * Check whether the last strongly-directional character in the string is LTR.
   * @param str the string to check
   * @param isHtml whether str is HTML / HTML-escaped
   * @return whether LTR exit directionality was detected
   */
  public boolean endsWithLtr(String str, boolean isHtml) {
    return endsWithLtr(stripHtmlIfNeeded(str, isHtml));
  }

  /**
   * Like {@link #endsWithRtl(String, boolean)}, but assumes {@code str} is not
   * HTML / HTML-escaped.
   */
  public boolean endsWithRtl(String str) {
    return LAST_STRONG_IS_RTL_RE.test(str);
  }

  /**
   * Check whether the last strongly-directional character in the string is RTL.
   * @param str the string to check
   * @param isHtml whether str is HTML / HTML-escaped
   * @return whether RTL exit directionality was detected
   */
  public boolean endsWithRtl(String str, boolean isHtml) {
    return endsWithRtl(stripHtmlIfNeeded(str, isHtml));
  }

  /**
   * Like {@link #estimateDirection(String, boolean)}, but assumes {@code str}
   * is not HTML / HTML-escaped.
   */
  public Direction estimateDirection(String str) {
    int rtlCount = 0;
    int total = 0;
    boolean hasWeaklyLtr = false;
    SplitResult tokens = WORD_SEPARATOR_RE.split(str);
    for (int i = 0; i < tokens.length(); i++) {
      String token = tokens.get(i);
      if (startsWithRtl(token)) {
        rtlCount++;
        total++;
      } else if (IS_REQUIRED_LTR_RE.test(token)) {
        hasWeaklyLtr = true;
      } else if (hasAnyLtr(token)) {
        total++;
      } else if (HAS_NUMERALS_RE.test(token)) {
        hasWeaklyLtr = true;
      }
    }

    return total == 0 ? (hasWeaklyLtr ? Direction.LTR : Direction.DEFAULT)
        : ((float) rtlCount / total > RTL_DETECTION_THRESHOLD ? Direction.RTL :
        Direction.LTR);
  }

  /**
   * Estimates the directionality of a string based on relative word counts.
   * If the number of RTL words is above a certain percentage of the total
   * number of strongly directional words, returns RTL.
   * Otherwise, if any words are strongly or weakly LTR, returns LTR.
   * Otherwise, returns DEFAULT, which is used to mean "neutral".
   * Numbers are counted as weakly LTR.
   * @param str the string to check
   * @param isHtml whether {@code str} is HTML / HTML-escaped. Use this to
   *        ignore HTML tags and escapes that would otherwise be mistaken for
   *        LTR text.
   * @return the string's directionality
   */
  public Direction estimateDirection(String str, boolean isHtml) {
    return estimateDirection(stripHtmlIfNeeded(str, isHtml));
  }

  /**
   * Like {@link #hasAnyLtr(String, boolean)}, but assumes {@code str} is not
   * HTML / HTML-escaped.
   * @param str the string to be tested
   * @return whether the string contains any LTR characters
   */
  public boolean hasAnyLtr(String str) {
    return HAS_ANY_LTR_RE.test(str);
  }

  /**
   * Checks if the given string has any LTR characters in it.
   * @param str the string to be tested
   * @param isHtml whether str is HTML / HTML-escaped
   * @return whether the string contains any LTR characters
   */
  public boolean hasAnyLtr(String str, boolean isHtml) {
    return hasAnyLtr(stripHtmlIfNeeded(str, isHtml));
  }

  /**
   * Like {@link #hasAnyRtl(String, boolean)}, but assumes {@code str} is not
   * HTML / HTML-escaped.
   * @param str the string to be tested
   * @return whether the string contains any RTL characters
   */
  public boolean hasAnyRtl(String str) {
    return HAS_ANY_RTL_RE.test(str);
  }

  /**
   * Checks if the given string has any RTL characters in it.
   * @param isHtml whether str is HTML / HTML-escaped
   * @param str the string to be tested
   * @return whether the string contains any RTL characters
   */
  public boolean hasAnyRtl(String str, boolean isHtml) {
    return hasAnyRtl(stripHtmlIfNeeded(str, isHtml));
  }

  /**
   * Like {@link #startsWithLtr(String, boolean)}, but assumes {@code str} is
   * not HTML / HTML-escaped.
   */
  public boolean startsWithLtr(String str) {
    return FIRST_STRONG_IS_LTR_RE.test(str);
  }

  /**
   * Check whether the first strongly-directional character in the string is
   * LTR.
   * @param str the string to check
   * @param isHtml whether str is HTML / HTML-escaped
   * @return whether LTR exit directionality was detected
   */
  public boolean startsWithLtr(String str, boolean isHtml) {
    return startsWithLtr(stripHtmlIfNeeded(str, isHtml));
  }

  /**
   * Like {@link #startsWithRtl(String, boolean)}, but assumes {@code str} is
   * not HTML / HTML-escaped.
   */
  public boolean startsWithRtl(String str) {
    return FIRST_STRONG_IS_RTL_RE.test(str);
  }

  /**
   * Check whether the first strongly-directional character in the string is
   * RTL.
   * @param str the string to check
   * @param isHtml whether {@code str} is HTML / HTML-escaped
   * @return whether RTL exit directionality was detected
   */
  public boolean startsWithRtl(String str, boolean isHtml) {
    return startsWithRtl(stripHtmlIfNeeded(str, isHtml));
  }

  /**
   * Returns the input text with spaces instead of HTML tags or HTML escapes, if
   * isStripNeeded is true. Else returns the input as is.
   * Useful for text directionality estimation.
   * Note: the function should not be used in other contexts; it is not 100%
   * correct, but rather a good-enough implementation for directionality
   * estimation purposes.
   */
  String stripHtmlIfNeeded(String str, boolean isStripNeeded) {
    return isStripNeeded ? SKIP_HTML_RE.replace(str, " ") : str;
  }
}
