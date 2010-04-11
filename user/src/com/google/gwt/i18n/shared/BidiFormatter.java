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

/**
 * Utility class for formatting text for display in a potentially
 * opposite-direction context without garbling. The direction of the context is
 * set at formatter creation and the direction of the text can be either
 * estimated or passed in when known. Provides the following functionality:
 * <p>
 * 1. BiDi Wrapping: When text in one language is mixed into a document in
 * another, opposite-direction language, e.g. when an English business name is
 * embedded in a Hebrew web page, both the inserted string and the text
 * following it may be displayed incorrectly unless the inserted string is
 * explicitly separated from the surrounding text in a "wrapper" that declares
 * its direction at the start and then resets it back at the end. This wrapping
 * can be done in HTML mark-up (e.g. a 'span dir=rtl' tag) or - only in contexts
 * where mark-up cannot be used - in Unicode BiDi formatting codes (LRE|RLE and
 * PDF). Optionally, the mark-up can be inserted even when the direction is the
 * same, in order to keep the DOM structure more stable. Providing such wrapping
 * services is the basic purpose of the BiDi formatter.
 * <p>
 * 2. Direction estimation: How does one know whether a string about to be
 * inserted into surrounding text has the same direction? Well, in many cases,
 * one knows that this must be the case when writing the code doing the
 * insertion, e.g. when a localized message is inserted into a localized page.
 * In such cases there is no need to involve the BiDi formatter at all. In some
 * other cases, it need not be the same as the context, but is either constant
 * (e.g. urls are always LTR) or otherwise known. In the remaining cases, e.g.
 * when the string is user-entered or comes from a database, the language of the
 * string (and thus its direction) is not known a priori, and must be estimated
 * at run-time. The BiDi formatter can do this automatically.
 * <p>
 * 3. Escaping: When wrapping plain text - i.e. text that is not already HTML or
 * HTML-escaped - in HTML mark-up, the text must first be HTML-escaped to
 * prevent XSS attacks and other nasty business. This of course is always true,
 * but the escaping can not be done after the string has already been wrapped in
 * mark-up, so the BiDi formatter also serves as a last chance and includes
 * escaping services.
 * <p>
 * Thus, in a single call, the formatter will escape the input string as
 * specified, determine its direction, and wrap it as necessary. It is then up
 * to the caller to insert the return value in the output.
 * 
 */
public class BidiFormatter {

  /**
   * A container class for direction-related string constants, e.g. Unicode
   * formatting characters.
   */
  static final class Format {
    /**
     * "left" string constant.
     */
    public static final String LEFT = "left";

    /**
     * Unicode "Left-To-Right Embedding" (LRE) character.
     */
    public static final char LRE = '\u202A';
    
    /**
     * Unicode "Left-To-Right Mark" (LRM) character.
     */ 
    public static final char LRM = '\u200E';
    
    /**
     * String representation of LRM.
     */
    public static final String LRM_STRING = Character.toString(LRM);
    
    /**
     * Unicode "Pop Directional Formatting" (PDF) character.
     */ 
    public static final char PDF = '\u202C';
    
    /**
     * "right" string constant.
     */
    public static final String RIGHT = "right";

    /**
     * Unicode "Right-To-Left Embedding" (RLE) character.
     */ 
    public static final char RLE = '\u202B';
    
    /**
     * Unicode "Right-To-Left Mark" (RLM) character.
     */
    public static final char RLM = '\u200F';

    /**
     * String representation of RLM.
     */
    public static final String RLM_STRING = Character.toString(RLM);

    // Not instantiable.
    private Format() {
    } 
  }

  /**
   * Factory for creating an instance of BidiFormatter given the context
   * direction. The default behavior of {@link #spanWrap} and its variations is
   * set to avoid span wrapping unless it's necessary ('dir' attribute needs to
   * be set).
   *
   * @param rtlContext Whether the context direction is RTL.
   *          In one simple use case, the context direction would simply be the
   *          locale direction, which can be retrieved using
   *          {@code LocaleInfo.getCurrentLocale().isRTL()}
   */
  public static BidiFormatter getInstance(boolean rtlContext) {
    return getInstance(rtlContext, false);
  }

  /**
   * Factory for creating an instance of BidiFormatter given the context
   * direction and the desired span wrapping behavior (see below).
   * 
   * @param rtlContext Whether the context direction is RTL. See an example of
   *          a simple use case at {@link #getInstance(boolean)}
   * @param alwaysSpan Whether {@link #spanWrap} (and its variations) should
   *          always use a 'span' tag, even when the input direction is neutral
   *          or matches the context, so that the DOM structure of the output
   *          does not depend on the combination of directions
   */
  public static BidiFormatter getInstance(boolean rtlContext,
      boolean alwaysSpan) {
    return new BidiFormatter(rtlContext ? Direction.RTL : Direction.LTR,
        alwaysSpan);
  }

