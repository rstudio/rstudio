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
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.http.client.UrlBuilder;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.junit.client.impl.GWTRunnerProxy.TestAccessor;
import com.google.gwt.junit.client.impl.JUnitHost.ClientInfo;
import com.google.gwt.junit.client.impl.JUnitHost.InitialResponse;
import com.google.gwt.junit.client.impl.JUnitHost.TestBlock;
import com.google.gwt.junit.client.impl.JUnitHost.TestInfo;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamFactory;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

import java.util.HashMap;

/**
 * The entry point class for GWTTestCases.
 *
 * This is the main test running logic. Each time a test completes, the results
 * are reported back through {@link #junitHost}, and the next method to run is
 * returned. This process repeats until the next method to run is null.
 */
public class GWTRunner implements EntryPoint {

  private final class InitialResponseListener implements
      AsyncCallback<InitialResponse> {

    /**
     * Delegate to the {@link TestBlockListener}.
     */
    public void onFailure(Throwable caught) {
      testBlockListener.onFailure(caught);
    }

    /**
     * Update our client info with the server-provided session id then delegate
     * to the {@link TestBlockListener}.
     */
    public void onSuccess(InitialResponse result) {
      clientInfo = new ClientInfo(result.getSessionId(),
          clientInfo.getUserAgent());
      testBlockListener.onSuccess(result.getTestBlock());
    }
  }

  /**
   * The RPC callback object for {@link GWTRunner#junitHost}. When
   * {@link #onSuccess} is called, it's time to run the next test case.
   */
  private final class TestBlockListener implements AsyncCallback<TestBlock> {

    /**
     * The number of times we've failed to communicate with the server on the
     * current test batch.
     */
    private int curRetryCount = 0;

    /**
     * A call to junitHost failed.
     */
    public void onFailure(Throwable caught) {
      if (maxRetryCount < 0 || curRetryCount < maxRetryCount) {
        // Try the call again
        curRetryCount++;
        new Timer() {
          @Override
          public void run() {
            syncToServer();
          }
        }.schedule(1000);
      } else {
        reportFatalError("Cannot sync back to GWT junit backend: " + caught);
      }
    }

    /**
     * A call to junitHost succeeded; run the next test case.
     */
    public void onSuccess(TestBlock nextTestBlock) {
      curRetryCount = 0;
      currentBlock = nextTestBlock;
      currentTestIndex = 0;
      currentResults.clear();
      if (currentBlock != null && currentBlock.getTests().length > 0) {
        doRunTest();
      }
    }
  }

  /**
   * The singleton instance.
   */
  static GWTRunner sInstance;

  /**
   * A query param specifying my unique session cookie.
   */
  private static final String SESSIONID_QUERY_PARAM = "gwt.junit.sessionId";

  /**
   * A query param specifying the test class to run, for serverless mode.
   */
  private static final String TESTCLASS_QUERY_PARAM = "gwt.junit.testclassname";

  /**
   * A query param specifying the test method to run, for serverless mode.
   */
  private static final String TESTFUNC_QUERY_PARAM = "gwt.junit.testfuncname";

  /**
   * A query param specifying the number of times to retry if the server fails
   * to respond.
   */
  private static final String RETRYCOUNT_QUERY_PARAM = "gwt.junit.retrycount";

  /**
   * A query param specifying the block index to start on.
   */
  private static final String BLOCKINDEX_QUERY_PARAM = "gwt.junit.blockindex";

  public static GWTRunner get() {
    return sInstance;
  }

  /**
   * This client's info.
   */
  private ClientInfo clientInfo;

  /**
   * The current block of tests to execute.
   */
  private TestBlock currentBlock;

  /**
   * Active test within current block of tests.
   */
  private int currentTestIndex = 0;

  /**
   * Results for all test cases in the current block.
   */
  private HashMap<TestInfo, JUnitResult> currentResults = new HashMap<TestInfo, JUnitResult>();

  /**
   * If set, all remaining tests will fail with the failure message.
   */
  private String failureMessage;

  /**
   * The remote service to communicate with.
   */
  private final JUnitHostAsync junitHost = (JUnitHostAsync) GWT.create(JUnitHost.class);

  /**
   * Handles all {@link InitialResponse InitialResponses}.
   */
  private final InitialResponseListener initialResponseListener = new InitialResponseListener();

  /**
   * Handles all {@link TestBlock TestBlocks}.
   */
  private final TestBlockListener testBlockListener = new TestBlockListener();

  /**
   * The maximum number of times to retry communication with the server per
   * test batch.
   */
  private int maxRetryCount;

  /**
   * If true, run a single test case with no RPC.
   */
  private boolean serverless = false;

  private String userAgentProperty;
  private TestAccessor testAccessor;

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
    GWTRunnerProxy proxy = GWT.create(GWTRunnerProxy.class);
    userAgentProperty = proxy.getUserAgentProperty();
    testAccessor = proxy.createTestAccessor();

