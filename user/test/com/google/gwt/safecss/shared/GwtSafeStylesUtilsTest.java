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
package com.google.gwt.safecss.shared;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Clear;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.TextAlign;
import com.google.gwt.dom.client.Style.TextDecoration;
import com.google.gwt.dom.client.Style.TextJustify;
import com.google.gwt.dom.client.Style.TextOverflow;
import com.google.gwt.dom.client.Style.TextTransform;
import com.google.gwt.dom.client.Style.WhiteSpace;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;

/**
 * GWT Unit tests for {@link SafeStylesUtils}.
 */
public class GwtSafeStylesUtilsTest extends GWTTestCase {

  static final String INVALID_STYLE_NAME = "contains;semicolon";

  static final String[] INVALID_STYLE_NAMES = {
      null, "", ";startsWithSemicolon", "endsWithSemicolon;", "contains;semicolon",
      "--starts-with-double-hyphen", "0starts-with-digit", "-0starts-with-hyphen-digit",
      "contains:colon"};

  static final String INVALID_STYLE_VALUE = "contains;semicolon";

  static final String[] INVALID_STYLE_VALUES = {
      null, "", "unmatched}close-bracket", "unmatched{open-bracket", "mismatched[bracket)",
      ";startsWithSemicolon", "endsWithSemicolon;", "contains;semicolon",
      "almost-escaped\\\\;semi-colon", "almost-escaped\\\\:colon", "unmatched'singlequote",
      "unmatched\"doublequote", "url(http://withUnmatched(Paren)", "url(http://unterminated",
      "end-in-escape-character\\"};

  static final String[] VALID_STYLE_NAMES = {
      "simple", "one-hyphen", "has-two-hyphens", "-starts-with-hyphen", "_startsWithUnderscore",
      "endsWithUnderscore_", "contains_underscore", "contains--double-hyphen", "contains-1-digit"};

  static final String[] VALID_STYLE_VALUES = {
      "simple", "one-hyphen", "has-two-hyphens", "-starts-with-hyphen", "[braces]",
      "curly{brackets}", "paren(thes)es", "(nested[pair]ables)",
      "(sibling)parenthesis{and[braces]}", "semicolon-in'single;quote'",
      "semicolon-in\"double;quote\"", "unmatched'in quote}'", "unmatched\"in [double quote\"",
      "escaped\\;semi-colon", "escaped\\:colon", "url(http://localhost)",
      "url('http://withSingleQuotes')", "url(\"http://withDoubleQuotes\")",
      "url(http://withSemiColon;)", "url(http://withUnmatchedBracket{[)",
      "url(http://withUnmatchedCloseBracket}])", "end-in-escaped-backslash\\\\", "u-near-end-u",
      "url-near-end-url", "absolute"};

  private static Boolean isAssertionEnabled;

  /**
   * Check if assertions are enabled.
   * 
   * @return true if enabled, false if not
   */
  static boolean isAssertionEnabled() {
    if (isAssertionEnabled == null) {
      try {
        assert false;
        isAssertionEnabled = false;
      } catch (AssertionError e) {
        isAssertionEnabled = true;
      }
    }
    return isAssertionEnabled;
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.safecss.SafeCss";
  }

  public void testForBackgroundImage() {
    SafeUri uri = UriUtils.fromSafeConstant("http://localhost");
    assertEquals("background-image:url(\"http://localhost\");", SafeStylesUtils.forBackgroundImage(
        uri).asString());
  }

  public void testForClear() {
    assertEquals("clear:both;", SafeStylesUtils.forClear(Clear.BOTH).asString());
    assertEquals("clear:left;", SafeStylesUtils.forClear(Clear.LEFT).asString());
    assertEquals("clear:none;", SafeStylesUtils.forClear(Clear.NONE).asString());
    assertEquals("clear:right;", SafeStylesUtils.forClear(Clear.RIGHT).asString());
  }

  public void testForDisplay() {
    assertEquals("display:none;", SafeStylesUtils.forDisplay(Display.NONE).asString());
  }
  
  public void testForTextAlign() {
    assertEquals("text-align:center;", SafeStylesUtils.forTextAlign(TextAlign.CENTER));
    assertEquals("text-align:justify;", SafeStylesUtils.forTextAlign(TextAlign.JUSTIFY));
    assertEquals("text-align:left;", SafeStylesUtils.forTextAlign(TextAlign.LEFT));
    assertEquals("text-align:right;", SafeStylesUtils.forTextAlign(TextAlign.RIGHT));
  }
  
  public void testForTextDecoration() {
    assertEquals("text-decoration:blink;", SafeStylesUtils.forTextDecoration(TextDecoration.BLINK));
    assertEquals("text-decoration:line-through;", SafeStylesUtils.forTextDecoration(TextDecoration.LINE_THROUGH));
    assertEquals("text-decoration:none;", SafeStylesUtils.forTextDecoration(TextDecoration.NONE));
    assertEquals("text-decoration:overline;", SafeStylesUtils.forTextDecoration(TextDecoration.OVERLINE));
    assertEquals("text-decoration:underline;", SafeStylesUtils.forTextDecoration(TextDecoration.UNDERLINE));
  }
  
  public void testForTextJustify() {
    assertEquals("text-justify:auto;", SafeStylesUtils.forTextJustify(TextJustify.AUTO));
    assertEquals("text-justify:distribute;", SafeStylesUtils.forTextJustify(TextJustify.DISTRIBUTE));
    assertEquals("text-justify:inter-cluster;", SafeStylesUtils.forTextJustify(TextJustify.INTER_CLUSTER));
    assertEquals("text-justify:inter-ideograph;", SafeStylesUtils.forTextJustify(TextJustify.INTER_IDEOGRAPH));
    assertEquals("text-justify:inter-word;", SafeStylesUtils.forTextJustify(TextJustify.INTER_WORD));
    assertEquals("text-justify:kashida;", SafeStylesUtils.forTextJustify(TextJustify.KASHIDA));
    assertEquals("text-justify:none;", SafeStylesUtils.forTextJustify(TextJustify.NONE));
  }
  
