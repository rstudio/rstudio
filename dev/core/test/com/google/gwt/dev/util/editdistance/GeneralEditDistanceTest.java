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
package com.google.gwt.dev.util.editdistance;

import junit.framework.TestCase;

/**
 * Test cases for the GeneralEditDistance class.
 */
public class GeneralEditDistanceTest extends TestCase {

  /**
   * A basic set of tests for any unit-cost Levenshtein edit distance
   * implementation.
   */
  protected static abstract class AbstractLevenshteinTestCase extends TestCase {
    Factory g;
    AbstractLevenshteinTestCase(Factory g) {
     this.g = g;
    }

    /**
     * Tests a Levenshtein engine against the DP-based computation
     * for a bunch of string pairs.
     */
    public void testLevenshteinOnWords() {
      for (int i = 0; i < words.length; i++) {
        for (int j = 0; j < words.length; j++) {
          GeneralEditDistance ed = g.getInstance(words[i]);
          specificAlgorithmVerify(ed,
                                  words[i], words[j],
                                  wordsDistances[i][j]);
        }
      }
    }

    /** Tests Levenshtein edit distance on a longer pattern */
    public void testLongerPattern() {
      genericLevenshteinVerify("abcdefghijklmnopqrstuvwxyz",
                               "abcefghijklMnopqrStuvwxyz..",
                               5 /* dMS.. */);
    }

    /** Tests Levenshtein edit distance on a very short pattern */
    public void testShortPattern() {
      genericLevenshteinVerify("short", "shirt", 1);
    }

    /** Verifies zero-length behavior */
    public void testZeroLengthPattern() {
      String nonEmpty = "target";
      genericLevenshteinVerify("", nonEmpty, nonEmpty.length());
      genericLevenshteinVerify(nonEmpty, "", nonEmpty.length());
    }

    /** Tests the default Levenshtein engine on a pair of strings */
    void genericLevenshteinVerify(CharSequence s1, CharSequence s2,
                                  int expectedResult) {
      specificAlgorithmVerify(g.getInstance(s1),
                              s1, s2, expectedResult);
    }
  }

  /**
   * A base class for other reusable tests
   */
  public interface Factory {
    public GeneralEditDistance getInstance(CharSequence s1);
  }

  /** Test of the "best-of-breed" Levenshtein engine (getLevenshteinDistance) */
  public static class GenericLevenshteinTest extends AbstractLevenshteinTestCase {
    public GenericLevenshteinTest() {
      super(new Factory() {
          @Override
          public GeneralEditDistance getInstance(CharSequence s1) {
            return GeneralEditDistances.getLevenshteinDistance(s1);
          }
        });
    }
  }

  /** A very large string for testing. */
  public final static String MAGNA =
        "We have granted to God, and by this our present Charter have " +
        "confirmed, for Us and our Heirs for ever, that the Church of " +
        "England shall be free, and shall have all her whole Rights and " +
        "Liberties inviolable.  We have granted also, and given to all " +
        "the Freemen of our Realm, for Us and our Heirs for ever, these " +
        "Liberties under-written, to have and to hold to them and their " +
        "Heirs, of Us and our Heirs for ever.";

  /**
   * A small set of words for testing, including at least some of
   * each of these: empty, very short, more than 32/64 character,
   * punctuation, non-ASCII characters
   */
  static String[] words = { "", "a", "b", "c", "ab", "ace",
    "fortressing",      "inadequately", "prank",        "authored",
    "fortresing",       "inadeqautely", "prang",        "awthered",
    "cruller's",        "fanatic",      "Laplace",      "recollections",
    "Kevlar",           "underpays",    "jalape\u00f1o","ch\u00e2telaine",
    "kevlar",           "overpaid",     "jalapeno",     "chatelaine",
    "A survey of algorithms for running text search by Navarro appeared",
    "in ACM Computing Surveys 33#1: http://portal.acm.org/citation.cfm?...",
    "Another algorithm (Four Russians) that Navarro",
    "long patterns and high limits was not evaluated for inclusion here.",
    "long patterns and low limits were evaluated for inclusion here.",
    "Filtering algorithms also improve running search",
    "for pure edit distance."
  };

  static int[][] wordsDistances = new int[words.length][words.length];
  static {
    int[][] expect = wordsDistances;
    for (int i = 0; i < words.length; i++) {
      for (int j = 0; j < i; j++) {
        expect[i][j] = GeneralEditDistanceTest
                       .dynamicProgrammingLevenshtein(words[i],words[j]);
        expect[j][i] = expect[i][j];
      }
    }
  }
  /**
   * Computes Levenshtein edit distance using the far-from-optimal
   * dynamic programming technique.  This is here purely to verify
   * the results of better algorithms.
   */
  public static int dynamicProgrammingLevenshtein(String s1, String s2) {
    int[] lastRow = new int[s1.length() + 1];
    for (int i = 0; i < lastRow.length; i++) {
      lastRow[i] = i;
    }
    for (int j = 0; j < s2.length(); j++) {
      int[] thisRow = new int[lastRow.length];
      thisRow[0] = j + 1;
      char s2c = s2.charAt(j);
      for (int i = 1; i < thisRow.length; i++) {
        thisRow[i] = Math.min(Math.min(lastRow[i] + 1,thisRow[i - 1] + 1),
                              lastRow[i - 1] + ((s2c == s1.charAt(i - 1)) ? 0 : 1));
      }
      lastRow = thisRow;
    }
    return lastRow[lastRow.length - 1];
  }