  /**
   * Factory for creating an instance of BidiFormatter given the context
   * direction. The default behavior of {@link #spanWrap} and its variations is
   * set to avoid span wrapping unless it's necessary ('dir' attribute needs to
   * be set).
   * 
   * @param contextDir The context direction. See an example of a simple use
   *          case at {@link #getInstance(boolean)}. Note: Direction.DEFAULT
   *          indicates unknown context direction. Try not to use it, since it
   *          is impossible to reset the direction back to the context when it
   *          is unknown
   */
  public static BidiFormatter getInstance(Direction contextDir) {
    return getInstance(contextDir, false);
  }

  /**
   * Factory for creating an instance of BidiFormatter given the context
   * direction and the desired span wrapping behavior (see below).
   * 
   * @param contextDir The context direction. See an example of a simple use
   *          case at {@link #getInstance(boolean)}. Note: Direction.DEFAULT
   *          indicates unknown context direction. Try not to use it, since it
   *          is impossible to reset the direction back to the context when it
   *          is unknown
   * @param alwaysSpan Whether {@link #spanWrap} (and its variations) should
   *          always use a 'span' tag, even when the input direction is neutral
   *          or matches the context, so that the DOM structure of the output
   *          does not depend on the combination of directions
   */
  public static BidiFormatter getInstance(Direction contextDir,
      boolean alwaysSpan) {
    return new BidiFormatter(contextDir, alwaysSpan);
  }

  private boolean alwaysSpan;
  private Direction contextDir;

  /**
   * @param contextDir The context direction
   * @param alwaysSpan Whether {@link #spanWrap} (and its variations) should
   *          always use a 'span' tag, even when the input direction is neutral
   *          or matches the context, so that the DOM structure of the output
   *          does not depend on the combination of directions
   */
  private BidiFormatter(Direction contextDir, boolean alwaysSpan) {
    this.contextDir = contextDir;
    this.alwaysSpan = alwaysSpan;
  }

  /**
   * Like {@link #dirAttr(String, boolean)}, but assumes {@code isHtml} is
   * false.
   * 
   * @param str String whose direction is to be estimated
   * @return "dir=rtl" for RTL text in non-RTL context; "dir=ltr" for LTR text
   *         in non-LTR context; else, the empty string.
   */
  public String dirAttr(String str) {
    return dirAttr(str, false);
  }

  /**
   * Returns "dir=ltr" or "dir=rtl", depending on {@code str}'s estimated
   * direction, if it is not the same as the context direction. Otherwise,
   * returns the empty string.
   * 
   * @param str String whose direction is to be estimated
   * @param isHtml Whether {@code str} is HTML / HTML-escaped
   * @return "dir=rtl" for RTL text in non-RTL context; "dir=ltr" for LTR text
   *         in non-LTR context; else, the empty string.
   */
  public String dirAttr(String str, boolean isHtml) {
    return knownDirAttr(BidiUtils.get().estimateDirection(str, isHtml));
  }

  /**
   * Returns "left" for RTL context direction. Otherwise (LTR or default /
   * unknown context direction) returns "right".
   */
  public String endEdge() {
    return contextDir == Direction.RTL ? Format.LEFT : Format.RIGHT;
  }

  /**
   * Like {@link #estimateDirection(String, boolean)}, but assumes {@code
   * isHtml} is false.
   * 
   * @param str String whose direction is to be estimated
   * @return {@code str}'s estimated overall direction
   */
  public Direction estimateDirection(String str) {
    return BidiUtils.get().estimateDirection(str);
  }

  /**
   * Estimates the direction of a string using the best known general-purpose
   * method, i.e. using relative word counts. Direction.DEFAULT return value
   * indicates completely neutral input.
   * 
   * @param str String whose direction is to be estimated
   * @param isHtml Whether {@code str} is HTML / HTML-escaped
   * @return {@code str}'s estimated overall direction
   */
  public Direction estimateDirection(String str, boolean isHtml) {
    return BidiUtils.get().estimateDirection(str, isHtml);
  }

  /**
   * @return Whether the span structure added by the formatter should be stable,
   *         i.e. spans added even when the direction does not need to be
   *         declared
   */
  public boolean getAlwaysSpan() {
    return alwaysSpan;
  }

  /**
   * @return The context direction
   */
  public Direction getContextDir() {
    return contextDir;
  }

