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
 * Tests for java.lang.Character.
 */
public class CharacterTest extends GWTTestCase {

  private static class CharSequenceAdapter implements CharSequence {
    private char[] charArray;
    private int start;
    private int end;

    public CharSequenceAdapter(char[] charArray) {
      this(charArray, 0, charArray.length);
    }
    
    public CharSequenceAdapter(char[] charArray, int start, int end) {
      this.charArray = charArray;
      this.start = start;
      this.end = end;
    }
    
    public char charAt(int index) {
      return charArray[index + start];
    }

    public int length() {
      return end - start;
    }

    public java.lang.CharSequence subSequence(int start, int end) {
      return new CharSequenceAdapter(charArray, this.start + start,
          this.start + end);
    }
  }

  /**
   * Helper class which applies some arbitrary char mutation function
   * to a string and returns it.
   */
  public abstract class Changer {
    String original;

    public Changer(String o) {
      original = o;
    }

    public abstract char change(char c);

    public String changed() {
      StringBuffer buf = new StringBuffer();
      for (int i = 0; i < original.length(); i++) {
        buf.append(change(original.charAt(i)));
      }
      return buf.toString();
    }
  }
  /**
   * Helper class which collects the set of characters which pass some
   * arbitrary boolean function. 
   */
  public abstract class Judge {
    String original;

    public Judge(String o) {
      original = o;
    }

    public String allPass() {
      StringBuffer buf = new StringBuffer();
      for (int i = 0; i < original.length(); i++) {
        if (pass(original.charAt(i))) {
          buf.append(original.charAt(i));
        }
      }
      return buf.toString();
    }

    public abstract boolean pass(char c);
  }

  class LowerCaseJudge extends Judge {
    public LowerCaseJudge(String s) {
      super(s);
    }

    public boolean pass(char c) {
      return Character.isLowerCase(c);
    }
  }

  class UpperCaseJudge extends Judge {
    public UpperCaseJudge(String s) {
      super(s);
    }

    public boolean pass(char c) {
      return Character.isUpperCase(c);
    }
  }

  public static String allChars;

  public static final int NUM_CHARS_HANDLED = 127;

  static {
    StringBuffer b = new StringBuffer();
    for (char c = 0; c < NUM_CHARS_HANDLED; c++) {
      b.append(c);
    }
    allChars = b.toString();
  }

  Judge digitJudge = new Judge(allChars) {
    public boolean pass(char c) {
      return Character.isDigit(c);
    }
  };
  Judge letterJudge = new Judge(allChars) {
    public boolean pass(char c) {
      return Character.isLetter(c);
    }
  };
  Judge letterOrDigitJudge = new Judge(allChars) {
    public boolean pass(char c) {
      return Character.isLetterOrDigit(c);
    }
  };
  Changer lowerCaseChanger = new Changer(allChars) {
    public char change(char c) {
      return Character.toLowerCase(c);
    }
  };
  Judge lowerCaseJudge = new LowerCaseJudge(allChars);
  Judge spaceJudge = new Judge(allChars) {
    @SuppressWarnings("deprecation") // Character.isSpace()
    public boolean pass(char c) {
      return Character.isSpace(c); // suppress deprecation
    }
  };
  Changer upperCaseChanger = new Changer(allChars) {
    public char change(char c) {
      return Character.toUpperCase(c);
    }
  };
  Judge upperCaseJudge = new UpperCaseJudge(allChars);

  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testCharValue() {
    assertEquals(new Character((char) 32).charValue(), (char) 32);
  }

