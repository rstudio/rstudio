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
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

import java.util.HashMap;

/**
 * A wrapper to {@link BidiFormatter} whose methods return {@code SafeHtml}
 * instead of {@code String}.
 */
public class SafeHtmlBidiFormatter extends BidiFormatterBase {

  static class Factory extends BidiFormatterBase.Factory<SafeHtmlBidiFormatter> {
    @Override
    public SafeHtmlBidiFormatter createInstance(Direction contextDir,
        boolean alwaysSpan) {
      return new SafeHtmlBidiFormatter(contextDir, alwaysSpan);
    }
  }

  private static Factory factory = new Factory();

  private static HashMap<String, SafeHtml> cachedSafeHtmlValues = null;

  /**
   * Factory for creating an instance of SafeHtmlBidiFormatter given the context
   * direction. The default behavior of {@link #spanWrap} and its variations is
   * set to avoid span wrapping unless it's necessary ('dir' attribute needs to
   * be set).
   *
   * @param rtlContext Whether the context direction is RTL.
   *          In one simple use case, the context direction would simply be the
   *          locale direction, which can be retrieved using
   *          {@code LocaleInfo.getCurrentLocale().isRTL()}
   */
  public static SafeHtmlBidiFormatter getInstance(boolean rtlContext) {
    return getInstance(rtlContext, false);
  }

  /**
   * Factory for creating an instance of SafeHtmlBidiFormatter given the context
   * direction and the desired span wrapping behavior (see below).
   *
   * @param rtlContext Whether the context direction is RTL. See an example of
   *          a simple use case at {@link #getInstance(boolean)}
   * @param alwaysSpan Whether {@link #spanWrap} (and its variations) should
   *          always use a 'span' tag, even when the input direction is neutral
   *          or matches the context, so that the DOM structure of the output
   *          does not depend on the combination of directions
   */
  public static SafeHtmlBidiFormatter getInstance(boolean rtlContext,
      boolean alwaysSpan) {
    return getInstance(rtlContext ? Direction.RTL : Direction.LTR, alwaysSpan);
  }

  /**
   * Factory for creating an instance of SafeHtmlBidiFormatter given the context
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
  public static SafeHtmlBidiFormatter getInstance(Direction contextDir) {
    return getInstance(contextDir, false);
  }

  /**
   * Factory for creating an instance of SafeHtmlBidiFormatter given the context
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
  public static SafeHtmlBidiFormatter getInstance(Direction contextDir,
      boolean alwaysSpan) {
    return factory.getInstance(contextDir, alwaysSpan);
  }

  /**
   * Factory for creating an instance of SafeHtmlBidiFormatter whose context
   * direction matches the current locale's direction. The default behavior of
   * {@link #spanWrap} and its variations is set to avoid span wrapping unless
   * it's necessary ('dir' attribute needs to be set).
   */
  public static SafeHtmlBidiFormatter getInstanceForCurrentLocale() {
    return getInstanceForCurrentLocale(false);
  }

  /**
   * Factory for creating an instance of SafeHtmlBidiFormatter whose context
   * direction matches the current locale's direction, and given the desired
   * span wrapping behavior (see below).
   *
   * @param alwaysSpan Whether {@link #spanWrap} (and its variations) should
   *          always use a 'span' tag, even when the input direction is neutral
   *          or matches the context, so that the DOM structure of the output
   *          does not depend on the combination of directions
   */
  public static SafeHtmlBidiFormatter getInstanceForCurrentLocale(
      boolean alwaysSpan) {
    return getInstance(LocaleInfo.getCurrentLocale().isRTL(), alwaysSpan);
  }

  /**
   * @param contextDir The context direction
   * @param alwaysSpan Whether {@link #spanWrap} (and its variations) should
   *          always use a 'span' tag, even when the input direction is neutral
   *          or matches the context, so that the DOM structure of the output
   *          does not depend on the combination of directions
   */
  private SafeHtmlBidiFormatter(Direction contextDir, boolean alwaysSpan) {
    super(contextDir, alwaysSpan);
  }

