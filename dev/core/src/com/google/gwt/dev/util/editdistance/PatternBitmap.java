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
 * A bitmap array generator for the positions that a given character
 * appears within a pattern.  The array of bitmaps is indexed by a
 * CharIndex that represents the alphabet for the pattern.
 * One bitmap is produced for each character.  The bitmap for
 * a character "c" has the <tt>(1<<i)</tt> bit set for each position "i"
 * where that character appears in the pattern.
 *
 * Several implementations are provided for different string
 * sizes: int, long, int[].  The caller provides a (zeroed) array of the
 * desired type, sized according to the CharIndex; the array is
 * returned as a convenience.
 */
public class PatternBitmap {
  /**
   * Computes a pattern bitmap for a short character sequence.
   * For each character in the alphabet (represented by a CharIndex),
   * bits are set where that character appears in the sequence.
   * For this generator, the pattern must fit in an "int": the
   * character sequence must not be longer than Integer.SIZE.
   * @param s the character sequence defining the bits to be set
   * @param idx the alphabet for which bitmaps should be produced
   * @param result an array (of size <tt>idx.size()</tt> or greater) to
   *        hold the resulting bitmaps
   * @return the result array passed as a parameter, for convenience
   */
  public static int[] map(CharSequence s, CharIndex idx, int[] result) {
    int len = s.length();
    assert (len <= Integer.SIZE);
    for (int i = 0; i < len; i++) {
      result[idx.lookup(s.charAt(i))] |= (1 << i);
    }
    return result;
  }

  /**
   * Computes a pattern bitmap for a character sequence of any length.
   * For each character in the alphabet (represented by a CharIndex),
   * bits are set where that character appears in the sequence.
   * Each (per-character) bitmap is represented by an "int[]".
   * @param s the character sequence defining the bits to be set
   * @param idx the alphabet for which bitmaps should be produced
   * @param result an array (of size <tt>idx.size()</tt> or greater) to
   *        hold the resulting bitmaps
   * @param width how many bits to be use in each word, no more
   *        than Integer.SIZE
   * @return the result array passed as a parameter, for convenience
   */
  public static int[][] map(CharSequence s, CharIndex idx,
                             int[][] result, int width) {
    assert (width <= Integer.SIZE);
    int len = s.length();
    int rowSize = (len + width - 1) / width;

    /*
     * Use one zero-filled bitmap for alphabet characters not in the pattern
     */
    int[] nullElement = new int[rowSize];
    java.util.Arrays.fill(result, nullElement);

    int wordIndex = 0;          /* Which word we are on now */
    int bitWithinWord = 0;      /* Which bit within that word */

    for (int i = 0; i < s.length(); i++) {
      int[] r = result[idx.lookup(s.charAt(i))];
      if (r == nullElement) {

        /* Create a separate zero-filled bitmap for this alphabet character */
        r = result[idx.lookup(s.charAt(i))] = new int[rowSize];
      }
      r[wordIndex] |= (1 << bitWithinWord);

      /* Step to the next bit (and word if appropriate) */
      if (++bitWithinWord == width) {
        bitWithinWord = 0;
        wordIndex++;
      }
    }
    return result;
  }

  /**
   * Computes a pattern bitmap for a medium character sequence.
   * For each character in the alphabet (represented by a CharIndex),
   * bits are set where that character appears in the sequence.
   * For this generator, the pattern must fit in a "long": the
   * character sequence must not be longer than Long.SIZE.
   * @param s the character sequence defining the bits to be set
   * @param idx the alphabet for which bitmaps should be produced
   * @param result an array (of size <tt>idx.size()</tt> or greater) to
   *        hold the resulting bitmaps
   * @return the result array passed as a parameter, for convenience
   */
  public static long[] map(CharSequence s, CharIndex idx, long[] result) {
    int len = s.length();
    assert (len <= Long.SIZE);
    for (int i = 0; i < len; i++) {
      result[idx.lookup(s.charAt(i))] |= (1L << i);
    }
    return result;
  }
  private PatternBitmap() { /* Prevent instantiation */ }
}