  /**
   * @return Whether the context direction is RTL
   */
  public boolean isRtlContext() {
    return contextDir == Direction.RTL;
  }

  /**
   * Returns "dir=ltr" or "dir=rtl", depending on the given direction, if it is
   * not the same as the context direction. Otherwise, returns the empty string.
   * 
   * @param dir Given direction
   * @return "dir=rtl" for RTL text in non-RTL context; "dir=ltr" for LTR text
   *         in non-LTR context; else, the empty string.
   */
  public String knownDirAttr(Direction dir) {
    if (dir != contextDir) {
      return dir == Direction.LTR ? "dir=ltr" : dir == Direction.RTL
          ? "dir=rtl" : "";
    }
    return "";
  }

  /**
   * Returns the Unicode BiDi mark matching the context direction (LRM for LTR
   * context direction, RLM for RTL context direction), or the empty string for
   * default / unknown context direction.
   */
  public String mark() {
    return contextDir == Direction.LTR ? Format.LRM_STRING
        : contextDir == Direction.RTL ? Format.RLM_STRING : "";
  }

  /**
   * Like {@link #markAfter(String, boolean)}, but assumes {@code isHtml} is
   * false.
   * 
   * @param str String after which the mark may need to appear
   * @return LRM for RTL text in LTR context; RLM for LTR text in RTL context;
   *         else, the empty string.
   */
  public String markAfter(String str) {
    return markAfter(str, false);
  }

  /**
   * Returns a Unicode BiDi mark matching the context direction (LRM or RLM) if
   * either the direction or the exit direction of {@code str} is opposite to
   * the context direction. Otherwise returns the empty string.
   * 
   * @param str String after which the mark may need to appear
   * @param isHtml Whether {@code str} is HTML / HTML-escaped
   * @return LRM for RTL text in LTR context; RLM for LTR text in RTL context;
   *         else, the empty string.
   */
  public String markAfter(String str, boolean isHtml) {
    str = BidiUtils.get().stripHtmlIfNeeded(str, isHtml);
    return dirResetIfNeeded(str, BidiUtils.get().estimateDirection(str), false,
        true);
  }

  /**
   * Like {@link #spanWrap(String, boolean, boolean)}, but assumes {@code
   * isHtml} is false and {@code dirReset} is true.
   * 
   * @param str The input string
   * @return Input string after applying the above processing.
   */
  public String spanWrap(String str) {
    return spanWrap(str, false, true);
  }

  /**
   * Like {@link #spanWrap(String, boolean, boolean)}, but assumes {@code
   * dirReset} is true.
   * 
   * @param str The input string
   * @param isHtml Whether {@code str} is HTML / HTML-escaped
   * @return Input string after applying the above processing.
   */
  public String spanWrap(String str, boolean isHtml) {
    return spanWrap(str, isHtml, true);
  }

  /**
   * Formats a string of unknown direction for use in HTML output of the context
   * direction, so an opposite-direction string is neither garbled nor garbles
   * what follows it.
   * <p>
   * The algorithm: estimates the direction of input argument {@code str}. In
   * case its direction doesn't match the context direction, wraps it with a
   * 'span' tag and adds a "dir" attribute (either 'dir=rtl' or 'dir=ltr').
   * <p>
   * If {@code setAlwaysSpan(true)} was used, the input is always wrapped with
   * 'span', skipping just the dir attribute when it's not needed.
   * <p>
   * If {@code dirReset}, and if the overall direction or the exit direction of
   * {@code str} are opposite to the context direction, a trailing unicode BiDi
   * mark matching the context direction is appended (LRM or RLM).
   * <p>
   * If !{@code isHtml}, HTML-escapes {@code str} regardless of wrapping.
   * 
   * @param str The input string
   * @param isHtml Whether {@code str} is HTML / HTML-escaped
   * @param dirReset Whether to append a trailing unicode bidi mark matching the
   *          context direction, when needed, to prevent the possible garbling
   *          of whatever may follow {@code str}
   * @return Input string after applying the above processing.
   */
  public String spanWrap(String str, boolean isHtml, boolean dirReset) {
    Direction dir = BidiUtils.get().estimateDirection(str, isHtml);
    return spanWrapWithKnownDir(dir, str, isHtml, dirReset);
  }

  /**
   * Like {@link #spanWrapWithKnownDir(Direction, String, boolean, boolean)},
   * but assumes {@code isHtml} is false and {@code dirReset} is true.
   * 
   * @param dir {@code str}'s direction
   * @param str The input string
   * @return Input string after applying the above processing.
   */
  public String spanWrapWithKnownDir(Direction dir, String str) {
    return spanWrapWithKnownDir(dir, str, false, true);
  }

