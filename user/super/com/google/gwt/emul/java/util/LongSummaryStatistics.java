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

import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 * See <a
 * href="https://docs.oracle.com/javase/8/docs/api/java/util/LongSummaryStatistics.html">the
 * official Java API doc</a> for details.
 */
public class LongSummaryStatistics implements LongConsumer, IntConsumer {

  private long count;
  private long min = Long.MAX_VALUE;
  private long max = Long.MIN_VALUE;
  private long sum;

  @Override
  public void accept(int value) {
    accept((long) value);
  }

  @Override
  public void accept(long value) {
    count++;
    min = Math.min(min, value);
    max = Math.max(max, value);
    sum += value;
  }

  public void combine(LongSummaryStatistics other) {
    count += other.count;
    min = Math.min(min, other.min);
    max = Math.max(max, other.max);
    sum += other.sum;
  }

  public double getAverage() {
    return count > 0 ? (double) sum / count : 0d;
  }

  public long getCount() {
    return count;
  }

  public long getMin() {
    return min;
  }

  public long getMax() {
    return max;
  }

  public long getSum() {
    return sum;
  }

  @Override
  public String toString() {
    return "LongSummaryStatistics[" +
        "count = " + count +
        ", avg = " + getAverage() +
        ", min = " + min +
        ", max = " + max +
        ", sum = " + sum +
        "]";
  }
}