  /**
   * @see BidiFormatter#dirAttr(String, boolean)
   *
   * @param html Html whose direction is to be estimated
   * @return "dir=rtl" for RTL text in non-RTL context; "dir=ltr" for LTR text
   *         in non-LTR context; else, the empty string.
   */
  public SafeHtml dirAttr(SafeHtml html) {
    return cachedSafeHtml(dirAttrBase(html.asString(), true));
  }

  /**
   * @see BidiFormatter#dirAttr
   *
   * @param str String whose direction is to be estimated
   * @return "dir=rtl" for RTL text in non-RTL context; "dir=ltr" for LTR text
   *         in non-LTR context; else, the empty string.
   */
  public SafeHtml dirAttr(String str) {
    return cachedSafeHtml(dirAttrBase(str, false));
  }

  /**
   * Returns "left" for RTL context direction. Otherwise (LTR or default /
   * unknown context direction) returns "right".
   */
  public SafeHtml endEdge() {
    return cachedSafeHtml(endEdgeBase());
  }

  /**
   * @see BidiFormatterBase#estimateDirection(String, boolean)
   *
   * @param html Html whose direction is to be estimated
   * @return {@code html}'s estimated overall direction
   */
  public Direction estimateDirection(SafeHtml html) {
    return estimateDirection(html.asString(), true);
  }

  /**
   * @see BidiFormatter#knownDirAttr
   *
   * @param dir Given direction
   * @return "dir=rtl" for RTL text in non-RTL context; "dir=ltr" for LTR text
   *         in non-LTR context; else, the empty string.
   */
  public SafeHtml knownDirAttr(Direction dir) {
    return cachedSafeHtml(knownDirAttrBase(dir));
  }

  /**
   * @see BidiFormatter#mark
   */
  public SafeHtml mark() {
    return cachedSafeHtml(markBase());
  }

  /**
   * @see BidiFormatter#markAfter
   *
   * @param html Html after which the mark may need to appear
   * @return LRM for RTL text in LTR context; RLM for LTR text in RTL context;
   *         else, the empty string.
   */
  public SafeHtml markAfter(SafeHtml html) {
    return cachedSafeHtml(markAfterBase(html.asString(), true));
  }

  /**
   * @see BidiFormatter#markAfter
   *
   * @param str String after which the mark may need to appear
   * @return LRM for RTL text in LTR context; RLM for LTR text in RTL context;
   *         else, the empty string.
   */
  public SafeHtml markAfter(String str) {
    return cachedSafeHtml(markAfterBase(str, false));
  }

  /**
   * @see BidiFormatter#spanWrap(String, boolean)
   *
   * @param html The input html
   * @return Input html after applying the above processing.
   */
  public SafeHtml spanWrap(SafeHtml html) {
    return spanWrap(html, true);
  }

  /**
   * @see BidiFormatter#spanWrap(String, boolean, boolean)
   *
   * @param html The input html
   * @param dirReset Whether to append a trailing unicode bidi mark matching the
   *          context direction, when needed, to prevent the possible garbling
   *          of whatever may follow {@code html}
   * @return Input html after applying the above processing.
   */
  public SafeHtml spanWrap(SafeHtml html, boolean dirReset) {
    return SafeHtmlUtils.fromTrustedString(
        spanWrapBase(html.asString(), true, dirReset));
  }

  /**
   * @see BidiFormatter#spanWrap(String)
   *
   * @param str The input string
   * @return Input string after applying the above processing.
   */
  public SafeHtml spanWrap(String str) {
    return spanWrap(str, true);
  }

  /**
   * @see BidiFormatter#spanWrap(String, boolean, boolean)
   *
   * @param str The input string
   * @param dirReset Whether to append a trailing unicode bidi mark matching the
   *          context direction, when needed, to prevent the possible garbling
   *          of whatever may follow {@code str}
   * @return Input string after applying the above processing.
   */
  public SafeHtml spanWrap(String str, boolean dirReset) {
    // This is safe since spanWrapBase escapes plain-text input.
    return SafeHtmlUtils.fromTrustedString(spanWrapBase(str, false, dirReset));
  }

