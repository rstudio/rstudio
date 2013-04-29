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
package com.google.gwt.junit;

import com.google.gwt.junit.client.DevModeOnCompiledScriptTest;
import com.google.gwt.junit.client.GWTTestCaseAsyncTest;
import com.google.gwt.junit.client.GWTTestCaseSetupTearDownTest;
import com.google.gwt.junit.client.GWTTestCaseStackTraceTest;
import com.google.gwt.junit.client.GWTTestCaseTest;
import com.google.gwt.junit.client.GWTTestCaseUncaughtExceptionHandlerTest;
import com.google.gwt.junit.client.PropertyDefiningGWTTest;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests of the junit package.
 */
public class JUnitSuite {
  public static Test suite() {
    TestSuite suite = new GwtTestSuiteWithExpectedFailures("Test suite for com.google.gwt.junit");

    suite.addTestSuite(GWTTestCaseTest.class);
    suite.addTestSuite(GWTTestCaseStackTraceTest.class);
    suite.addTestSuite(GWTTestCaseUncaughtExceptionHandlerTest.class);
    suite.addTest(new TestSuiteWithOrder(GWTTestCaseAsyncTest.class));
    suite.addTest(new TestSuiteWithOrder(GWTTestCaseSetupTearDownTest.class));

    suite.addTestSuite(DevModeOnCompiledScriptTest.class);

    // Must run after a GWTTestCase so JUnitShell is initialized.
    suite.addTestSuite(BatchingStrategyTest.class);
    suite.addTestSuite(CompileStrategyTest.class);

    suite.addTestSuite(FakeMessagesMakerTest.class);
    suite.addTestSuite(GWTMockUtilitiesTest.class);
    suite.addTestSuite(JUnitMessageQueueTest.class);
    suite.addTestSuite(GWTTestCaseNoClientTest.class);

    // Intended only to be run manually. See class comments
    // suite.addTestSuite(ParallelRemoteTest.class);

    // remote
    // Run manually only, launches servers that die on port contention
    // suite.addTestSuite(BrowserManagerServerTest.class);

    suite.addTestSuite(PropertyDefiningStrategyTest.class);
    suite.addTestSuite(PropertyDefiningGWTTest.class);

    suite.addTestSuite(JUnitShellTest.class);

    return suite;
  }
}
