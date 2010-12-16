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
package com.google.gwt.regexp.shared;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Unit tests for {@link RegExp}.
 * 
 * <p>
 * Tests the pure-Java implementation, using a hack that causes a GWTTestCase to
 * run as a vanilla JUnit TestCase (see {@link #getModuleName()}). The JS
 * implementation of RegExp is tested by {@link GwtRegExpTest}, which extends
 * this class.
 * <p>
 * The pure Java and GWT implementations are not 100% compatible. Some
 * discrepancies have been found when writing this test: search for DISCREPANCY
 * to find them.
 */
public class RegExpTest extends GWTTestCase {
  
  public static final String WORD_CHARACTERS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_";
  private static final String NOT_SPACE_CHARACTERS = allAsciiCharsBut(
      " \f\n\r\t\u000b\u00a0", 255);
  private static final String SPACE_CHARACTERS = " \f\n\r\t\u000b";

  /**
   * Generates a string containing all characters up to the given limit but the
   * characters in the given string.
   * 
   * @param exclude the characters not to return
   * @param limit the last character to include, typically 127 or 255
   * @return an ASCII string
   */
  private static String allAsciiCharsBut(String exclude, int limit) {
    StringBuilder sb = new StringBuilder();
    // DISCREPANCY: character 0 is handled differently by Webkit
    for (char c = 1; c <= limit; c++) {
      if (!exclude.contains(String.valueOf(c))) {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  private RegExp regExp;
  
  // This is a hack to force a GWTTestCase to run as a vanilla JUnit TestCase.
  @Override
  public String getModuleName() {
    return null;
  }
  
  public void testCompile_duplicatedFlags() {
    checkCompileThrows("regexp", "igg", true);
  }

  public void testCompile_unknownFlags() {
    checkCompileThrows("regexp", "z", true);
  }

  public void testExec_atLeastNtimes() {
    regExp = RegExp.compile("a{3,}");
    checkExecNoMatch("_a_");
    checkExecNoMatch("_aa_");
    checkExec("_aaa_", 1, "aaa");
    checkExec("_aaaab_", 1, "aaaa");
  }

  /** 
   * Checks that backreferences with two digits are accepted.
   */
  public void testExec_backreferenceMoreThanNine() {
    regExp = RegExp.compile("(1)(2)(3)(4)(5)(6)(7)(8)(9)(a)(b)\\1\\2\\3\\4\\5\\6\\7\\8\\9\\10\\11");
    checkExec("123456789ab123456789ab", 0, "123456789ab123456789ab", "1", "2",
        "3", "4", "5", "6", "7", "8", "9", "a", "b");
  }

  public void testExec_backreferenceNested() {
    regExp = RegExp.compile("(([ab])X\\2)([-,])\\1\\3\\2");
    checkExec("_aXa-aXa-a_", 1, "aXa-aXa-a", "aXa", "a", "-");
    checkExec("_aXa,aXa,a_", 1, "aXa,aXa,a", "aXa", "a", ",");
    checkExec("_bXb-bXb-b_", 1, "bXb-bXb-b", "bXb", "b", "-");
    checkExecNoMatch("_aXa-bXb-a_");
    checkExecNoMatch("_aXa,aXa,b_");
    checkExecNoMatch("_aXa,aXa-a_");
  }

  public void testExec_backreferenceOne() {
    regExp = RegExp.compile("([ab])X\\1");
    checkExec("_aXa_", 1, "aXa", "a");
    checkExec("_bXb_", 1, "bXb", "b");
    checkExecNoMatch("_aXb_");
  }

  public void testExec_carriageReturn() {
    checkAlias("\\r", "\r");
  }

  public void testExec_characterSetList() {
    regExp = RegExp.compile("[ace]");
    checkExec("abcde", 0, "a");
    checkExec("XXcYY", 2, "c");
    checkExecNoMatch("bdf");
  }

  public void testExec_characterSetRange() {
    regExp = RegExp.compile("[a-ce-z]");
    checkExec("abcde", 0, "a");
    checkExec("XXcYY", 2, "c");
    checkExecNoMatch("XXdYY");
  }

  // DISCREPANCY: chrome does not support \cX
  /*
  public void testExec_controlCharacterInvalid() {
    // regExp = RegExp.compile("\\c5");
    // checkExecNoMatch("c5");
    // DISCREPANCY: Java specificity: \c accepts any character; \cX is an alias 
    // for the character of code: asciiCode(X) ^ 64
    // checkExecNoMatch(allAsciiCharsBut(""));
    checkExecNoMatch(allAsciiCharsBut(Character.toString((char)('5' ^ 64)),
        255));
  }
  
  public void testExec_controlCharacterValid() {
    regExp = RegExp.compile("\\cM");
    // DISCREPANCY: does not work on Development Mode
    // checkExec("\r", 0, "\r");
    checkExecNoMatch(allAsciiCharsBut("\r", 255));
  }
  */

  public void testExec_digit() {
    checkAlias("\\d", "0123456789");
  }

  public void testExec_disjunction() {
    regExp = RegExp.compile("a|abc|def");
    checkExec("_a_", 1, "a");
    checkExec("_ab_", 1, "a");
    checkExec("_abc_", 1, "a"); // First expression has precedence
    checkExec("_def_", 1, "def");
    checkExecNoMatch("_de_");
  }

  public void testExec_dotMultiLine() {
    testExec_dot("m");
  }

  public void testExec_dotSingleLine() {
    testExec_dot("");
  }

  public void testExec_emptyFlags() {
    regExp = RegExp.compile("a", "");
    // DISCREPANCY: IE always sets lastIndex, even in non-global mode
    // regExp.setLastIndex(42);
    // checkExec("0a_3a_", 1, 42, "a");
    checkExec("0a_3a_", 1, "a");
  }

  public void testExec_escaping() {
    regExp = RegExp.compile("a\\(b");
    checkExec("_a(b_", 1, "a(b");
    checkExecNoMatch("_a\\(b_");
  }

  public void testExec_formFeed() {
    checkAlias("\\f", "\f");
  }

  public void testExec_fromNtoMtimes() {
    regExp = RegExp.compile("a{2,3}");
    checkExecNoMatch("_a_");
    checkExec("_aa_", 1, "aa");
    checkExec("_aaa_", 1, "aaa");
    checkExec("_aaaab_", 1, "aaa");
  }

  public void testExec_global() {
    regExp = RegExp.compile("a", "g");
    checkExec("0a_3a_", 1, 2, "a");
    checkExec("0a_3a_", 4, 5, "a");
    checkExecNoMatch("0a_3a_", 0);
  }

  public void testExec_greedinessDisjunction() {
    regExp = RegExp.compile("a|ab");
    checkExec("_ab_", 1, "a"); // First expression has precedence
  }

  public void testExec_greedinessOptionalMatch() {
    regExp = RegExp.compile("ab?");
    checkExec("_ab_", 1, "ab"); // Optional match has precedence
  }

  public void testExec_greedinessSeveralMatches() {
    regExp = RegExp.compile("a");
    checkExec("_a_a_", 1, "a"); // First match returned
  }

  public void testExec_hexa2() {
    checkAlias("\\x41", "A");
    checkAlias("\\x42", "B");
    checkAlias("\\x20", " ");
    checkAlias("\\xff", "\377");
  }

  public void testExec_lineFeed() {
    checkAlias("\\n", "\n");
  }

  public void testExec_matchBeginningMultiLine() {
    regExp = RegExp.compile("^ab+", "m");
    checkExec("abbc", 0, "abb");
    checkExecNoMatch("_abbc");
    checkExec("\nabbc\n", 1, "abb");
    checkExec("xxx\nabbc", 4, "abb");
  }

  public void testExec_matchBeginningSingleLine() {
    regExp = RegExp.compile("^ab+");
    checkExec("abbc", 0, "abb");
    checkExecNoMatch("_abbc");
    checkExecNoMatch("\nabbc\n");
    checkExecNoMatch("xxx\nabbc");
  }

  public void testExec_matchEndMultiLine() {
    regExp = RegExp.compile("b+a$", "m");
    checkExec("cbba", 1, "bba");
    checkExecNoMatch("cbba_");
    checkExec("\ncbba\n", 2, "bba");
    checkExec("cbba\nxxx", 1, "bba");
  }

  public void testExec_matchEndSingleLine() {
    regExp = RegExp.compile("b+a$");
    checkExec("cbba", 1, "bba");
    checkExecNoMatch("cbba_");
    // DISCREPANCY: Java specificity: $ matches end of input string even if it
    // has a trailing \n
    // checkExecNoMatch("\ncbba\n");
    // The discrepancy disappears if the trailing \n is doubled.
    checkExecNoMatch("\ncbba\n\n");
    checkExecNoMatch("cbba\nxxx");
  }

  public void testExec_negativeCharacterSetList() {
    regExp = RegExp.compile("[^ace]");
    checkExec("abcde", 1, "b");
    checkExec("aaBcc", 2, "B");
    checkExec("bdf", 0, "b");
  }

  public void testExec_negativeCharacterSetRange() {
    regExp = RegExp.compile("[^a-ce-z]");
    checkExec("abcde", 3, "d");
    checkExecNoMatch("xxcyy");
    checkExec("xxdyy", 2, "d");
  }

  public void testExec_negativeLookahead() {
    regExp = RegExp.compile("ab(?!cc)");
    checkExecNoMatch("_abcc_");
    checkExecNoMatch("_abccd_");
    checkExec("_abc_", 1, "ab");
  }

  public void testExec_negativeLookaheadComplex() {
    regExp = RegExp.compile("ab(?!cc).+");
    checkExecNoMatch("_ab");
    checkExec("_abX", 1, "abX");
    checkExecNoMatch("_abcc");
    checkExecNoMatch("_abccc");
    checkExec("_abXcc", 1, "abXcc");
  }

  public void testExec_negativeLookaheadNested() {
    regExp = RegExp.compile("ab(?=cc(?=d))");
    checkExec("_abccd_", 1, "ab");
    checkExecNoMatch("_abcc_");
  }

  public void testExec_nestedCapturingGroups() {
    regExp = RegExp.compile("a(?:b(?:c+)d)e");
    checkExec("_abcccde_", 1, "abcccde");

    regExp = RegExp.compile("a(?:b(?:c+)?d)e");
    checkExec("_abde_", 1, "abde");
    checkExec("_abcccde_", 1, "abcccde");
    checkExecNoMatch("_ac_");
  }

  public void testExec_noFlags() {
    regExp = RegExp.compile("a");
    // DISCREPANCY: IE always sets lastIndex, even in non-global mode
    // regExp.setLastIndex(42);
    // checkExec("0a_3a_", 1, 42, "a");
    checkExec("0a_3a_", 1, "a");
  }

  public void testExec_nonDigit() {
    checkAlias("\\D", allAsciiCharsBut("0123456789", 255));
  }

  public void testExec_nonSpace() {
    checkAlias("\\S", NOT_SPACE_CHARACTERS, SPACE_CHARACTERS);
  }

  public void testExec_nonWord() {
    checkAlias("\\W", allAsciiCharsBut(WORD_CHARACTERS, 127), WORD_CHARACTERS);
  }

  public void testExec_nonWordBoundary() {
    regExp = RegExp.compile("\\BX");
    checkExecNoMatch("ab X cd");
    checkExecNoMatch("ab\fX cd");
    checkExecNoMatch("ab\nX cd");
    checkExecNoMatch("ab\rX cd");
    checkExecNoMatch("ab\tX cd");
    checkExecNoMatch("ab\13X cd");
    checkExecNoMatch("ab\33X cd");
    checkExecNoMatch("ab\34X cd");
    for (int i = 0; i <= 32; i++) {
      checkExecNoMatch("ab" + (char) i + "X cd");
    }
    checkExec("aX", 1, "X");
    checkExec("_X", 1, "X");
    checkExec(" X aX", 4, "X");
  }

  public void testExec_nTimes() {
    regExp = RegExp.compile("a{3}");
    checkExecNoMatch("_a_");
    checkExecNoMatch("_aa_");
    checkExec("_aaa_", 1, "aaa");
    checkExec("_aaaab_", 1, "aaa");
  }

  public void testExec_nullInput() {
    regExp = RegExp.compile("a", "");
    // DISCREPANCY: IE always sets lastIndex, even in non-global mode
    // regExp.setLastIndex(42);
    // checkExecNoMatch(null, 42);
    checkExecNoMatch(null);

    regExp = RegExp.compile("a", "g");
    checkExecNoMatch(null, 0);
  }

  public void testExec_oneCapturingGroup() {
    regExp = RegExp.compile("a(b+)c");
    checkExec("_abbbc_", 1, "abbbc", "bbb");
    checkExecNoMatch("_ac_");
  }

  public void testExec_oneNonCapturingGroup() {
    regExp = RegExp.compile("a(?:b+c)?d");
    checkExec("_abbcd_", 1, "abbcd");
    checkExec("_ad_", 1, "ad");
  }

  public void testExec_optionalCapturingGroup() {
    regExp = RegExp.compile("a(b+)?c");
    checkExec("_abbbc_", 1, "abbbc", "bbb");
    checkExec("_ac_", 1, "ac", null);
  }

  public void testExec_plus() {
    regExp = RegExp.compile("a[bc]+d");
    checkExecNoMatch("ad");
    checkExec("abd", 0, "abd");
    checkExec("acd", 0, "acd");
    checkExec("abcbcd", 0, "abcbcd");
    checkExecNoMatch("abbbec");
  }

  public void testExec_positiveLookahead() {
    regExp = RegExp.compile("ab(?=cc)");
    checkExec("_abcc_", 1, "ab");
    checkExec("_abccd_", 1, "ab");
    checkExecNoMatch("_abc_");
  }

  public void testExec_positiveLookaheadDouble() {
    regExp = RegExp.compile("ab(?=cc)cc(?=d)");
    checkExec("_abccd_", 1, "abcc");
    checkExecNoMatch("_abcc_");
  }

  public void testExec_positiveLookaheadNested() {
    regExp = RegExp.compile("ab(?=cc(?=d))");
    checkExec("_abccd_", 1, "ab");
    checkExecNoMatch("_abcc_");
  }

  public void testExec_questionMark() {
    regExp = RegExp.compile("a[bc]?d");
    checkExec("ad", 0, "ad");
    checkExec("abd", 0, "abd");
    checkExec("acd", 0, "acd");
    checkExecNoMatch("abcd");
    checkExecNoMatch("aXd");
    checkExecNoMatch("ab");
  }

  public void testExec_space() {
    checkAlias("\\s", SPACE_CHARACTERS, NOT_SPACE_CHARACTERS);
  }

  public void testExec_star() {
    regExp = RegExp.compile("a[bc]*d");
    checkExec("ad", 0, "ad");
    checkExec("abd", 0, "abd");
    checkExec("acd", 0, "acd");
    checkExec("abcbcd", 0, "abcbcd");
    checkExecNoMatch("abbbec");
  }

  public void testExec_tab() {
    checkAlias("\\t", "\t");
  }

  public void testExec_unicode2() {
    // DISCREPANCY: IE bug and Java specificity: '\'u HH is not recognized.
    // checkAlias("\\u41", "A");
    // checkAlias("\\u42", "B");
    // checkAlias("\\u20", " ");
    // checkAlias("\\uff", "\255");
  }

  public void testExec_unicode4() {
    checkAlias("\\u0041", "A");
    checkAlias("\\u0042", "B");
    checkAlias("\\u0020", " ");
    checkAlias("\\u00ff", "\377");

    checkAlias("\\u0100", "\u0100");
    checkAlias("\\u1234", "\u1234");
    checkAlias("\\uffff", "\uffff");
  }

  public void testExec_verticalTab() {
    checkAlias("\\v", "\u000b");
  }

  public void testExec_word() {
    checkAlias("\\w", WORD_CHARACTERS, allAsciiCharsBut(WORD_CHARACTERS, 127));
  }

  public void testExec_wordBoundary() {
    regExp = RegExp.compile("\\bX");
    checkExec("ab X cd", 3, "X");
    checkExec("ab\fX cd", 3, "X");
    checkExec("ab\nX cd", 3, "X");
    checkExec("ab\rX cd", 3, "X");
    checkExec("ab\tX cd", 3, "X");
    checkExec("ab\13X cd", 3, "X");
    checkExec("ab\33X cd", 3, "X");
    checkExec("ab\34X cd", 3, "X");
    // DISCREPANCY: character 0 is non-space on Webkit
    for (int i = 1; i <= 32; i++) {
      checkExec("ab" + (char) i + "X cd", 3, "X");
    }
    checkExecNoMatch("aX");
    checkExecNoMatch("_X");
    checkExec("aX X", 3, "X");
  }

  public void testExec_zero() {
    // DISCREPANCY: Java specificity: \0 is not recognized.
    // checkAlias("\\0", "\0");
  }

  public void testGetGlobal() {
    assertTrue(RegExp.compile("test", "g").getGlobal());
    assertFalse(RegExp.compile("test", "im").getGlobal());
    assertFalse(RegExp.compile("test").getGlobal());
  }

  public void testGetIgnoreCase() {
    assertTrue(RegExp.compile("test", "i").getIgnoreCase());
    assertFalse(RegExp.compile("test", "gm").getIgnoreCase());
    assertFalse(RegExp.compile("test").getIgnoreCase());
  }

  public void testGetLastIndex_initiallyZero() {
    assertEquals(0, RegExp.compile("test").getLastIndex());
  }

  public void testGetMultiline() {
    assertTrue(RegExp.compile("test", "m").getMultiline());
    assertFalse(RegExp.compile("test", "ig").getMultiline());
    assertFalse(RegExp.compile("test").getMultiline());
  }

  public void testGetSource() {
    assertEquals("a(b|c)+d$", RegExp.compile("a(b|c)+d$").getSource());
  }

  public void testReplace_backslashReplacement() {
    regExp = RegExp.compile("A+");
    checkReplace("Abc", "\\x", "\\xbc");
  }

  public void testReplace_dollarReplacement() {
    regExp = RegExp.compile("A+");
    checkReplace("the A stops here", "$$", "the $ stops here");
    checkReplace("the A stops here", "$$$$", "the $$ stops here");
  }

  public void testReplace_doubleDigitGroupReplacement() {
    regExp = RegExp.compile("(1)(2)(3)(4)(5)(6)(7)(8)(9)(a)(b)(c)");
    checkReplace("it's 123456789abc.", "[$11]", "it's [b].");
  }

  public void testReplace_emptyGlobalRegExp() {
    regExp = RegExp.compile("", "g");
    checkReplace("abc", "x", "xaxbxcx");
  }

  public void testReplace_emptyInput() {
    regExp = RegExp.compile("A+");
    checkReplace("", "x", "");
  }

  public void testReplace_emptyRegExp() {
    regExp = RegExp.compile("");
    checkReplace("abc", "x", "xabc");
  }

  public void testReplace_emptyReplacement() {
    regExp = RegExp.compile("A+");
    checkReplace("AAA", "", "");
  }

  public void testReplace_first() {
    regExp = RegExp.compile("A+");
    checkReplace("AAA AAA", "x", "x AAA");
  }

  public void testReplace_global() {
    regExp = RegExp.compile("A+", "g");
    checkReplace("AAA AAA", "x", "x x");
  }

  public void testReplace_groupAmpersandReplacement() {
    regExp = RegExp.compile("A(B+)A");
    checkReplace("he likes ABBBA", "'$&'", "he likes 'ABBBA'");
    checkReplace("he likes ABBBA", "'$$&'", "he likes '$&'");
    checkReplace("he likes ABBBA", "'$$$&'", "he likes '$ABBBA'");
    checkReplace("he likes ABBBA", "'$$$$&'", "he likes '$$&'");
    checkReplace("he likes ABBBA", "'$$$$$&'", "he likes '$$ABBBA'");
    checkReplace("he likes ABBBA", "$&!", "he likes ABBBA!");
    checkReplace("he likes ABBBA", "$$&!", "he likes $&!");
    checkReplace("he likes ABBBA", "$$$&!", "he likes $ABBBA!");
    checkReplace("he likes ABBBA", "$$$$&!", "he likes $$&!");
    checkReplace("he likes ABBBA", "$$$$$&!", "he likes $$ABBBA!");
    checkReplace("he likes ABBBA", "$&$&", "he likes ABBBAABBBA");
    checkReplace("he likes ABBBA", "$$&$$&", "he likes $&$&");
    checkReplace("he likes ABBBA", "$& $&\n$&\n\n$&",
        "he likes ABBBA ABBBA\nABBBA\n\nABBBA");
    checkReplace("he likes ABBBA", "$$& $$&\n$$&\n\n$$&",
        "he likes $& $&\n$&\n\n$&");
  }

  public void testReplace_groupReplacement() {
    regExp = RegExp.compile("A(B+)A");
    checkReplace("he likes ABBBA", "$1", "he likes BBB");
  }

  public void testReplace_noMatch() {
    regExp = RegExp.compile("A+");
    checkReplace("none here", "x", "none here");
  }
  
  public void testReplace_unsupportedReplacement() {
    regExp = RegExp.compile("foo");
    checkReplaceThrows("", "$`", true);
    checkReplaceThrows("", "($`)", true);
    checkReplaceThrows("", "$$$`", true);
    checkReplaceThrows("", "($$$`)", true);
    checkReplaceThrows("", "$$$$$`", true);
    checkReplaceThrows("", "($$$$$`)", true);
    checkReplaceThrows("", "\n$`", true);
    checkReplaceThrows("", "\n\n$`", true);
    checkReplaceThrows("", " $`", true);
    checkReplace("foo", "$$`$$`", "$`$`");
    checkReplace("foo", "$$$$`$$$$`", "$$`$$`");
    checkReplace("foo", "\n$$`", "\n$`");
    checkReplace("foo", "\n\n$$`", "\n\n$`");
    checkReplace("foo", " $$`", " $`");
    checkReplaceThrows("", "$'", true);
    checkReplaceThrows("", "($')", true);
    checkReplaceThrows("", "$$$'", true);
    checkReplaceThrows("", "($$$')", true);
    checkReplaceThrows("", "$$$$$'", true);
    checkReplaceThrows("", "($$$$$')", true);
    checkReplaceThrows("", "\n$'", true);
    checkReplaceThrows("", "\n\n$'", true);
    checkReplaceThrows("", " $'", true);
    checkReplace("foo", "$$'$$'", "$'$'");
    checkReplace("foo", "$$$$'$$$$'", "$$'$$'");
    checkReplace("foo", "\n$$'", "\n$'");
    checkReplace("foo", "\n\n$$'", "\n\n$'");
    checkReplace("foo", " $$'", " $'");
  }

  public void testSetLastIndex() {
    regExp = RegExp.compile("test");
    regExp.setLastIndex(3);
    assertEquals(3, regExp.getLastIndex());
  }

  public void testSplit_emptyInput() {
    regExp = RegExp.compile(",");
    checkSplit("", "");
  }

  public void testSplit_emptyParts() {
    regExp = RegExp.compile(",");
    // DISCREPANCY: IE discards empty parts
    // checkSplit(",a,,b,", "", "a", "", "b", "");
  }

  public void testSplit_emptySeparator() {
    regExp = RegExp.compile("");
    checkSplit("ab", "a", "b");
  }

  public void testSplit_emptySeparatorExactLimit() {
    regExp = RegExp.compile("");
    checkSplit("ab", 2, "a", "b");
  }

  public void testSplit_emptySeparatorHighLimit() {
    regExp = RegExp.compile("");
    checkSplit("ab", 3, "a", "b");
  }

  public void testSplit_emptySeparatorLowLimit() {
    regExp = RegExp.compile("");
    checkSplit("ab", 1, "a");
  }

  public void testSplit_emptySeparatorZeroLimit() {
    regExp = RegExp.compile("");
    checkSplit("ab", 0);
  }

  public void testSplit_exactLimit() {
    regExp = RegExp.compile(",");
    checkSplit("a,b", 2, "a", "b");
  }

  public void testSplit_highLimit() {
    regExp = RegExp.compile(",");
    checkSplit("a,b", 3, "a", "b");
  }

  public void testSplit_lowLimit() {
    regExp = RegExp.compile(",");
    checkSplit("a,b", 1, "a");
  }

  public void testSplit_negLimit() {
    regExp = RegExp.compile(",");
    checkSplit("a,b", -1, "a", "b");
  }

  public void testSplit_noMatch() {
    regExp = RegExp.compile(",");
    checkSplit("ab", "ab");
  }

  public void testSplit_regExpSeparator() {
    regExp = RegExp.compile(" +|,");
    checkSplit("a   b,c", "a", "b", "c");
  }

  public void testSplit_veryHighLimit() {
    regExp = RegExp.compile(",");
    checkSplit("a,b", 1000, "a", "b");
  }

  public void testSplit_zeroLimit() {
    regExp = RegExp.compile(",");
    checkSplit("a,b", 0);
  }

  public void testTest() {
    assertTrue(RegExp.compile("test").test("test"));
    assertTrue(RegExp.compile("test", "i").test("TeSt"));
    assertFalse(RegExp.compile("test").test("mismatch"));
    assertTrue(RegExp.compile("[a-z]+").test("abc"));
    assertFalse(RegExp.compile("[a-z]+").test("42"));
  }

  /**
   * Checks that a regular expression matches all characters of a string and no
   * other.
   * 
   * @param regexp the regular expression
   * @param matched all ASCII characters the regexp must exactly match, on all
   *          browsers and in Java
   */
  private void checkAlias(String regexp, String matched) {
    checkAlias(regexp, matched, allAsciiCharsBut(matched, 255));
  }

  /**
   * Checks that a regular expression matches all characters of a string and
   * none of another.
   * 
   * <p>
   * In theory {@code matched} and {@code notMatched} should be the same. In
   * practice, discrepancies of regular expressions implementation across
   * browsers and in Java force to ignore some characters. This leads {@code
   * matched} to be a subset of {@code notMatched}.
   * 
   * @param regexp the regular expression
   * @param matched all characters the regexp must match, on all browsers and in
   *          Java
   * @param notMatched all characters the regexp must not match, on all browsers
   *          and in Java
   */
  private void checkAlias(String regexp, String matched, String notMatched) {
    regExp = RegExp.compile(regexp + "+");
    checkExec(matched, 0, matched);
    checkExecNoMatch(notMatched);
  }

  private void checkCompileThrows(String regexp, String flags,
      boolean onlyPureJava) {
    boolean thrown = false;
    try {
      RegExp.compile(regexp, flags);
    } catch (RuntimeException e) {
      thrown = true;
    }
    if (!onlyPureJava || !GWT.isClient()) {
      assertTrue(thrown);
    }
  }
  
  private void checkExec(String input, int expectedIndex,
      int expectedLastIndex, String... expectedGroups) {
    MatchResult matchResult = regExp.exec(input);
    assertNotNull("Match expected", matchResult);
    assertEquals("Wrong result input", input, matchResult.getInput());
    assertEquals("Wrong result index", expectedIndex, matchResult.getIndex());
    if (expectedLastIndex >= 0) {
      assertEquals("Wrong last index", expectedLastIndex,
          regExp.getLastIndex());
    }
    assertEquals("Wrong group count", expectedGroups.length,
        matchResult.getGroupCount());
    for (int group = 0; group < expectedGroups.length; group++) {
      String expectedGroup = expectedGroups[group];
      String actualGroup = matchResult.getGroup(group);
      if (expectedGroup == null && "".equals(actualGroup)) {
        // IE sets non-matching groups to "" instead of null.
      } else {
        assertEquals("Wrong group " + group, expectedGroup, actualGroup);
      }
    }
  }

  private void checkExec(String input, int expectedIndex,
      String... expectedGroups) {
    checkExec(input, expectedIndex, -1, expectedGroups);
  }

  private void checkExecNoMatch(String input) {
    checkExecNoMatch(input, 0);
  }

  private void checkExecNoMatch(String input, int expectedLastIndex) {
    MatchResult matchResult = regExp.exec(input);
    assertNull(matchResult);
    assertEquals("Wrong last index", expectedLastIndex, regExp.getLastIndex());
  }

  private void checkReplace(String input, String replacement, String expected) {
    String result = regExp.replace(input, replacement);
    assertNotNull("Replace result expected", result);
    assertEquals("Wrong replace result", expected, result);
  }

  private void checkReplaceThrows(String input, String replacement,
      boolean onlyPureJava) {
    boolean thrown = false;
    try {
      regExp.replace(input, replacement);
    } catch (RuntimeException e) {
      thrown = true;
    }
    if (!onlyPureJava || !GWT.isClient()) {
      assertTrue(thrown);
    }
  }
  
  private void checkSplit(SplitResult splitResult, String[] expectedParts) {
    assertNotNull("Split result expected", splitResult);
    assertEquals("Wrong result length", expectedParts.length,
        splitResult.length());
    for (int i = 0; i < expectedParts.length; i++) {
      String expectedPart = expectedParts[i];
      String actualPart = splitResult.get(i);
      assertNotNull("Split part expected", actualPart);
      assertEquals("Wrong split part " + i, expectedPart, actualPart);
    }
  }

  private void checkSplit(String input, int limit, String... expectedParts) {
    SplitResult splitResult = regExp.split(input, limit);
    checkSplit(splitResult, expectedParts);
  }

  private void checkSplit(String input, String... expectedParts) {
    SplitResult splitResult = regExp.split(input);
    checkSplit(splitResult, expectedParts);
  }

  private void testExec_dot(String flags) {
    regExp = RegExp.compile("a.c", flags);
    checkExecNoMatch("ac");
    checkExec("abc", 0, "abc");
    checkExecNoMatch("a\nc");
    // DISCREPANCY: Firefox bug: '.' should not match '\r'
    // checkExec("a\rc", 0, "a\rc");
    checkExecNoMatch("a\r\nc");
  }
}
