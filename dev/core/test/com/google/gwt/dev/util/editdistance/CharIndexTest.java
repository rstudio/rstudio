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
 * Tests for {@link CharIndex}.
 */
public class CharIndexTest extends TestCase {
  /**
   * Tests a mapping where the masked reduction fails (where
   * the low-order bits of several characters overlap.
   */
  public void testFullHash() throws Exception {
    String string = "abc\u0001\u0101\u0201\u0301";
    CharIndex idx = CharIndex.getInstance(string + string);
    assertEquals(idx.getClass(), CharIndex.FullHash.class);
    assertEquals(idx.lookup('c'), 3);
    assertEquals(idx.nullElement(), 0);

    /* because string has no duplicates: */
    assertEquals(idx.size(), string.length() + 1);

    generalVerify(idx, string, "xyz012\u0123\u1234");
  }

  /** Tests fullhash.Char overridden methods */
  public void testFullHashChar() {
    CharIndex.FullHash.Char x = new CharIndex.FullHash.Char();
    x.c = 'A';
    assertEquals("'A'", x.toString());
  }

  /**
   * Tests a masked mapping (where characters all fall within
   * a slice of Unicode defined by a simple mask).
   */
  public void testMasked() throws Exception {
    String string = "\u0141\u0142\u0171\u0131";
    CharIndex idx = CharIndex.getInstance(string + string);
    assertEquals(idx.getClass(), CharIndex.Masked.class);
    assertEquals(idx.lookup('\u0141'), 0x0041);
    assertEquals(idx.size(), (idx.nullElement() + 1));
    generalVerify(idx, string, "xyz012\u0123\u1234");
  }

  /**
   * Tests a straight mapping (where characters are all less
   * than some maximum, including at least ASCII.
   */
  public void testStraight() throws Exception {
    String string = "abcdandmore";
    CharIndex idx = CharIndex.getInstance(string + string);
    assertEquals(idx.getClass(), CharIndex.Straight.class);
    assertEquals(idx.lookup('a'), 'a');
    assertEquals(idx.size(), (idx.nullElement() + 1));
    generalVerify(idx, string, "xyz012\u0123\u1234");
  }

  /**
   * Verifies abstract properties of any CharIndex
   * @param idx index to test
   * @param string characters in the pattern for that index
   * @param more characters not in the pattern
   */
  void generalVerify(CharIndex idx, String string, String more) {
    /* Test the map() method */
    int[] mapped = idx.map(string, new int[0]);

    for (int i = 0; i < string.length(); i++) {
      char ci = string.charAt(i);
      for (int j = 0; j < string.length(); j++) {
        char cj = string.charAt(j);

        /* Each character in the pattern should get a unique index */
        assertTrue((mapped[i] == idx.lookup(cj)) == (ci == cj));
      }
      for (int j = 0; j < more.length(); j++) {
        char cj = more.charAt(j);

        /*
         * Characters not in the pattern should not match any that
         * are in the pattern (but they may match one another).
         */
        assertTrue(idx.lookup(ci) != idx.lookup(cj));
      }
    }
  }
}
