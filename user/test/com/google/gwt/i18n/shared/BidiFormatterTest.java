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

import static com.google.gwt.i18n.shared.BidiFormatterBase.Format.LEFT;
import static com.google.gwt.i18n.shared.BidiFormatterBase.Format.LRM;
import static com.google.gwt.i18n.shared.BidiFormatterBase.Format.LRM_STRING;
import static com.google.gwt.i18n.shared.BidiFormatterBase.Format.PDF;
import static com.google.gwt.i18n.shared.BidiFormatterBase.Format.RIGHT;
import static com.google.gwt.i18n.shared.BidiFormatterBase.Format.RLE;

import com.google.gwt.i18n.client.HasDirection.Direction;

import junit.framework.TestCase;

/**
 * Unit tests for {@link BidiFormatter}.
 * Tests only methods added in {@code BidiFormatter}, i.e. instantiating and
 * method overloading.
 */
public class BidiFormatterTest extends TestCase {

  static final Direction DEFAULT = Direction.DEFAULT;
  static final Direction LTR = Direction.LTR;
  static final Direction RTL = Direction.RTL;

  String en = "abba";
  String he = "\u05e0\u05e1";
  String longEn = "abba sabba gabba ";
  String longHtmlTag = "<some nasty nasty nasty tag/>";
  BidiFormatter ltrFormatter = BidiFormatter.getInstance(LTR, false);

  public void testDirAttr() {
    assertEquals("dirAttr(String)", "dir=rtl", ltrFormatter.dirAttr(he));
    assertEquals("dirAttr(String, boolean)", "dir=rtl",
        ltrFormatter.dirAttr(he + longHtmlTag, true));
  }

  public void testEndEdge() {
    assertEquals(RIGHT, ltrFormatter.endEdge());
  }

  public void testGetInstance() {
    // Check contextDir
    assertEquals(LTR, BidiFormatter.getInstance(false).getContextDir());
    assertEquals(RTL, BidiFormatter.getInstance(true).getContextDir());
    assertEquals(LTR, BidiFormatter.getInstance(LTR).getContextDir());
    assertEquals(RTL, BidiFormatter.getInstance(RTL).getContextDir());
    assertEquals(DEFAULT,
        BidiFormatter.getInstance(DEFAULT).getContextDir());

    // Check alwaysSpan
    assertEquals(true, BidiFormatter.getInstance(false, true).getAlwaysSpan());
    assertEquals(false, BidiFormatter.getInstance(false, false).getAlwaysSpan());
  }

  public void testKnownDirAttr() {
    assertEquals("dir=rtl", ltrFormatter.knownDirAttr(RTL));
  }

  public void testMark() {
    assertEquals(LRM_STRING, ltrFormatter.mark());
  }

  public void testMarkAfter() {
    assertEquals("markAfter(String)", LRM_STRING, ltrFormatter.markAfter(longEn
        + he));
    assertEquals("markAfter(String, boolean)", LRM_STRING,
        ltrFormatter.markAfter(longEn + he + longHtmlTag, true));
  }

  public void testSpanWrap() {
    assertEquals("spanWrap(String)",
        "<span dir=rtl>" + he + "&lt;</span>" + LRM,
        ltrFormatter.spanWrap(he + "<"));
    assertEquals("spanWrap(String, boolean)", "<span dir=rtl>" + he
        + longHtmlTag + "</span>" + LRM, ltrFormatter.spanWrap(
        he + longHtmlTag, true));
    assertEquals("spanWrap(String, boolean, boolean)", "<span dir=rtl>" + he
        + longHtmlTag + "</span>", ltrFormatter.spanWrap(he + longHtmlTag,
        true, false));
  }

  public void testSpanWrapWithKnownDir() {
    assertEquals("spanWrapWithKnownDir(Direction, String)", "<span dir=rtl>"
        + en + "&lt;</span>" + LRM,
        ltrFormatter.spanWrapWithKnownDir(RTL, en + "<"));
    assertEquals("spanWrapWithKnownDir(Direction, String, boolean)",
        "<span dir=rtl>" + en + longHtmlTag + "</span>" + LRM,
        ltrFormatter.spanWrapWithKnownDir(RTL, en + longHtmlTag, true));
    assertEquals("spanWrapWithKnownDir(Direction, String, boolean, boolean)",
        "<span dir=rtl>" + en + longHtmlTag + "</span>",
        ltrFormatter.spanWrapWithKnownDir(RTL, en + longHtmlTag, true, false));
  }

  public void testStartEdge() {
    assertEquals(LEFT, ltrFormatter.startEdge());
  }

  public void testUnicodeWrap() {
    assertEquals("unicodeWrap(String)", RLE + he + PDF + LRM,
        ltrFormatter.unicodeWrap(he));
    assertEquals("unicodeWrap(String, boolean)", RLE + he + longHtmlTag + PDF
        + LRM, ltrFormatter.unicodeWrap(he + longHtmlTag, true));
    assertEquals("unicodeWrap(String, boolean, boolean)", RLE + he
        + longHtmlTag + PDF, ltrFormatter.unicodeWrap(he + longHtmlTag, true,
        false));
  }

  public void testUnicodeWrapWithKnownDir() {
    assertEquals("unicodeWrapWithKnownDir(Direction, String)", RLE + en + PDF
        + LRM, ltrFormatter.unicodeWrapWithKnownDir(RTL, en));
    assertEquals("unicodeWrapWithKnownDir(Direction, String, boolean)", RLE
        + en + longHtmlTag + PDF + LRM, ltrFormatter.unicodeWrapWithKnownDir(
        RTL, en + longHtmlTag, true));
    assertEquals(
        "unicodeWrapWithKnownDir(Direction, String, boolean, boolean)", RLE
         + en + longHtmlTag + PDF, ltrFormatter.unicodeWrapWithKnownDir(RTL,
         en + longHtmlTag, true, false));
  }
}
