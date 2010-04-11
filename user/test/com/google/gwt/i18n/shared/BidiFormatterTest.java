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

import static com.google.gwt.i18n.shared.BidiFormatter.Format.*;

import com.google.gwt.i18n.client.HasDirection.Direction;

import junit.framework.TestCase;

/**
 * Unit tests for BidiFormatter.
 */
public class BidiFormatterTest extends TestCase {
  static final Direction DEFAULT = Direction.DEFAULT;
  static final Direction LTR = Direction.LTR;
  static final Direction RTL = Direction.RTL;

  String en = "abba";
  String he = "\u05e0\u05e1";
  String html = "&lt;";
  String longEn = "abba sabba gabba ";
  String longHe = "\u05e0 \u05e1 \u05e0 ";
  BidiFormatter ltrFmt = BidiFormatter.getInstance(LTR); // LTR context
  BidiFormatter rtlFmt = BidiFormatter.getInstance(RTL); // RTL context
  BidiFormatter unkFmt = BidiFormatter.getInstance(DEFAULT); // DEFAULT context
  
  public void testDirAttr() {
    // Regular cases:
    assertEquals("dir=rtl", ltrFmt.dirAttr(he, true));
    assertEquals("", rtlFmt.dirAttr(he, true));
    assertEquals("dir=ltr", rtlFmt.dirAttr(en, true));
    assertEquals("", ltrFmt.dirAttr(en, true));

    // Text contains HTML or HTML-escaping:
    assertEquals("dir=rtl", ltrFmt.dirAttr(he + "<some sort of an HTML tag>",
        true));
    assertEquals("", ltrFmt.dirAttr(he + "<some sort of an HTML tag>", false));
  }

  public void testEndEdge() {
    assertEquals(LEFT, rtlFmt.endEdge());
    assertEquals(RIGHT, ltrFmt.endEdge());
    assertEquals(RIGHT, unkFmt.endEdge());
  }

  public void testEstimateDirection() {
    // Regular cases.
    assertEquals(DEFAULT, ltrFmt.estimateDirection(""));
    assertEquals(DEFAULT, rtlFmt.estimateDirection(""));
    assertEquals(DEFAULT, unkFmt.estimateDirection(""));
    assertEquals(LTR, ltrFmt.estimateDirection(en));
    assertEquals(LTR, rtlFmt.estimateDirection(en));
    assertEquals(LTR, unkFmt.estimateDirection(en));
    assertEquals(RTL, ltrFmt.estimateDirection(he));
    assertEquals(RTL, rtlFmt.estimateDirection(he));
    assertEquals(RTL, unkFmt.estimateDirection(he));

    // Text contains HTML or HTML-escaping.
    assertEquals(LTR, ltrFmt.estimateDirection("<some sort of tag/>" + he
        + " &amp;", false));
    assertEquals(RTL, ltrFmt.estimateDirection(he + "<some sort of tag/>" + he
        + " &amp;", true));
  }

  public void testGetContextDir() {
    assertEquals(LTR, ltrFmt.getContextDir());
    assertEquals(RTL, rtlFmt.getContextDir());
    assertEquals(DEFAULT, unkFmt.getContextDir());
  }

  public void testGetInstanceForRtlContext() {
    assertEquals(LTR, BidiFormatter.getInstance(false).getContextDir());
    assertEquals(RTL, BidiFormatter.getInstance(true).getContextDir());
  }

  public void testIsRtlContext() {
    assertEquals(false, ltrFmt.isRtlContext());
    assertEquals(true, rtlFmt.isRtlContext());
    assertEquals(false, unkFmt.isRtlContext());
  }

  public void testKnownDirAttr() {
    // Regular cases:
    assertEquals("dir=rtl", ltrFmt.knownDirAttr(RTL));
    assertEquals("", rtlFmt.knownDirAttr(RTL));
    assertEquals("dir=ltr", rtlFmt.knownDirAttr(LTR));
    assertEquals("", ltrFmt.knownDirAttr(LTR));
  }

  public void testMark() {
    assertEquals(RLM_STRING, rtlFmt.mark());
    assertEquals(LRM_STRING, ltrFmt.mark());
    assertEquals("", unkFmt.mark());
  }

  public void testMarkAfter() {
    assertEquals("exit dir (RTL) is opposite to context dir (LTR)", LRM_STRING,
        ltrFmt.markAfter(longEn + he + html, true));
    assertEquals("exit dir (LTR) is opposite to context dir (RTL)", RLM_STRING,
        rtlFmt.markAfter(longHe + en, true));
    assertEquals("exit dir (LTR) doesnt match context dir (DEFAULT)", "",
        unkFmt.markAfter(longEn + en, true));
    assertEquals("overall dir (RTL) is opposite to context dir (LTR)",
        LRM_STRING, ltrFmt.markAfter(longHe + en, true));
    assertEquals("overall dir (LTR) is opposite to context dir (RTL)",
        RLM_STRING, rtlFmt.markAfter(longEn + he, true));
    assertEquals("exit dir and overall dir match context dir (LTR)", "",
        ltrFmt.markAfter(longEn + he + html, false));
    assertEquals("exit dir and overall dir matches context dir (RTL)", "",
        rtlFmt.markAfter(longHe + he, true));
  }

