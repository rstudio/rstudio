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

import static com.google.gwt.i18n.shared.BidiFormatterBase.Format.*;

import com.google.gwt.i18n.client.HasDirection.Direction;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

import junit.framework.TestCase;

/**
 * Unit tests for {@link SafeHtmlBidiFormatter}.
 * Tests only methods added in {@code SafeHtmlBidiFormatter}, i.e.
 * instantiating and method overloading.
 */
public class SafeHtmlBidiFormatterTest extends TestCase {

  static final Direction DEFAULT = Direction.DEFAULT;
  static final Direction LTR = Direction.LTR;
  static final Direction RTL = Direction.RTL;

  String en = "abba";
  String he = "\u05e0\u05e1";
  String longEn = "abba sabba gabba ";
  String htmlTag = "<br/>";
  String htmlTagEscaped = "&lt;br/&gt;";
  SafeHtmlBidiFormatter ltrFormatter = SafeHtmlBidiFormatter.getInstance(LTR,
      false);

  public void testDirAttr() {
    assertEquals("dirAttr(SafeHtml)", "dir=rtl",
        ltrFormatter.dirAttr(toSafeHtml(he)).asString());
    assertEquals("dirAttr(String)", "dir=rtl",
        ltrFormatter.dirAttr(he).asString());
  }

  public void testEndEdge() {
    assertEquals(toSafeHtml(RIGHT), ltrFormatter.endEdge());
  }

  public void testGetInstance() {
    // Check contextDir
    assertEquals(LTR, SafeHtmlBidiFormatter.getInstance(false).getContextDir());
    assertEquals(RTL, SafeHtmlBidiFormatter.getInstance(true).getContextDir());
    assertEquals(LTR, SafeHtmlBidiFormatter.getInstance(LTR).getContextDir());
    assertEquals(RTL, SafeHtmlBidiFormatter.getInstance(RTL).getContextDir());
    assertEquals(DEFAULT,
        SafeHtmlBidiFormatter.getInstance(DEFAULT).getContextDir());

    // Check alwaysSpan
    assertEquals(true,
        SafeHtmlBidiFormatter.getInstance(false, true).getAlwaysSpan());
    assertEquals(false,
        SafeHtmlBidiFormatter.getInstance(false, false).getAlwaysSpan());
  }

  public void testKnownDirAttr() {
    assertEquals("dir=rtl", ltrFormatter.knownDirAttr(RTL).asString());
  }

  public void testMark() {
    assertEquals(toSafeHtml(LRM_STRING), ltrFormatter.mark());
  }

  public void testMarkAfter() {
    String text = longEn + he;
    assertEquals("markAfter(SafeHtml)", LRM_STRING, ltrFormatter.markAfter(
        toSafeHtml(text)).asString());
    assertEquals("markAfter(String)", LRM_STRING,
        ltrFormatter.markAfter(text).asString());
  }

  public void testSafeHtmlEstimateDirection() {
    assertEquals(LTR, ltrFormatter.estimateDirection(toSafeHtml(he
        + "<some verbose tag/>")));
  }

  public void testSpanWrap() {
    String text = he + htmlTag;
    String baseResult = "<span dir=rtl>" + he + htmlTagEscaped + "</span>";
    assertEquals("spanWrap(SafeHtml)", baseResult + LRM, ltrFormatter.spanWrap(
        toSafeHtml(text)).asString());
    assertEquals("spanWrap(String)", baseResult + LRM, ltrFormatter.spanWrap(
        text).asString());
    assertEquals("spanWrap(SafeHtml, boolean)", baseResult,
        ltrFormatter.spanWrap(toSafeHtml(text), false).asString());
    assertEquals("spanWrap(String, boolean)", baseResult,
        ltrFormatter.spanWrap(text, false).asString());
  }

  public void testSpanWrapWithKnownDir() {
    String text = en + htmlTag;
    String baseResult = "<span dir=rtl>" + en + htmlTagEscaped + "</span>";
    assertEquals("spanWrapWithKnownDir(Direction, SafeHtml)", baseResult + LRM,
        ltrFormatter.spanWrapWithKnownDir(RTL, toSafeHtml(text)).asString());
    assertEquals("spanWrapWithKnownDir(Direction, String)", baseResult + LRM,
        ltrFormatter.spanWrapWithKnownDir(RTL, text).asString());
    assertEquals("spanWrapWithKnownDir(Direction, SafeHtml, boolean)",
        baseResult, ltrFormatter.spanWrapWithKnownDir(RTL, toSafeHtml(text),
            false).asString());
    assertEquals("spanWrapWithKnownDir(Direction, String, boolean)",
        baseResult,
        ltrFormatter.spanWrapWithKnownDir(RTL, text, false).asString());
  }

  public void testStartEdge() {
    assertEquals(toSafeHtml(LEFT), ltrFormatter.startEdge());
  }

  public void testUnicodeWrap() {
    String text = he + htmlTag;
    String baseResult = RLE + he + htmlTagEscaped + PDF;
    assertEquals("unicodeWrap(SafeHtml)", baseResult + LRM,
        ltrFormatter.unicodeWrap(toSafeHtml(text)).asString());
    assertEquals("unicodeWrap(String)", baseResult + LRM,
        ltrFormatter.unicodeWrap(text).asString());
    assertEquals("unicodeWrap(SafeHtml, boolean)", baseResult,
        ltrFormatter.unicodeWrap(toSafeHtml(text), false).asString());
    assertEquals("unicodeWrap(String, boolean)", baseResult,
        ltrFormatter.unicodeWrap(text, false).asString());
  }

  public void testUnicodeWrapWithKnownDir() {
    String text = en + htmlTag;
    String baseResult = RLE + en + htmlTagEscaped + PDF;
    assertEquals("unicodeWrap(SafeHtml)", baseResult + LRM,
        ltrFormatter.unicodeWrapWithKnownDir(RTL, toSafeHtml(text)).asString());
    assertEquals("unicodeWrap(String)", baseResult + LRM,
        ltrFormatter.unicodeWrapWithKnownDir(RTL, text).asString());
    assertEquals(
        "unicodeWrap(SafeHtml, boolean)",
        baseResult, ltrFormatter.unicodeWrapWithKnownDir(RTL, toSafeHtml(text),
        false).asString());
    assertEquals("unicodeWrap(String, boolean)", baseResult,
        ltrFormatter.unicodeWrapWithKnownDir(RTL, text, false).asString());
  }

  private SafeHtml toSafeHtml(String untrustedString) {
    return SafeHtmlUtils.fromString(untrustedString);
  }
}