  /**
   * Performs some edits on a string in a StringBuilder.
   * @param b string to be modified in place
   * @param alphabet some characters guaranteed not to be in the original
   * @param replaces how many single-character replacements to try
   * @param inserts how many characters to insert
   * @return the number of edits actually performed
   */
  public static int performSomeEdits(StringBuilder b,
                                     String alphabet,
                                     int replaces, int inserts) {
    java.util.Random r = new java.util.Random(768614336404564651L);
    int edits = 0;

    for (int i = 0; i < inserts; i++) {
      int where = r.nextInt(b.length());
      b.insert(where, alphabet.charAt(r.nextInt(alphabet.length())));
      edits++;
    }
    for (int i = 0; i < replaces; i++) {
      int where = r.nextInt(b.length());
      if (alphabet.indexOf(b.charAt(where)) < 0) {
        String sub = "" + alphabet.charAt(r.nextInt(alphabet.length()));
        b.replace(where, (where + 1), sub);
        edits++;
      }
    }
    return edits;
  }

  /**
   * Generates a long random alphabetic string,
   * suitable for use with testSomeEdits (using digits for the alphabet).
   * @param size desired string length
   * @param seed random number generator seed
   * @return random alphabetic string of the requested length
   */
  static String generateRandomString(int size, Long seed) {
    char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    /* Create a (repeatable) random string from the alphabet */
    java.util.Random rand = new java.util.Random(seed);
    StringBuilder huge = new StringBuilder(size);
    for (int i = 0; i < size; i++) {
      huge.append(alphabet[rand.nextInt(alphabet.length)]);
    }
    return huge.toString();
  }

  /** Exercises an edit distance engine across a wide range of limit values */
  static void genericVerification(GeneralEditDistance ed,
                                  CharSequence s1, CharSequence s2,
                                  int expectedResult) {

    if (s1.length() < 500) {
      /*
       * For small strings, try every limit
       */
      int max = Math.max(s1.length(), s2.length()) + 2;
      for (int k = 0; k < max; k++) {
        verifyResult(s1, s2, expectedResult, k,
                     ed.getDistance(s2, k));
      }
    } else {
      /*
       * For big strings, try a sampling of limits:
       *   0 to 3,
       *   another 4 on either side of the expected result
       *   s2 length
       */
      for (int k = 0; k < 4; k++) {
        verifyResult(s1, s2, expectedResult, k,
                     ed.getDistance(s2, k));
      }
      for (int k = Math.max(4, expectedResult - 4); k < expectedResult + 4; k++) {
        verifyResult(s1, s2, expectedResult, k,
                     ed.getDistance(s2, k));
      }
      verifyResult(s1, s2, expectedResult, s2.length(),
                   ed.getDistance(s2, s2.length()));
    }

    /* Always try near MAX_VALUE */
    assertEquals(ed.getDistance(s2, Integer.MAX_VALUE - 1), expectedResult);
    assertEquals(ed.getDistance(s2, Integer.MAX_VALUE), expectedResult);
  }

  /** Tests a specific engine on a pair of strings */
  static void specificAlgorithmVerify(GeneralEditDistance ed,
                                      CharSequence s1, CharSequence s2,
                                      int expectedResult) {
    genericVerification(ed, s1, s2, expectedResult);

    /* Try again with the same instance */
    genericVerification(ed, s1, s2, expectedResult);

    /* Try again with a duplicate instance */
    genericVerification(ed.duplicate(), s1, s2, expectedResult);
  }

  /**
   * Verifies the distance between an original string and some
   * number of simple edits on it.  The distance is assumed to
   * be unit-cost Levenshtein distance.
   */
  static void testSomeEdits(Factory g, String original,
                            int replaces, int inserts) {
    StringBuilder modified = new StringBuilder(original);
    int edits = performSomeEdits(modified, "0123456789", replaces, inserts);

    GeneralEditDistanceTest.specificAlgorithmVerify(
        g.getInstance(original), original, modified, edits);

    GeneralEditDistanceTest.specificAlgorithmVerify(
        g.getInstance(modified), modified, original, edits);

    GeneralEditDistanceTest.specificAlgorithmVerify(
        g.getInstance(modified).duplicate(), modified, original, edits);
  }

  /**
   * Verifies a single edit distance result.
   * If the expected distance is within limit, result must b
   * be correct; otherwise, result must be over limit.
   *
   * @param s1 one string compared
   * @param s2 other string compared
   * @param expectedResult correct distance from s1 to s2
   * @param k limit applied to computation
   * @param d distance computed
   */
  static void verifyResult(CharSequence s1, CharSequence s2,
                           int expectedResult, int k, int d) {
    if (k >= expectedResult) {
      assertEquals("Distance from " + s1 + " to " + s2
                    + " should be " + expectedResult
                    + " (within limit=" + k + ") but was " + d,
                   expectedResult, d);
    } else {
      assertTrue("Distance from " + s1 + " to " + s2
                 + " should be " + expectedResult
                 + " (exceeding limit=" + k + ") but was " + d,
                 d > k);
    }
  }

  /** Tests special case of Levenshtein edit distance with small limits */
  public void testAtMostOneError() {
    assertEquals(
        GeneralEditDistances.atMostOneError("unchanged", "unchanged"),
        0);

    StringBuilder un = new StringBuilder("un");
    assertEquals(
        GeneralEditDistances.atMostOneError("unchanged", un.append("changed")),
        0);

    assertEquals(GeneralEditDistances.atMostOneError("shirt", "short"), 1);
    assertEquals(GeneralEditDistances.atMostOneError("shirt", "shirts"), 1);
    assertEquals(GeneralEditDistances.atMostOneError("sort", "short"), 1);

    assertTrue(GeneralEditDistances.atMostOneError("short", "verylong") > 1);
    assertTrue(GeneralEditDistances.atMostOneError("short", "sharp") > 1);
    assertTrue(GeneralEditDistances.atMostOneError("small", "malls") > 1);
  }
}