    clientInfo = new ClientInfo(parseQueryParamInteger(
        SESSIONID_QUERY_PARAM, -1), getUserAgentProperty());
    maxRetryCount = parseQueryParamInteger(RETRYCOUNT_QUERY_PARAM, 3);
    currentBlock = checkForQueryParamTestToRun();
    if (currentBlock != null) {
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
    if (failureMessage != null) {
      RuntimeException ex = new RuntimeException(failureMessage);
      result.setException(ex);
    } else if (result.exceptionWrapper != null) {
      ensureSerializable(result.exceptionWrapper);
    }
    TestInfo currentTest = getCurrentTest();
    currentResults.put(currentTest, result);
    ++currentTestIndex;
    if (currentTestIndex < currentBlock.getTests().length) {
      // Run the next test after a short delay.
      Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
        public void execute() {
          doRunTest();
        }
      });
    } else {
      syncToServer();
    }
  }

  /**
   * Convert unserializable exceptions into generic serializable ones.
   */
  private void ensureSerializable(ExceptionWrapper wrapper) {
    if (wrapper == null) {
      return;
    }

    ensureSerializable(wrapper.causeWrapper);
    try {
      SerializationStreamFactory fac = (SerializationStreamFactory) junitHost;
      SerializationStreamWriter dummyWriter = fac.createStreamWriter();
      dummyWriter.writeObject(wrapper.exception);
    } catch (SerializationException e) {
      wrapper.exception = new Exception(wrapper.exception.toString() +
          " (unserializable exception)");
    }
  }

  /**
   * Executes a test on provided test class instance.
   */
  public void executeTestMethod(GWTTestCase testCase, String className, String methodName)
      throws Throwable {
    testAccessor.invoke(testCase, className, methodName);
  }

  /**
   * Get the value of the user agent property.
   */
  protected String getUserAgentProperty() {
    return userAgentProperty;
  }

  private TestBlock checkForQueryParamTestToRun() {
    String testClass = Window.Location.getParameter(TESTCLASS_QUERY_PARAM);
    String testMethod = Window.Location.getParameter(TESTFUNC_QUERY_PARAM);
    if (testClass == null || testMethod == null) {
      return null;
    }
    // TODO: support blocks of tests?
    TestInfo[] tests = new TestInfo[] {new TestInfo(GWT.getModuleName(),
        testClass, testMethod)};
    return new TestBlock(tests, 0);
  }

  private void doRunTest() {
    // Make sure the module matches.
    String currentModule = GWT.getModuleName();
    String newModule = getCurrentTest().getTestModule();
    if (currentModule.equals(newModule)) {
      // The module is correct.
      runTest();
    } else {
      /*
       * We're being asked to run a test in a different module. We must navigate
       * the browser to a new URL which will run that other module.  We retain
       * the same path suffix (e.g., '/junit.html') as the current URL.
       */
      String currentPath = Window.Location.getPath();
      String pathSuffix = currentPath.substring(currentPath.lastIndexOf('/'));

      UrlBuilder builder = Window.Location.createUrlBuilder();
      builder.setParameter(BLOCKINDEX_QUERY_PARAM,
          Integer.toString(currentBlock.getIndex())).setPath(
          newModule + pathSuffix);
      // Hand off the session id to the next module.
      if (clientInfo.getSessionId() >= 0) {
        builder.setParameter(SESSIONID_QUERY_PARAM,
            String.valueOf(clientInfo.getSessionId()));
      }
      // Replace "%3A" with ":" as a hack to support broken DevMode plugins.
      Window.Location.replace(builder.buildString().replaceAll("%3A", ":"));
      currentBlock = null;
      currentTestIndex = 0;
    }
  }

  private TestInfo getCurrentTest() {
    return currentBlock.getTests()[currentTestIndex];
  }

  /**
   * Parse an integer from a query parameter, returning the default value if
   * the parameter cannot be found.
   *
   * @param paramName the parameter name
   * @param defaultValue the default value
   * @return the integer value of the parameter
   */
  private int parseQueryParamInteger(String paramName, int defaultValue) {
    String value = Window.Location.getParameter(paramName);
    if (value != null) {
      try {
        return Integer.parseInt(value);
      } catch (NumberFormatException e) {
        setFailureMessage("'" + value + "' is not a valid value for " +
            paramName + ".");
        return defaultValue;
      }
    }
    return defaultValue;
  }

  private void runTest() {
    // Dynamically create a new test case.
    TestInfo currentTest = getCurrentTest();
    GWTTestCase testCase = null;
    try {
      testCase = testAccessor.newInstance(currentTest.getTestClass());
    } catch (Throwable e) {
      RuntimeException ex = new RuntimeException(currentTest
          + ": could not instantiate the requested class", e);
      JUnitResult result = new JUnitResult();
      result.setException(ex);
      reportResultsAndGetNextMethod(result);
      return;
    }
    testCase.init(currentTest.getTestClass(), currentTest.getTestMethod());
    testCase.__doRunTest();
  }

  /**
   * Fail all tests with the specified message.
   */
  private void setFailureMessage(String message) {
    failureMessage = message;
  }

  private void syncToServer() {
    if (currentBlock == null) {
      int firstBlockIndex = parseQueryParamInteger(BLOCKINDEX_QUERY_PARAM, 0);
      junitHost.getTestBlock(firstBlockIndex, clientInfo,
          initialResponseListener);
    } else {
      junitHost.reportResultsAndGetTestBlock(currentResults,
          currentBlock.getIndex() + 1, clientInfo, testBlockListener);
    }
  }

  private static native void reportFatalError(String errorMsg)/*-{
    $wnd.junitError("fatal", errorMsg);
  }-*/;
}