  /**
   * @see BidiFormatter#spanWrapWithKnownDir(
   * com.google.gwt.i18n.client.HasDirection.Direction, String, boolean)
   *
   * @param dir {@code str}'s direction
   * @param html The input html
   * @return Input html after applying the above processing.
   */
  public SafeHtml spanWrapWithKnownDir(Direction dir, SafeHtml html) {
    return spanWrapWithKnownDir(dir, html, true);
  }

  /**
   * @see BidiFormatter#spanWrapWithKnownDir(
   * com.google.gwt.i18n.client.HasDirection.Direction, String, boolean, boolean)
   *
   * @param dir {@code html}'s direction
   * @param html The input html
   * @param dirReset Whether to append a trailing unicode bidi mark matching the
   *          context direction, when needed, to prevent the possible garbling
   *          of whatever may follow {@code html}
   * @return Input html after applying the above processing.
   */
  public SafeHtml spanWrapWithKnownDir(Direction dir, SafeHtml html,
      boolean dirReset) {
    return SafeHtmlUtils.fromTrustedString(
        spanWrapWithKnownDirBase(dir, html.asString(), true, dirReset));
  }

  /**
   * @see BidiFormatter#spanWrapWithKnownDir(
   * com.google.gwt.i18n.client.HasDirection.Direction, String)
   *
   * @param dir {@code str}'s direction
   * @param str The input string
   * @return Input string after applying the above processing.
   */
  public SafeHtml spanWrapWithKnownDir(Direction dir, String str) {
    return spanWrapWithKnownDir(dir, str, true);
  }

  /**
   * @see BidiFormatter#spanWrapWithKnownDir(
   * com.google.gwt.i18n.client.HasDirection.Direction, String, boolean, boolean)
   *
   * @param dir {@code str}'s direction
   * @param str The input string
   * @param dirReset Whether to append a trailing unicode bidi mark matching the
   *          context direction, when needed, to prevent the possible garbling
   *          of whatever may follow {@code str}
   * @return Input string after applying the above processing.
   */
  public SafeHtml spanWrapWithKnownDir(Direction dir, String str,
      boolean dirReset) {
    // This is safe since spanWrapWithKnownDirBase escapes plain-text input.
    return SafeHtmlUtils.fromTrustedString(
        spanWrapWithKnownDirBase(dir, str, false, dirReset));
  }

  /**
   * Returns "right" for RTL context direction. Otherwise (LTR or default /
   * unknown context direction) returns "left".
   */
  public SafeHtml startEdge() {
    return cachedSafeHtml(startEdgeBase());
  }

  /**
   * @see BidiFormatter#unicodeWrap(String, boolean)
   *
   * @param html The input html
   * @return Input html after applying the above processing.
   */
  public SafeHtml unicodeWrap(SafeHtml html) {
    return unicodeWrap(html, true);
  }

  /**
   * @see BidiFormatter#unicodeWrap(String, boolean, boolean)
   *
   * @param html The input html
   * @param dirReset Whether to append a trailing unicode bidi mark matching the
   *          context direction, when needed, to prevent the possible garbling
   *          of whatever may follow {@code html}
   * @return Input html after applying the above processing.
   */
  public SafeHtml unicodeWrap(SafeHtml html, boolean dirReset) {
    return SafeHtmlUtils.fromTrustedString(
        unicodeWrapBase(html.asString(), true, dirReset));
  }

  /**
   * @see BidiFormatter#unicodeWrap(String)
   *
   * @param str The input string
   * @return Input string after applying the above processing.
   */
  public SafeHtml unicodeWrap(String str) {
    return unicodeWrap(str, true);
  }

