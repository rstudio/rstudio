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
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Unit tests for BidiUtils.
 * Uses the pure Java implementation of
 * {@link com.google.gwt.regexp.shared.RegExp}. {@link GwtBidiUtilsTest}
 * performs all the tests using the GWT version of RegExp. This is needed
 * due to several differences between the two implementations of RegExp (see
 * {@link com.google.gwt.regexp.shared.RegExpTest} for details). 
 */
public class BidiUtilsTest extends GWTTestCase {
  
  private static BidiUtils bidiUtils = BidiUtils.get();

  // This is a hack to force a GWTTestCase to run as a vanilla JUnit TestCase.
  @Override
  public String getModuleName() {
    return null;
  }
  
  public void testEndsWithLtr() {
    assertTrue(bidiUtils.endsWithLtr("a"));
    assertTrue(bidiUtils.endsWithLtr("abc"));
    assertTrue(bidiUtils.endsWithLtr("a (!)"));
    assertTrue(bidiUtils.endsWithLtr("a.1"));
    assertTrue(bidiUtils.endsWithLtr("http://www.google.com "));
    assertTrue(bidiUtils.endsWithLtr("\u05e0 \u05e0 \u05e0a"));
    assertTrue(bidiUtils.endsWithLtr(" \u05e0 \u05e0\u05e1a \u05e2 a !"));
    assertFalse(bidiUtils.endsWithLtr(""));
    assertFalse(bidiUtils.endsWithLtr(" "));
    assertFalse(bidiUtils.endsWithLtr("1"));
    assertFalse(bidiUtils.endsWithLtr("\u05e0"));
    assertFalse(bidiUtils.endsWithLtr("\u05e0 1(!)"));
    assertFalse(bidiUtils.endsWithLtr("a a a \u05e0"));
    assertFalse(bidiUtils.endsWithLtr("a a abc\u05e0\u05e1def\u05e2. 1"));

    assertTrue(bidiUtils.endsWithLtr("a a abc\u05e0<nasty tag>", false));
    assertFalse(bidiUtils.endsWithLtr("a a abc\u05e0<nasty tag>", true));
  }

  public void testEndsWithRtl() {
    assertTrue(bidiUtils.endsWithRtl("\u05e0"));
    assertTrue(bidiUtils.endsWithRtl("\u05e0\u05e1\u05e2"));
    assertTrue(bidiUtils.endsWithRtl("\u05e0 (!)"));
    assertTrue(bidiUtils.endsWithRtl("\u05e0.1"));
    assertTrue(bidiUtils.endsWithRtl("http://www.google.com/\u05e0 "));
    assertTrue(bidiUtils.endsWithRtl("a a a a\u05e0"));
    assertTrue(bidiUtils.endsWithRtl(" a a a abc\u05e0def\u05e3. 1"));
    assertFalse(bidiUtils.endsWithRtl(""));
    assertFalse(bidiUtils.endsWithRtl(" "));
    assertFalse(bidiUtils.endsWithRtl("1"));
    assertFalse(bidiUtils.endsWithRtl("a"));
    assertFalse(bidiUtils.endsWithRtl("a 1(!)"));
    assertFalse(bidiUtils.endsWithRtl("\u05e0 \u05e0 \u05e0a"));
    assertFalse(bidiUtils.endsWithRtl("\u05e0 \u05e0\u05e1ab\u05e2 a (!)"));

    assertFalse(bidiUtils.endsWithRtl("a a abc\u05e0<nasty tag>", false));
    assertTrue(bidiUtils.endsWithRtl("a a abc\u05e0<nasty tag>", true));
  }

