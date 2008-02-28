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
      // We're not doing anything, which will stop the test harness.
      // TODO: try the call again?
    }

    /**
     * A call to junitHost succeeded; run the next test case.
     */
    public void onSuccess(TestInfo nextTest) {
      if (nextTest != null) {
        runTest(nextTest);
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

  private static native String getQuery() /*-{
    return $wnd.location.search || '';  
  }-*/;

  private static String getQueryParam(String query, String queryParam) {
    int pos = query.indexOf("?" + queryParam + "=");
    if (pos < 0) {
      pos = query.indexOf("&" + queryParam + "=");
    }
    if (pos < 0) {
      return null;
    }
    // advance past param name to to param value; +2 for the '&' and '='
    pos += queryParam.length() + 2;
    String result = query.substring(pos);
    // trim any query params that follow
    pos = result.indexOf('&');
    if (pos >= 0) {
      result = result.substring(0, pos);
    }
    return result;
  }

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
    TestInfo queryParamTestToRun = checkForQueryParamTestToRun();
    if (queryParamTestToRun != null) {
      /*
       * Just run a single test with no server-side interaction.
       */
      serverless = true;
      runTest(queryParamTestToRun);
    } else {
      /*
       * Normal operation: Kick off the test running process by getting the
       * first method to run from the server.
       */
      junitHost.getFirstMethod(GWT.getModuleName(), junitHostListener);
    }
  }

  public void reportResultsAndGetNextMethod(JUnitResult result) {
    if (serverless) {
      // That's it, we're done
      return;
    }
    junitHost.reportResultsAndGetNextMethod(GWT.getModuleName(), result,
        junitHostListener);
  }

  /**
   * Implemented by the generated subclass. Creates an instance of the specified
   * test class by fully qualified name.
   */
  protected abstract GWTTestCase createNewTestCase(String testClass);

  private TestInfo checkForQueryParamTestToRun() {
    String query = getQuery();
    String testClass = getQueryParam(query, TESTCLASS_QUERY_PARAM);
    String testMethod = getQueryParam(query, TESTFUNC_QUERY_PARAM);
    if (testClass == null || testMethod == null) {
      return null;
    }
    return new TestInfo(testClass, testMethod);
  }

  private void runTest(TestInfo testToRun) {
    // Dynamically create a new test case.
    GWTTestCase testCase = createNewTestCase(testToRun.getTestClass());
    if (testCase == null) {
      RuntimeException ex = new RuntimeException(testToRun
          + ": could not instantiate the requested class");
      JUnitResult result = new JUnitResult();
      result.setExceptionWrapper(new ExceptionWrapper(ex));
      reportResultsAndGetNextMethod(result);
    }

    testCase.setName(testToRun.getTestMethod());
    testCase.__doRunTest();
  }

}
