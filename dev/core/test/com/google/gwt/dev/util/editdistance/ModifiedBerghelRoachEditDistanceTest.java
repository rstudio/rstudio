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

import static com.google.gwt.dev.util.editdistance.GeneralEditDistanceTest.MAGNA;
import static com.google.gwt.dev.util.editdistance.GeneralEditDistanceTest.generateRandomString;
import static com.google.gwt.dev.util.editdistance.GeneralEditDistanceTest.testSomeEdits;

import com.google.gwt.dev.util.editdistance.GeneralEditDistanceTest.AbstractLevenshteinTestCase;

/**
 * Test cases for the ModifiedBerghelRoachEditDistance class.
 *
 * The bulk of the test is provided by the superclass, for
 * which we provide GeneralEditDistance instances.
 *
 * Since Berghel-Roach is superior for longer strings with moderately
 * low edit distances, we try a few of those specifically.
 * This Modified form uses less space, and can handle yet larger ones.
 */
public class ModifiedBerghelRoachEditDistanceTest extends junit.framework.TestCase {
  /** Basic Levenshtein tests for ModifedBerghelRoachEditDistance */
  public static class Basic extends AbstractLevenshteinTestCase {
    Basic() {
      super(new Factory());
    }
  }
  private static class Factory implements GeneralEditDistanceTest.Factory {
    @Override
    public GeneralEditDistance getInstance(CharSequence s) {
      return ModifiedBerghelRoachEditDistance.getInstance(s.toString());
    }
  }

  static final Factory FACTORY = new Factory();

  public void testHugeEdit() {
    final int SIZE = 10000;
    final long SEED = 1;

    testSomeEdits(FACTORY, generateRandomString(SIZE, SEED), (SIZE / 50), (SIZE / 50));
  }

  public void testHugeString() {
    /*
     * An even larger size is feasible, but the test would no longer
     * qualify as "small".
     */
    final int SIZE = 20000;
    final long SEED = 1;

    testSomeEdits(FACTORY, generateRandomString(SIZE, SEED), 30, 25);
  }

  public void testLongString() {
    testSomeEdits(FACTORY, MAGNA, 8, 10);
  }

  public void testLongStringMoreEdits() {
    testSomeEdits(FACTORY, MAGNA, 40, 30);
  }
}
