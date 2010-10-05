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
 * A performance-oriented character-indexed map.
 * Provides a mapping from characters in a limited alphabet
 * (defined by a CharSequence) to integer values (for array indexing).
 * Emphasis is on performance (e.g., compared to using a generic HashMap),
 * in terms of speed of (post-construction) lookup and space required.
 *
 * Maps are constructed using a static <tt>getInstance</tt> method that
 * chooses an efficient implementation for a given string.
 * The map interface consists of:
 *    a <tt>size</tt> method for determining the maximum range of indices used;
 *    a <tt>lookup</tt> method for translating a character to its index; and,
 *    a <tt>nullElement</tt> method that returns the index that is not
 *     returned by <tt>lookup</tt> for any characer actually in the map.
 *
 * The target application is the mapping of natural-language strings.
 * Most strings include characters from a single language, and thus
 * fall into a small slice of the Unicode space.  In particular,
 * many strings fall into just the Latin series.
 */
public abstract class CharIndex {
  /**
   * An index based on a garden-variety hash map.
   * This is the implementation of last recourse (from a
   * performance perspective).
   *
   * TODO(mwyoung): there may be value in an implementation
   * that falls in between the masked and fullhash variants;
   * for example, one using a closed hash table with rehashing.
   */
  public static class FullHash extends CharIndex {
    /**
     * Mutable holder for a character.
     */
    static class Char {
      char c;
      @Override
      public boolean equals(Object x) {
        return (x != null) && (((Char) x).c == this.c);
      }
      @Override
      public int hashCode() {
        return c;
      }
      @Override
      public String toString() {
        return "'" + c + "'";
      }
    }
    
    static final int NULL_ELEMENT = 0;
    protected int lastUsed = NULL_ELEMENT;
    /**
     * Mapping from pattern characters to their integer index values.
     */
    final java.util.HashMap<Char,Integer> map;

    /**
     * Constructs a full hash-based mapping.
     */
    FullHash(CharSequence s) {
      /* Choose a hash size larger at least twice the string length */
      int len = s.length();
      int power = Integer.highestOneBit(len);

      /* Create the map */
      map = new java.util.HashMap<Char,Integer>(
          power << ((power == len) ? 1 : 2));

      /*
       * Add characters one at a time to the map
       */

      Char test = new Char();                   /* (re)used for lookup */
      for (int i = 0; i < s.length(); i++) {
        test.c = s.charAt(i);
        if (map.get(test) == null) {
          /* Not there... add it */
          map.put(test, new Integer(++lastUsed));

          /* Grow a new holder for subsequent lookups */
          test = new Char();
        }
      }
    }
    
    @Override
    public int lookup(char c) {
      final Char lookupTest = new Char();
      lookupTest.c = c;
      Integer result = map.get(lookupTest);
      return (result != null) ? result.intValue() : 0;
    }

    @Override
    public int[] map(CharSequence s, int[] mapped) {
      /* Create one mutable Char, and reuse it for all lookups */
      final Char lookupTest = new Char();

      int len = s.length();
      if (mapped.length < len) {
        mapped = new int[len];
      }

      for (int i = 0; i < len; i++) {
        lookupTest.c = s.charAt(i);
        Integer result = map.get(lookupTest);
        mapped[i] = (result != null) ? result.intValue() : NULL_ELEMENT;
      }
      return mapped;
    }

    @Override
    public int nullElement() {
      return NULL_ELEMENT;
    }

    @Override
    public int size() {
      return lastUsed + 1;
    }
  }
  
  /**
   * An index based on a simple mask: index(c) = (c & MASK).
   * This allows for a very fast index function for many
   * languages outside of the ASCII spectrum.  Most languages
   * fit in a single 8-bit Unicode slice.  Even languages
   * that cross slices for special characters (such as extended
   * Latin) often have no collisions under simple masking.
   */
  public static class Masked extends CharIndex {
    /**
     * Hash table size.
     */
    static final int SIZE = 0x100;

    /**
     * Mask used for hashing.
     */
    static final int MASK = (SIZE - 1);

    /**
     * Where we may invalid characters: beyond the hash table.
     */
    static final int NULL_ELEMENT = SIZE;

