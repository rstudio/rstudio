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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.core.client.impl.Impl;
import com.google.gwt.junit.client.impl.GWTRunner;
import com.google.gwt.junit.client.impl.JUnitResult;
import com.google.gwt.user.client.Timer;

import junit.framework.TestCase;

/**
 * The translatable implementation of {@link GWTTestCase}.
 */
@SuppressWarnings("unused")
public abstract class GWTTestCase extends TestCase {

  /**
   * A watchdog class for use with asynchronous mode. On construction,
   * immediately schedules itself for the specified timeout. If the timeout
   * expires before this timer is canceled, causes the enclosing test case to
   * fail with {@link TimeoutException}.
   */
  private final class KillTimer extends Timer {

    /**
     * Stashed so the timeout can be reported via {@link TimeoutException}.
     */
    private final int timeoutMillis;

    public KillTimer(int timeoutMillis) {
      this.timeoutMillis = timeoutMillis;
      schedule(timeoutMillis);
    }

    public void run() {
      if (timer == this) {
        // The test has failed due to timeout
        reportResultsAndRunNextMethod(new TimeoutException(timeoutMillis));
      } else {
        // Something happened so that we are no longer the active timer.
        // Just do nothing.
      }
    }
  }

  /**
   * UncaughtExceptionHandler used to catch exceptions reported via
   * {@link GWT#reportUncaughtException}.
   */
  private final class TestCaseUncaughtExceptionHandler implements UncaughtExceptionHandler {
    @Override
    public void onUncaughtException(Throwable e) {
      reportUncaughtException(e);
    }
  }

  /**
   * Tracks whether the main test body has run (for asynchronous mode).
   */
  private boolean mainTestHasRun = false;

  /**
   * Tracks whether this test is completely done.
   */
  private boolean testIsFinished = false;

  /**
   * If non-null, a timer to kill the current test case (for asynchronous mode).
   */
  private KillTimer timer;

  // Holds the first exception that's thrown "synchronously", meaning "before
  // the test method returns".
  private Throwable synchronousException = null;

  /**
   * Name of the test class.
   */
  private String testClass;

  public void init(String testClass, String testName) {
    this.testClass = testClass;
    setName(testName);
  }

  // CHECKSTYLE_OFF
  /**
   * Actually run the user's test. Called from {@link GWTRunner}.
   */
  public void __doRunTest() {
    if (shouldCatchExceptions()) {
      try {
        runBare();
      } catch (Throwable e) {
        // If an exception was explicitly reported, it must have happened
        // before the exception was thrown from the test method.
        if (synchronousException == null) {
          synchronousException = e;
        }
      }
    } else {
      runBareTestCaseAvoidingExceptionDecl();
    }

    // Mark that the main test body has now run. From this point, if
    // timer != null we are in true asynchronous mode.
    mainTestHasRun = true;

    if (synchronousException != null || timer == null) {
      reportResultsAndRunNextMethod(synchronousException);
    } // else Test is still running; wait for asynchronous completion.
  }
  // CHECKSTYLE_ON

  public boolean catchExceptions() {
    return true;
  }

  public abstract String getModuleName();

  public boolean isPureJava() {
    return false;
  }

  @Override
  public void runBare() throws Throwable {
    setUp();
    runTest();
    // No tearDown call here; we do it from reportResults.
  }

  @Override
  protected void doRunTest(String name) throws Throwable {
    GWTRunner.get().executeTestMethod(this, testClass, name);
  }

  public void setForcePureJava(boolean forcePureJava) {
    // Ignore completely. The test is being run in GWT mode,
    // hence assumed not to be pure Java.
  }

  protected final void delayTestFinish(int timeoutMillis) {
    if (timer != null) {
      // Cancel the pending timer
      timer.cancel();
    }

    // Set a new timer for the specified new timeout
    timer = new KillTimer(timeoutMillis);
  }

  protected final void finishTest() {
    if (testIsFinished) {
      // This test is totally done already, just ignore the call.
      return;
    }

    if (timer == null) {
      throw new IllegalStateException(
          "This test is not in asynchronous mode; call delayTestFinish() first");
    }

    if (mainTestHasRun) {
      // This is a correct, successful async finish.
      reportResultsAndRunNextMethod(null);
    } else {
      // The user tried to finish the test before the main body returned!
      // Just let the test continue running normally.
      resetAsyncState();
    }
  }

  protected void gwtSetUp() throws Exception {
  }

  protected void gwtTearDown() throws Exception {
  }

  @Override
  protected final void setUp() throws Exception {
    // Make sure all exceptions escape to the browser if shouldCatchExceptions returns false
    setAllUncaughtExceptionHandlers(
        shouldCatchExceptions() ? new TestCaseUncaughtExceptionHandler() : null);
    gwtSetUp();
  }

  @Override
  protected final void tearDown() throws Exception {
    try {
      gwtTearDown();
    } finally {
      testIsFinished = true;
      setAllUncaughtExceptionHandlers(null);
      resetAsyncState();
    }
  }

  protected void reportUncaughtException(Throwable ex) {
    assertTestState();

    if (mainTestHasRun && timer != null) {
      // Asynchronous mode; uncaught exceptions cause an immediate failure.
      reportResultsAndRunNextMethod(ex);
    } else {
      // Synchronous mode: hang on to it for after the test method returns.
      // We can't call reportResultsAndRunNextMethod() yet, as it will cause
      // a race condition that often causes the same test to be run again.
      if (synchronousException == null) {
        synchronousException = ex;
      }
    }
  }

  private void assertTestState() {
    // TODO(goktug): Add assertTestState to other calls (e.g. delayTestFinish)
    // TODO(goktug): Report any problems via GWTRunner.
    assert (!testIsFinished);
  }

  /**
   * Cleans up any outstanding state, reports ex to the remote runner, and kicks off the next test.
   *
   * @param ex The results of this test.
   */
  private void reportResultsAndRunNextMethod(Throwable ex) {
    try {
      tearDown();
    } catch (Throwable e) {
      // ignore any exceptions thrown from tearDown
    }

    JUnitResult myResult = new JUnitResult();
    if (ex != null) {
      myResult.setException(ex);
    }

    GWTRunner.get().reportResultsAndGetNextMethod(myResult);
  }

  /**
   * Cleans up any asynchronous mode state.
   */
  private void resetAsyncState() {
    // clear our timer if there is one
    if (timer != null) {
      timer.cancel();
      timer = null;
    }
  }

  /**
   * In the mode where we need to let uncaught exceptions escape to the browser,
   * this method serves as a hack to avoid "throws" clause problems.
   */
  private native void runBareTestCaseAvoidingExceptionDecl() /*-{
    this.@junit.framework.TestCase::runBare()();
  }-*/;

  /**
   * A helper method to determine if we should catch exceptions. Wraps the call
   * into user code with a try/catch; if the user's code throws an exception, we
   * just ignore the exception and use the default behavior.
   *
   * @return <code>true</code> if exceptions should be handled normally,
   *         <code>false</code> if they should be allowed to escape.
   */
  private boolean shouldCatchExceptions() {
    try {
      return catchExceptions();
    } catch (Throwable e) {
      return true;
    }
  }

  private static void setAllUncaughtExceptionHandlers(UncaughtExceptionHandler handler) {
    Impl.setUncaughtExceptionHandlerForTest(handler);
    // TODO(goktug) There is still code out there using GWT#getUncaughtExceptionHandler to report
    // exceptions, so we need to keep setting the production exception handler for compatibility:
    GWT.setUncaughtExceptionHandler(handler);
  }
}
