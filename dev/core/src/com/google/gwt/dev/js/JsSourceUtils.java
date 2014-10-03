/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.dev.js;

import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utils for handling JS source.
 */
public class JsSourceUtils {
  // Regular expression that maches a JS string.
  @VisibleForTesting
  static final Pattern JS_STRING_PATTERN = Pattern.compile("(\"([^\"\\\\]|(\\\\.))*\")");

  // A regular expression that matches a single line comment
  private static final Pattern OPEN_TRAILING_BLOCK_COMMENT_PATTERN =
      Pattern.compile("/\\*([^*]|\\*[^/])*");

  // Regular expression that maches a C-style a comment of the form /* .... */.
  @VisibleForTesting
  static final Pattern BLOCK_COMMENT_PATTERN =
      Pattern.compile(OPEN_TRAILING_BLOCK_COMMENT_PATTERN + "\\*/");

  // Regular expression that maches a JS regular expression.
  @VisibleForTesting
  static final Pattern JS_REG_EXP_PATTERN = Pattern.compile("(/([^/\\\\]|\\\\.)+/([mgi]{0,3}))");

  // A regular expression that matches the part of a JS program line that is not part of
  // either a line comment (//) or an unfinished block comment (/*)
  private static final Pattern REGULAR_TEXT_PATTERN = Pattern.compile("("
      +   "[^\"/]|(/[^/*])|"          // any sequence of that does not contain ", // or /*
      + BLOCK_COMMENT_PATTERN + "|"
      + JS_STRING_PATTERN + "|"
      + JS_REG_EXP_PATTERN
      + ")*"                          // any sequence of the former.
  );

  // A regular expression that matches a single line comment
  private static final Pattern TRAILING_SINGLE_LINE_COMMENT_PATTERN = Pattern.compile("//.*");

  /**
   * Pattern that matches lines that end up with a single line comment (// ....), while making sure
   * that // can appear inside strings. DOES NOT HANDLE // inside multiline comments.
   * <p>
   * Group 7 will contain the whole single line comment if matched.
   *
   */
  private static final Pattern matchCommentToEndOfLine = Pattern.compile(REGULAR_TEXT_PATTERN +
      "(?<trailing>" +
      "(?<single>" + TRAILING_SINGLE_LINE_COMMENT_PATTERN + ")" + "|" +
      "(?<block>" + OPEN_TRAILING_BLOCK_COMMENT_PATTERN + "))");

  /**
   * Transforms a multiline JS snippet into the equivalent single line program. This is
   * necessary to prepend to the output JS without affecting the (already computed) sourcemap.
   */
  public static String condenseAndRemoveLineBreaks(String snippet) {
    StringBuilder oneLinerBuilder = new StringBuilder();
    boolean inMultiline = false;
    for (String line : snippet.split("\n")) {
      if (inMultiline) {
        // strip up to */ if any
        int endBlockCommentPosition = line.indexOf("*/");
        if (endBlockCommentPosition == -1) {
          // still in multiline
          continue;
        }
        line = line.substring(endBlockCommentPosition + 2);
        inMultiline = false;
      }
      Matcher matcher = matchCommentToEndOfLine.matcher(line);
      if (matcher.matches()) {
        assert matcher.group("trailing") != null;
        oneLinerBuilder.append(
            line.substring(0, line.length() - matcher.group("trailing").length()));
        inMultiline = matcher.group("block") != null;
      } else {
        oneLinerBuilder.append(line);
      }
      oneLinerBuilder.append(" ");
    }

    // Remove all characters that can be interpreted by the browser as a line break.
    return oneLinerBuilder.toString().replaceAll("[\\v\\r\\n]", " ");
  }

  private JsSourceUtils() {
  }
}