  public void testCodePoint() {
    assertEquals(1, Character.charCount(65));
    assertEquals(2, Character.charCount(Character.MIN_SUPPLEMENTARY_CODE_POINT));
    char[] testPlain = new char[] { 'C', 'A', 'T' };
    char[] testUnicode = new char[] { 'C', '\uD801', '\uDF00', 'T' };
    CharSequence plainSequence = new CharSequenceAdapter(testPlain);
    CharSequence unicodeSequence = new CharSequenceAdapter(testUnicode);
    assertEquals(65, Character.codePointAt(testPlain, 1));
    assertEquals(65, Character.codePointAt(plainSequence, 1));
    assertEquals("codePointAt fails on surrogate pair", 67328,
        Character.codePointAt(testUnicode, 1));
    assertEquals("codePointAt fails on surrogate pair", 67328,
        Character.codePointAt(unicodeSequence, 1));
    assertEquals("codePointAt fails on first char of surrogate pair", 0xD801,
        Character.codePointAt(testUnicode, 1, 2));
    assertEquals(65, Character.codePointBefore(testPlain, 2));
    assertEquals(65, Character.codePointBefore(plainSequence, 2));
    assertEquals("codePointBefore fails on surrogate pair", 67328,
        Character.codePointBefore(testUnicode, 3));
    assertEquals("codePointBefore fails on surrogate pair", 67328,
        Character.codePointBefore(unicodeSequence, 3));
    assertEquals("codePointBefore fails on second char of surrogate pair",
        0xDF00, Character.codePointBefore(testUnicode, 3, 2));
    assertEquals("codePointCount(plain): ", 3,
        Character.codePointCount(testPlain, 0, 3));
    assertEquals("codePointCount(plain): ", 3,
        Character.codePointCount(plainSequence, 0, 3));
    assertEquals("codePointCount(unicode): ", 3,
        Character.codePointCount(testUnicode, 0, 4));
    assertEquals("codePointCount(unicode): ", 3,
        Character.codePointCount(unicodeSequence, 0, 4));
    assertEquals(1, Character.codePointCount(testPlain, 1, 1));
    assertEquals(1, Character.codePointCount(plainSequence, 1, 2));
    assertEquals(1, Character.codePointCount(testUnicode, 1, 2));
    assertEquals(1, Character.codePointCount(unicodeSequence, 1, 3));
    assertEquals(2, Character.codePointCount(testUnicode, 2, 2));
    assertEquals(2, Character.codePointCount(unicodeSequence, 2, 4));
    assertEquals(1, Character.offsetByCodePoints(testUnicode, 0, 4, 0, 1));
    assertEquals(1, Character.offsetByCodePoints(unicodeSequence, 0, 1));
    assertEquals("offsetByCodePoints(1,1): ", 3,
        Character.offsetByCodePoints(testUnicode, 0, 4, 1, 1));
    assertEquals("offsetByCodePoints(1,1): ", 3,
        Character.offsetByCodePoints(unicodeSequence, 1, 1));
    assertEquals("offsetByCodePoints(2,1): ", 3,
        Character.offsetByCodePoints(testUnicode, 0, 4, 2, 1));
    assertEquals("offsetByCodePoints(2,1): ", 3,
        Character.offsetByCodePoints(unicodeSequence, 2, 1));
    assertEquals(4, Character.offsetByCodePoints(testUnicode, 0, 4, 3, 1));
    assertEquals(4, Character.offsetByCodePoints(unicodeSequence, 3, 1));
    assertEquals(1, Character.offsetByCodePoints(testUnicode, 0, 4, 2, -1));
    assertEquals(1, Character.offsetByCodePoints(unicodeSequence, 2, -1));
    assertEquals(1, Character.offsetByCodePoints(testUnicode, 0, 4, 3, -1));
    assertEquals(1, Character.offsetByCodePoints(unicodeSequence, 3, -1));
    assertEquals("offsetByCodePoints(4.-1): ", 3,
        Character.offsetByCodePoints(testUnicode, 0, 4, 4, -1));
    assertEquals("offsetByCodePoints(4.-1): ", 3,
        Character.offsetByCodePoints(unicodeSequence, 4, -1));
    assertEquals(0, Character.offsetByCodePoints(testUnicode, 0, 4, 3, -2));
    assertEquals(0, Character.offsetByCodePoints(unicodeSequence, 3, -2));
    char[] nonBmpChar = new char[] { '\uD800', '\uDF46' };
    assertEquals(0x10346, Character.codePointAt(nonBmpChar, 0));
    assertEquals(1, Character.codePointCount(nonBmpChar, 0, 2));
  }
  
