/*
 * Copyright 2012 Google Inc.
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

import static com.google.gwt.junit.client.GWTTestCaseSetupTearDownTest.SetUpTearDownState.SETUP;
import static com.google.gwt.junit.client.GWTTestCaseSetupTearDownTest.SetUpTearDownState.TEARDOWN;
import static com.google.gwt.junit.client.GWTTestCaseSetupTearDownTest.SetUpTearDownState.TESTCASE;

import com.google.gwt.junit.ExpectedFailure;
import com.google.gwt.user.client.Timer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class tests {@link GWTTestCase#setUp()} and {@link GWTTestCase#tearDown()}.
 *
 * Note: This test requires some test methods to be executed in a specific order.
 */
public class GWTTestCaseSetupTearDownTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.junit.JUnit";
  }

  /**
   * Tracks setup, teardown and testcase runs.
   */
  protected enum SetUpTearDownState {
    SETUP, TEARDOWN, TESTCASE
  }

  private static List<SetUpTearDownState> executions = new ArrayList<SetUpTearDownState>();

  @Override
  protected void gwtSetUp() {
    executions.add(SETUP);
  }

  @Override
  protected void gwtTearDown() {
    executions.add(TEARDOWN);
  }

  /**
   * This pseudo testcase for recording setup/testcase/teardown order.
   */
  public void testSetUpTearDown() {
    executions.add(TESTCASE);
  }

  /**
   * Delays test finish.
   */
  public void testSetUpTearDownAsync() {
    delayTestFinish(1000);
    new Timer() {
      @Override
      public void run() {
        executions.add(TESTCASE);
        finishTest();
      }
    }.schedule(1);
  }

  /**
   * Fails.
   */
  @ExpectedFailure
  public void testSetUpTearDownFail() {
    executions.add(TESTCASE);
    fail();
  }

  /**
   * Fails with delayed test finish.
   */
  @ExpectedFailure(withMessage = "testSetUpTearDownFailAsync")
  public void testSetUpTearDownFailSync() {
    executions.add(TESTCASE);
    delayTestFinish(1000);
    fail("testSetUpTearDownFailAsync");
  }

  /**
   * Fails async.
   */
  @ExpectedFailure(withMessage = "testSetUpTearDownFailAsync")
  public void testSetUpTearDownFailAsync() {
    delayTestFinish(1000);
    new Timer() {
      @Override
      public void run() {
        executions.add(TESTCASE);
        fail("testSetUpTearDownFailAsync");
      }
    }.schedule(1);
  }

  /**
   * Times out async.
   */
  @ExpectedFailure(withType = TimeoutException.class)
  public void testSetUpTearDownTimeout() {
    executions.add(TESTCASE);
    delayTestFinish(1);
  }

  /**
   * This is the last test to be is executed (under_score forces that). Will assert all setups &
   * teardowns.
   */
  public void test_assertAllSetUpTearDowns() {
    // These list needs to kept in alphabetical order for correct failure messaging.
    final String[] testCases = {
        "testSetUpTearDown",
        "testSetUpTearDownAsync",
        "testSetUpTearDownFail",
        "testSetUpTearDownFailAsync",
        "testSetUpTearDownFailSync",
        "testSetUpTearDownTimeout"};

    Iterator<SetUpTearDownState> iterator = executions.iterator();
    for (String testCase : testCases) {
      assertSame(testCase, SETUP, iterator.next());
      assertSame(testCase, TESTCASE, iterator.next());
      assertSame(testCase, TEARDOWN, iterator.next());
    }
    assertSame(SETUP, iterator.next()); // one last setup call for this test case
    assertFalse(iterator.hasNext());
  }
}
