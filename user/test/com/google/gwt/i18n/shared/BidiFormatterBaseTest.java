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
import static com.google.gwt.i18n.shared.BidiFormatterBase.Format.LRE;
import static com.google.gwt.i18n.shared.BidiFormatterBase.Format.LRM;
import static com.google.gwt.i18n.shared.BidiFormatterBase.Format.LRM_STRING;
import static com.google.gwt.i18n.shared.BidiFormatterBase.Format.PDF;
import static com.google.gwt.i18n.shared.BidiFormatterBase.Format.RIGHT;
import static com.google.gwt.i18n.shared.BidiFormatterBase.Format.RLE;
import static com.google.gwt.i18n.shared.BidiFormatterBase.Format.RLM;
import static com.google.gwt.i18n.shared.BidiFormatterBase.Format.RLM_STRING;

import com.google.gwt.i18n.client.HasDirection.Direction;

import junit.framework.TestCase;

/**
 * Unit tests for {@link BidiFormatterBase}.
 */
public class BidiFormatterBaseTest extends TestCase {

  /**
   * A trivial extension to {@link BidiFormatterBase} for testing purposes.
   */
  public static class TestableBidiFormatterBase extends BidiFormatterBase {
    static class Factory extends BidiFormatterBase.Factory<TestableBidiFormatterBase> {
      @Override
      public TestableBidiFormatterBase createInstance(Direction contextDir,
          boolean alwaysSpan) {
        return new TestableBidiFormatterBase(contextDir, alwaysSpan);
      }
    }

    private static Factory factory = new Factory();

    public static TestableBidiFormatterBase getInstance(Direction contextDir,
        boolean alwaysSpan) {
      return factory.getInstance(contextDir, alwaysSpan);
    }

    private TestableBidiFormatterBase(Direction contextDir, boolean alwaysSpan) {
      super(contextDir, alwaysSpan);
    }
  }

  static final Direction DEFAULT = Direction.DEFAULT;
  static final Direction LTR = Direction.LTR;
  static final Direction RTL = Direction.RTL;

  String en = "abba";
  String he = "\u05e0\u05e1";
  String html = "&lt;";
  String longEn = "abba sabba gabba ";
  String longHe = "\u05e0 \u05e1 \u05e0 ";
  BidiFormatterBase ltrFmt = TestableBidiFormatterBase.getInstance(LTR,
      false); // LTR context
  BidiFormatterBase rtlFmt = TestableBidiFormatterBase.getInstance(RTL,
      false); // RTL context
  BidiFormatterBase unkFmt = TestableBidiFormatterBase.getInstance(DEFAULT,
      false); // DEFAULT context
  BidiFormatterBase ltrAlwaysSpanFmt =
      TestableBidiFormatterBase.getInstance(LTR, true); // LTR context
  BidiFormatterBase rtlAlwaysSpanFmt =
      TestableBidiFormatterBase.getInstance(RTL, true); // RTL context
  BidiFormatterBase unkAlwaysSpanFmt =
      TestableBidiFormatterBase.getInstance(DEFAULT, true); // DEFAULT context

  public void testGetInstance() {
    assertEquals(ltrFmt.getContextDir(), LTR);
    assertEquals(rtlFmt.getContextDir(), RTL);
    assertEquals(unkFmt.getContextDir(), DEFAULT);
    assertFalse(ltrFmt.getAlwaysSpan());
    assertFalse(rtlFmt.getAlwaysSpan());
    assertFalse(unkFmt.getAlwaysSpan());

    // Always-span formatters
    assertEquals(ltrAlwaysSpanFmt.getContextDir(), LTR);
    assertEquals(rtlAlwaysSpanFmt.getContextDir(), RTL);
    assertEquals(unkAlwaysSpanFmt.getContextDir(), DEFAULT);
    assertTrue(ltrAlwaysSpanFmt.getAlwaysSpan());
    assertTrue(rtlAlwaysSpanFmt.getAlwaysSpan());
    assertTrue(unkAlwaysSpanFmt.getAlwaysSpan());

    // Assert that instances with similar parameters are identical.
    assertEquals(ltrFmt,
        TestableBidiFormatterBase.getInstance(LTR, false));
    assertEquals(rtlFmt,
        TestableBidiFormatterBase.getInstance(RTL, false));
    assertEquals(unkFmt,
        TestableBidiFormatterBase.getInstance(DEFAULT, false));
    assertEquals(ltrAlwaysSpanFmt,
        TestableBidiFormatterBase.getInstance(LTR, true));
    assertEquals(rtlAlwaysSpanFmt,
        TestableBidiFormatterBase.getInstance(RTL, true));
    assertEquals(unkAlwaysSpanFmt,
        TestableBidiFormatterBase.getInstance(DEFAULT, true));
  }

