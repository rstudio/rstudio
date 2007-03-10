/*
 * Copyright 2007 Google Inc.
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
 * TODO: document me.
 */
public class CharacterTest extends GWTTestCase {

  public static final int NUM_CHARS_HANDLED = 127;
  public static String allChars;

  /** Sets module name so that javascript compiler can operate */
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  static {
    StringBuffer b = new StringBuffer();
    for (char c = 0; c < NUM_CHARS_HANDLED; c++) {
      b.append(c);
    }
    allChars = b.toString();
  }

  /**
   * TODO: document me.
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

  class UpperCaseJudge extends Judge {
    public UpperCaseJudge(String s) {
      super(s);
    }

    public boolean pass(char c) {
      return Character.isUpperCase(c);
    }
  }

  class LowerCaseJudge extends Judge {
    public LowerCaseJudge(String s) {
      super(s);
    }

    public boolean pass(char c) {
      return Character.isLowerCase(c);
    }
  }

  Judge upperCaseJudge = new UpperCaseJudge(allChars);
  Judge lowerCaseJudge = new LowerCaseJudge(allChars);
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
  Judge digitJudge = new Judge(allChars) {
    public boolean pass(char c) {
      return Character.isDigit(c);
    }
  };
  Judge spaceJudge = new Judge(allChars) {
    public boolean pass(char c) {
      return Character.isSpace(c);
    }
  };
  Changer upperCaseChanger = new Changer(allChars) {
    public char change(char c) {
      return Character.toUpperCase(c);
    }
  };
  Changer lowerCaseChanger = new Changer(allChars) {
    public char change(char c) {
      return Character.toLowerCase(c);
    }
  };

  public static void testToFromDigit() {
    for (int i = 0; i < 16; i++) {
      assertEquals(i, Character.digit(Character.forDigit(i, 16), 16));
    }
    assertEquals(1, Character.digit('1', 10));
    assertEquals('9', Character.forDigit(9, 10));
  }

  /**
   * TODO: document me.
   */
  public abstract class Changer {
    String original;

    public Changer(String o) {
      original = o;
    }

    public String changed() {
      StringBuffer buf = new StringBuffer();
      for (int i = 0; i < original.length(); i++) {
        buf.append(change(original.charAt(i)));
      }
      return buf.toString();
    }

    public abstract char change(char c);
  }

  public void testConstructor() {
    assertEquals(new Character((char) 32), new Character(' '));
  }

  public void testCharValue() {
    assertEquals(new Character((char) 32).charValue(), (char) 32);
  }

  public void testToString() {
    assertEquals(new Character((char) 32).toString(), " ");
  }

  public void testUpperCase() {
    assertEquals("wrong number of uppercase letters", 26,
        upperCaseJudge.allPass().length());
    assertEquals("wrong number of uppercase letters after toUpperCase", 52,
        new UpperCaseJudge(upperCaseChanger.changed()).allPass().length());
  }

  public void testLowerCase() {
    assertEquals("wrong number of lowercase letters", 26,
        lowerCaseJudge.allPass().length());
    assertEquals("wrong number of lowercase letters after toLowerCase", 52,
        new LowerCaseJudge(lowerCaseChanger.changed()).allPass().length());
  }

  public void testDigit() {
    assertEquals("wrong number of digits", 10, digitJudge.allPass().length());
  }

  public void testLetter() {
    assertEquals("wrong number of letters", 52, letterJudge.allPass().length());
  }

  public void testLetterOrDigit() {
    assertEquals("wrong number of letters", 62,
        letterOrDigitJudge.allPass().length());
  }

  public void testSpace() {
    assertEquals("wrong number of spaces", 5, spaceJudge.allPass().length());
  }

}
