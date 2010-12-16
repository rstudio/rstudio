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

package com.google.gwt.benchmarks.client;

/**
 * Basic Benchmark testing.
 */
public class BenchmarkTest extends Benchmark {

  /**
   * Test enum.
   */
  protected enum TestEnum {
    A, B, C;
  }

  final IntRange intRange = new IntRange(0, 20, Operator.ADD, 5);

  final IntRange intRange2 = new IntRange(10, 1000, Operator.MULTIPLY, 10);

  long startTime;

  String stateString = null;

  final String stringField = "foo";

  final IntRange veryLargeRange = new IntRange(0, Integer.MAX_VALUE,
      Operator.ADD, 1);

  public void disabledTestTimeLimit() {
  }

  /**
   * Tests {@link @IterationTimeLimit}.
   * 
   * <p>
   * TODO(tobyr) Disabled, because it can hang some browsers (Safari at least)
   * TimeLimits work in general (as evidenced by working benchmarks), but
   * there's something peculiar about this test causing problems.
   * </p>
   * 
   * @param numIterations
   */
  @IterationTimeLimit(1L)
  public void disabledTestTimeLimit(@RangeField("veryLargeRange")
  Integer numIterations) {

    somethingExpensive();

    // Make sure we hit the time limit, instead of running through all
    // iterations.
    assertTrue(numIterations < Integer.MAX_VALUE);
  }

  public String getModuleName() {
    return "com.google.gwt.benchmarks.Benchmarks";
  }

  public void disableTestAutoboxing() {
  }

  /**
   * Tests that autoboxing works correctly.
   * 
   * <p>
   * TODO(tobyr): this causes the generated code to not compile; should probably
   * be a warning or error if autoboxing args isn't supported.
   * </p>
   */
  public void disableTestAutoboxing(@SuppressWarnings("unused")
  @RangeField("intRange")
  int value) {
  }

  public void testEnumRange() {
  }

  /**
   * Tests that we receive the enums in a range.
   * 
   * @param enumValue
   */
  @Setup("setupEnum")
  @Teardown("teardownEnum")
  public void testEnumRange(@RangeEnum(TestEnum.class)
  TestEnum enumValue) {
    assertNotNull(enumValue);
  }

  /**
   * Tests that a zero argument function works correctly.
   * 
   */
  public void testNoParameters() {
    assertEquals("foo", stringField);
  }

  public void testOneParameterField() {
  }

  /**
   * Tests that a single argument function works correctly.
   * 
   * @param value
   */
  public void testOneParameterField(@RangeField("intRange")
  Integer value) {
    assertTrue(value >= 0 && value <= 100 && value % 5 == 0);
  }

  /**
   * Tests {@link Setup} and {@link Teardown}.
   * 
   */
  @Setup("customSetup")
  @Teardown("customTeardown")
  public void testSetupAndTeardown() {
    assertEquals("setup", stateString);
    stateString = "running";
  }

  /**
   * Tests that this method without a corresponding zero-arg method won't break
   * the compile.
   */
  public void testStrayMethodCompiles(@SuppressWarnings("unused")
  @RangeField("intRange")
  int value) {
  }

  public void testTwoParameterField() {
  }

  /**
   * Tests that a multiple argument function works correctly.
   * 
   */
  public void testTwoParameterField(@RangeField("intRange")
  Integer intOne, @RangeField("intRange2")
  Integer intTwo) {
    assertTrue(intOne >= 0 && intOne <= 100 && intOne % 5 == 0);
    assertTrue(intTwo >= 10 && intTwo <= 1000 && intTwo % 10 == 0);
  }

  protected void customSetup() {
    assertNull(stateString);
    stateString = "setup";
  }

  protected void customTeardown() {
    assertNotNull(stateString);
    assertTrue(stateString.equals("running") || stateString.equals("setup"));
    stateString = null;
  }

  /**
   * Do something that is relatively expensive both in Development Mode and
   * Production Mode.
   */
  private native void somethingExpensive() /*-{
    var deadField = 0;
    for (var i = 0; i < 10000; ++i) {
      deadField += Math.pow(deadField, i);
    }
  }-*/;
}
