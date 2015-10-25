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

import static java.lang.Double.MAX_VALUE;
import static java.lang.Double.MIN_VALUE;
import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.NaN;
import static java.lang.Double.POSITIVE_INFINITY;

import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.List;

/**
 * Tests {@link DoubleSummaryStatistics}.
 */
public class DoubleSummaryStatisticsTest extends EmulTestBase {

  private DoubleSummaryStatistics stats;

  @Override
  protected void gwtSetUp() throws Exception {
    stats = new DoubleSummaryStatistics();
  }

  public void testAverageAndSumWithCompensation() throws Exception {
    assertEquals(0d, stats.getAverage());
    assertEquals(0d, stats.getSum());

    double initial = 1.0d;
    long count = 1000000;

    // 'precision' is the hardcoded result of Math.ulp(initial) in JVM,
    // since GWT does not emulate Math.ulp().
    // This value represents the distance from 'initial' (1.0d) to the
    // previous/next double. If we add half or less of that distance/precision
    // to 'initial' then the result will be truncated to 'initial' again due to
    // floating point arithmetic rounding.
    // With Kahan summation such rounding errors are detected and compensated
    // so the summation result should (nearly) equal the expected sum.
    double precision = 2.220446049250313E-16;
    double value = precision / 2;
    double expectedSum = initial + (count * value);
    long expectedCount = count + 1;
    double expectedAverage = expectedSum / expectedCount;

    stats.accept(initial);
    for (int i = 0; i < count; ++i) {
      stats.accept(value);
    }

    // TODO (jnehlmeier): While delta = 0 works we probably want to allow some error?
    // Or maybe use ulp differences instead of a delta?
    assertEquals(expectedAverage, stats.getAverage(), 0);
    assertEquals(expectedSum, stats.getSum(), 0);
  }

  public void testCombine() throws Exception {
    stats.accept(1.0d);
    stats.accept(2.0d);

    DoubleSummaryStatistics otherStats = new DoubleSummaryStatistics();
    otherStats.accept(3.0d);
    otherStats.accept(4.0d);

    stats.combine(otherStats);

    assertEquals(2.5d, stats.getAverage());
    assertEquals(1d, stats.getMin());
    assertEquals(4d, stats.getMax());
    assertEquals(10d, stats.getSum());
    assertEquals(4, stats.getCount());
  }

  public void testCountMaxMin() {
    assertEquals(0, stats.getCount());
    assertEquals(NEGATIVE_INFINITY, stats.getMax());
    assertEquals(POSITIVE_INFINITY, stats.getMin());

    double[][] testData = {
        //          aDouble,               max,               min
        {               1.0,               1.0,               1.0 },
        {              -1.0,               1.0,              -1.0 },
        {               2.5,               2.5,              -1.0 },
        {              -2.5,               2.5,              -2.5 },
        {         MAX_VALUE,         MAX_VALUE,              -2.5 },
        {         MIN_VALUE,         MAX_VALUE,              -2.5 },
        { POSITIVE_INFINITY, POSITIVE_INFINITY,              -2.5 },
        { NEGATIVE_INFINITY, POSITIVE_INFINITY, NEGATIVE_INFINITY },
    };

    for (int i = 0; i < testData.length; ++i) {
      long expectedCount = i + 1;
      double aDouble = testData[i][0];
      double expectedMax = testData[i][1];
      double expectedMin = testData[i][2];

      stats.accept(aDouble);

      assertEquals(expectedCount, stats.getCount());
      assertEquals(expectedMax, stats.getMax());
      assertEquals(expectedMin, stats.getMin());
    }
  }

  public void testInfinity() {
    stats.accept(NEGATIVE_INFINITY);
    stats.accept(NEGATIVE_INFINITY);
    assertEquals(NEGATIVE_INFINITY, stats.getAverage());
    assertEquals(NEGATIVE_INFINITY, stats.getMax());
    assertEquals(NEGATIVE_INFINITY, stats.getMin());
    assertEquals(NEGATIVE_INFINITY, stats.getSum());

    stats.accept(POSITIVE_INFINITY);
    assertTrue(Double.isNaN(stats.getAverage()));
    assertEquals(POSITIVE_INFINITY, stats.getMax());
    assertEquals(NEGATIVE_INFINITY, stats.getMin());
    assertTrue(Double.isNaN(stats.getSum()));

    stats = new DoubleSummaryStatistics();
    stats.accept(POSITIVE_INFINITY);
    stats.accept(POSITIVE_INFINITY);
    assertEquals(POSITIVE_INFINITY, stats.getAverage());
    assertEquals(POSITIVE_INFINITY, stats.getMax());
    assertEquals(POSITIVE_INFINITY, stats.getMin());
    assertEquals(POSITIVE_INFINITY, stats.getSum());

    stats.accept(NEGATIVE_INFINITY);
    assertTrue(Double.isNaN(stats.getAverage()));
    assertEquals(POSITIVE_INFINITY, stats.getMax());
    assertEquals(NEGATIVE_INFINITY, stats.getMin());
    assertTrue(Double.isNaN(stats.getSum()));
  }

  public void testNaN() {
    List<Double> testData = Arrays.asList(
        NaN, -1.5d, 2.5d, MAX_VALUE, MIN_VALUE, NaN,
        NEGATIVE_INFINITY, POSITIVE_INFINITY);

    stats.accept(5.0d);
    for (Double aDouble : testData) {
      stats.accept(aDouble);
      assertTrue(Double.isNaN(stats.getAverage()));
      assertTrue(Double.isNaN(stats.getMax()));
      assertTrue(Double.isNaN(stats.getMin()));
      assertTrue(Double.isNaN(stats.getSum()));
    }
  }
}