  /**
   * @see BidiFormatter#unicodeWrap(String, boolean, boolean)
   *
   * @param str The input string
   * @param dirReset Whether to append a trailing unicode bidi mark matching the
   *          context direction, when needed, to prevent the possible garbling
   *          of whatever may follow {@code str}
   * @return Input string after applying the above processing.
   */
  public SafeHtml unicodeWrap(String str, boolean dirReset) {
    // unicodeWrapBase does not HTML-escape, so its return value is not trusted.
    return SafeHtmlUtils.fromString(unicodeWrapBase(str, false, dirReset));
  }

  /**
   * @see BidiFormatter#unicodeWrapWithKnownDir(
   * com.google.gwt.i18n.client.HasDirection.Direction, String, boolean)
   *
   * @param dir {@code html}'s direction
   * @param html The input html
   * @return Input html after applying the above processing.
   */
  public SafeHtml unicodeWrapWithKnownDir(Direction dir, SafeHtml html) {
    return unicodeWrapWithKnownDir(dir, html, true);
  }

  /**
   * @see BidiFormatter#unicodeWrapWithKnownDir(
   * com.google.gwt.i18n.client.HasDirection.Direction, String, boolean, boolean)
   *
   * @param dir {@code html}'s direction
   * @param html The input html
   * @param dirReset Whether to append a trailing unicode bidi mark matching the
   *          context direction, when needed, to prevent the possible garbling
   *          of whatever may follow {@code html}
   * @return Input html after applying the above processing.
   */
  public SafeHtml unicodeWrapWithKnownDir(Direction dir, SafeHtml html,
      boolean dirReset) {
    return SafeHtmlUtils.fromTrustedString(
        unicodeWrapWithKnownDirBase(dir, html.asString(), true, dirReset));
  }

  /**
   * @see BidiFormatter#unicodeWrapWithKnownDir(
   * com.google.gwt.i18n.client.HasDirection.Direction, String)
   *
   * @param dir {@code str}'s direction
   * @param str The input string
   * @return Input string after applying the above processing.
   */
  public SafeHtml unicodeWrapWithKnownDir(Direction dir, String str) {
    return unicodeWrapWithKnownDir(dir, str, true);
  }

  /**
   * @see BidiFormatter#unicodeWrapWithKnownDir(
   * com.google.gwt.i18n.client.HasDirection.Direction, String, boolean, boolean)
   *
   * @param dir {@code str}'s direction
   * @param str The input string
   * @param dirReset Whether to append a trailing unicode bidi mark matching the
   *          context direction, when needed, to prevent the possible garbling
   *          of whatever may follow {@code str}
   * @return Input string after applying the above processing.
   */
  public SafeHtml unicodeWrapWithKnownDir(Direction dir, String str,
      boolean dirReset) {
    /*
     * unicodeWrapWithKnownDirBase does not HTML-escape, so its return value is
     * not trusted.
     */
    return SafeHtmlUtils.fromString(
        unicodeWrapWithKnownDirBase(dir, str, false, dirReset));
  }

  /**
   * Converts an input string into a SafeHtml. Input String must be safe (see
   * {@link com.google.gwt.safehtml.shared.SafeHtml}).
   * Implementation: first, tries to find the input in the static map,
   * {@link #cachedSafeHtmlValues}. If not found, creates a SafeHtml instance
   * for the string and adds it to the map.
   *
   * @param str String to search for. Must be safe (see
   * {@link com.google.gwt.safehtml.shared.SafeHtml}).
   *
   * @return Input as SafeHtml
   */
  private SafeHtml cachedSafeHtml(String str) {
    if (cachedSafeHtmlValues == null) {
      cachedSafeHtmlValues = new HashMap<String, SafeHtml>();
    }
    SafeHtml entry = cachedSafeHtmlValues.get(str);
    if (entry == null) {
      entry = SafeHtmlUtils.fromString(str);
      cachedSafeHtmlValues.put(str, entry);
    }
    return entry;
  }
}
