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

import com.google.gwt.junit.client.GWTTestCase;

/**
 * This test verifies that the static evaluation performed by the compiler
 * on constant string expressions is correct.
 * 
 * TODO: this is just copied from the old StringTest before it was improved,
 *     but we need to go through and remove tests that do not actually
 *     test the compiler. 
 */
public class CompilerConstantStringTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testCharAt() {
    assertEquals("abc".charAt(1), 'b');
  }

  public void testConcat() {
    assertEquals("abcdef", "abc" + "def");
    assertEquals("abcdef", "abc".concat("def"));
    assertEquals("".concat(""), "");
    char c = 'd';
    String s = "abc";
    assertEquals("abcd", "abc" + 'd');
    assertEquals("abcd", "abc" + c);
    assertEquals("abcd", s + 'd');
    assertEquals("abcd", s + c);
    s += c;
    assertEquals("abcd", s);
  }

  public void testConstructor() {
    char[] chars = {'a', 'b', 'c', 'd', 'e', 'f'};
    String constant = "abcdef";
    String shortString = "cde";
    assertEquals(new String(constant), constant);
    assertEquals(new String(chars), constant);
    assertEquals(new String(chars, 2, 3), shortString);
    assertEquals(new String(""), "");
    assertEquals(new String(new String(new String(new String("")))), "");
    assertEquals(new String(new char[] {}), "");
  }

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
        "constructor", "__proto__"};
    int[] savedHash = new int[testStrings.length];
    for (int i = 0; i < testStrings.length; ++i) {
      savedHash[i] = testStrings[i].hashCode();

      /*
       * Verify that the resulting hash code is numeric, since this is not
       * enforced in Production Mode.
       */
      String str = Integer.toString(savedHash[i]);
      for (int j = 0; j < str.length(); ++j) {
        char ch = str.charAt(j);
        assertTrue("Bad character '" + ch + "' (U+0" + Integer.toHexString(ch)
            + ")", ch == '-' || ch == ' ' || Character.isDigit(ch));
      }
    }
    // verify the hash codes are constant for a given string
    for (int i = 0; i < testStrings.length; ++i) {
      assertEquals(savedHash[i], testStrings[i].hashCode());
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

  public void testLastIndexOf() {
    String x = "abcdeabcdef";
    assertEquals(9, x.lastIndexOf("e"));
    assertEquals(10, x.lastIndexOf("f"));
    assertEquals(-1, x.lastIndexOf("f", 1));
  }

  public void testLength() {
    assertEquals(3, "abc".length());
    String str = "x";
    for (int i = 0; i < 16; i++) {
      str = str + str;
    }
    assertEquals(1 << 16, str.length());
  }

  public void testLowerCase() {
    assertEquals("abc", "AbC".toLowerCase());
    assertEquals("abc", "abc".toLowerCase());
    assertEquals("", "".toLowerCase());
  }

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

  public void testReplace() {
    assertEquals("axax".replace('x', 'a'), "aaaa");
    assertEquals("aaaa".replace('x', 'a'), "aaaa");
    for (char from = 32; from < 250; ++from) {
      char to = (char) (from + 5);
      assertEquals(toS(to), toS(from).replace(from, to));
    }
    for (char to = 32; to < 250; ++to) {
      char from = (char) (to + 5);
      assertEquals(toS(to), toS(from).replace(from, to));
    }
    // issue 1480
    assertEquals("example xd", "example xd".replace('\r', ' ').replace('\n', ' '));
    assertEquals("dog food", "dog\u0120food".replace('\u0120', ' '));
    assertEquals("ABABAB", "\u1111B\u1111B\u1111B".replace('\u1111', 'A'));
  }

  public void testReplaceAll() {
    assertEquals("abcdef", "xxxxabcxxdexf".replaceAll("x*", ""));
    assertEquals("1\\1abc123\\123de1234\\1234f", "1abc123de1234f".replaceAll(
        "([1234]+)", "$1\\\\$1"));
    assertEquals("\n  \n", "x  x".replaceAll("x", "\n"));
    assertEquals("x  x", "\n  \n".replaceAll("\\\n", "x"));
    assertEquals("x\"\\", "x".replaceAll("x", "\\x\\\"\\\\"));
    assertEquals("$$x$", "x".replaceAll("(x)", "\\$\\$$1\\$"));
  }

  public void testSplit() {
    compareList("fullSplit", new String[] {"abc", "", "", "de", "f"},
        "abcxxxdexfxx".split("x"));
    compareList("emptyRegexSplit", new String[] {
        "", "a", "b", "c", "x", "x", "d", "e", "x", "f", "x"},
        "abcxxdexfx".split(""));
    compareList("2:", "boo:and:foo".split(":", 2), new String[] {
        "boo", "and:foo"});
    compareList("5:", "boo:and:foo".split(":", 5), new String[] {
        "boo", "and", "foo"});
    compareList("-2:", "boo:and:foo".split(":", -2), new String[] {
        "boo", "and", "foo"});
    compareList("5o", "boo:and:foo".split("o", 5), new String[] {
        "b", "", ":and:f", "", ""});
    compareList("-2o", "boo:and:foo".split("o", -2), new String[] {
        "b", "", ":and:f", "", ""});
    compareList("0o", "boo:and:foo".split("o", 0), new String[] {
        "b", "", ":and:f"});
    compareList("0:", "boo:and:foo".split(":", 0), new String[] {
        "boo", "and", "foo"});
  }

  public void testStartsWith() {
    String haystack = "abcdefghi";
    assertTrue(haystack.startsWith("abc"));
    assertTrue(haystack.startsWith("bc", 1));
    assertTrue(haystack.startsWith(haystack));
    assertFalse(haystack.startsWith(haystack + "j"));
  }

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

  public void testUpperCase() {
    assertEquals("abc", "AbC".toLowerCase());
    assertEquals("abc", "abc".toLowerCase());
    assertEquals("", "".toLowerCase());
  }

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
   */
  public void trimRightAssertEquals(String left, String right) {
    assertEquals(left, right.trim());
  }

  /**
   * Helper method for testTrim to avoid compiler optimizations.
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