  public void testEstimateDirection() {
    assertEquals(Direction.DEFAULT, bidiUtils.estimateDirection("", false));
    assertEquals(Direction.DEFAULT, bidiUtils.estimateDirection(" ", false));
    assertEquals(Direction.DEFAULT, bidiUtils.estimateDirection("! (...)",
        false));
    assertEquals(Direction.LTR, bidiUtils.estimateDirection(
        "Pure Ascii content", false));
    assertEquals(Direction.LTR, bidiUtils.estimateDirection("-17.0%", false));
    assertEquals(Direction.LTR, bidiUtils.estimateDirection("http://foo/bar/",
        false));
    assertEquals(Direction.LTR, bidiUtils.estimateDirection(
        "http://foo/bar/?s=\u05d0\u05d0\u05d0\u05d0\u05d0\u05d0\u05d0\u05d0"
        + "\u05d0\u05d0\u05d0\u05d0\u05d0\u05d0\u05d0\u05d0\u05d0\u05d0\u05d0"
        + "\u05d0\u05d0\u05d0\u05d0\u05d0\u05d0", false));
    assertEquals(Direction.RTL, bidiUtils.estimateDirection("\u05d0", false));
    assertEquals(Direction.RTL, bidiUtils.estimateDirection(
        "9 \u05d0 -> 17.5, 23, 45, 19", false));
    assertEquals(Direction.RTL, bidiUtils.estimateDirection(
        "http://foo/bar/ \u05d0 http://foo2/bar2/ http://foo3/bar3/", false));
    assertEquals(Direction.RTL, bidiUtils.estimateDirection(
        "\u05d0\u05d9\u05df \u05de\u05de\u05e9 "
        + "\u05de\u05d4 \u05dc\u05e8\u05d0\u05d5\u05ea: "
        + "\u05dc\u05d0 \u05e6\u05d9\u05dc\u05de\u05ea\u05d9 "
        + "\u05d4\u05e8\u05d1\u05d4 \u05d5\u05d2\u05dd \u05d0"
        + "\u05dd \u05d4\u05d9\u05d9\u05ea\u05d9 \u05de\u05e6\u05dc"
        + "\u05dd, \u05d4\u05d9\u05d4 \u05e9\u05dd", false));
    assertEquals(Direction.RTL, bidiUtils.estimateDirection(
        "\u05db\u05d0\u05df - http://geek.co.il/gallery/v/2007-06"
        + " - \u05d0\u05d9\u05df \u05de\u05de\u05e9 \u05de\u05d4 "
        + "\u05dc\u05e8\u05d0\u05d5\u05ea: \u05dc\u05d0 \u05e6"
        + "\u05d9\u05dc\u05de\u05ea\u05d9 \u05d4\u05e8\u05d1\u05d4 "
        + "\u05d5\u05d2\u05dd \u05d0\u05dd \u05d4\u05d9\u05d9\u05ea"
        + "\u05d9 \u05de\u05e6\u05dc\u05dd, \u05d4\u05d9\u05d4 "
        + "\u05e9\u05dd \u05d1\u05e2\u05d9\u05e7\u05e8 \u05d4\u05e8"
        + "\u05d1\u05d4 \u05d0\u05e0\u05e9\u05d9\u05dd. \u05de"
        + "\u05d4 \u05e9\u05db\u05df - \u05d0\u05e4\u05e9\u05e8 "
        + "\u05dc\u05e0\u05e6\u05dc \u05d0\u05ea \u05d4\u05d4 "
        + "\u05d3\u05d6\u05de\u05e0\u05d5\u05ea \u05dc\u05d4\u05e1"
        + "\u05ea\u05db\u05dc \u05e2\u05dc \u05db\u05de\u05d4 "
        + "\u05ea\u05de\u05d5\u05e0\u05d5\u05ea \u05de\u05e9\u05e2"
        + "\u05e9\u05e2\u05d5\u05ea \u05d9\u05e9\u05e0\u05d5\u05ea "
        + "\u05d9\u05d5\u05ea\u05e8 \u05e9\u05d9\u05e9 \u05dc"
        + "\u05d9 \u05d1\u05d0\u05ea\u05e8", false));
    assertEquals(Direction.RTL, bidiUtils.estimateDirection(
        "CAPTCHA \u05de\u05e9\u05d5\u05db\u05dc\u05dc "
        + "\u05de\u05d3\u05d9?", false));
    assertEquals(Direction.RTL, bidiUtils.estimateDirection(
        "Yes Prime Minister \u05e2\u05d3\u05db\u05d5\u05df. "
        + "\u05e9\u05d0\u05dc\u05d5 \u05d0\u05d5\u05ea\u05d9 "
        + "\u05de\u05d4 \u05d0\u05e0\u05d9 \u05e8\u05d5\u05e6"
        + "\u05d4 \u05de\u05ea\u05e0\u05d4 \u05dc\u05d7\u05d2",
        false));
    assertEquals(Direction.RTL, bidiUtils.estimateDirection(
        "17.4.02 \u05e9\u05e2\u05d4:13-20 .15-00 .\u05dc\u05d0 "
        + "\u05d4\u05d9\u05d9\u05ea\u05d9 \u05db\u05d0\u05df.",
        false));
    assertEquals(Direction.RTL, bidiUtils.estimateDirection(
        "5710 5720 5730. \u05d4\u05d3\u05dc\u05ea. "
        + "\u05d4\u05e0\u05e9\u05d9\u05e7\u05d4", false));
    assertEquals(Direction.RTL, bidiUtils.estimateDirection(
        "\u05d4\u05d3\u05dc\u05ea http://www.google.com "
        + "http://www.gmail.com", false));
    assertEquals(Direction.LTR, bidiUtils.estimateDirection(
        "\u05d4\u05d3\u05dc\u05ea <some quite nasty html mark up>", false));
    assertEquals(Direction.RTL, bidiUtils.estimateDirection(
        "\u05d4\u05d3\u05dc\u05ea <some quite nasty html mark up>", true));
    assertEquals(Direction.LTR, bidiUtils.estimateDirection(
        "\u05d4\u05d3\u05dc\u05ea &amp; &lt; &gt;", false));
    assertEquals(Direction.RTL, bidiUtils.estimateDirection(
        "\u05d4\u05d3\u05dc\u05ea &amp; &lt; &gt;", true));
  }