  public void testForTextOverflow() {
    assertEquals("text-overflow:clip;", SafeStylesUtils.forTextOverflow(TextOverflow.CLIP));
    assertEquals("text-overflow:ellipsis;", SafeStylesUtils.forTextOverflow(TextOverflow.ELLIPSIS));
  }
  
  public void testForTextTransform() {
    assertEquals("text-transform:capitalize;", SafeStylesUtils.forTextTransform(TextTransform.CAPITALIZE));
    assertEquals("text-transform:lowercase;", SafeStylesUtils.forTextTransform(TextTransform.LOWERCASE));
    assertEquals("text-transform:none;", SafeStylesUtils.forTextTransform(TextTransform.NONE));
    assertEquals("text-transform:uppercase;", SafeStylesUtils.forTextTransform(TextTransform.UPPERCASE));
  }

  public void testForWhiteSpace() {
    assertEquals(
        "white-space:normal;", SafeStylesUtils.forWhiteSpace(WhiteSpace.NORMAL).asString());
    assertEquals(
        "white-space:nowrap;", SafeStylesUtils.forWhiteSpace(WhiteSpace.NOWRAP).asString());
    assertEquals("white-space:pre;", SafeStylesUtils.forWhiteSpace(WhiteSpace.PRE).asString());
    assertEquals(
        "white-space:pre-line;", SafeStylesUtils.forWhiteSpace(WhiteSpace.PRE_LINE).asString());
    assertEquals(
        "white-space:pre-wrap;", SafeStylesUtils.forWhiteSpace(WhiteSpace.PRE_WRAP).asString());
  }

  public void testForZIndex() {
    assertEquals("z-index: 5;", SafeStylesUtils.forZIndex(5).asString());
  }

  public void testFromTrustedNameAndValue() {
    assertEquals("name:value;", SafeStylesUtils.fromTrustedNameAndValue("name", "value").asString());
    assertEquals("name-top:value;", SafeStylesUtils.fromTrustedNameAndValue("name-top", "value")
        .asString());
    assertEquals("-name-top:value;", SafeStylesUtils.fromTrustedNameAndValue("-name-top", "value")
        .asString());
    assertEquals("_name_:value;", SafeStylesUtils.fromTrustedNameAndValue("_name_", "value")
        .asString());

    assertEquals("name:1px solid red;", SafeStylesUtils.fromTrustedNameAndValue("name",
        "1px solid red").asString());
    assertEquals("name:url('test.png');", SafeStylesUtils.fromTrustedNameAndValue("name",
        "url('test.png')").asString());
    assertEquals("name:url(\"test.png\");", SafeStylesUtils.fromTrustedNameAndValue("name",
        "url(\"test.png\")").asString());
  }

  public void testFromTrustedNameAndValueInvalidName() {
    if (GWT.isProdMode()) {
      // fromTrustedNameAndValue only catches errors in dev mode.
      return;
    }

    for (String s : INVALID_STYLE_NAMES) {
      boolean caught = false;
      try {
        SafeStylesUtils.fromTrustedNameAndValue(s, "value");
      } catch (IllegalArgumentException e) {
        // Expected.
        caught = true;
      } catch (AssertionError e) {
        // Expected.
        caught = true;
      }
      if (!caught) {
        fail("Expected an exception for invalid style name: '" + s + "'");
      }
    }
  }

  public void testFromTrustedNameAndValueInvalidValue() {
    if (GWT.isProdMode()) {
      // fromTrustedNameAndValue only catches errors in dev mode.
      return;
    }

    boolean caught = false;
    for (String s : INVALID_STYLE_VALUES) {
      try {
        SafeStylesUtils.fromTrustedNameAndValue("name", s);
      } catch (IllegalArgumentException e) {
        // Expected.
        caught = true;
      } catch (AssertionError e) {
        // Expected.
        caught = true;
      }
      if (!caught) {
        fail("Expected an exception for invalid style value: '" + s + "'");
      }
    }
  }

  public void testFromTrustedNameAndValueValidName() {
    if (GWT.isProdMode()) {
      // fromTrustedNameAndValue only catches errors in dev mode.
      return;
    }

    for (String s : VALID_STYLE_NAMES) {
      try {
        SafeStyles styles = SafeStylesUtils.fromTrustedNameAndValue(s, "value");
        assertEquals(s + ":value;", styles);
      } catch (Exception e) {
        fail("Unexpected exception thrown for valid style name: '" + s + "'.\n" + e.getMessage());
      }
    }
  }

  public void testFromTrustedNameAndValueValidValue() {
    if (GWT.isProdMode()) {
      // fromTrustedNameAndValue only catches errors in dev mode.
      return;
    }

    for (String s : VALID_STYLE_VALUES) {
      try {
        SafeStyles styles = SafeStylesUtils.fromTrustedNameAndValue("name", s);
        assertEquals("name" + ":" + s + ";", styles);
      } catch (Exception e) {
        fail("Unexpected exception thrown for valid style value: '" + s + "'.\n" + e.getMessage());
      }
    }
  }

  public void testFromTrustedString() {
    assertEquals("name:value;", SafeStylesUtils.fromTrustedString("name:value;"));
  }
  
  private void assertEquals(String cssValue, SafeStyles safeStyles) {
    assertEquals(cssValue, safeStyles.asString());
  }
}
