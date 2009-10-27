/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.emultest.java.lang;

import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * TODO: COMPILER OPTIMIZATIONS HAVE MADE THIS TEST NOT ACTUALLY TEST ANYTHING!
 * NEED A VERSION THAT DOESN'T USE STATICALLY DETERMINABLE STRINGS!
 * 
 * See individual method TODOs for ones that still need work -- the ones without
 * comments are already protected against optimization.
 */
public class StringTest extends GWTTestCase {

  /**
   * TODO(jat): use volatile fields instead of this.
   */
  private static <T> T hideFromCompiler(T value) {
    int i = 7;
    while (i > 0) {
      i -= 2;
    }
    return (i & 1) != 0 ? value : null;
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  /*
   * TODO: needs rewriting to avoid compiler optimizations.
   */
  public void testCharAt() {
    assertEquals("abc".charAt(1), 'b');
  }

  public void testCodePoint() {
    String testPlain = hideFromCompiler("CAT");
    String testUnicode = hideFromCompiler("C\uD801\uDF00T");
    assertEquals("CAT", new String(new int[] {'C', 'A', 'T'}, 0, 3));
    assertEquals("C\uD801\uDF00T",
        new String(new int[] {'C', 67328, 'T'}, 0, 3));
    assertEquals("\uD801\uDF00", new String(new int[] {'C', 67328, 'T'}, 1, 1));
    assertEquals(65, testPlain.codePointAt(1));
    assertEquals("codePointAt fails on surrogate pair", 67328,
        testUnicode.codePointAt(1));
    assertEquals(65, testPlain.codePointBefore(2));
    assertEquals("codePointBefore fails on surrogate pair", 67328,
        testUnicode.codePointBefore(3));
    assertEquals("codePointCount(plain): ", 3, testPlain.codePointCount(0, 3));
    assertEquals("codePointCount(unicode): ", 3, testUnicode.codePointCount(0,
        4));
    assertEquals(1, testPlain.codePointCount(1, 2));
    assertEquals(1, testUnicode.codePointCount(1, 2));
    assertEquals(2, testUnicode.codePointCount(2, 4));
    assertEquals(1, testUnicode.offsetByCodePoints(0, 1));
    assertEquals("offsetByCodePoints(1,1): ", 3,
        testUnicode.offsetByCodePoints(1, 1));
    assertEquals("offsetByCodePoints(2,1): ", 3,
        testUnicode.offsetByCodePoints(2, 1));
    assertEquals(4, testUnicode.offsetByCodePoints(3, 1));
    assertEquals(1, testUnicode.offsetByCodePoints(2, -1));
    assertEquals(1, testUnicode.offsetByCodePoints(3, -1));
    assertEquals("offsetByCodePoints(4.-1): ", 3,
        testUnicode.offsetByCodePoints(4, -1));
    assertEquals(0, testUnicode.offsetByCodePoints(3, -2));
    /*
     * The next line contains a Unicode character outside the base multilingual
     * plane -- it may not show properly depending on your fonts, etc. The
     * character is the Gothic letter Faihu, or U+10346. We use it to verify
     * that multi-char UTF16 characters are handled properly.
     * 
     * In Windows 2000, registry changes are required to support non-BMP
     * characters (or surrogates in general) -- surrogates are not supported
     * before Win2k and they are enabled by default in WinXP and later.
     * 
     * [HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows
     * NT\CurrentVersion\LanguagePack] SURROGATE=(REG_DWORD)0x00000002
     * 
     * [HKEY_CURRENT_USER\Software\Microsoft\Internet
     * Explorer\International\Scripts\42] IEFixedFontName=[Surrogate Font Face
     * Name] IEPropFontName=[Surrogate Font Face Name]
     */
    String nonBmpChar = hideFromCompiler("𐍆");
    assertEquals("\uD800\uDF46", nonBmpChar);
    assertEquals(0x10346, nonBmpChar.codePointAt(0));
    assertEquals(2, nonBmpChar.length());
    assertEquals(1, nonBmpChar.codePointCount(0, 2));
  }

  public void testConcat() {
    String abc = String.valueOf(new char[] {'a', 'b', 'c'});
    String def = String.valueOf(new char[] {'d', 'e', 'f'});
    String empty = String.valueOf(new char[] {});
    assertEquals("abcdef", abc + def);
    assertEquals("abcdef", abc.concat(def));
    assertEquals("", empty.concat(empty));
    char c = def.charAt(0);
    String s = abc;
    assertEquals("abcd", abc + 'd');
    assertEquals("abcd", abc + c);
    assertEquals("abcd", s + 'd');
    assertEquals("abcd", s + c);
    s += c;
    assertEquals("abcd", s);
  }

  public void testConstructor() {
    char[] chars = {'a', 'b', 'c', 'd', 'e', 'f'};
    String constant = String.valueOf(new char[] {'a', 'b', 'c', 'd', 'e', 'f'});
    String shortString = String.valueOf(new char[] {'c', 'd', 'e'});
    assertEquals(constant, new String(hideFromCompiler(constant)));
    assertEquals(constant, new String(chars), constant);
    assertEquals(shortString, new String(chars, 2, 3), shortString);
    assertEquals("", new String(hideFromCompiler("")));
    assertEquals("", new String(new String(new String(new String(
        hideFromCompiler(""))))));
    assertEquals("", new String(new char[] {}));
    StringBuffer buf = new StringBuffer();
    buf.append('c');
    buf.append('a');
    buf.append('t');
    assertEquals("cat", new String(buf));
    StringBuilder sb = new StringBuilder();
    sb.append('c');
    sb.append('a');
    sb.append('t');
    assertEquals("cat", new String(sb));
  }

  /*
   * TODO: needs rewriting to avoid compiler optimizations. (StringBuffer tests
   * are ok)
   */
  public void testContains() {
    // at the beginning
    assertTrue("abcdef".contains("ab"));
    assertTrue("abcdef".contains(new StringBuffer("ab")));
    // at the end
    assertTrue("abcdef".contains("ef"));
    assertTrue("abcdef".contains(new StringBuffer("ef")));
    // in the middle
    assertTrue("abcdef".contains("cd"));
    assertTrue("abcdef".contains(new StringBuffer("cd")));
    // the same
    assertTrue("abcdef".contains("abcdef"));
    assertTrue("abcdef".contains(new StringBuffer("abcdef")));
    // not present
    assertFalse("abcdef".contains("z"));
    assertFalse("abcdef".contains(new StringBuffer("z")));
  }

  public void testEndsWith() {
    String haystack = "abcdefghi";
    assertTrue("a", haystack.endsWith("defghi"));
    assertTrue("b", haystack.endsWith(haystack));
    assertFalse("c", haystack.endsWith(haystack + "j"));
  }

  /*
   * TODO: needs rewriting to avoid compiler optimizations.
   */
  public void testEquals() {
    assertFalse("ABC".equals("abc"));
    assertFalse("abc".equals("ABC"));
    assertTrue("abc".equals("abc"));
    assertTrue("ABC".equals("ABC"));
    assertFalse("AbC".equals("aBC"));
    assertFalse("AbC".equals("aBC"));
    assertTrue("".equals(""));
    assertFalse("".equals(null));
  }

  /*
   * TODO: needs rewriting to avoid compiler optimizations.
   */
  public void testEqualsIgnoreCase() {
    assertTrue("ABC".equalsIgnoreCase("abc"));
    assertTrue("abc".equalsIgnoreCase("ABC"));
    assertTrue("abc".equalsIgnoreCase("abc"));
    assertTrue("ABC".equalsIgnoreCase("ABC"));
    assertTrue("AbC".equalsIgnoreCase("aBC"));
    assertTrue("AbC".equalsIgnoreCase("aBC"));
    assertTrue("".equalsIgnoreCase(""));
    assertFalse("".equalsIgnoreCase(null));
  }

  /**
   * Tests hashing with strings.
   * 
   * The specific strings used in this test used to trigger failures because we
   * use a JavaScript object as a hash map to cache the computed hash codes.
   * This conflicts with built-in properties defined on objects -- see issue
   * #631.
   * 
   */
  public void testHashCode() {
    String[] testStrings = {
        "watch", "unwatch", "toString", "toSource", "eval", "valueOf",
        "constructor", "__proto__", "polygenelubricants", "xy", "x", "" };
    int[] javaHashes = {
        112903375, -274141738, -1776922004, -1781441930, 3125404, 231605032,
        -1588406278, 2139739112, Integer.MIN_VALUE, 3841, 120, 0 };

    for (int i = 0; i < testStrings.length; ++i) {
      String testString = testStrings[i];
      int expectedHash = javaHashes[i];

      // verify that the hash codes of these strings match their java
      // counterparts
      assertEquals("Unexpected hash for string " + testString, expectedHash,
          testString.hashCode());

      /*
       * Verify that the resulting hash code is numeric, since this is not
       * enforced in web mode.
       */
      String str = Integer.toString(expectedHash);
      for (int j = 0; j < str.length(); ++j) {
        char ch = str.charAt(j);
        assertTrue("Bad character '" + ch + "' (U+0" + Integer.toHexString(ch)
            + ")", ch == '-' || ch == ' ' || Character.isDigit(ch));
      }

      // get hashes again to verify the values are constant for a given string
      assertEquals(expectedHash, testStrings[i].hashCode());
    }
  }

  public void testIndexOf() {
    String haystack = "abcdefghi";
    assertEquals(haystack.indexOf("q"), -1);
    assertEquals(haystack.indexOf('q'), -1);
    assertEquals(haystack.indexOf("a"), 0);
    assertEquals(haystack.indexOf('a'), 0);
    assertEquals(haystack.indexOf('a', 1), -1);
    assertEquals(haystack.indexOf("bc"), 1);
    assertEquals(haystack.indexOf(""), 0);
  }

  public void testIntern() {
    String s1 = String.valueOf(new char[] {'a', 'b', 'c', 'd', 'e', 'f'});
    String s2 = String.valueOf(new char[] {'a', 'b', 'c', 'd', 'e', 'f'});
    assertTrue("strings not equal", s1.equals(s2));
    assertSame("interns are not the same reference", s1.intern(), s2.intern());
  }

  public void testLastIndexOf() {
    String x = "abcdeabcdef";
    assertEquals(9, x.lastIndexOf("e"));
    assertEquals(10, x.lastIndexOf("f"));
    assertEquals(-1, x.lastIndexOf("f", 1));
  }

  public void testLength() {
    String abc = String.valueOf(new char[] {'a', 'b', 'c'});
    assertEquals(3, abc.length());
    String str = "x";
    for (int i = 0; i < 16; i++) {
      str = str + str;
    }
    assertEquals(1 << 16, str.length());
    String cat = String.valueOf(new char[] {'C', '\uD801', '\uDF00', 'T'});
    assertEquals(4, cat.length());
  }

  /*
   * TODO: needs rewriting to avoid compiler optimizations.
   */
  public void testLowerCase() {
    assertEquals("abc", "AbC".toLowerCase());
    assertEquals("abc", "abc".toLowerCase());
    assertEquals("", "".toLowerCase());
  }

  /*
   * TODO: needs rewriting to avoid compiler optimizations.
   */
  public void testMatch() {
    assertFalse("1f", "abbbbcd".matches("b*"));
    assertFalse("2f", "abbbbcd".matches("b+"));
    assertTrue("3t", "abbbbcd".matches("ab*bcd"));
    assertTrue("4t", "abbbbcd".matches("ab+cd"));
    assertTrue("5t", "abbbbcd".matches("ab+bcd"));
    assertFalse("6f", "abbbbcd".matches(""));
    assertTrue("7t", "abbbbcd".matches("a.*d"));
    assertFalse("8f", "abbbbcd".matches("a.*e"));
  }

  /*
   * TODO: needs rewriting to avoid compiler optimizations.
   */
  public void testNull() {
    assertNull(returnNull());
    /*
     * The ""+ is there because GWT currently does not translate a+b
     * defensively enough to handle the case that both a and b are null.
     * Revisit this test if that is ever changed.
     */
    String a = "" + returnNull() + returnNull();
    assertEquals("nullnull", a);
  }

  public void testRegionMatches() {
    String test = String.valueOf(new char[] {'a', 'b', 'c', 'd', 'e', 'f'});
    assertTrue(test.regionMatches(1, "bcd", 0, 3));
    assertTrue(test.regionMatches(1, "bcdx", 0, 3));
    assertFalse(test.regionMatches(1, "bcdx", 0, 4));
    assertFalse(test.regionMatches(1, "bcdx", 1, 3));
    assertTrue(test.regionMatches(true, 0, "XAbCd", 1, 4));
    assertTrue(test.regionMatches(true, 1, "BcD", 0, 3));
    assertTrue(test.regionMatches(true, 1, "bCdx", 0, 3));
    assertFalse(test.regionMatches(true, 1, "bCdx", 0, 4));
    assertFalse(test.regionMatches(true, 1, "bCdx", 1, 3));
    assertTrue(test.regionMatches(true, 0, "xaBcd", 1, 4));
    test = test.toUpperCase();
    assertTrue(test.regionMatches(true, 0, "XAbCd", 1, 4));
    assertTrue(test.regionMatches(true, 1, "BcD", 0, 3));
    assertTrue(test.regionMatches(true, 1, "bCdx", 0, 3));
    assertFalse(test.regionMatches(true, 1, "bCdx", 0, 4));
    assertFalse(test.regionMatches(true, 1, "bCdx", 1, 3));
    assertTrue(test.regionMatches(true, 0, "xaBcd", 1, 4));
  }

  public void testReplace() {
    String axax = String.valueOf(new char[] {'a', 'x', 'a', 'x'});
    String aaaa = String.valueOf(new char[] {'a', 'a', 'a', 'a'});
    assertEquals("aaaa", axax.replace('x', 'a'));
    assertEquals("aaaa", aaaa.replace('x', 'a'));
    for (char from = 32; from < 250; ++from) {
      char to = (char) (from + 5);
      assertEquals(toS(to), toS(from).replace(from, to));
    }
    for (char to = 32; to < 250; ++to) {
      char from = (char) (to + 5);
      assertEquals(toS(to), toS(from).replace(from, to));
    }
    // issue 1480
    String exampleXd = String.valueOf(new char[] {
        'e', 'x', 'a', 'm', 'p', 'l', 'e', ' ', 'x', 'd'});
    assertEquals("example xd", exampleXd.replace('\r', ' ').replace('\n', ' '));
    String dogFood = String.valueOf(new char[] {
        'd', 'o', 'g', '\u0120', 'f', 'o', 'o', 'd'});
    assertEquals("dog food", dogFood.replace('\u0120', ' '));
    String testStr = String.valueOf(new char[] {
        '\u1111', 'B', '\u1111', 'B', '\u1111', 'B'});
    assertEquals("ABABAB", testStr.replace('\u1111', 'A'));
  }

  @DoNotRunWith(Platform.HtmlUnit)
  public void testReplaceAll() {
    String regex = hideFromCompiler("*[").replaceAll(
        "([/\\\\\\.\\*\\+\\?\\|\\(\\)\\[\\]\\{\\}])", "\\\\$1");
    assertEquals("\\*\\[", regex);
    String replacement = hideFromCompiler("\\").replaceAll("\\\\", "\\\\\\\\").replaceAll(
        "\\$", "\\\\$");
    assertEquals("\\\\", replacement);
    assertEquals("+1", hideFromCompiler("*[1").replaceAll(regex, "+"));
    String x1 = String.valueOf(new char[] {
        'x', 'x', 'x', 'a', 'b', 'c', 'x', 'x', 'd', 'e', 'x', 'f'});
    assertEquals("abcdef", x1.replaceAll("x*", ""));
    String x2 = String.valueOf(new char[] {
        '1', 'a', 'b', 'c', '1', '2', '3', 'd', 'e', '1', '2', '3', '4', 'f'});
    assertEquals("1\\1abc123\\123de1234\\1234f", x2.replaceAll("([1234]+)",
        "$1\\\\$1"));
    String x3 = String.valueOf(new char[] {'x', ' ', ' ', 'x'});
    assertEquals("\n  \n", x3.replaceAll("x", "\n"));
    String x4 = String.valueOf(new char[] {'\n', ' ', ' ', '\n'});
    assertEquals("x  x", x4.replaceAll("\\\n", "x"));
    String x5 = String.valueOf(new char[] {'x'});
    assertEquals("x\"\\", x5.replaceAll("x", "\\x\\\"\\\\"));
    assertEquals("$$x$", x5.replaceAll("(x)", "\\$\\$$1\\$"));
  }

  @DoNotRunWith(Platform.HtmlUnit)
  public void testReplaceString() {
    assertEquals("foobar", hideFromCompiler("bazbar").replace("baz", "foo"));
    assertEquals("$0bar", hideFromCompiler("foobar").replace("foo", "$0"));
    assertEquals("$1bar", hideFromCompiler("foobar").replace("foo", "$1"));
    assertEquals("\\$1bar", hideFromCompiler("foobar").replace("foo", "\\$1"));
    assertEquals("\\1", hideFromCompiler("*[)1").replace("*[)", "\\"));

    // issue 2363
    assertEquals("cb", hideFromCompiler("$ab").replace("$a", "c"));
    assertEquals("cb", hideFromCompiler("^ab").replace("^a", "c"));

    // test JS replacement characters
    assertEquals("a$$b", hideFromCompiler("a[x]b").replace("[x]", "$$"));
    assertEquals("a$1b", hideFromCompiler("a[x]b").replace("[x]", "$1"));
    assertEquals("a$`b", hideFromCompiler("a[x]b").replace("[x]", "$`"));
    assertEquals("a$'b", hideFromCompiler("a[x]b").replace("[x]", "$'"));
  }

  public void testSplit() {
    compareList("fullSplit", new String[] {"abc", "", "", "de", "f"},
        hideFromCompiler("abcxxxdexfxx").split("x"));
    compareList("emptyRegexSplit", new String[] {
        "", "a", "b", "c", "x", "x", "d", "e", "x", "f", "x"},
        hideFromCompiler("abcxxdexfx").split(""));
    String booAndFoo = hideFromCompiler("boo:and:foo");
    compareList("2:", new String[] {"boo", "and:foo"}, booAndFoo.split(":", 2));
    compareList("5:", new String[] {"boo", "and", "foo"}, booAndFoo.split(":",
        5));
    compareList("-2:", new String[] {"boo", "and", "foo"}, booAndFoo.split(":",
        -2));
    compareList("5o", new String[] {"b", "", ":and:f", "", ""},
        booAndFoo.split("o", 5));
    compareList("-2o", new String[] {"b", "", ":and:f", "", ""},
        booAndFoo.split("o", -2));
    compareList("0o", new String[] {"b", "", ":and:f"}, booAndFoo.split("o", 0));
    compareList("0:", new String[] {"boo", "and", "foo"}, booAndFoo.split(":",
        0));
    // issue 2742
    compareList("issue2742", new String[] {}, hideFromCompiler("/").split("/", 0));
  }

  public void testStartsWith() {
    String haystack = "abcdefghi";
    assertTrue(haystack.startsWith("abc"));
    assertTrue(haystack.startsWith("bc", 1));
    assertTrue(haystack.startsWith(haystack));
    assertFalse(haystack.startsWith(haystack + "j"));
  }

  /*
   * TODO: needs rewriting to avoid compiler optimizations.
   */
  public void testSubstring() {
    String haystack = "abcdefghi";
    assertEquals("cd", haystack.substring(2, 4));
    assertEquals("bc", "abcdef".substring(1, 3));
    assertEquals("bcdef", "abcdef".substring(1));
  }

  public void testToCharArray() {
    char[] a1 = "abc".toCharArray();
    char[] a2 = new char[] {'a', 'b', 'c'};
    for (int i = 0; i < a1.length; i++) {
      assertEquals(a1[i], a2[i]);
    }
  }

  /*
   * TODO: needs rewriting to avoid compiler optimizations.
   */
  public void testTrim() {
    trimRightAssertEquals("abc", "   \t abc \n  ");
    trimRightAssertEquals("abc", "abc".trim());
    trimRightAssertSame("abc", "abc");
    String s = '\u0023' + "hi";
    trimRightAssertSame(s, s);
    trimRightAssertEquals("abc", " abc".trim());
    trimRightAssertEquals("abc", "abc ".trim());
    trimRightAssertEquals("", "".trim());
    trimRightAssertEquals("", "   \t ".trim());
  }

  /*
   * TODO: needs rewriting to avoid compiler optimizations.
   */
  public void testUpperCase() {
    assertEquals("abc", "AbC".toLowerCase());
    assertEquals("abc", "abc".toLowerCase());
    assertEquals("", "".toLowerCase());
  }

  /*
   * TODO: needs rewriting to avoid compiler optimizations.
   */
  public void testValueOf() {
    assertTrue(String.valueOf(C.FLOAT_VALUE).startsWith(C.FLOAT_STRING));
    assertEquals(C.INT_STRING, String.valueOf(C.INT_VALUE));
    assertEquals(C.LONG_STRING, String.valueOf(C.LONG_VALUE));
    assertTrue(String.valueOf(C.DOUBLE_VALUE).startsWith(C.DOUBLE_STRING));
    assertEquals(C.CHAR_STRING, String.valueOf(C.CHAR_VALUE));
    assertEquals(C.CHAR_ARRAY_STRING, String.valueOf(C.CHAR_ARRAY_VALUE));
    assertEquals(C.CHAR_ARRAY_STRING_SUB, String.valueOf(C.CHAR_ARRAY_VALUE, 1,
        4));
    assertEquals(C.FALSE_STRING, String.valueOf(C.FALSE_VALUE));
    assertEquals(C.TRUE_STRING, String.valueOf(C.TRUE_VALUE));
  }

  /**
   * Helper method for testTrim to avoid compiler optimizations.
   * 
   * TODO: insufficient, compiler now inlines.
   */
  public void trimRightAssertEquals(String left, String right) {
    assertEquals(left, right.trim());
  }

  /**
   * Helper method for testTrim to avoid compiler optimizations.
   * 
   * TODO: insufficient, compiler now inlines.
   */
  public void trimRightAssertSame(String left, String right) {
    assertSame(left, right.trim());
  }

  private void compareList(String category, String[] desired, String[] got) {
    assertEquals(category + " length", desired.length, got.length);
    for (int i = 0; i < desired.length; i++) {
      assertEquals(category + " " + i, desired[i], got[i]);
    }
  }

  private String returnNull() {
    return null;
  }

  private String toS(char from) {
    return Character.toString(from);
  }

}