  /**
   * Like {@link #spanWrapWithKnownDir(Direction, String, boolean, boolean)},
   * but assumes {@code dirReset} is true.
   * 
   * @param dir {@code str}'s direction
   * @param str The input string
   * @param isHtml Whether {@code str} is HTML / HTML-escaped
   * @return Input string after applying the above processing.
   */
  public String spanWrapWithKnownDir(Direction dir, String str, boolean isHtml) {
    return spanWrapWithKnownDir(dir, str, isHtml, true);
  }

  /**
   * Formats a string of given direction for use in HTML output of the context
   * direction, so an opposite-direction string is neither garbled nor garbles
   * what follows it.
   * <p>
   * The algorithm: estimates the direction of input argument {@code str}. In
   * case its direction doesn't match the context direction, wraps it with a
   * 'span' tag and adds a "dir" attribute (either 'dir=rtl' or 'dir=ltr').
   * <p>
   * If {@code setAlwaysSpan(true)} was used, the input is always wrapped with
   * 'span', skipping just the dir attribute when it's not needed.
   * <p>
   * If {@code dirReset}, and if the overall direction or the exit direction of
   * {@code str} are opposite to the context direction, a trailing unicode BiDi
   * mark matching the context direction is appended (LRM or RLM).
   * <p>
   * If !{@code isHtml}, HTML-escapes {@code str} regardless of wrapping.
   * 
   * @param dir {@code str}'s direction
   * @param str The input string
   * @param isHtml Whether {@code str} is HTML / HTML-escaped
   * @param dirReset Whether to append a trailing unicode bidi mark matching the
   *          context direction, when needed, to prevent the possible garbling
   *          of whatever may follow {@code str}
   * @return Input string after applying the above processing.
   */
  public String spanWrapWithKnownDir(Direction dir, String str, boolean isHtml,
      boolean dirReset) {
    boolean dirCondition = dir != Direction.DEFAULT && dir != contextDir;
    String origStr = str;
    if (!isHtml) {
      str = htmlEscape(str);
    }

    StringBuilder result = new StringBuilder();
    if (alwaysSpan || dirCondition) {
      result.append("<span");
      if (dirCondition) {
        result.append(" ");
        result.append(dir == Direction.RTL ? "dir=rtl" : "dir=ltr");
      }
      result.append(">" + str + "</span>");
    } else {
      result.append(str);
    }
    // origStr is passed (more efficient when isHtml is false).
    result.append(dirResetIfNeeded(origStr, dir, isHtml, dirReset));
    return result.toString();
  }

  /**
   * Returns "right" for RTL context direction. Otherwise (LTR or default /
   * unknown context direction) returns "left".
   */
  public String startEdge() {
    return contextDir == Direction.RTL ? Format.RIGHT : Format.LEFT;
  }

  /**
   * Like {@link #unicodeWrap(String, boolean, boolean)}, but assumes {@code
   * isHtml} is false and {@code dirReset} is true.
   * 
   * @param str The input string
   * @return Input string after applying the above processing.
   */
  public String unicodeWrap(String str) {
    return unicodeWrap(str, false, true);
  }

  /**
   * Like {@link #unicodeWrap(String, boolean, boolean)}, but assumes {@code
   * dirReset} is true.
   * 
   * @param str The input string
   * @param isHtml Whether {@code str} is HTML / HTML-escaped
   * @return Input string after applying the above processing.
   */
  public String unicodeWrap(String str, boolean isHtml) {
    return unicodeWrap(str, isHtml, true);
  }

  /**
   * Formats a string of unknown direction for use in plain-text output of the
   * context direction, so an opposite-direction string is neither garbled nor
   * garbles what follows it. As opposed to {@link #spanWrap}, this makes use of
   * Unicode BiDi formatting characters. In HTML, its *only* valid use is inside
   * of elements that do not allow mark-up, e.g. an 'option' tag.
   * <p>
   * The algorithm: estimates the direction of input argument {@code str}. In
   * case it doesn't match the context direction, wraps it with Unicode BiDi
   * formatting characters: RLE+{@code str}+PDF for RTL text, or LRE+ {@code
   * str}+PDF for LTR text.
   * <p>
   * If {@code opt_dirReset}, and if the overall direction or the exit direction
   * of {@code str} are opposite to the context direction, a trailing unicode
   * BiDi mark matching the context direction is appended (LRM or RLM).
   * <p>
   * Does *not* do HTML-escaping regardless of the value of {@code isHtml}.
   * 
   * @param str The input string
   * @param isHtml Whether {@code str} is HTML / HTML-escaped
   * @param dirReset Whether to append a trailing unicode bidi mark matching the
   *          context direction, when needed, to prevent the possible garbling
   *          of whatever may follow {@code str}
   * @return Input string after applying the above processing.
   */
  public String unicodeWrap(String str, boolean isHtml, boolean dirReset) {
    Direction dir = BidiUtils.get().estimateDirection(str, isHtml);
    return unicodeWrapWithKnownDir(dir, str, isHtml, dirReset);
  }