  public void testSpanWrap() {
    // The main testing of the logic is done in testSpanWrapWithKnownDir.
    assertEquals("<span dir=rtl>" + he + "</span>" + LRM, ltrFmt.spanWrap(he,
        true));
    assertEquals(he, rtlFmt.spanWrap(he, true));
    assertEquals("<span dir=ltr>" + en + "</span>" + RLM, rtlFmt.spanWrap(en,
        true));
    assertEquals(en, ltrFmt.spanWrap(en, true));
  }

  public void testSpanWrapWithKnownDir() {
    assertEquals("overall dir matches context dir (LTR)", en + "&lt;",
        ltrFmt.spanWrapWithKnownDir(LTR, en + "<"));
    assertEquals("overall dir matches context dir (LTR), HTML", en + "<br>",
        ltrFmt.spanWrapWithKnownDir(LTR, en + "<br>", true));
    assertEquals("overall dir matches context dir (RTL)", he + "&lt;",
        rtlFmt.spanWrapWithKnownDir(RTL, he + "<"));
    assertEquals("overall dir matches context dir (RTL), HTML", he
        + " <some strange tag>", rtlFmt.spanWrapWithKnownDir(RTL, he
        + " <some strange tag>", true));

    assertEquals("overall dir (RTL) doesnt match context dir (LTR)",
        "<span dir=rtl>" + he + "</span>" + LRM, ltrFmt.spanWrapWithKnownDir(
            RTL, he));
    assertEquals(
        "overall dir (RTL) doesnt match context dir (LTR), no dirReset",
        "<span dir=rtl>" + he + "</span>", ltrFmt.spanWrapWithKnownDir(RTL, he,
            false, false));
    assertEquals("overall dir (LTR) doesnt match context dir (RTL)",
        "<span dir=ltr>" + en + "</span>" + RLM, rtlFmt.spanWrapWithKnownDir(
            LTR, en));
    assertEquals(
        "overall dir (LTR) doesnt match context dir (RTL), no dirReset",
        "<span dir=ltr>" + en + "</span>", rtlFmt.spanWrapWithKnownDir(LTR, en,
            false, false));
    assertEquals("overall dir (RTL) doesnt match context dir (unknown)",
        "<span dir=rtl>" + he + "</span>", unkFmt.spanWrapWithKnownDir(RTL, he));
    assertEquals(
        "overall dir (LTR) doesnt match context dir (unknown), no dirReset",
        "<span dir=ltr>" + en + "</span>", unkFmt.spanWrapWithKnownDir(LTR, en,
            false, false));
    assertEquals("overall dir (neutral) doesnt match context dir (LTR)", ".",
        ltrFmt.spanWrapWithKnownDir(DEFAULT, "."));

    assertEquals("exit dir (but not overall dir) is opposite to context dir",
        longEn + he + LRM, ltrFmt.spanWrapWithKnownDir(LTR, longEn + he));
    assertEquals("overall dir (but not exit dir) is opposite to context dir",
        "<span dir=ltr>" + longEn + he + "</span>" + RLM,
        rtlFmt.spanWrapWithKnownDir(LTR, longEn + he));

    assertEquals("exit dir (but not overall dir) is opposite to context dir",
        longEn + he + html + LRM, ltrFmt.spanWrapWithKnownDir(LTR, longEn + he
            + html, true, true));
    assertEquals(
        "overall dir (but not exit dir) is opposite to context dir, dirReset",
        "<span dir=ltr>" + longEn + he + "</span>" + RLM,
        rtlFmt.spanWrapWithKnownDir(LTR, longEn + he, true, true));

    assertEquals("plain text overall and exit dir same as context dir",
        "&lt;br&gt; " + he + " &lt;br&gt;", ltrFmt.spanWrapWithKnownDir(LTR,
            "<br> " + he + " <br>", false));
    assertEquals("HTML overall and exit dir opposite to context dir",
        "<span dir=rtl><br> " + he + " <br></span>" + LRM,
        ltrFmt.spanWrapWithKnownDir(RTL, "<br> " + he + " <br>", true));

    BidiFormatter ltrAlwaysSpanFmt = BidiFormatter.getInstance(LTR, true);
    BidiFormatter rtlAlwaysSpanFmt = BidiFormatter.getInstance(RTL, true);
    BidiFormatter unkAlwaysSpanFmt = BidiFormatter.getInstance(DEFAULT, true);

    assertEquals("alwaysSpan, overall dir matches context dir (LTR)", "<span>"
        + en + "</span>", ltrAlwaysSpanFmt.spanWrapWithKnownDir(LTR, en));
    assertEquals(
        "alwaysSpan, overall dir matches context dir (LTR), no dirReset",
        "<span>" + en + "</span>", ltrAlwaysSpanFmt.spanWrapWithKnownDir(LTR,
            en, false, false));
    assertEquals("alwaysSpan, overall dir matches context dir (RTL)", "<span>"
        + he + "</span>", rtlAlwaysSpanFmt.spanWrapWithKnownDir(RTL, he));
    assertEquals(
        "alwaysSpan, overall dir matches context dir (RTL), no dirReset",
        "<span>" + he + "</span>", rtlAlwaysSpanFmt.spanWrapWithKnownDir(RTL,
            he, false, false));

    assertEquals(
        "alwaysSpan, exit dir (but not overall dir) is opposite to context dir",
        "<span>" + longEn + he + "</span>" + LRM,
        ltrAlwaysSpanFmt.spanWrapWithKnownDir(LTR, longEn + he, true, true));
    assertEquals(
        "alwaysSpan, overall dir (but not exit dir) is opposite to context dir, dirReset",
        "<span dir=ltr>" + longEn + he + "</span>" + RLM,
        rtlAlwaysSpanFmt.spanWrapWithKnownDir(LTR, longEn + he, true, true));

    assertEquals(
        "alwaysSpan, plain text overall and exit dir same as context dir",
        "<span>&lt;br&gt; " + he + " &lt;br&gt;</span>",
        ltrAlwaysSpanFmt.spanWrapWithKnownDir(LTR, "<br> " + he + " <br>",
            false));
    assertEquals(
        "alwaysSpan, HTML overall and exit dir opposite to context dir",
        "<span dir=rtl><br> " + he + " <br></span>" + LRM,
        ltrAlwaysSpanFmt.spanWrapWithKnownDir(RTL, "<br> " + he + " <br>", true));
  }

