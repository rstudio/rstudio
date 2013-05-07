/*
 * Copyright 2013 Google Inc.
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


import static java.util.Arrays.asList;

/**
 * This class tests inherited tests are executed conforming to junit3 rules.
 *
 * Note: This test requires some test methods to be executed in a specific order.
 */
public class GWTTestCaseInheritanceTest extends GWTTestCaseInheritanceTestBase {

  @Override
  protected void gwtTearDown() throws Exception {
    executions.add(getName());
  }

  @ExpectedFailure
  @Override
  public void testOverridden() {
    fail("failed on purpose");
  }

  /**
   * This is the last test to be executed (under_score forces that). Will assert all test runs.
   */
  public void test_assertExecution() {
    assertEquals(asList("testBasic", "testOverridden", "testStatic"), executions);
  }
}