  public void testDirAttrBase() {
    // Regular cases:
    assertEquals("dir=rtl", ltrFmt.dirAttrBase(he, true));
    assertEquals("", rtlFmt.dirAttrBase(he, true));
    assertEquals("dir=ltr", rtlFmt.dirAttrBase(en, true));
    assertEquals("", ltrFmt.dirAttrBase(en, true));

    // Text contains HTML or HTML-escaping:
    assertEquals("dir=rtl",
        ltrFmt.dirAttrBase(he + "<some sort of an HTML tag>", true));
    assertEquals("",
        ltrFmt.dirAttrBase(he + "<some sort of an HTML tag>", false));
  }

  public void testEndEdge() {
    assertEquals(LEFT, rtlFmt.endEdgeBase());
    assertEquals(RIGHT, ltrFmt.endEdgeBase());
    assertEquals(RIGHT, unkFmt.endEdgeBase());
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

  public void testIsRtlContext() {
    assertEquals(false, ltrFmt.isRtlContext());
    assertEquals(true, rtlFmt.isRtlContext());
    assertEquals(false, unkFmt.isRtlContext());
  }

  public void testKnownDirAttrBase() {
    // Regular cases:
    assertEquals("dir=rtl", ltrFmt.knownDirAttrBase(RTL));
    assertEquals("", rtlFmt.knownDirAttrBase(RTL));
    assertEquals("dir=ltr", rtlFmt.knownDirAttrBase(LTR));
    assertEquals("", ltrFmt.knownDirAttrBase(LTR));
  }

  public void testMarkBase() {
    assertEquals(RLM_STRING, rtlFmt.markBase());
    assertEquals(LRM_STRING, ltrFmt.markBase());
    assertEquals("", unkFmt.markBase());
  }

  public void testMarkAfterBase() {
    assertEquals("exit dir (RTL) is opposite to context dir (LTR)", LRM_STRING,
        ltrFmt.markAfterBase(longEn + he + html, true));
    assertEquals("exit dir (LTR) is opposite to context dir (RTL)", RLM_STRING,
        rtlFmt.markAfterBase(longHe + en, true));
    assertEquals("exit dir (LTR) doesnt match context dir (DEFAULT)", "",
        unkFmt.markAfterBase(longEn + en, true));
    assertEquals("overall dir (RTL) is opposite to context dir (LTR)",
        LRM_STRING, ltrFmt.markAfterBase(longHe + en, true));
    assertEquals("overall dir (LTR) is opposite to context dir (RTL)",
        RLM_STRING, rtlFmt.markAfterBase(longEn + he, true));
    assertEquals("exit dir and overall dir match context dir (LTR)", "",
        ltrFmt.markAfterBase(longEn + he + html, false));
    assertEquals("exit dir and overall dir matches context dir (RTL)", "",
        rtlFmt.markAfterBase(longHe + he, true));
  }

  public void testSpanWrapBase() {
    // The main testing of the logic is done in testSpanWrapWithKnownDirBase.
    assertEquals("<span dir=rtl>" + he + "</span>" + LRM, ltrFmt.spanWrapBase(
        he, true, true));
    assertEquals(he, rtlFmt.spanWrapBase(he, true, true));
    assertEquals("<span dir=ltr>" + en + "</span>" + RLM, rtlFmt.spanWrapBase(
        en, true, true));
    assertEquals(en, ltrFmt.spanWrapBase(en, true, true));
  }

  public void testSpanWrapWithKnownDirBase() {
    assertEquals("overall dir matches context dir (LTR)", en + "&lt;",
        ltrFmt.spanWrapWithKnownDirBase(LTR, en + "<", false, true));
    assertEquals("overall dir matches context dir (LTR), HTML", en + "<br>",
        ltrFmt.spanWrapWithKnownDirBase(LTR, en + "<br>", true, true));
    assertEquals("overall dir matches context dir (RTL)", he + "&lt;",
        rtlFmt.spanWrapWithKnownDirBase(RTL, he + "<", false, true));
    assertEquals("overall dir matches context dir (RTL), HTML", he
        + " <some strange tag>", rtlFmt.spanWrapWithKnownDirBase(RTL, he
        + " <some strange tag>", true, true));

    assertEquals("overall dir (RTL) doesnt match context dir (LTR)",
        "<span dir=rtl>" + he + "</span>" + LRM,
        ltrFmt.spanWrapWithKnownDirBase(RTL, he, false, true));
    assertEquals(
        "overall dir (RTL) doesnt match context dir (LTR), no dirReset",
        "<span dir=rtl>" + he + "</span>",
        ltrFmt.spanWrapWithKnownDirBase(RTL, he, false, false));
    assertEquals("overall dir (LTR) doesnt match context dir (RTL)",
        "<span dir=ltr>" + en + "</span>" + RLM,
        rtlFmt.spanWrapWithKnownDirBase(LTR, en, false, true));
    assertEquals(
        "overall dir (LTR) doesnt match context dir (RTL), no dirReset",
        "<span dir=ltr>" + en + "</span>",
        rtlFmt.spanWrapWithKnownDirBase(LTR, en, false, false));
    assertEquals("overall dir (RTL) doesnt match context dir (unknown)",
        "<span dir=rtl>" + he + "</span>",
        unkFmt.spanWrapWithKnownDirBase(RTL, he, false, true));
    assertEquals(
        "overall dir (LTR) doesnt match context dir (unknown), no dirReset",
        "<span dir=ltr>" + en + "</span>",
        unkFmt.spanWrapWithKnownDirBase(LTR, en, false, false));
    assertEquals("overall dir (neutral) doesnt match context dir (LTR)", ".",
        ltrFmt.spanWrapWithKnownDirBase(DEFAULT, ".", false, true));

    assertEquals("exit dir (but not overall dir) is opposite to context dir",
        longEn + he + LRM,
        ltrFmt.spanWrapWithKnownDirBase(LTR, longEn + he, false, true));
    assertEquals("overall dir (but not exit dir) is opposite to context dir",
        "<span dir=ltr>" + longEn + he + "</span>" + RLM,
        rtlFmt.spanWrapWithKnownDirBase(LTR, longEn + he, false, true));

    assertEquals("exit dir (but not overall dir) is opposite to context dir",
        longEn + he + html + LRM,
        ltrFmt.spanWrapWithKnownDirBase(LTR, longEn + he + html, true, true));
    assertEquals(
        "overall dir (but not exit dir) is opposite to context dir, dirReset",
        "<span dir=ltr>" + longEn + he + "</span>" + RLM,
        rtlFmt.spanWrapWithKnownDirBase(LTR, longEn + he, true, true));

    assertEquals("plain text overall and exit dir same as context dir",
        "&lt;br&gt; " + he + " &lt;br&gt;", ltrFmt.spanWrapWithKnownDirBase(LTR,
        "<br> " + he + " <br>", false, true));
    assertEquals("HTML overall and exit dir opposite to context dir",
        "<span dir=rtl><br> " + he + " <br></span>" + LRM,
        ltrFmt.spanWrapWithKnownDirBase(RTL, "<br> " + he + " <br>", true,
        true));

    // Always-span formatters
    assertEquals("alwaysSpan, overall dir matches context dir (LTR)", "<span>"
        + en + "</span>",
        ltrAlwaysSpanFmt.spanWrapWithKnownDirBase(LTR, en, false, true));
    assertEquals(
        "alwaysSpan, overall dir matches context dir (LTR), no dirReset",
        "<span>" + en + "</span>",
        ltrAlwaysSpanFmt.spanWrapWithKnownDirBase(LTR, en, false, false));
    assertEquals("alwaysSpan, overall dir matches context dir (RTL)", "<span>"
        + he + "</span>",
        rtlAlwaysSpanFmt.spanWrapWithKnownDirBase(RTL, he, false, true));
    assertEquals(
        "alwaysSpan, overall dir matches context dir (RTL), no dirReset",
        "<span>" + he + "</span>",
        rtlAlwaysSpanFmt.spanWrapWithKnownDirBase(RTL, he, false, false));

    assertEquals(
        "alwaysSpan, exit dir (but not overall dir) is opposite to context dir",
        "<span>" + longEn + he + "</span>" + LRM,
        ltrAlwaysSpanFmt.spanWrapWithKnownDirBase(LTR, longEn + he, true, true));
    assertEquals(
        "alwaysSpan, overall dir (but not exit dir) is opposite to context dir, dirReset",
        "<span dir=ltr>" + longEn + he + "</span>" + RLM,
        rtlAlwaysSpanFmt.spanWrapWithKnownDirBase(LTR, longEn + he, true, true));

    assertEquals(
        "alwaysSpan, plain text overall and exit dir same as context dir",
        "<span>&lt;br&gt; " + he + " &lt;br&gt;</span>",
        ltrAlwaysSpanFmt.spanWrapWithKnownDirBase(LTR, "<br> " + he + " <br>",
        false, true));
    assertEquals(
        "alwaysSpan, HTML overall and exit dir opposite to context dir",
        "<span dir=rtl><br> " + he + " <br></span>" + LRM,
        ltrAlwaysSpanFmt.spanWrapWithKnownDirBase(RTL, "<br> " + he + " <br>",
        true, true));
  }

  public void testStartEdgeBase() {
    assertEquals(RIGHT, rtlFmt.startEdgeBase());
    assertEquals(LEFT, ltrFmt.startEdgeBase());
    assertEquals(LEFT, unkFmt.startEdgeBase());
  }

  public void testUnicodeWrapBase() {
    // The main testing of the logic is done in testUnicodeWrapWithKnownDirBase.
    assertEquals(RLE + he + PDF + LRM, ltrFmt.unicodeWrapBase(he, true, true));
    assertEquals(he, rtlFmt.unicodeWrapBase(he, true, true));
    assertEquals(LRE + en + PDF + RLM, rtlFmt.unicodeWrapBase(en, true, true));
    assertEquals(en, ltrFmt.unicodeWrapBase(en, true, true));
  }

  public void testUnicodeWrapWithKnownDirBase() {
    assertEquals("overall dir matches context dir (LTR)", en + "<",
        ltrFmt.unicodeWrapWithKnownDirBase(LTR, en + "<", false, true));
    assertEquals("overall dir matches context dir (LTR), HTML", en + "<br>",
        ltrFmt.unicodeWrapWithKnownDirBase(LTR, en + "<br>", true, true));
    assertEquals("overall dir matches context dir (RTL)", he + "<",
        rtlFmt.unicodeWrapWithKnownDirBase(RTL, he + "<", false, true));
    assertEquals("overall dir matches context dir (RTL), HTML", he
        + " <some strange tag>", rtlFmt.unicodeWrapWithKnownDirBase(RTL, he
        + " <some strange tag>", true, true));

    assertEquals("overall dir (RTL) doesnt match context dir (LTR), dirReset",
        RLE + he + PDF + LRM,
        ltrFmt.unicodeWrapWithKnownDirBase(RTL, he, false, true));
    assertEquals(
        "overall dir (RTL) doesnt match context dir (LTR), no dirReset", RLE
            + he + PDF,
            ltrFmt.unicodeWrapWithKnownDirBase(RTL, he, false, false));
    assertEquals("overall dir (LTR) doesnt match context dir (RTL), dirReset",
        LRE + en + PDF + RLM,
        rtlFmt.unicodeWrapWithKnownDirBase(LTR, en, false, true));
    assertEquals(
        "overall dir (LTR) doesnt match context dir (RTL), no dirReset", LRE
            + en + PDF,
            rtlFmt.unicodeWrapWithKnownDirBase(LTR, en, false, false));
    assertEquals(
        "overall dir (RTL) doesnt match context dir (unknown), dirReset", RLE
            + he + PDF,
            unkFmt.unicodeWrapWithKnownDirBase(RTL, he, false, true));
    assertEquals(
        "overall dir (LTR) doesnt match context dir (unknown), no dirReset",
        LRE + en + PDF,
        unkFmt.unicodeWrapWithKnownDirBase(LTR, en, false, false));
    assertEquals(
        "overall dir (neutral) doesnt match context dir (LTR), dirReset", ".",
        ltrFmt.unicodeWrapWithKnownDirBase(DEFAULT, ".", false, true));

    assertEquals("exit dir (but not overall dir) is opposite to context dir",
        longEn + he + LRM,
        ltrFmt.unicodeWrapWithKnownDirBase(LTR, longEn + he, false, true));
    assertEquals("overall dir (but not exit dir) is opposite to context dir",
        LRE + longEn + he + PDF + RLM,
        rtlFmt.unicodeWrapWithKnownDirBase(LTR, longEn + he, false, true));

    assertEquals("plain text overall and exit dir same as context dir", html
        + " " + he + " " + html, ltrFmt.unicodeWrapWithKnownDirBase(
        LTR, html + " " + he + " " + html, false, true));
    assertEquals("HTML overall and exit dir opposite to context dir", RLE
        + html + " " + he + " " + html + PDF + LRM,
        ltrFmt.unicodeWrapWithKnownDirBase(RTL, html + " " + he + " " + html,
        true, true));
  }
}
