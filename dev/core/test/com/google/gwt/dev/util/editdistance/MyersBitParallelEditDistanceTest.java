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

import com.google.gwt.dev.util.editdistance.GeneralEditDistance;
import com.google.gwt.dev.util.editdistance.MyersBitParallelEditDistance;

import junit.framework.TestCase;

/**
 * Test cases for the MyersBitParallelEditDistance class.
 *
 * Note that the GeneralEditDistance class tests provide
 * good general coverage of this class as well.
 *
 * The tests here look for boundary conditions in the
 * specific algorithm: near 32- and 64-bit edges.
 */
public class MyersBitParallelEditDistanceTest extends TestCase {

  /** Generates an instance (just a notational shorthand) */
  static MyersBitParallelEditDistance generate(String pattern) {
    return MyersBitParallelEditDistance.getInstance(pattern);
  }

  public void test32end() {
    String s1 = "abcdefghijklmnopqrstuvwxyz012345";
    String s2 = "abcdefghijklmnopqrstuvwxyz01234";
    GeneralEditDistanceTest.genericVerification(generate(s1),
                                                s1, s2,
                                                1);
    GeneralEditDistanceTest.genericVerification(generate(s1),
                                                s1, s2+"x",
                                                1);
  }

  public void test32start() {
    String s1 = "abcdefghijklmnopqrstuvwxyz012345";
    String s2 = "Abcdefghijklmnopqrstuvwxyz012345";
    GeneralEditDistanceTest.genericVerification(generate(s1),
                                                s1, s2,
                                                1);
    GeneralEditDistanceTest.genericVerification(generate(s1),
                                                s1, s2.substring(1),
                                                1);
  }

  public void test32various() {
    String s1 = "abcdefghijklmnopqrstuvwxyz012345";
    String s2 = "abcdeghijklmNopqrstu@vwxyz1234";
    GeneralEditDistanceTest.genericVerification(generate(s1),
                                                s1, s2,
                                                5 /*fN@05*/);
  }
  
  /*
   * Some tests with strings exactly 32 characters long -- just enough
   * for an "int" bitmap.
   */

  public void test33end() {
    String s1 = "abcdefghijklmnopqrstuvwxyz0123456";
    String s2 = "abcdefghijklmnopqrstuvwxyz012345";
    GeneralEditDistanceTest.genericVerification(generate(s1),
                                                s1, s2,
                                                1);
    GeneralEditDistanceTest.genericVerification(generate(s1),
                                                s1, s2+"x",
                                                1);
  }

  public void test33start() {
    String s1 = "abcdefghijklmnopqrstuvwxyz0123456";
    String s2 = "Abcdefghijklmnopqrstuvwxyz0123456";
    GeneralEditDistanceTest.genericVerification(generate(s1),
                                                s1, s2,
                                                1);
    GeneralEditDistanceTest.genericVerification(generate(s1),
                                                s1, s2.substring(1),
                                                1);
  }

  public void test64end() {
    String s1 = "abcdefghijklmnopqrstuvwxyz0123456";
    String s2 = "abcdefghijklmnopqrstuvwxyz012345";
    GeneralEditDistanceTest.genericVerification(generate(s1+s1),
                                                s1+s1, s1+s2,
                                                1);
    GeneralEditDistanceTest.genericVerification(generate(s1+s1),
                                                s1+s1, s1+s2+"x",
                                                1);
  }

  public void test64middle() {
    String s1 = "abcdefghijklmnopqrstuvwxyz0123456";
    String s2 = "abcdefghijklmnopqrstuvwxyz012345";
    GeneralEditDistanceTest.genericVerification(generate(s1+s1),
                                                s1+s1, s1+"x"+s2,
                                                2);
    GeneralEditDistanceTest.genericVerification(generate(s1+s1),
                                                s1+s1, s2+"x"+s1,
                                                1);
  }

  public void test64start() {
    String s1 = "abcdefghijklmnopqrstuvwxyz0123456";
    String s2 = "Abcdefghijklmnopqrstuvwxyz0123456";
    GeneralEditDistanceTest.genericVerification(generate(s1+s1),
                                                s1+s1, s2+s1,
                                                1);
    GeneralEditDistanceTest.genericVerification(generate(s1),
                                                s1, s2.substring(1),
                                                1);
  }

  public void testbig() {
    String s1base = "abcdefghijklmnopqrstuvwxyz0123456";
    String s1 = s1base + s1base + s1base + s1base + s1base;
    String s2 = s1base
                + "abcdeghijklmNopqrstu@vwxyz12346"
                + s1base
                + "bcdefGhijklmno$pqrstuvwxyz012345"
                + s1base;

    GeneralEditDistanceTest.genericVerification(generate(s1),
                                                s1, s2, 
                                                5+4 /*fN@05, aG$6*/);
  }

  /** Verifies the choice of bit array sizing */
  public void testBitArraySizing() {
    String thirtyTwo = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
    assertEquals(generate("").getClass(),
                 MyersBitParallelEditDistance.Empty.class);
    assertEquals(generate(thirtyTwo).getClass(),
                 MyersBitParallelEditDistance.TYPEint.class);
    assertEquals(generate(thirtyTwo+"x").getClass(),
                 MyersBitParallelEditDistance.TYPElong.class);
    assertEquals(generate(thirtyTwo+thirtyTwo).getClass(),
                 MyersBitParallelEditDistance.TYPElong.class);
    assertEquals(generate(thirtyTwo+thirtyTwo+"x").getClass(),
                 MyersBitParallelEditDistance.Multi.class);
  }

  /** Tests an "impossible" exception path */
  public void testInternalCloneNotSupportedException() {
    try {
      new MyersBitParallelEditDistance.Multi("not really impossible") {
        @Override
        public Object clone() throws CloneNotSupportedException {
          throw new CloneNotSupportedException("do the impossible");
        }
      }.duplicate();
      assertTrue("Failed to throw exception", false);
    } catch (IllegalStateException x) {
      /* EXPECTED RESULT */
    }
  }

  /** Test main programs to make sure they do not die unnaturally */
  public void testMainProgramsForSanity() {
    MyersBitParallelEditDistance.main(new String[] { "yes", "no", "5" });
    MyersBitParallelEditDistance.Multi.main(new String[] { "yes", "no", "5" });
  }

  /** Test on a variety of word pairs, reusing an instance appropriately */
  public void testOnWordSet() {
    String [] words = GeneralEditDistanceTest.words;
    int [][] expect = GeneralEditDistanceTest.wordsDistances;
    for (int i = 0; i < words.length; i++) {
      GeneralEditDistance engine = generate(words[i]);
      for (int j = 0; j <= i; j++) {
        GeneralEditDistanceTest.genericVerification(engine,
                                                    words[i], words[j],
                                                    expect[i][j]);
      }
    }
  }

  /** Tests a short pattern and target */
  public void testShort() {
    String s1 = "short";
    String s2 = "snorts";
    GeneralEditDistanceTest.genericVerification(generate(s1),
                                                s1, s2, 2);
  }

  /** Tests zero-length patterns and targets; distance is other length */
  public void testZeroLength() {
    assertEquals(0, generate("").getDistance("", 1));

    String other = "other";
    GeneralEditDistanceTest.genericVerification(generate(""),
                                                "", other,
                                                other.length());
    GeneralEditDistanceTest.genericVerification(generate(other),
                                                other, "",
                                                other.length());
  }
}
