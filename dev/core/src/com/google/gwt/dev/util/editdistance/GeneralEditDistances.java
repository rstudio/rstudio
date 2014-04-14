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

/**
 * A collection of instance generators for the GeneralEditDistance interface.
 */
public class GeneralEditDistances {
  /**
   * Chooses the best implementation of Levenshtein string edit distance
   * available at the current time.
   */
  /*
   * As of 2007-08-23, the best algorithm known (to the author=mwyoung) for
   * short strings is one due to Eugene Myers, except for the special case
   * where the distance limit is 0 or 1.  The Myers algorithm also has good
   * worst-case performance for long strings when the edit distance is not
   * reasonably bounded.
   *
   * When there is a good bound, a variant of the Ukkonen algorithm due to
   * Berghel and Roach (modified by Michael Young to use linear space)
   * is faster for long strings.
   *
   * Note that other algorithms that perform better in some cases for running
   * text searches do not outperform Myers for rigid distance computations.
   * Notably:
   *   Navarro/Baeza-Yates (Algorithmica 23,2) simulates an NFA with an
   *   epsilon-cycle on the initial state (appropriate for running texts)
   *   and reports success without computing exact distance.  When adjusted
   *   to a fixed starting point and computing distance, its state machine
   *   is larger and it underperforms.
   *
   *   BITAP (Baeza-Yates/Gonnet, Manber/Wu) also simulates an NFA, and
   *   Navarro claims that it wins for small patterns and small limits for
   *   running search.  Experiments with a Java implementation showed that
   *   it beat Myers on pure string edit distance only for limits where the
   *   special 0-1 limit applied, where special-case comparison beats all.
   *
   * A survey of algorithms for running text search by Navarro appeared
   * in ACM Computing Surveys 33#1: http://portal.acm.org/citation.cfm?id=375365
   * Another algorithm (Four Russians) that Navarro claims superior for very
   * long patterns and high limits was not evaluated for inclusion here.
   * Filtering algorithms also improve running search, but do not help
   * for pure edit distance.
   */
  private static class Levenshtein implements GeneralEditDistance {
    /**
     * Long+bounded implementation class: distance-only Berghel-Roach.
     */
    private ModifiedBerghelRoachEditDistance berghel;

    /**
     * Short/unbounded implementation class: Myers bit-parallel.
     */
    private MyersBitParallelEditDistance myers;

    /**
     * Saved pattern, for specialized comparisons.
     */
    private final CharSequence pattern;

    /**
     * Length of saved pattern.
     */
    private final int patternLength;

    private Levenshtein(CharSequence pattern) {
      this.pattern = pattern;
      this.patternLength = pattern.length();
    }

    @Override
    public GeneralEditDistance duplicate() {
      Levenshtein dup = new Levenshtein(pattern);

      /* Duplicate the Myers engine, as it is cheaper than rebuilding */
      if (this.myers != null) {
        dup.myers = (MyersBitParallelEditDistance) this.myers.duplicate();
      }

      /* Do not duplicate the Berghel engine; it provides no savings. */

      return dup;
    }

    @Override
    public int getDistance(CharSequence target, int limit) {
      /* When the limit is 0 or 1, specialized comparisons are much faster. */
      if (limit <= 1) {
        return limit == 0 ?
                 (pattern.equals(target) ? 0 : 1) :
                 atMostOneError(pattern, target);
      }

      /*
       * The best algorithm for long strings depends on the resulting
       * edit distance (or the limit placed on it).  Without further
       * information on the likelihood of a low distance, we guess
       * based on the provided limit.  We currently lean toward using
       * the Myers algorithm unless we are pretty sure that the
       * Berghel-Roach algorithm will win (based on the limit).
       *
       * Note that when the string lengths are small (fewer characters
       * than bits in a long), Myers wins regardless of limit.
       */
      if ((patternLength > 64)
          && (limit < (target.length() / 10))) {
        if (berghel == null) {
          berghel = ModifiedBerghelRoachEditDistance.getInstance(pattern);
        }
        return berghel.getDistance(target, limit);
      }

      if (myers == null) {
        myers = MyersBitParallelEditDistance.getInstance(pattern);
      }

      return myers.getDistance(target, limit);
    }
  }

  /**
   * Compares two strings for at most one insert/delete/substitute difference.
   * Since operations cannot be composed, a simple case analysis is possible.
   *
   * @param s1 one string to be compared
   * @param s2 the other string to be compared
   * @return Levenshtein edit distance if no greater than 1;
   *         otherwise, more than 1
   */
  public static int atMostOneError(CharSequence s1, CharSequence s2) {
    int s1Length = s1.length();
    int s2Length = s2.length();
    int errors = 0;             /* running count of edits required */

    switch(s2Length - s1Length) {
      /*
       * Strings are the same length.  No single insert/delete is possible;
       * at most one substitution can be present.
       */
      case 0:
        for (int i = 0; i < s2Length; i++) {
          if ((s2.charAt(i) != s1.charAt(i)) && (errors++ != 0)) {
            break;
          }
        }
        return errors;

      /*
       * Strings differ in length by 1, so we have an insertion
       * (and therefore cannot have any other substitutions).
       */
      case 1: /* s2Length > s1Length */
        for (int i = 0; i < s1Length; i++) {
          if (s2.charAt(i) != s1.charAt(i)) {
            for (; i < s1Length; i++) {
              if (s2.charAt(i + 1) != s1.charAt(i)) {
                return 2;
              }
            }
            return 1;
          }
        }
        return 1;

      /* Same as above case, with strings reversed */
      case -1: /* s1Length > s2Length */
        for (int i = 0; i < s2Length; i++) {
          if (s2.charAt(i) != s1.charAt(i)) {
            for (; i < s2Length; i++) {
              if (s2.charAt(i) != s1.charAt(i + 1)) {
                return 2;
              }
            }
            return 1;
          }
        }
        return 1;

      /* Edit distance is at least difference in lengths; more than 1 here. */
      default:
        return 2;
    }
  }

  /**
   * Generates an GeneralEditDistance engine for a particular pattern string
   * based on Levenshtein distance.  Caller must ensure that the
   * pattern does not change (consider using pattern.toString() if
   * necessary) as long as the generated object is to be used.
   *
   * @param pattern a string from which distance computations are desired
   * @return an engine for computing Levenshtein distances from that pattern
   */
  public static GeneralEditDistance
      getLevenshteinDistance(CharSequence pattern) {
    return new Levenshtein(pattern);
  }

  private GeneralEditDistances() { }
}
