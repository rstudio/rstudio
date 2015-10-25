/*
 * Copyright 2015 Google Inc.
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
package java.util;

import java.util.function.DoubleConsumer;

/**
 * See <a
 * href="https://docs.oracle.com/javase/8/docs/api/java/util/DoubleSummaryStatistics.html">the
 * official Java API doc</a> for details.
 */
public class DoubleSummaryStatistics implements DoubleConsumer {

  private long count;
  private double min = Double.POSITIVE_INFINITY;
  private double max = Double.NEGATIVE_INFINITY;
  private double sum;
  private double sumError;
  // Because of Kahan summation compensation a naive summation is required
  // to keep track of infinity values correctly.
  private double naiveSum;

  @Override
  public void accept(double value) {
    count++;
    min = Math.min(min, value);
    max = Math.max(max, value);
    naiveSum += value;
    sum(value);
  }

  public void combine(DoubleSummaryStatistics other) {
    count += other.count;
    min = Math.min(min, other.min);
    max = Math.max(max, other.max);
    naiveSum += other.naiveSum;
    sum(other.sum);
    sum(other.sumError);
  }

  public double getAverage() {
    return count > 0 ? getSum() / count : 0d;
  }

  public long getCount() {
    return count;
  }

  public double getMin() {
    return min;
  }

  public double getMax() {
    return max;
  }

  public double getSum() {
    // Adding sum and sumError here to get a better result
    // because Kahan summation always applies error compensation
    // on the next summation.
    double compensatedSum = sum + sumError;
    // sumError can be NaN if infinity values had been accepted.
    if (Double.isNaN(compensatedSum) && Double.isInfinite(naiveSum)) {
      return naiveSum;
    }
    return compensatedSum;
  }

  @Override
  public String toString() {
    return "DoubleSummaryStatistics[" +
        "count = " + count +
        ", avg = " + getAverage() +
        ", min = " + min +
        ", max = " + max +
        ", sum = " + getSum() +
        "]";
  }

  /**
   * Adds a new value to the current sum using Kahan summation
   * algorithm for improved summation precision.
   *
   * https://en.wikipedia.org/wiki/Kahan_summation_algorithm
   *
   * @param value the value being added to the sum
   */
  private void sum(double value) {
    double compensatedValue = value - sumError;
    double newSum = sum + compensatedValue;
    // Logically 'summationError' always evaluates to 0
    // but in reality it contains a small summation error
    // that can occur because of rounding in floating point arithmetic.
    // For example 1.0 + EPSILON with EPSILON being half machine precision
    // (basically Math.ulp(1.0)/2) will result in 1.0.
    // Tests should verify that GWT compiler / Closure compiler do not
    // remove this line during optimizations.
    // NOTE: sumError can become NaN for infinity values.
    sumError = (newSum - sum) - compensatedValue;
    sum = newSum;
  }
}
