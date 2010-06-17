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

package com.google.gwt.emultest.benchmarks.java.lang;

import com.google.gwt.benchmarks.client.Benchmark;
import com.google.gwt.benchmarks.client.IntRange;
import com.google.gwt.benchmarks.client.IterationTimeLimit;
import com.google.gwt.benchmarks.client.Operator;
import com.google.gwt.benchmarks.client.RangeField;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.impl.StringBufferImpl;
import com.google.gwt.core.client.impl.StringBufferImplAppend;
import com.google.gwt.core.client.impl.StringBufferImplArray;
import com.google.gwt.core.client.impl.StringBufferImplConcat;
import com.google.gwt.core.client.impl.StringBufferImplPush;

/**
 * Tests StringBuilder impl directly against each other. Useful when profiling
 * browser behavior.
 */
public class StringBufferImplBenchmark extends Benchmark {

  /**
   * The type of StringBuilder to use for a test.
   */
  protected enum SBType {
    APPEND("Append"), ARRAY("Array"), CONCAT("Concat"), PUSH("Push");

    public String description;

    private SBType(String description) {
      this.description = description;
    }

    @Override
    public String toString() {
      return description;
    }
  }

  @SuppressWarnings("unused")
  private static volatile String result;

  private static volatile Object[] stashSomeGarbage;

  static {
    if (GWT.isClient()) {
      stashSomeGarbage = new Object[10000];
      for (int i = 0; i < stashSomeGarbage.length; ++i) {
        stashSomeGarbage[i] = new Object();
      }
    }
  }

  final SBType[] appendKindsRange = new SBType[] {
      SBType.APPEND, SBType.ARRAY, SBType.CONCAT, SBType.PUSH};

  final IntRange manyTimesRange = new IntRange(32, 8192, Operator.MULTIPLY, 2);

  final IntRange singleTimesRange = new IntRange(32, 8192, Operator.MULTIPLY, 2);

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuiteBenchmarks";
  }

  public void testManyAppends() {
  }

  @IterationTimeLimit(0)
  public void testManyAppends(@RangeField("manyTimesRange")
  Integer times, @RangeField("appendKindsRange")
  SBType sbtype) {
    int number = (int) Math.sqrt(times.intValue());
    switch (sbtype) {
      case APPEND:
        for (int i = 0; i < number; ++i) {
          result = doAppend(number);
          result = null;
        }
        break;
      case ARRAY:
        for (int i = 0; i < number; ++i) {
          result = doArray(number);
          result = null;
        }
        break;
      case CONCAT:
        for (int i = 0; i < number; ++i) {
          result = doConcat(number);
          result = null;
        }
        break;
      case PUSH:
        for (int i = 0; i < number; ++i) {
          result = doPush(number);
          result = null;
        }
        break;
    }
  }

  public void testSingleAppend() {
  }

  @IterationTimeLimit(0)
  public void testSingleAppend(@RangeField("singleTimesRange")
  Integer times, @RangeField("appendKindsRange")
  SBType sbtype) {
    int number = times;
    switch (sbtype) {
      case APPEND:
        result = doAppend(number);
        break;
      case ARRAY:
        result = doArray(number);
        break;
      case CONCAT:
        result = doConcat(number);
        break;
      case PUSH:
        result = doPush(number);
        break;
    }
    result = null;
  }

  private String doAppend(int limit) {
    StringBufferImpl impl = new StringBufferImplAppend();
    Object data = impl.createData();
    for (int i = 0; i < limit; i++) {
      impl.appendNonNull(data, "hello");
    }
    return impl.toString(data);
  }

  private String doArray(int limit) {
    StringBufferImpl impl = new StringBufferImplArray();
    Object data = impl.createData();
    for (int i = 0; i < limit; i++) {
      impl.appendNonNull(data, "hello");
    }
    return impl.toString(data);
  }

  private String doConcat(int limit) {
    StringBufferImpl impl = new StringBufferImplConcat();
    Object data = impl.createData();
    for (int i = 0; i < limit; i++) {
      impl.appendNonNull(data, "hello");
    }
    return impl.toString(data);
  }

  private String doPush(int limit) {
    StringBufferImpl impl = new StringBufferImplPush();
    Object data = impl.createData();
    for (int i = 0; i < limit; i++) {
      impl.appendNonNull(data, "hello");
    }
    return impl.toString(data);
  }
}