  public void testHasAnyLtr() {
    assertFalse(bidiUtils.hasAnyLtr(""));
    assertFalse(bidiUtils.hasAnyLtr("\u05e0\u05e1\u05e2"));
    assertTrue(bidiUtils.hasAnyLtr("\u05e0\u05e1z\u05e2"));
    assertFalse(bidiUtils.hasAnyLtr("123\t...  \n"));
  }

  public void testHasAnyRtl() {
    assertFalse(bidiUtils.hasAnyRtl(""));
    assertFalse(bidiUtils.hasAnyRtl("abc"));
    assertTrue(bidiUtils.hasAnyRtl("ab\u05e0c"));
    assertFalse(bidiUtils.hasAnyRtl("123\t...  \n"));
  }

  public void testStartsWithLtr() {
    assertTrue(bidiUtils.startsWithLtr("a"));
    assertTrue(bidiUtils.startsWithLtr("abc"));
    assertTrue(bidiUtils.startsWithLtr("(!) a"));
    assertTrue(bidiUtils.startsWithLtr("1.a"));
    assertTrue(bidiUtils.startsWithLtr("/a/\u05e0/\u05e1/\u05e2"));
    assertTrue(bidiUtils.startsWithLtr("a\u05e0 \u05e0 \u05e0"));
    assertTrue(bidiUtils.startsWithLtr("! a \u05e0 \u05e0\u05e1a \u05e2"));
    assertFalse(bidiUtils.startsWithLtr(""));
    assertFalse(bidiUtils.startsWithLtr(" "));
    assertFalse(bidiUtils.startsWithLtr("1"));
    assertFalse(bidiUtils.startsWithLtr("\u05e0"));
    assertFalse(bidiUtils.startsWithLtr("1(!) \u05e0"));
    assertFalse(bidiUtils.startsWithLtr("\u05e0 a a a"));

    assertTrue(bidiUtils.startsWithLtr("<nasty tag>\u05e0:a a abc", false));
    assertFalse(bidiUtils.startsWithLtr("<nasty tag>\u05e0:a a abc", true));
  }

  public void testStartsWithRtl() {
    assertTrue(bidiUtils.startsWithRtl("\u05e0"));
    assertTrue(bidiUtils.startsWithRtl("\u05e0\u05e1\u05e2"));
    assertTrue(bidiUtils.startsWithRtl("(!) \u05e0"));
    assertTrue(bidiUtils.startsWithRtl("1.\u05e0"));
    assertTrue(bidiUtils.startsWithRtl("/\u05e0/a/b/c"));
    assertTrue(bidiUtils.startsWithRtl("\u05e0a a a a"));
    assertTrue(bidiUtils.startsWithRtl("1. \u05e0. a a a abc\u05e1 def"));
    assertFalse(bidiUtils.startsWithRtl(""));
    assertFalse(bidiUtils.startsWithRtl(" "));
    assertFalse(bidiUtils.startsWithRtl("1"));
    assertFalse(bidiUtils.startsWithRtl("a"));
    assertFalse(bidiUtils.startsWithRtl("(!) a"));
    assertFalse(bidiUtils.startsWithRtl("a \u05e0 \u05e0 \u05e0"));

    assertFalse(bidiUtils.startsWithRtl("<nasty tag>\u05e0:a a abc", false));
    assertTrue(bidiUtils.startsWithRtl("<nasty tag>\u05e0:a a abc", true));
  }

  public void testStripHtmlIfNeeded() {
    String str = "foo&lt;gev<nasty tag/>";
    String stripped = "foo gev ";
    assertEquals(stripped, bidiUtils.stripHtmlIfNeeded(str, true));
    assertEquals(str, bidiUtils.stripHtmlIfNeeded(str, false));
  }
}
