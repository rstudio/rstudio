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

package com.google.gwt.junit.client;

import com.google.gwt.junit.client.annotations.RangeField;
import com.google.gwt.junit.client.annotations.RangeEnum;
import com.google.gwt.junit.client.annotations.Setup;
import com.google.gwt.junit.client.annotations.Teardown;
import com.google.gwt.junit.client.annotations.IterationTimeLimit;

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

  public String getModuleName() {
    return "com.google.gwt.junit.JUnit";
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
  @Setup("setup")
  @Teardown("teardown")
  public void testSetupAndTeardown() {
    assertEquals("setup", stateString);
    stateString = "running";
  }

  public void testTimeLimit() {
  }

  /**
   * Tests {@link @IterationTimeLimit}.
   * 
   * @param numIterations
   */
  @IterationTimeLimit(1L)
  public void testTimeLimit(@RangeField("veryLargeRange")
  Integer numIterations) {

    somethingExpensive();

    // Make sure we hit the time limit, instead of running through all
    // iterations.
    assertTrue(numIterations < Integer.MAX_VALUE);
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

  protected void setup() {
    assertNull(stateString);
    stateString = "setup";
  }

  protected void teardown() {
    assertNotNull(stateString);
    assertTrue(stateString.equals("running") || stateString.equals("setup"));
    stateString = null;
  }

  /**
   * Do something that is relatively expensive both in hosted mode and web mode.
   */
  private native void somethingExpensive() /*-{
    var deadField = 0;
    for (var i = 0; i < 10000; ++i) {
      deadField += Math.pow(deadField, i);
    }
  }-*/;
}