  /**
   * Like {@link #unicodeWrapWithKnownDir(Direction, String, boolean, boolean)},
   * but assumes {@code isHtml} is false and {@code dirReset} is true.
   * 
   * @param dir {@code str}'s direction
   * @param str The input string
   * @return Input string after applying the above processing.
   */
  public String unicodeWrapWithKnownDir(Direction dir, String str) {
    return unicodeWrapWithKnownDir(dir, str, false, true);
  }

  /**
   * Like {@link #unicodeWrapWithKnownDir(Direction, String, boolean, boolean)},
   * but assumes {@code dirReset} is true.
   * 
   * @param dir {@code str}'s direction
   * @param str The input string
   * @param isHtml Whether {@code str} is HTML / HTML-escaped
   * @return Input string after applying the above processing.
   */
  public String unicodeWrapWithKnownDir(Direction dir, String str,
      boolean isHtml) {
    return unicodeWrapWithKnownDir(dir, str, isHtml, true);
  }

  /**
   * Formats a string of given direction for use in plain-text output of the
   * context direction, so an opposite-direction string is neither garbled nor
   * garbles what follows it. As opposed to {@link #spanWrapWithKnownDir}, this
   * makes use of unicode BiDi formatting characters. In HTML, its *only* valid
   * use is inside of elements that do not allow mark-up, e.g. an 'option' tag.
   * <p>
   * The algorithm: estimates the direction of input argument {@code str}. In
   * case it doesn't match the context direction, wraps it with Unicode BiDi
   * formatting characters: RLE+{@code str}+PDF for RTL text, or LRE+ {@code
   * str}+PDF for LTR text.
   * <p>
   * If {@code opt_dirReset}, and if the overall direction or the exit direction
   * of {@code str} are opposite to the context direction, a trailing unicode
   * BiDi mark matching the context direction is appended (LRM or RLM).
   * <p>
   * Does *not* do HTML-escaping regardless of the value of {@code isHtml}.
   * 
   * @param dir {@code str}'s direction
   * @param str The input string
   * @param isHtml Whether {@code str} is HTML / HTML-escaped
   * @param dirReset Whether to append a trailing unicode bidi mark matching the
   *          context direction, when needed, to prevent the possible garbling
   *          of whatever may follow {@code str}
   * @return Input string after applying the above processing.
   */
  public String unicodeWrapWithKnownDir(Direction dir, String str,
      boolean isHtml, boolean dirReset) {
    StringBuilder result = new StringBuilder();
    if (dir != Direction.DEFAULT && dir != contextDir) {
      result.append(dir == Direction.RTL ? Format.RLE : Format.LRE);
      result.append(str);
      result.append(Format.PDF);
    } else {
      result.append(str);
    }

    result.append(dirResetIfNeeded(str, dir, isHtml, dirReset));
    return result.toString();
  }

  /**
   * Returns a unicode BiDi mark matching the context direction (LRM or RLM) if
   * {@code dirReset}, and if the overall direction or the exit direction of
   * {@code str} are opposite to the context direction. Otherwise returns the
   * empty string.
   * 
   * @param str The input string
   * @param dir {@code str}'s overall direction
   * @param isHtml Whether {@code str} is HTML / HTML-escaped
   * @param dirReset Whether to perform the reset
   * @return A unicode BiDi mark or the empty string.
   */
  private String dirResetIfNeeded(String str, Direction dir, boolean isHtml,
      boolean dirReset) {
    // endsWithRtl and endsWithLtr are called only if needed (short-circuit).
    if (dirReset
        && ((contextDir == Direction.LTR &&
            (dir == Direction.RTL ||
             BidiUtils.get().endsWithRtl(str, isHtml))) ||
            (contextDir == Direction.RTL &&
            (dir == Direction.LTR ||
             BidiUtils.get().endsWithLtr(str, isHtml))))) {
      return contextDir == Direction.LTR ? Format.LRM_STRING
          : Format.RLM_STRING;
    } else {
      return "";
    }
  }

  private String htmlEscape(String str) {
    // TODO(tomerigo): use a proper escaper class once such exists.
    return str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
  }
}
