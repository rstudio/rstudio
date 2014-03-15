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
 * Tests for {@link PatternBitmap}.
 */
public class PatternBitmapTest extends TestCase {

  /** Reverses a string */
  public static String reverse(String s) {
    StringBuilder b = new StringBuilder();
    for (int i = s.length(); --i >= 0;) {
      b.append(s.charAt(i));
    }
    return b.toString();
  }

  /**
   * Tests the int[] form of mapping.
   */
  public void testInteger() throws Exception {
    String string = "abcdandmore";
    CharIndex idx = CharIndex.getInstance(string);

    int[] map = PatternBitmap.map(string, idx, new int[idx.size()]);

    /* Spot check some */
    assertEquals(0x11, map[idx.lookup('a')]);
    assertEquals(0x02, map[idx.lookup('b')]);
    assertEquals(0x04, map[idx.lookup('c')]);
    assertEquals(0x48, map[idx.lookup('d')]);

    /* Check all others for zero/non-zero */
    for (int i = 0; i < 0xffff; i++) {
      char c = (char)i;
      int where = string.indexOf(c);
      if (where >= 0) {
        assertTrue("Map for pattern character '" + c + "' should be non-zero",
                     (map[idx.lookup(c)] & (1 << where)) != 0);
      } else {
        assertEquals("Map for unused character '" + c + "' should be zero",
                     0, map[idx.lookup(c)]);
      }
    }
  }

  /**
   * Tests the int[][] form of mapping.
   */
  public void testIntegerArray() throws Exception {
    String string = "x012345678901234567890123456789"
                  + "01234567890123456789x0123456789"
                  + "01234x567890123456789";
    int wordSize = 31;

    CharIndex idx = CharIndex.getInstance(string);

    int[][] map = PatternBitmap.map(string, idx, new int[idx.size()][],
                                     wordSize);

    /* Spot check some */
    assertEquals(1 << 0,  map[idx.lookup('x')][0]);
    assertEquals(1 << 20, map[idx.lookup('x')][1]);
    assertEquals(1 << 5,  map[idx.lookup('x')][2]);

    /* Check all others for null element/not */
    int[] notThere = map[idx.lookup('\u0000')];
    for (int i = 0; i < 0xffff; i++) {
      char c = (char)i;
      int where = string.indexOf(c);
      if (where >= 0) {
        assertTrue("Map for pattern character '" + c + "'"
                   + " should be non-zero",
                   (map[idx.lookup(c)] != notThere));
        int bit = map[idx.lookup(c)][where / wordSize] & (1 << (where % wordSize));
        assertTrue("Map for pattern character '" + c + "'"
                   + " should have " + where + " bit on",
                   (bit != 0));
      } else {
        assertEquals("Map for unused character '" + c + "' should be none",
                     notThere, map[idx.lookup(c)]);
      }
    }
  }

  /**
   * Tests the long[] form of mapping.
   */
  public void testLong() throws Exception {
    String string = reverse("The quick brown fox jumps over the lazy dog.");
    CharIndex idx = CharIndex.getInstance(string);

    long[] map = PatternBitmap.map(string, idx, new long[idx.size()]);

    /* Spot check some */
    long e = Long.parseLong("00100000000000000000000000001000010000000000", 2);
             /* ............ The quick brown fox jumps over the lazy dog. */

    assertEquals(e,  map[idx.lookup('e')]);

    long o = Long.parseLong("00000000000010000100000000100000000000000100", 2);
             /* ............ The quick brown fox jumps over the lazy dog. */
    assertEquals(o, map[idx.lookup('o')]);

    /* Check all others for zero/non-zero */
    for (int i = 0; i < 0xffff; i++) {
      char c = (char)i;
      int where = string.indexOf(c);
      if (where >= 0) {
        assertTrue("Map for pattern character '" + c + "' should be non-zero",
                   (map[idx.lookup(c)] & (1L << where)) != 0);
      } else {
        assertEquals("Map for unused character '" + c + "' should be zero",
                     0, map[idx.lookup(c)]);
      }
    }
  }
}
