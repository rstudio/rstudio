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
package com.google.gwt.junit.client.impl;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.junit.client.impl.JUnitHost.TestInfo;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

/**
 * The entry point class for GWTTestCases.
 * 
 * This is the main test running logic. Each time a test completes, the results
 * are reported back through {@link #junitHost}, and the next method to run is
 * returned. This process repeats until the next method to run is null.
 */
public abstract class GWTRunner implements EntryPoint {

  /**
   * The RPC callback object for {@link GWTRunner#junitHost}. When
   * {@link #onSuccess(Object)} is called, it's time to run the next test case.
   */
  private final class JUnitHostListener implements AsyncCallback<TestInfo> {

    /**
     * A call to junitHost failed.
     */
    public void onFailure(Throwable caught) {
      // Try the call again
      new Timer() {
        @Override
        public void run() {
          syncToServer();
        }
      }.schedule(1000);
    }

    /**
     * A call to junitHost succeeded; run the next test case.
     */
    public void onSuccess(TestInfo nextTest) {
      currentTest = nextTest;
      if (currentTest != null) {
        doRunTest();
      }
    }
  }

  /**
   * The singleton instance.
   */
  private static GWTRunner sInstance;

  /**
   * A query param specifying the test class to run, for serverless mode.
   */
  private static final String TESTCLASS_QUERY_PARAM = "gwt.junit.testclassname";

  /**
   * A query param specifying the test method to run, for serverless mode.
   */
  private static final String TESTFUNC_QUERY_PARAM = "gwt.junit.testfuncname";

  public static GWTRunner get() {
    return sInstance;
  }

  private JUnitResult currentResult;

  private TestInfo currentTest;

  /**
   * The remote service to communicate with.
   */
  private final JUnitHostAsync junitHost = (JUnitHostAsync) GWT.create(JUnitHost.class);

  /**
   * Handles all RPC responses.
   */
  private final JUnitHostListener junitHostListener = new JUnitHostListener();

  /**
   * If true, run a single test case with no RPC.
   */
  private boolean serverless = false;

  // TODO(FINDBUGS): can this be a private constructor to avoid multiple
  // instances?
  public GWTRunner() {
    sInstance = this;

    // Bind junitHost to the appropriate url.
    ServiceDefTarget endpoint = (ServiceDefTarget) junitHost;
    String url = GWT.getModuleBaseURL() + "junithost";
    endpoint.setServiceEntryPoint(url);

    // Null out the default uncaught exception handler since we will control it.
    GWT.setUncaughtExceptionHandler(null);
  }

  public void onModuleLoad() {
    currentTest = checkForQueryParamTestToRun();
    if (currentTest != null) {
      /*
       * Just run a single test with no server-side interaction.
       */
      serverless = true;
      runTest();
    } else {
      /*
       * Normal operation: Kick off the test running process by getting the
       * first method to run from the server.
       */
      syncToServer();
    }
  }

  public void reportResultsAndGetNextMethod(JUnitResult result) {
    if (serverless) {
      // That's it, we're done
      return;
    }
    currentResult = result;
    syncToServer();
  }

  /**
   * Implemented by the generated subclass. Creates an instance of the specified
   * test class by fully qualified name.
   */
  protected abstract GWTTestCase createNewTestCase(String testClass);

  private TestInfo checkForQueryParamTestToRun() {
    String testClass = Window.Location.getParameter(TESTCLASS_QUERY_PARAM);
    String testMethod = Window.Location.getParameter(TESTFUNC_QUERY_PARAM);
    if (testClass == null || testMethod == null) {
      return null;
    }
    return new TestInfo(GWT.getModuleName(), testClass, testMethod);
  }

  private void doRunTest() {
    // Make sure the module matches.
    String currentModule = GWT.getModuleName();
    String newModule = currentTest.getTestModule();
    if (currentModule.equals(newModule)) {
      // The module is correct.
      runTest();
    } else {
      /*
       * We're being asked to run a test in a different module. We must navigate
       * the browser to a new URL which will run that other module.
       */
      String href = Window.Location.getHref();
      String newHref = href.replace(currentModule, newModule);
      Window.Location.replace(newHref);
    }
  }

  private void runTest() {
    // Dynamically create a new test case.
    GWTTestCase testCase = null;
    Throwable caught = null;
    try {
      testCase = createNewTestCase(currentTest.getTestClass());
    } catch (Throwable e) {
      caught = e;
    }
    if (testCase == null) {
      RuntimeException ex = new RuntimeException(currentTest
          + ": could not instantiate the requested class", caught);
      JUnitResult result = new JUnitResult();
      result.setExceptionWrapper(new ExceptionWrapper(ex));
      reportResultsAndGetNextMethod(result);
      return;
    }

    testCase.setName(currentTest.getTestMethod());
    testCase.__doRunTest();
  }

  private void syncToServer() {
    if (currentTest == null) {
      junitHost.getFirstMethod(junitHostListener);
    } else {
      junitHost.reportResultsAndGetNextMethod(currentTest, currentResult,
          junitHostListener);
    }
  }

}
