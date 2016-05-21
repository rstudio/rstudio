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
package com.google.gwt.emultest.java8.lang;

import com.google.gwt.emultest.java.lang.C;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.testing.TestUtils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;

/**
 * TODO: COMPILER OPTIMIZATIONS HAVE MADE THIS TEST NOT ACTUALLY TEST ANYTHING!
 * NEED A VERSION THAT DOESN'T USE STATICALLY DETERMINABLE STRINGS!
 *
 * See individual method TODOs for ones that still need work -- the ones without
 * comments are already protected against optimization.
 */
public class StringTest extends GWTTestCase {

  static volatile boolean TRUE = true;

  private static <T> T hideFromCompiler(T value) {
    return TRUE ? value : null;
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
    sb = new StringBuilder();
    sb.appendCodePoint(0x21);
    assertEquals("\u0021", new String(sb));
    sb = new StringBuilder();
    sb.appendCodePoint(0x10400);
    assertEquals("\uD801\uDC00", new String(sb));
  }

  public void testConstructorBytes() {
    byte bytes[] = new byte[] {
        'a', 'b', 'c', 'd', 'e', 'f'
    };
    String str = new String(bytes);
    assertEquals("abcdef", str);
    str = new String(bytes, 1, 3);
    assertEquals("bcd", str);
    try {
      new String(bytes, 1, 6);
      assertTrue("Should have thrown IOOB in Development Mode", !TestUtils.isJvm());
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      new String(bytes, -1, 2);
      assertTrue("Should have thrown IOOB in Development Mode", !TestUtils.isJvm());
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      new String(bytes, 6, 2);
      assertTrue("Should have thrown IOOB in Development Mode", !TestUtils.isJvm());
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  public void testConstructorLatin1() throws UnsupportedEncodingException {
    internalTestConstructorLatin1("ISO-8859-1");
    internalTestConstructorLatin1("iso-8859-1");
  }

  private void internalTestConstructorLatin1(String encoding) throws UnsupportedEncodingException {
    byte bytes[] = new byte[] {
        (byte) 0xE0, (byte) 0xDF, (byte) 0xE7, (byte) 0xD0, (byte) 0xE9, 'f'
    };
    String str = new String(bytes, encoding);
    assertEquals("àßçÐéf", str);
    str = new String(bytes, 1, 3, encoding);
    assertEquals("ßçÐ", str);
    try {
      new String(bytes, 1, 6, encoding);
      assertTrue("Should have thrown IOOB in Development Mode", !TestUtils.isJvm());
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      new String(bytes, -1, 2, encoding);
      assertTrue("Should have thrown IOOB in Development Mode", !TestUtils.isJvm());
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      new String(bytes, 6, 2, encoding);
      assertTrue("Should have thrown IOOB in Development Mode", !TestUtils.isJvm());
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      new String(bytes, 1, 6, Charset.forName(encoding));
      assertTrue("Should have thrown IOOB in Development Mode", !TestUtils.isJvm());
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      new String(bytes, -1, 2, Charset.forName(encoding));
      assertTrue("Should have thrown IOOB in Development Mode", !TestUtils.isJvm());
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      new String(bytes, 6, 2, Charset.forName(encoding));
      assertTrue("Should have thrown IOOB in Development Mode", !TestUtils.isJvm());
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  public void testConstructorUtf8() throws UnsupportedEncodingException {
    internalTestConstructorUtf8("UTF-8");
    internalTestConstructorUtf8("utf-8");
  }

  private void internalTestConstructorUtf8(String encoding) throws UnsupportedEncodingException {
    byte bytes[] = new byte[] {
        (byte) 0xC3, (byte) 0xA0, (byte) 0xC3, (byte) 0x9F, (byte) 0xC3,
        (byte) 0xA7, (byte) 0xC3, (byte) 0x90, (byte) 0xC3, (byte) 0xA9, 'f'
    };
    String str = new String(bytes, encoding);
    assertEquals("àßçÐéf", str);
    str = new String(bytes, 2, 6, encoding);
    assertEquals("ßçÐ", str);
    try {
      new String(bytes, 2, 12, encoding);
      assertTrue("Should have thrown IOOB in Development Mode", !TestUtils.isJvm());
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      new String(bytes, -1, 2, encoding);
      assertTrue("Should have thrown IOOB in Development Mode", !TestUtils.isJvm());
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      new String(bytes, 12, 2, encoding);
      assertTrue("Should have thrown IOOB in Development Mode", !TestUtils.isJvm());
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      new String(bytes, 2, 12, Charset.forName(encoding));
      assertTrue("Should have thrown IOOB in Development Mode", !TestUtils.isJvm());
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      new String(bytes, -1, 2, Charset.forName(encoding));
      assertTrue("Should have thrown IOOB in Development Mode", !TestUtils.isJvm());
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      new String(bytes, 12, 2, Charset.forName(encoding));
      assertTrue("Should have thrown IOOB in Development Mode", !TestUtils.isJvm());
    } catch (IndexOutOfBoundsException expected) {
    }
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

  public void testEquals() {
    assertFalse("ABC".equals("abc"));
    assertFalse("abc".equals("ABC"));
    assertTrue("abc".equals("abc"));
    assertTrue("ABC".equals("ABC"));
    assertFalse("AbC".equals("aBC"));
    assertFalse("AbC".equals("aBC"));
    assertTrue("".equals(""));
    assertFalse("".equals(null));

    // Randomize the string to avoid static eval.
    double r = Math.random();
    assertTrue((r + "").equals(r + ""));
    assertFalse((r + "ABC").equals(r + "abc"));
    assertFalse((r + "abc").equals(r + "ABC"));
    assertTrue((r + "abc").equals(r + "abc"));
    assertTrue((r + "ABC").equals(r + "ABC"));
    assertFalse((r + "AbC").equals(r + "aBC"));
    assertFalse((r + "AbC").equals(r + "aBC"));
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

  public void testGetBytesAscii() {
    // Simple ASCII should get through any standard encoding (EBCDIC users,
    // you're out of luck).
    String str = "This is a simple ASCII string";
    byte[] bytes = str.getBytes();
    assertEquals(str.length(), bytes.length);
    for (int i = 0; i < str.length(); ++i) {
      assertEquals((byte) str.charAt(i), bytes[i]);
    }
  }

  public void testGetBytesLatin1() throws UnsupportedEncodingException {
    internalTestGetBytesLatin1("ISO-8859-1");
    internalTestGetBytesLatin1("iso-8859-1");
  }

  private void internalTestGetBytesLatin1(String encoding) throws UnsupportedEncodingException {
    // Contains only ISO-Latin-1 characters.
    String str = "Îñtérñåtîöñålîzåtîöñ";
    byte[] bytes = str.getBytes(encoding);
    assertEquals(str.length(), bytes.length);
    for (int i = 0; i < str.length(); ++i) {
      assertEquals("latin1 byte " + i + " differs", (byte) str.charAt(i),
          bytes[i]);
    }
  }

  public void testGetBytesUtf8() throws UnsupportedEncodingException {
    internalTestGetBytesUtf8("UTF-8");
    internalTestGetBytesUtf8("utf-8");
  }

  private void internalTestGetBytesUtf8(String encoding) throws UnsupportedEncodingException {
    // Test a range of characters getting encoded to UTF8 in 1-2 bytes.
    char[] chars = new char[384];
    for (int i = 0; i < chars.length; ++i) {
      chars[i] = (char) i;
    }
    String str = String.copyValueOf(chars);
    byte[] bytes = str.getBytes(encoding);
    assertEquals(640, bytes.length);
    for (int i = 0; i < 128; ++i) {
      assertEquals("Position " + i, i, bytes[i]);
    }
    for (int i = 128; i < chars.length; ++i) {
      byte first = bytes[2 * i - 128];
      byte second = bytes[2 * i - 127];
      char ch = str.charAt(i);
      assertEquals("byte " + i + " differs", ch,
          ((first & 31) << 6) | (second & 63));
    }

    // non-BMP characters, all take 4 UTF8 bytes.
    int firstCodePoint = 0x100000;
    int numChars = chars.length / 2;
    for (int i = 0; i < numChars; ++i) {
      Character.toChars(firstCodePoint + i, chars, 2 * i);
    }
    str = String.copyValueOf(chars);
    bytes = str.getBytes(encoding);
    assertEquals(4 * numChars, bytes.length);
    for (int i = 0; i < numChars; ++i) {
      assertEquals("1st byte of " + i, (byte) 0xF4, bytes[4 * i]);
      assertEquals("2nd byte of " + i, (byte) 0x80, bytes[4 * i + 1]);
      assertEquals("3rd byte of " + i, (byte) 0x80 + ((i >> 6) & 63),
          bytes[4 * i + 2]);
      assertEquals("4th byte of " + i, (byte) 0x80 + (i & 63),
          bytes[4 * i + 3]);
    }
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
       * enforced in Production Mode.
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

  public void testJoin() {
    assertEquals("", String.join("", ""));
    assertEquals("", String.join(",", ""));
    assertEquals("", String.join(",", Arrays.<String>asList()));

    assertEquals("a", String.join("", "a"));
    assertEquals("a", String.join(",", "a"));
    assertEquals("a", String.join(",", Arrays.asList("a")));

    assertEquals("ab", String.join("", "a", "b"));
    assertEquals("a,b", String.join(",", "a", "b"));
    assertEquals("a,b", String.join(",", Arrays.asList("a", "b")));

    assertEquals("abc", String.join("", "a", "b", "c"));
    assertEquals("a,b,c", String.join(",", "a", "b", "c"));
    assertEquals("a,b,c", String.join(",", Arrays.asList("a", "b", "c")));
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

    assertEquals("abc", "AbC".toLowerCase(Locale.US));
    assertEquals("abc", "abc".toLowerCase(Locale.US));
    assertEquals("", "".toLowerCase(Locale.US));

    assertEquals("abc", "AbC".toLowerCase(Locale.getDefault()));
    assertEquals("abc", "abc".toLowerCase(Locale.getDefault()));
    assertEquals("", "".toLowerCase(Locale.getDefault()));
  }

  public void testMatch() {
    assertFalse("1f", hideFromCompiler("abbbbcd").matches("b*"));
    assertFalse("2f", hideFromCompiler("abbbbcd").matches("b+"));
    assertTrue("3t", hideFromCompiler("abbbbcd").matches("ab*bcd"));
    assertTrue("4t", hideFromCompiler("abbbbcd").matches("ab+cd"));
    assertTrue("5t", hideFromCompiler("abbbbcd").matches("ab+bcd"));
    assertFalse("6f", hideFromCompiler("abbbbcd").matches(""));
    assertTrue("7t", hideFromCompiler("abbbbcd").matches("a.*d"));
    assertFalse("8f", hideFromCompiler("abbbbcd").matches("a.*e"));
    // issue #7736
    assertTrue("9t.1", hideFromCompiler("").matches("(|none)"));
    assertTrue("9t.2", hideFromCompiler("none").matches("(|none)"));
    assertFalse("9f.1", hideFromCompiler("ab").matches("(|none)"));
    assertFalse("9f.2", hideFromCompiler("anoneb").matches("(|none)"));
    assertTrue("10t", hideFromCompiler("none").matches("^(|none)$"));
    assertFalse("10f", hideFromCompiler("abbbbcd").matches("^b*$"));
    assertTrue("11t.1", hideFromCompiler("").matches("|none"));
    assertTrue("11t.2", hideFromCompiler("none").matches("|none"));
    assertFalse("11f.1", hideFromCompiler("ab").matches("|none"));
    assertFalse("11f.2", hideFromCompiler("anoneb").matches("|none"));
    assertTrue("12t", hideFromCompiler("none").matches("^|none$"));
  }

  public void testNull() {
    {
      assertNull(returnNull());
      String a = returnNull() + returnNull();
      assertEquals("nullnull", a);

      String s = returnNull();
      s += returnNull();
      assertEquals("nullnull", s);
    }

    // Same tests allowing the compiler to propagate constants.
    {
      String a = null + (String) null;
      assertEquals("nullnull", a);

      String s = null;
      s += null;
      assertEquals("nullnull", s);
    }
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
    test = test.toUpperCase(Locale.ROOT);
    assertTrue(test.regionMatches(true, 0, "XAbCd", 1, 4));
    assertTrue(test.regionMatches(true, 1, "BcD", 0, 3));
    assertTrue(test.regionMatches(true, 1, "bCdx", 0, 3));
    assertFalse(test.regionMatches(true, 1, "bCdx", 0, 4));
    assertFalse(test.regionMatches(true, 1, "bCdx", 1, 3));
    assertTrue(test.regionMatches(true, 0, "xaBcd", 1, 4));

    try {
      test.regionMatches(-1, null, -1, -1);
      fail();
    } catch (NullPointerException expected) {
      // NPE must be thrown before any range checks
    }

    try {
      test.regionMatches(true, -1, null, -1, -1);
      fail();
    } catch (NullPointerException expected) {
      // NPE must be thrown before any range checks
    }
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

    // Splitting an empty string should result in an array containing a single
    // empty string.
    String[] s = "".split(",");
    assertTrue(s != null);
    assertTrue(s.length == 1);
    assertTrue(s[0] != null);
    assertTrue(s[0].length() == 0);
  }

  public void testSplit_emptyExpr() {
    // TODO(rluble):  implement JDK8 string.split semantics and fix test.
    String[] expected = (TestUtils.getJdkVersion() > 7) ?
        new String[] {"a", "b", "c", "x", "x", "d", "e", "x", "f", "x"} :
        new String[] {"", "a", "b", "c", "x", "x", "d", "e", "x", "f", "x"};
    compareList("emptyRegexSplit", expected, "abcxxdexfx".split(""));
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

  public void testToString() {
    String str = hideFromCompiler("abc");
    assertSame("str same as str.toString()", str, str.toString());
    // that one is mostly to debug how the code gets compiled to JS.
    assertEquals("concat with str.toString()", "0abcd", "0" + str.toString() + "d");

    // issue 4301
    Object s = TRUE ? "abc" : createJavaScriptObject();
    assertSame("s same as s.toString()", s, s.toString());
  }

  /*
   * TODO: needs rewriting to avoid compiler optimizations.
   */
  public void testTrim() {
    trimRightAssertEquals("abc", "   \t abc \n  ");
    trimRightAssertEquals("abc", "abc");
    trimRightAssertSame("abc", "abc");
    String s = '\u0023' + "hi";
    trimRightAssertSame(s, s);
    trimRightAssertEquals("abc", " abc");
    trimRightAssertEquals("abc", "abc ");
    trimRightAssertEquals("", "");
    trimRightAssertEquals("", "   \t ");

    // Check for removal of nulls; trim treats nulls as if they are whitespace.
    // See issue 8534.

    trimRightAssertEquals("abc", "\0\0\0 abc");
    trimRightAssertEquals("abc", "abc\0\0\0");
    trimRightAssertEquals("abc", "abc   \0\0\0");
    trimRightAssertEquals("abc", "   \0 \0 \0" + "abc");
    trimRightAssertEquals("abc \0 a", "    abc \0 a");
    trimRightAssertEquals("abc \0 a", "    abc \0 a   \0");
    trimRightAssertEquals("abc \0 a", "    abc \0 a   \0   ");
    trimRightAssertEquals("\u0021\u0020abc", "\u0019\u0017\u0021\u0020abc\u0019\u0017\u0018 ");
    trimRightAssertEquals("\u0021 abc",
        "\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007\u0008\u0009" +
        "\n" + "\u000b\u000c" + "\r" + "\u000e\u000f" +
        "\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019" +
        "\u001A\u001b\u001c\u001d\u001e\u001f\u0020\u0021 " + "abc");

    // JavaScript would trim \u2029 and other unicode whitespace type characters; but Java wont
    trimRightAssertEquals("\u2029abc\u00a0","\u2029abc\u00a0");
  }

  /*
   * TODO: needs rewriting to avoid compiler optimizations.
   */
  public void testUpperCase() {
    assertEquals("ABC", "AbC".toUpperCase());
    assertEquals("ABC", "abc".toUpperCase());
    assertEquals("", "".toUpperCase());

    assertEquals("ABC", "AbC".toUpperCase(Locale.US));
    assertEquals("ABC", "abc".toUpperCase(Locale.US));
    assertEquals("", "".toUpperCase(Locale.US));

    assertEquals("ABC", "AbC".toUpperCase(Locale.getDefault()));
    assertEquals("ABC", "abc".toUpperCase(Locale.getDefault()));
    assertEquals("", "".toUpperCase(Locale.getDefault()));
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
    assertEquals(
        C.CHAR_ARRAY_STRING_SUB, String.valueOf(C.CHAR_ARRAY_VALUE, 1,
        4));
    assertEquals(C.FALSE_STRING, String.valueOf(C.FALSE_VALUE));
    assertEquals(C.TRUE_STRING, String.valueOf(C.TRUE_VALUE));
    assertEquals(C.getLargeCharArrayString(), String.valueOf(C.getLargeCharArrayValue()));
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
    if (Math.random() < -1) {
      // Can never happen, but fools the compiler enough not to optimize this call.
      fail();
      return "";
    }
    return null;
  }

  private String toS(char from) {
    return Character.toString(from);
  }

  private static Object createJavaScriptObject() {
    if (TestUtils.isJvm()) {
      return new Object();
    }
    return createJavaScriptObject0();
  }

  private static native Object createJavaScriptObject0() /*-{
    return {};
  }-*/;

}