  public void testStartEdge() {
    assertEquals(RIGHT, rtlFmt.startEdge());
    assertEquals(LEFT, ltrFmt.startEdge());
    assertEquals(LEFT, unkFmt.startEdge());
  }

  public void testUnicodeWrap() {
    // The main testing of the logic is done in testUnicodeWrapWithKnownDir.
    assertEquals(RLE + he + PDF + LRM, ltrFmt.unicodeWrap(he, true));
    assertEquals(he, rtlFmt.unicodeWrap(he, true));
    assertEquals(LRE + en + PDF + RLM, rtlFmt.unicodeWrap(en, true));
    assertEquals(en, ltrFmt.unicodeWrap(en, true));
  }

  public void testUnicodeWrapWithKnownDir() {
    assertEquals("overall dir matches context dir (LTR)", en + "<",
        ltrFmt.unicodeWrapWithKnownDir(LTR, en + "<"));
    assertEquals("overall dir matches context dir (LTR), HTML", en + "<br>",
        ltrFmt.unicodeWrapWithKnownDir(LTR, en + "<br>", true));
    assertEquals("overall dir matches context dir (RTL)", he + "<",
        rtlFmt.unicodeWrapWithKnownDir(RTL, he + "<"));
    assertEquals("overall dir matches context dir (RTL), HTML", he
        + " <some strange tag>", rtlFmt.unicodeWrapWithKnownDir(RTL, he
        + " <some strange tag>", true));

    assertEquals("overall dir (RTL) doesnt match context dir (LTR), dirReset",
        RLE + he + PDF + LRM, ltrFmt.unicodeWrapWithKnownDir(RTL, he));
    assertEquals(
        "overall dir (RTL) doesnt match context dir (LTR), no dirReset", RLE
            + he + PDF, ltrFmt.unicodeWrapWithKnownDir(RTL, he, false, false));
    assertEquals("overall dir (LTR) doesnt match context dir (RTL), dirReset",
        LRE + en + PDF + RLM, rtlFmt.unicodeWrapWithKnownDir(LTR, en));
    assertEquals(
        "overall dir (LTR) doesnt match context dir (RTL), no dirReset", LRE
            + en + PDF, rtlFmt.unicodeWrapWithKnownDir(LTR, en, false, false));
    assertEquals(
        "overall dir (RTL) doesnt match context dir (unknown), dirReset", RLE
            + he + PDF, unkFmt.unicodeWrapWithKnownDir(RTL, he));
    assertEquals(
        "overall dir (LTR) doesnt match context dir (unknown), no dirReset",
        LRE + en + PDF, unkFmt.unicodeWrapWithKnownDir(LTR, en, false, false));
    assertEquals(
        "overall dir (neutral) doesnt match context dir (LTR), dirReset", ".",
        ltrFmt.unicodeWrapWithKnownDir(DEFAULT, "."));

    assertEquals("exit dir (but not overall dir) is opposite to context dir",
        longEn + he + LRM, ltrFmt.unicodeWrapWithKnownDir(LTR, longEn + he));
    assertEquals("overall dir (but not exit dir) is opposite to context dir",
        LRE + longEn + he + PDF + RLM, rtlFmt.unicodeWrapWithKnownDir(LTR,
            longEn + he));

    assertEquals("plain text overall and exit dir same as context dir", html
        + " " + he + " " + html, ltrFmt.unicodeWrapWithKnownDir(LTR, html + " "
        + he + " " + html, false));
    assertEquals("HTML overall and exit dir opposite to context dir", RLE
        + html + " " + he + " " + html + PDF + LRM,
        ltrFmt.unicodeWrapWithKnownDir(RTL, html + " " + he + " " + html, true));
  }
}