    /**
     * Generates an instance of this implementation if possible.
     * @param s pattern string
     * @return an instance of this CharIndex if the pattern satisfies
     *         the constraints for this implementation; otherwise, null
     */
    static Masked generate(CharSequence s) {
      /*
       * This implementation requires that there be no hash collisions
       * among the pattern characters, using a simple mask-based hash.
       */
      char [] contains = new char[SIZE];

      /* Ensure that for all x, hash(contains[x]) != x initially. */
      contains[0] = (char) 1;

      /* Hash characters, recording values seen, rejecting collisions */
      for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);
        int index = c & MASK;
        if (contains[index] != c) {
          if ((contains[index] & MASK) == index) {
            return null;
          }
          contains[index] = c;
        }
      }
      return new Masked(contains);
    }

    /**
     * Closed hash table: characters actually indexed.
     * Unused cells contain values inappropriate to their index.
     */
    final char[] contains;

    /**
     * Constructor based on hash table built by generate().
     */
    private Masked(char [] contains) {
      this.contains = contains;
    }
    
    @Override
    public int lookup(char c) {
      int index = c & MASK;
      return (c == contains[index]) ? index : NULL_ELEMENT;
    }

    @Override
    public int[] map(CharSequence s, int[] mapped) {
      int len = s.length();
      if (mapped.length < len) {
        mapped = new int[len];
      }
      for (int i = 0; i < len; i++) {
        char c = s.charAt(i);
        int index = c & MASK;
        mapped[i] = (c == contains[index]) ? index : NULL_ELEMENT;
      }
      return mapped;
    }

    @Override
    public int nullElement() { return NULL_ELEMENT; }

    @Override
    public int size() { return NULL_ELEMENT + 1; }
  }

  /**
   * An index based on the identity mapping: index(c) = c.
   * This requires minimal computation and no additional space
   * for the basic ASCII character set.
   */
  public static class Straight extends CharIndex {
    /**
     * The largest character we will map (directly).
     */
    static final int MAX = 0x80;

    /** 
     * A mask used to find characters that fall outside.
     */
    static final int MASK = ~(MAX - 1);

    /**
     * A map result we never generate for valid characters.
     */
    static final int NULL_ELEMENT = MAX;

    /**
     * Generates an instance of this implementation if possible.
     * @param s pattern string
     * @return an instance of this CharIndex if the pattern satisfies
     *         the constraints for this implementation; otherwise, null
     */
    static Straight generate(CharSequence s) {
      /* This implementation requires that all characters fall below MAX */
      for (int i = 0; i < s.length(); i++) {
        if ((s.charAt(i) & MASK) != 0) {
          return null;
        }
      }

      return new Straight();
    }

    /**
     * Simple private constructor, no state required.
     */
    private Straight() { }

    @Override
    public int lookup(char c) {
      return ((c & MASK) == 0) ? c : NULL_ELEMENT;
    }

    @Override
    public int[] map(CharSequence s, int[] mapped) {
      int len = s.length();
      if (mapped.length < len) {
        mapped = new int[len];
      }
      for (int i = 0; i < len; i++) {
        char c = s.charAt(i);
        mapped[i] = ((c & MASK) == 0) ? c : NULL_ELEMENT;
      }
      return mapped;
    }

    @Override
    public int nullElement() {
      return NULL_ELEMENT;
    }

    @Override
    public int size() {
      return NULL_ELEMENT + 1;
    }
  }

  /**
   * Generates an index for a given alphabet, defined by a character sequence.
   * An appropriate implementation is chosen for the alphabet.  The
   * character sequence is allowed to contain multiple instances of
   * the same character.
   *
   * @param s the alphabet to be indexed
   * @return an index appropriate to this alphabet
   */
  public static CharIndex getInstance(CharSequence s) {
    CharIndex result;

    /* Try fastest mappings first, then more general ones */

    if ((result = Straight.generate(s)) != null) {
      return result;
    }

    if ((result = Masked.generate(s)) != null) {
      return result;
    }

    /* Full hash always works */
    return new FullHash(s);
  }
  
  /**
   * Returns the index (mapping result) for a given character.
   * If the character is present in the alphabet, then a unique index
   * is returned; other characters may be mapped to unique
   * values, *or* may be mapped to a shared <tt>nullElement</tt> value.
   * @param c character for which an index is sought
   * @return an integer index for the character
   */
  public abstract int lookup(char c);

  /**
   * Performs lookups on an entire sequence of characters.
   * This allows an entire string to be mapped with fewer
   * method invocations, and possibly with some added internal
   * efficiencies.
   *
   * @param s character sequence to be indexed
   * @param mapped array for storing results
   * @return an array of indices for the character sequence,
   *  either the <tt>mapped</tt> parameter or a larger newly-allocated
   *  array if required
   */
  public abstract int[] map(CharSequence s, int[] mapped);

  /**
   * Returns an index that is not used for any character in
   * the alphabet.  This index *may* be returned for characters
   * not in the alphabet.
   * @return an index not used for a valid character
   */
  public abstract int nullElement();

  /**
   * Returns the maximum index result that can be returned by
   * the <tt>lookup</tt> function (including the <tt>nullElement</tt> result
   * if applicable).  No negative values are ever returned
   * from <tt>lookup</tt>.
   * @return maximum index returned from any <tt>lookup</tt>
   */
  public abstract int size();
}
