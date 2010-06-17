/*
 * Copyright 2009 Google Inc.
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

package com.google.gwt.benchmarks;

import com.google.gwt.benchmarks.client.Benchmark;
import com.google.gwt.junit.GWTTestCaseNoClientTest;
import com.google.gwt.junit.JUnitShell.Strategy;

/**
 * Tests for {@link Benchmark} that cannot run on the client.
 */
public class BenchmarkNoClientTest extends GWTTestCaseNoClientTest {

  /**
   * A mock version of the {@link Benchmark} used for testing.
   */
  private static class MockBenchmark extends Benchmark {
    @Override
    public String getModuleName() {
      return "com.google.gwt.mock.Mock";
    }
  }

  @Override
  public void testGetStrategy() {
    Benchmark testCase = new MockBenchmark();
    Strategy strategy = testCase.getStrategy();
    assertEquals("com.google.gwt.benchmarks.Benchmarks",
        strategy.getModuleInherit());
    assertEquals("Benchmarks", strategy.getSyntheticModuleExtension());
    assertEquals("com.google.gwt.mock.Mock.Benchmarks",
        testCase.getSyntheticModuleName());
  }
}
