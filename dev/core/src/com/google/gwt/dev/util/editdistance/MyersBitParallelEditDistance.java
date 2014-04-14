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
 * Computes Levenshtein string-edit distance using the
 * algorithm of Eugene Myers (see "A fast bit-vector algorithm for
 * approximate string matching based on dynamic progamming",
 * Journal of the ACM, 46(3): 395-415, 1999), using the reformulations
 * of Heikki Hyyro (see "Explaining and Extending the Bit-parallel
 * Approximate String Matching Algorithm of Myers") along with
 * Gonzalo Navarro ("Faster Bit-Parallel Approximate String Matching",
 * Proc. 13th Combinatorial Pattern Matching (CPM'2002)).
 */
public abstract class MyersBitParallelEditDistance
    implements GeneralEditDistance, Cloneable {
  /*
   * The Myers algorithm is based on an adaptation of the traditional
   * dynamic programming algorithm, in which a matrix of distances
   * is computed column-wise (or row-wise).  For strings of length
   * M and N, an MxN matrix (D) is filled with distances for substrings:
   * D[i,j] holds the distance for string1[0..i]=>string2[0..j].
   * The matrix is initially populated with the trivial values
   * D[0,j]=j and D[i,0]=i; and then expanded with the rule:
   *    D[i,j] = min( D[i-1,j]+1,       // insertion
   *                  D[i,j-1]+1,       // deletion
   *                  (D[i-1,j-1]
   *                   + (string1[i]==string2[j])
   *                      ? 0           // match
   *                      : 1           // substitution ) )
   *
   * The Myers algorithm takes advantage of the observation that
   * the difference from one matrix position to its higher diagonal
   * is either 0 or 1, and consequently row-to-row and column-to-column
   * differences are 1, 0, or -1.  It organizes whole columns of
   * differences into bit arrays, computing new columns with
   * arithmetic on whole words.  The edit distance is computed
   * by tracking the column changes in the last row.
   *
   * The description below follows the naming from the Hyrro papers above.
   * The pattern string (length M) is arranged vertically; the
   * string to be matched (length N) is horizontal.  Columns are
   * represented by bit-arrays with (1<<j) set for row i.  The
   * inter-cell differences are grouped into column-variables:
   * VN has bits set when the transition from row i-1 to row i is
   * negative; VP has bits set when it is positive; HN and HP
   * have bits set for column j-1 to j transitions that are
   * negative or positive, respectively.  DZ has bits set when
   * the diagonal from (i-1,j-1) to (i,j) is zero.
   *
   * The computation proceeds as follows:
   *    We start at column 0, where all VP bits are 1.  The distance
   *    is accumulated in row M, with initial value M in the matrix.
   *
   *    For each new column, we compute the diagonal-zero (DZ) first.
   *    This happens under three circumstances:
   *
   *      when the pattern matches in this position ("match" above):
   *        We make use of a bitmask, one for each character
   *        in the alphabet, that has bits set wherever that character
   *        appears in the pattern.  These bitmasks are pre-computed
   *        when the pattern is established (in the class constructor).
   *        In the column loop, we simply look up the bitmask for the
   *        character in this column.  We call this PM.
   *
   *      when the prior column has a negative vertical transition
   *       in this row ("deletion"):
   *        This is simply the VN bit array from the prior column.
   *
   *      when the row above has a negative transition in this column
   *       ("insertion").
   *        This happens when the diagonal coming into the cell above
   *        (in *this* column) is zero but the vertical transition
   *        in the preceding column for that row is positive.  That is:
   *
   *                D[i-2,j-1]
   *                    |     \
   *                    +1       0
   *                    |           \
   *                D[i-1,j-1]  -1 -> D[i-1,j]
   *                    |     \         |
   *                    ?        0      +1
   *                    |            \  |
   *                D[i,j-1]          D[i,j]
   *
   *        That is DZ[i,j] = DZ[i-1,j] & VP[i-1,j-1].
   *        Note that this is recursive in the DZ[*,j] bit array, but we
   *        want to compute the whole array at once.  We need an expression
   *        that will propagate a 1 bit down the DZ column when the associated
   *        VP bit is set.  Noting that the DZ[*,0] is the least significant
   *        bit, this means propagating to more significant bits.  The carry bit
   *        of an addition has this behavior.  If we set
   *            DZ = (PM | VN)          // first two cases
   *        then we can propagate bits up iff the corresponding VP bit
   *        is set with:
   *            DZ |= (((DZ & VP) + VP) ^ VP);
   *        The (DZ&VP) piece covers our required condition on VP.  Adding
   *        the VP bits causes any 1 bits from (DZ&VP) to result in a carry,
   *        but also introduces a 1 bit where VP=1 but DZ=0, which are then
   *        cleared with the ^VP piece.  The propagated bits are |ed into the
   *        base case to get a final result.
   *
   *    Once we have DZ, we can compute HP and HN.  Note that VP and VN have
   *    not yet been updated, and so refer to the preceding column; DZ refers
   *    to the current column.
   *      HN is true when DZ is true in this column but VP was true
   *        in the prior column (HN = DZ & VP)
   *      HP is true when: the prior VN is true (otherwise, the diagonal would
   *        be negative, and that cannot happen); or, DZ is false (meaning
   *        the diagonal increased by 1) and the prior-column VP is also false
   *        (HP = VN | ~(DZ|VP))
   *
   *    Once we have HP and HN, we can tally the effect on the last row (adding
   *    or subtracting one, respectively).
   *
   *    Finally, we can compute VP and VN for this column, in preparation
   *    for the next round.  These computations are analagous to the ones for
   *    HN and HP above, except that there, the incoming V values already
   *    reflected the prior column and DZ this one, but now the H and DZ values
   *    reflect the same column.  So, we first shift the H values to associate
   *    with the proper bits from DZ.
   */

  /**
   * A trivial implementation for the zero-length string.
   */
  static class Empty extends MyersBitParallelEditDistance {
    Empty(CharSequence s) {
      super(s);
    }

    @Override
    public GeneralEditDistance duplicate() {
      return this;      /* thread-safe */
    }

    @Override
    public int getDistance(CharSequence s, int k) {
      return s.length();
    }
  }

  /**
   * An implementation using multiple integers for each column.
   * This follows the pattern for the single-word implementation, with
   * these changes:
   *    sums/shifts must propagate from one word to the next;
   *    to make that easier (and cheaper), we use only (SIZE-1) bits per word;
   */
  static class Multi extends MyersBitParallelEditDistance {
    /* How many integers we use per column */
    int count;

    /**
     * Where the last-row bit lives -- only in the last array slot, though.
     */
    final int lastBitPosition;

    /**
     * Bitmaps for pattern string: [pattern_index][column_index].
     */
    final int[][] positions;

    int[] verticalNegativesReusable;
    /* Reusable arrays for vertical changes */
    int[] verticalPositivesReusable;

    /**
     * A mask with those bits set.
     */
    final int wordMask = (-1 >>> 1);

    /**
     * How many bits we use per word.
     */
    final int wordSize = Integer.SIZE - 1;

    /**
     * Constructs a multi-word engine.
     */
    Multi(CharSequence s) {
      super(s);

      /* Compute number of words to use (rounding up) */
      count = (m + wordSize - 1) / wordSize;

      /* Precompute bitmaps for pattern string */
      positions = PatternBitmap.map(s, idx, new int [idx.size()][], wordSize);

      /* Where in last word does last row appear */
      lastBitPosition = (1 << ((m - 1) % wordSize));

      /* Initialize scratchpad items */
      perThreadInit();
    }

    @Override
    public int getDistance(CharSequence s, int k) {
      indices = idx.map(s, indices);

      /* Initialize verticalPositive to all bits on, verticalNegative all off */
      int[] verticalPositives = verticalPositivesReusable;
      java.util.Arrays.fill(verticalPositives, wordMask);
      int[] verticalNegatives = verticalNegativesReusable;
      java.util.Arrays.fill(verticalNegatives, 0);

      int distance = m;
      int len = s.length();

      /* We can only miss the distance-- below this many times: */
      int maxMisses = k + len - m;
      if (maxMisses < 0) {
        maxMisses = Integer.MAX_VALUE;
      }

      outer:
      for (int j = 0; j < len; j++) {
        int[] position = positions[indices[j]];

        /* Carry bits from one word to the next */
        int sum = 0;
        int horizontalPositiveShift = 1;
        int horizontalNegativeShift = 0;

        /* Iterate through words for this column */
        for (int i = 0; i < count; i++) {
          int verticalNegative = verticalNegatives[i];
          int patternMatch = (position[i] | verticalNegative);
          int verticalPositive = verticalPositives[i];
          sum = (verticalPositive & patternMatch)
                + (verticalPositive) + (sum >>> wordSize);
          int diagonalZero = ((sum & wordMask) ^ verticalPositive)
                             | patternMatch;
          int horizontalPositive = (verticalNegative
                                   | ~(diagonalZero | verticalPositive));
          int horizontalNegative = diagonalZero & verticalPositive;

          if (i == (count - 1)) {            /* only last bit in last word */
            if ((horizontalNegative & lastBitPosition) != 0) {
              distance--;
            } else if ((horizontalPositive & lastBitPosition) != 0) {
              distance++;
              if ((maxMisses -= 2) < 0) {
                break outer;
              }
            } else if (--maxMisses < 0) {
              break outer;
            }
          }

          horizontalPositive = ((horizontalPositive << 1)
                               | horizontalPositiveShift);
          horizontalPositiveShift = (horizontalPositive >>> wordSize);

          horizontalNegative = ((horizontalNegative << 1)
                               | horizontalNegativeShift);
          horizontalNegativeShift = (horizontalNegative >>> wordSize);

          verticalPositives[i] = (horizontalNegative
                                  | ~(diagonalZero | horizontalPositive))
                                 & wordMask;
          verticalNegatives[i] = (diagonalZero & horizontalPositive) & wordMask;
        }
      }
      return distance;
    }

    @Override
    protected void perThreadInit() {
      super.perThreadInit();

      /* Allocate verticalPositive/verticalNegative arrays */
      verticalPositivesReusable = new int[count];
      verticalNegativesReusable = new int[count];
    }
  }

  /*
   * The following code is duplicated with both "int" and "long"
   * as the word primitive type.  To improve maintainability, all instances
   * of the word type are marked with "WORD" in comments.  This
   * allows the "long" version to be regenerated from the "int" version
   * through a mechanical replacement.  To make this work, the
   * class name also needs to include the primitive type (without
   * altered capitalization) and the WORD marker -- this is a quite
   * intentional violation of the usual class naming rules.
   */
  /**
   * An implementation using "int" as the word size.
   */
  // Replace "int/*WORD*/" with "long/*WORD*/" to generate the long version.
  static class TYPEint/*WORD*/ extends MyersBitParallelEditDistance {
    final int/*WORD*/ lastBitPosition;
    final int/*WORD*/[] map;

    @SuppressWarnings("cast")
    TYPEint/*WORD*/(CharSequence s) {
      super(s);
      /* Precompute bitmaps for this pattern */
      map = PatternBitmap.map(s, idx, new int/*WORD*/[idx.size()]);
      /* Compute the bit that represents a change in the last row */
      lastBitPosition = (((int/*WORD*/) 1) << (m - 1));
    }

    @Override
    public int getDistance(CharSequence s, int k) {
      int len = s.length();

      /* Quick check based on length */
      if (((len - m) > k) || ((m - len) > k)) {
        return k + 1;
      }

      /* Map characters to their integer positions in the bitmap array */
      indices = idx.map(s, indices);

      /* Initially, vertical change is all positive (none negative) */
      int/*WORD*/ verticalPositive = -1;
      int/*WORD*/ verticalNegative = 0;
      int distance = m;

      /* We can only miss the "distance--" below this many times: */
      int maxMisses = k + len - m;
      if (maxMisses < 0) {
        maxMisses = Integer.MAX_VALUE;
      }

      for (int j = 0; j < len; j++) {
        /* Where is diagonal zero: matches, or prior VN; plus recursion */
        int/*WORD*/ diagonalZero = map[indices[j]] | verticalNegative;
        diagonalZero |= (((diagonalZero & verticalPositive) + verticalPositive)
                        ^ verticalPositive);

        /* Compute horizontal changes */
        int/*WORD*/ horizontalPositive = verticalNegative
                                         | ~(diagonalZero | verticalPositive);
        int/*WORD*/ horizontalNegative = diagonalZero & verticalPositive;

        /* Update final distance based on horizontal changes */
        if ((horizontalNegative & lastBitPosition) != 0) {
          distance--;
        } else if ((horizontalPositive & lastBitPosition) != 0) {
          distance++;
          if ((maxMisses -= 2) < 0) {
            break;
          }
        } else if (--maxMisses < 0) {
          break;
        }

        /* Shift Hs to next row, compute new Vs analagously to Hs above */
        horizontalPositive = (horizontalPositive << 1) | 1;
        verticalPositive = (horizontalNegative << 1)
                           | ~(diagonalZero | horizontalPositive);
        verticalNegative = diagonalZero & horizontalPositive;
      }
      return distance;
    }
  }

  /**
   * An implementation using "long" as the word size.
   * GENERATED MECHANICALLY FROM THE "int" VERSION ABOVE.
   * DO NOT EDIT THIS ONE -- EDIT ABOVE AND REAPPLY QUERY/REPLACE.
   */
  static class TYPElong/*WORD*/ extends MyersBitParallelEditDistance {
    final long/*WORD*/ lastBitPosition;
    final long/*WORD*/[] map;

    TYPElong/*WORD*/(CharSequence s) {
      super(s);
      /* Precompute bitmaps for this pattern */
      map = PatternBitmap.map(s, idx, new long/*WORD*/[idx.size()]);
      /* Compute the bit that represents a change in the last row */
      lastBitPosition = (((long/*WORD*/) 1) << (m - 1));
    }

    @Override
    public int getDistance(CharSequence s, int k) {
      int len = s.length();

      /* Quick check based on length */
      if (((len - m) > k) || ((m - len) > k)) {
        return k + 1;
      }

      /* Map characters to their integer positions in the bitmap array */
      indices = idx.map(s, indices);

      /* Initially, vertical change is all positive (none negative) */
      long/*WORD*/ verticalPositive = -1;
      long/*WORD*/ verticalNegative = 0;
      int distance = m;

      /* We can only miss the "distance--" below this many times: */
      int maxMisses = k + len - m;
      if (maxMisses < 0) {
        maxMisses = Integer.MAX_VALUE;
      }

      for (int j = 0; j < len; j++) {
        /* Where is diagonal zero: matches, or prior VN; plus recursion */
        long/*WORD*/ diagonalZero = map[indices[j]] | verticalNegative;
        diagonalZero |= (((diagonalZero & verticalPositive) + verticalPositive)
                        ^ verticalPositive);

        /* Compute horizontal changes */
        long/*WORD*/ horizontalPositive = verticalNegative
                                         | ~(diagonalZero | verticalPositive);
        long/*WORD*/ horizontalNegative = diagonalZero & verticalPositive;

        /* Update final distance based on horizontal changes */
        if ((horizontalNegative & lastBitPosition) != 0) {
          distance--;
        } else if ((horizontalPositive & lastBitPosition) != 0) {
          distance++;
          if ((maxMisses -= 2) < 0) {
            break;
          }
        } else if (--maxMisses < 0) {
          break;
        }

        /* Shift Hs to next row, compute new Vs analagously to Hs above */
        horizontalPositive = (horizontalPositive << 1) | 1;
        verticalPositive = (horizontalNegative << 1)
                           | ~(diagonalZero | horizontalPositive);
        verticalNegative = diagonalZero & horizontalPositive;
      }
      return distance;
    }
  }

  /**
   * Chooses an appropriate implementation for a given pattern string.
   * @param s pattern string
   * @return distance calculator appropriate for the pattern
   */
  public static MyersBitParallelEditDistance getInstance(CharSequence s) {
    int m = s.length();
    return (m <= Integer.SIZE) ?
             ((m == 0) ? new Empty(s) : new TYPEint(s)) :
           (s.length() <= Long.SIZE) ?
             new TYPElong(s) :
             new Multi(s);
  }

  /**
   * Tests a computation manually.
   */
  public static void main(String[] args) {
    MyersBitParallelEditDistance b = getInstance(args[0]);
    int k = args.length > 2 ? Integer.parseInt(args[2]) : 0;
    System.out.println("Result: " + b.getDistance(args[1], k));
  }

  /**
   * Index mapping for pattern string.
   */
  final CharIndex idx;

  /**
   * Reusable array of indices for target strings.
   */
  int[] indices = new int[0];

  /**
   * Length of pattern.
   */
  final int m;

  /**
   * Constructs a distance calculator for a given string.
   */
  protected MyersBitParallelEditDistance(CharSequence s) {
    m = s.length();
    idx = CharIndex.getInstance(s);
  }

  @Override
  public GeneralEditDistance duplicate() {
    try {
      return (MyersBitParallelEditDistance) clone();
    } catch (CloneNotSupportedException x) { /*IMPOSSIBLE */
      throw new IllegalStateException("Cloneable object would not clone");
    }
  }

  /**
   * Computes distance from the pattern to a given string, bounded by
   * a limiting distance @see(GeneralEditDistance.getDistance(CharSequence,int)).
   */
  @Override
  public abstract int getDistance(CharSequence s, int k);

  @Override
  protected Object clone() throws CloneNotSupportedException {
    Object obj = super.clone();

    /* Re-initialize any non-thread-safe parts */
    ((MyersBitParallelEditDistance) obj).perThreadInit();

    return obj;
  }

  /**
   * Initializes items that cannot be shared among threads.
   */
  protected void perThreadInit() {
    indices = new int[0];
  }
}
