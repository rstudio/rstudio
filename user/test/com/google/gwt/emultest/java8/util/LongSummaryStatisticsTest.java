/*
 * Copyright 2016 Google Inc.
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

package com.google.gwt.emultest.java8.util;

import com.google.gwt.emultest.java.util.EmulTestBase;

import static java.lang.Long.MAX_VALUE;
import static java.lang.Long.MIN_VALUE;

import java.util.LongSummaryStatistics;

/**
 * Tests {@link LongSummaryStatistics}.
 */
public class LongSummaryStatisticsTest extends EmulTestBase {

  private LongSummaryStatistics stats;

  @Override
  protected void gwtSetUp() throws Exception {
    stats = new LongSummaryStatistics();
  }

  public void testCombine() throws Exception {

    stats.accept(1);
    stats.accept(2);

    LongSummaryStatistics otherStats = new LongSummaryStatistics();
    otherStats.accept(3);
    otherStats.accept(4);

    stats.combine(otherStats);

    assertEquals(2.5d, stats.getAverage());
    assertEquals(1, stats.getMin());
    assertEquals(4, stats.getMax());
    assertEquals(10L, stats.getSum());
    assertEquals(4L, stats.getCount());
  }

  public void testStats() {

    assertEquals(0L, stats.getCount());
    assertEquals(0d, stats.getAverage());
    assertEquals(MIN_VALUE, stats.getMax());
    assertEquals(MAX_VALUE, stats.getMin());
    assertEquals(0L, stats.getSum());

    long[][] testData = {
        //    aLong,       max,       min,       sum
        {         1,         1,         1,         1 },
        {        -1,         1,        -1,         0 },
        {         2,         2,        -1,         2 },
        {        -2,         2,        -2,         0 },
        { MAX_VALUE, MAX_VALUE,        -2, MAX_VALUE },
        { MIN_VALUE, MAX_VALUE, MIN_VALUE,        -1 },
    };

    for (int i = 0; i < testData.length; ++i) {
      long expectedCount = i + 1;
      long aLong = testData[i][0];
      long expectedMax = testData[i][1];
      long expectedMin = testData[i][2];
      long expectedSum = testData[i][3];
      double expectedAverage = expectedSum / (double) expectedCount;

      stats.accept(aLong);

      assertEquals(expectedAverage, stats.getAverage());
      assertEquals(expectedCount, stats.getCount());
      assertEquals(expectedMax, stats.getMax());
      assertEquals(expectedMin, stats.getMin());
      assertEquals(expectedSum, stats.getSum());
    }
  }
}