  public void testCompareTo() {
    assertTrue(Character.valueOf('A').compareTo('B') < 0);
    assertTrue(Character.valueOf('B').compareTo('A') > 0);
    assertTrue(Character.valueOf('C').compareTo('C') == 0);
    assertTrue(Character.valueOf('\uA001').compareTo('\uA000') > 0);
  }

  public void testConstructor() {
    assertEquals(new Character((char) 32), new Character(' '));
  }

  public void testDigit() {
    assertEquals("wrong number of digits", 10, digitJudge.allPass().length());
  }
  
  public void testSurrogates() {
    assertFalse(Character.isHighSurrogate('\uDF46'));
    assertTrue(Character.isLowSurrogate('\uDF46'));
    assertTrue(Character.isHighSurrogate('\uD800'));
    assertFalse(Character.isLowSurrogate('\uD800'));
    assertFalse(Character.isHighSurrogate('X'));
    assertFalse(Character.isLowSurrogate('X'));
    assertTrue(Character.isSurrogatePair('\uD800', '\uDF46'));
    assertFalse(Character.isSurrogatePair('\uDF46', '\uD800'));
    assertFalse(Character.isSurrogatePair('A', '\uDF46'));
    assertFalse(Character.isSurrogatePair('\uD800', 'A'));
    char[] chars = Character.toChars(0x10346);
    assertEquals(0xD800, chars[0]);
    assertEquals(0xDF46, chars[1]);
    assertEquals(2, Character.toChars(67328, chars, 0));
    assertEquals(0xD801, chars[0]);
    assertEquals(0xDF00, chars[1]);
    assertEquals(1, Character.toChars(65, chars, 0));
    assertEquals('A', chars[0]);
    assertTrue(Character.isSupplementaryCodePoint(0x10346));
    assertFalse(Character.isSupplementaryCodePoint(65));
    assertTrue(Character.isValidCodePoint(0x10346));
    assertTrue(Character.isValidCodePoint(65));
    assertFalse(Character.isValidCodePoint(0x1FFFFFFF));
    assertEquals(0x10346, Character.toCodePoint('\uD800', '\uDF46'));
  }

  public void testLetter() {
    assertEquals("wrong number of letters", 52, letterJudge.allPass().length());
  }

  public void testLetterOrDigit() {
    assertEquals("wrong number of letters", 62,
        letterOrDigitJudge.allPass().length());
  }

  public void testLowerCase() {
    assertEquals("wrong number of lowercase letters", 26,
        lowerCaseJudge.allPass().length());
    assertEquals("wrong number of lowercase letters after toLowerCase", 52,
        new LowerCaseJudge(lowerCaseChanger.changed()).allPass().length());
  }

  public void testSpace() {
    assertEquals("wrong number of spaces", 5, spaceJudge.allPass().length());
  }

  public void testToFromDigit() {
    for (int i = 0; i < 16; i++) {
      assertEquals(i, Character.digit(Character.forDigit(i, 16), 16));
    }
    assertEquals(1, Character.digit('1', 10));
    assertEquals('9', Character.forDigit(9, 10));
    assertEquals(-1, Character.digit('7', 6));
    assertEquals(-1, Character.digit('8', 8));
    assertEquals(-1, Character.digit('A', 10));
  }

  public void testToString() {
    assertEquals(" ", new Character((char) 32).toString());
  }

  public void testUpperCase() {
    assertEquals("wrong number of uppercase letters", 26,
        upperCaseJudge.allPass().length());
    assertEquals("wrong number of uppercase letters after toUpperCase", 52,
        new UpperCaseJudge(upperCaseChanger.changed()).allPass().length());
  }
  
  public void testValueOf() {
    assertEquals('A', Character.valueOf('A').charValue());
  }
}
