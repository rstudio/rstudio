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

import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.google.gwt.thirdparty.guava.common.base.Strings;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tests for {@link JsSourceUtils}.
 */
public class JsSourceUtilsTest extends OptimizerTestBase {

  public void testMatchDoesNotThrowStackOverflow() {
    String large = Strings.repeat("a\\\\", 2000000);
    Pattern textPattern =
        Pattern.compile(JsSourceUtils.REGULAR_TEXT_PATTERN.toString());
    Matcher matcher = textPattern.matcher(large);
    assertTrue(matcher.matches());

    large = Strings.repeat("ab/2/6/* hhh -- */", 20000);
    matcher = textPattern.matcher(large);
    assertTrue(matcher.matches());

    large = Strings.repeat("ab/2/6/* hhh -- */ / \\/* /", 20000);
    matcher = textPattern.matcher(large);
    assertTrue(matcher.matches());
  }

  public void testMatchString() {
    Pattern stringPattern = JsSourceUtils.JS_STRING_PATTERN;

    assertMatches("\"\"", stringPattern);
    assertMatches("\"--\"", stringPattern);
    assertMatches("\"\\\"\"", stringPattern);
    assertMatches("\"Hello\"", stringPattern);
    assertMatches("\"He\\\\llo\"", stringPattern);
    assertMatches("\"He\\\"llo\"", stringPattern);

    assertDoesNotMatch("Hello", stringPattern);
    assertDoesNotMatch("\"Hello", stringPattern);
    assertDoesNotMatch("\"Hello\" out", stringPattern);
    assertDoesNotMatch("\"Hello\" out\"", stringPattern);
    assertDoesNotMatch("\\\"Hell\\\"o\\\"", stringPattern);
  }

  public void testMatchJsRegExp() {
    Pattern jsRegExpPattern = JsSourceUtils.JS_REG_EXP_PATTERN;

    assertMatches("/ /", jsRegExpPattern);
    assertMatches("/\"\\\"\"/", jsRegExpPattern);
    assertMatches("/ --/", jsRegExpPattern);
    assertMatches("/ \\//", jsRegExpPattern);

    assertDoesNotMatch("//", jsRegExpPattern);
    assertDoesNotMatch("///", jsRegExpPattern);
  }

  public void testBlockComments() {
    Pattern blockCommentPattern = JsSourceUtils.BLOCK_COMMENT_PATTERN;

    assertMatches("/* */", blockCommentPattern);
    assertMatches("/* /* */", blockCommentPattern);
    assertMatches("/* -- */", blockCommentPattern);
    assertMatches("/* \" */", blockCommentPattern);
    assertMatches("/* \" \" */", blockCommentPattern);

    assertDoesNotMatch("/* */ */", blockCommentPattern);
  }

  private void assertMatches(String string, Pattern regExp) {
    assertTrue("Regular expression " + regExp.toString() + " does not match '" + string + "'",
        regExp.matcher(string).matches());
  }

  private void assertDoesNotMatch(String string, Pattern regExp) {
    assertTrue("Regular expression " + regExp.toString() + " should not match '" + string + "'",
        !regExp.matcher(string).matches());
  }

  public void testOneLinePrefix_blockCommentWithDoubleSlash() {
    String snippet = Joiner.on('\n').join(
        "var a = {};",
        "var b = {};",
        "function someF() { /* some // comment */ return /s/;}"
    );

    assertOneLinerCorrect(snippet, snippet);
  }

  public void testOneLinePrefix_lineCommentWithDoubleSlash() {
    String snippet =
        "var a = {}; // this is the end and an extra //  sss";

    String expected =
        "var a = {}; ";

    assertOneLinerCorrect(expected, snippet);
  }

  public void testOneLinePrefix_lineCommentBlockCommentStart() {
    String snippet = Joiner.on('\n').join(
        "var a = {}; // this is the end and an extra /*",
        "",
        " return; //  */");

    String expected = Joiner.on(' ').join(
        "var a = {}; ",
        "",
        " return; ");

    assertOneLinerCorrect(expected, snippet);
  }

  public void testOneLinePrefix_stringWithDoubleSlash() {
    String snippet = Joiner.on('\n').join(
        "var a = {};",
        "var b = {};",
        "function someF() { return \" some // string \";}"
    );

    assertOneLinerCorrect(snippet, snippet);
  }

  public void testOneLinePrefix_stringWithEscapedQuote() {
    String snippet = Joiner.on('\n').join(
        "var a = {};",
        "var b = {};",
        "function someF() { return \" some string \\\" \";}"
    );

    assertOneLinerCorrect(snippet, snippet);
  }

  public void testOneLinePrefix_commentsStripped() {
    String snippet = Joiner.on('\n').join(
        "var a = {}; // this is var a",
        " // this is a comment line",
        "var b = {}; // and this is var b and some \"quotes\"",
        "function someF() { /* some comment",
        " this is still in the comment",
        "  */ return \"//\"; // and finally some // in a string",
        "}"
    );

    String expected = Joiner.on(' ').join(
        "var a = {}; ",
        " ",
        "var b = {}; ",
        "function someF() { ",
        " return \"//\"; ",
        "}"
    );

    assertOneLinerCorrect(expected, snippet);
  }

  public void testOneLinePrefix_doubleSlashInMultilineComments() {
    String snippet = Joiner.on('\n').join(
        "/* this is a comment line",
        "// */ var a = {};"
    );

    String expected = " var a = {};";

    assertOneLinerCorrect(expected, snippet);
  }

  private static  void assertOneLinerCorrect(String expected, String actual) {
    String oneLineSnippet = JsSourceUtils.condenseAndRemoveLineBreaks(actual);
    assertTrue("Last character not a blank",
        oneLineSnippet.charAt(oneLineSnippet.length() - 1) == ' ');
    assertEquals(expected.replace('\n', ' ').trim(), oneLineSnippet.trim());
  }
}
