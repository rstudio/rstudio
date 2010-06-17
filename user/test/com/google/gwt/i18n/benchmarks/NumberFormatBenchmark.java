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

package com.google.gwt.i18n.benchmarks;

import com.google.gwt.benchmarks.client.Benchmark;
import com.google.gwt.benchmarks.client.IntRange;
import com.google.gwt.benchmarks.client.Operator;
import com.google.gwt.benchmarks.client.RangeField;
import com.google.gwt.i18n.client.NumberFormat;

/**
 * Benchmark for number format.
 */
public class NumberFormatBenchmark extends Benchmark {
  final IntRange sizeRange = new IntRange(50, 2000, Operator.ADD, 50);

  /**
   * Must refer to a valid module that inherits from com.google.gwt.junit.JUnit.
   */
  @Override
  public String getModuleName() {
    return "com.google.gwt.i18n.I18NBenchmarks";
  }

  // Required for JUnit
  public void testCurrency() {
  }

  /**
   * Basic test of currency format.
   * 
   * @param size the size
   */
  public void testCurrency(@RangeField("sizeRange")
  Integer size) {
    NumberFormat format = NumberFormat.getCurrencyFormat();
    int limit = size.intValue();
    for (int i = 0; i < limit; i++) {
      format.format(i);
    }
  }
}
