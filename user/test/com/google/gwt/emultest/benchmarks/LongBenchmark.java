/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.emultest.benchmarks;

import com.google.gwt.benchmarks.client.Benchmark;
import com.google.gwt.benchmarks.client.IntRange;
import com.google.gwt.benchmarks.client.Operator;
import com.google.gwt.benchmarks.client.RangeField;

/**
 * Benchmark for operations on <code>long</code>.
 */
public class LongBenchmark extends Benchmark {
  /**
   * Which implementations can be benchmarked. ALT is present for benchmarking
   * alternative implementations against the standard one.
   */
  enum LongKind {
    ALT, STANDARD
  }

  protected final IntRange incrementRange = new IntRange(0, 62, Operator.ADD, 1);
  protected final LongKind[] toStringKinds = new LongKind[] {LongKind.STANDARD};
  protected final IntRange toStringRange = new IntRange(0, 62, Operator.ADD, 1);

  /**
   * This field is used as a target of assignments that should not be pruned.
   */
  @SuppressWarnings("unused")
  private volatile long volatileLong;

  /**
   * This field is used as a target of assignments that should not be pruned.
   */
  @SuppressWarnings("unused")
  private volatile String volatileString;

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuiteBenchmarks";
  }

  public void testIncrement() {
  }

  public void testIncrement(@RangeField("incrementRange")
  Integer xPwr) {
    long x = 1L << xPwr;
    for (int i = 1000; i != 0; i--) {
      x++;
      x++;
      x++;
    }
    volatileLong = x;
  }

  public void testToString() {
  }

  public void testToString(@RangeField("toStringRange")
  Integer xPwr, @RangeField("toStringKinds")
  LongKind longKind) {
    long x = 1L << xPwr;
    switch (longKind) {
      case STANDARD:
        for (int i = 100; i != 0; i--) {
          volatileString = String.valueOf(x);
          volatileString = String.valueOf(x);
          volatileString = String.valueOf(x);
        }
        break;
    }
  }
}
