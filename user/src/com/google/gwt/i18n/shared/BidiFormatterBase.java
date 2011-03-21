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
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

/**
 * Base class for {@link BidiFormatter} and {@link SafeHtmlBidiFormatter} that
 * contains their common implementation.
 */
public abstract class BidiFormatterBase {

  /**
   * Abstract factory class for BidiFormatterBase.
   * BidiFormatterBase subclasses will usually have a non-abstract inner Factory
   * class subclassed from this one, and use a static member of that class in
   * order to prevent the needless creation of objects. For example, see
   * {@link BidiFormatter}.
   */
  protected abstract static class Factory<T extends BidiFormatterBase> {
    private T[] instances;

    @SuppressWarnings("unchecked")
    public Factory() {
      instances = (T[]) new BidiFormatterBase[6];
    }

    public abstract T createInstance(Direction contextDir,
        boolean alwaysSpan);

    public T getInstance(Direction contextDir,
        boolean alwaysSpan) {
      int index = calculateIndex(contextDir, alwaysSpan);
      T formatter = instances[index];
      if (formatter == null) {
        formatter = createInstance(contextDir, alwaysSpan);
        instances[index] = formatter;
      }
      return formatter;
    }

    // Index should be in the range [0, 5].
    private int calculateIndex(Direction contextDir, boolean alwaysSpan) {
      int i = contextDir == Direction.LTR ? 0 : contextDir == Direction.RTL ? 1
          : 2;
      if (alwaysSpan) {
        i += 3;
      }
      return i;
    }
  }

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

  private boolean alwaysSpan;
  private Direction contextDir;

  protected BidiFormatterBase(Direction contextDir, boolean alwaysSpan) {
    this.contextDir = contextDir;
    this.alwaysSpan = alwaysSpan;
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
   * Returns whether the span structure added by the formatter should be stable,
   * i.e., spans added even when the direction does not need to be declared.
   */
  public boolean getAlwaysSpan() {
    return alwaysSpan;
  }

  /**
   * Returns the context direction.
   */
  public Direction getContextDir() {
    return contextDir;
  }

  /**
   * Returns whether the context direction is RTL.
   */
  public boolean isRtlContext() {
    return contextDir == Direction.RTL;
  }

  /**
   * @see BidiFormatter#dirAttr(String, boolean)
   *
   * @param str String whose direction is to be estimated
   * @param isHtml Whether {@code str} is HTML / HTML-escaped
   * @return "dir=rtl" for RTL text in non-RTL context; "dir=ltr" for LTR text
   *         in non-LTR context; else, the empty string.
   */
  protected String dirAttrBase(String str, boolean isHtml) {
    return knownDirAttrBase(BidiUtils.get().estimateDirection(str, isHtml));
  }

  /**
   * @see BidiFormatter#endEdge
   */
  protected String endEdgeBase() {
    return contextDir == Direction.RTL ? Format.LEFT : Format.RIGHT;
  }

  /**
   * @see BidiFormatter#knownDirAttr(HasDirection.Direction)
   *
   * @param dir Given direction
   * @return "dir=rtl" for RTL text in non-RTL context; "dir=ltr" for LTR text
   *         in non-LTR context; else, the empty string.
   */
  protected String knownDirAttrBase(Direction dir) {
    if (dir != contextDir) {
      return dir == Direction.LTR ? "dir=ltr" : dir == Direction.RTL
          ? "dir=rtl" : "";
    }
    return "";
  }

  /**
   * @see BidiFormatter#markAfter(String, boolean)
   *
   * @param str String after which the mark may need to appear
   * @param isHtml Whether {@code str} is HTML / HTML-escaped
   * @return LRM for RTL text in LTR context; RLM for LTR text in RTL context;
   *         else, the empty string.
   */
  protected String markAfterBase(String str, boolean isHtml) {
    str = BidiUtils.get().stripHtmlIfNeeded(str, isHtml);
    return dirResetIfNeeded(str, BidiUtils.get().estimateDirection(str), false,
        true);
  }

  /**
   * @see BidiFormatter#mark()
   */
  protected String markBase() {
    return contextDir == Direction.LTR ? Format.LRM_STRING
        : contextDir == Direction.RTL ? Format.RLM_STRING : "";
  }

  /**
   * @see BidiFormatter#spanWrap(String, boolean, boolean)
   *
   * @param str The input string
   * @param isHtml Whether {@code str} is HTML / HTML-escaped
   * @param dirReset Whether to append a trailing unicode bidi mark matching the
   *          context direction, when needed, to prevent the possible garbling
   *          of whatever may follow {@code str}
   * @return Input string after applying the above processing.
   */
  protected String spanWrapBase(String str, boolean isHtml, boolean dirReset) {
    Direction dir = BidiUtils.get().estimateDirection(str, isHtml);
    return spanWrapWithKnownDirBase(dir, str, isHtml, dirReset);
  }

  /**
   * @see BidiFormatter#spanWrapWithKnownDir(HasDirection.Direction, String, boolean, boolean)
   *
   * @param dir {@code str}'s direction
   * @param str The input string
   * @param isHtml Whether {@code str} is HTML / HTML-escaped
   * @param dirReset Whether to append a trailing unicode bidi mark matching the
   *          context direction, when needed, to prevent the possible garbling
   *          of whatever may follow {@code str}
   * @return Input string after applying the above processing.
   */
  protected String spanWrapWithKnownDirBase(Direction dir, String str,
      boolean isHtml, boolean dirReset) {
    boolean dirCondition = dir != Direction.DEFAULT && dir != contextDir;
    String origStr = str;
    if (!isHtml) {
      str = SafeHtmlUtils.htmlEscape(str);
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
   * @see BidiFormatter#startEdge
   */
  protected String startEdgeBase() {
    return contextDir == Direction.RTL ? Format.RIGHT : Format.LEFT;
  }

  /**
   * @see BidiFormatter#unicodeWrap(String, boolean, boolean)
   *
   * @param str The input string
   * @param isHtml Whether {@code str} is HTML / HTML-escaped
   * @param dirReset Whether to append a trailing unicode bidi mark matching the
   *          context direction, when needed, to prevent the possible garbling
   *          of whatever may follow {@code str}
   * @return Input string after applying the above processing.
   */
  protected String unicodeWrapBase(String str, boolean isHtml,
      boolean dirReset) {
    Direction dir = BidiUtils.get().estimateDirection(str, isHtml);
    return unicodeWrapWithKnownDirBase(dir, str, isHtml, dirReset);
  }

  /**
   * @see BidiFormatter#unicodeWrapWithKnownDir(HasDirection.Direction, String, boolean, boolean)
   *
   * @param dir {@code str}'s direction
   * @param str The input string
   * @param isHtml Whether {@code str} is HTML / HTML-escaped
   * @param dirReset Whether to append a trailing unicode bidi mark matching the
   *          context direction, when needed, to prevent the possible garbling
   *          of whatever may follow {@code str}
   * @return Input string after applying the above processing.
   */
  protected String unicodeWrapWithKnownDirBase(Direction dir, String str,
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
}
