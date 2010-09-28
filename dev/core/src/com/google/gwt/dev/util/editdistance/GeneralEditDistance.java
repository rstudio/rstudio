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
 * An engine definition for computing string edit distances, and
 * implementation generators.
 *
 * Generally, edit distance is the minimum number of edit operations required
 * to transform one string into another.  Being a minimum transform, it
 * forms a metric space (one that is non-negative, symmetric, and obeys
 * triangle inequality).
 *
 * The most common form is Levenshtein distance, where the set of operations
 * consists of:
 *   deleting one character;
 *   inserting one character; or,
 *   substituting one character for another,
 * each having a "cost" of 1.
 *
 * Another common form is Damerau-Levenshtein distance, which adds
 *   transpose two adjacent characters
 * to the set of operations.  Note that many implementations of
 * Damerau distance do not account for insertion after transposition;
 * this "restricted" Damerau distance does not obey triangle inequality,
 * but is substantially easier to compute than the unrestricted metric.
 *
 * Another variation is to apply differential costs for the various
 * operations.  For example, substitutions could be weighted by
 * character type (e.g., vowel<=>vowel being a cheaper edit than
 * vowel<=>consonant).
 *
 * This class defines an engine for computing edit distance from
 * a fixed ("pattern") string to other ("target") strings.  The pattern
 * is fixed at instantiation time; this allows an implementation to
 * perform precomputations to make individual target computations faster.
 *
 * To allow further performance optimization, the GeneralEditDistance
 * contract does not require thread-safety.  A <tt>duplicate</tt> method is
 * defined to allow additional equivalent instances to be created
 * for separate threads; it is essentially a type-knowledgeable <tt>clone</tt>.
 */
public interface GeneralEditDistance {
  /**
   * Creates a duplicate (clone) of this distance engine, typically
   * in order to use it in another thread (as the GeneralEditDistance
   * contract does not specify thread safety).
   *
   * This method differs from Object.clone() in two ways:
   * it is typed (to GeneralEditDistance); and, it must not throw
   * an exception.
   *
   * @return     a clone of this instance.
   */
  GeneralEditDistance duplicate();

  /**
   * Computes edit distance from the pattern string (used to
   * construct this GeneralEditDistance instance) to a given target
   * string, bounded by a limit of interest.
   *
   * The specified limit must be non-negative.  If the actual distance
   * exceeds the limit, this method may return *any* value above the limit;
   * it need not be the actual distance.  The behavior of
   * illegal limit values is undefined; an implementation may
   * return an incorrect result or raise a runtime exception.
   *
   * @param target string for which distance is to be compared
   *        against a predefined pattern
   * @param limit maximum distance for which exact result is
   *        required (and beyond which any over-limit result
   *        can be returned)
   * @return edit distance from the predefined pattern to the
   *        target (or any value above limit, if the actual distance
   *        exceeds the prescribed limit)
   */
  int getDistance(CharSequence target, int limit);
}
