/*
 * Copyright 2006 Google Inc.
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.junit.client.TimeoutException;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

import java.util.ArrayList;
import java.util.List;

/**
 * The implementation class for the translatable {@link GWTTestCase}.
 * 
 * This is the main test running logic. Each time a test completes, the results
 * are reported back through {@link #junitHost}, and the next method to run is
 * returned. This process repeats until the next method to run is null.
 * 
 * This class is split out of {@link GWTTestCase} to make debugging work. Trying
 * to debug the translatable {@link GWTTestCase} confuses the debugger, which
 * tends to use the non-translatable version.
 */
public class GWTTestCaseImpl implements UncaughtExceptionHandler {

  /**
   * The RPC callback object for {@link GWTTestCaseImpl#junitHost}. When
   * {@link #onSuccess(Object)} is called, it's time to run the next test case.
   */
  private final class JUnitHostListener implements AsyncCallback {

    /**
     * A call to junitHost failed.
     */
    public void onFailure(Throwable caught) {
      // just stop the test?
    }

    /**
     * A call to junitHost succeeded; run the next test case.
     */
    public void onSuccess(Object result) {
      if (result != null) {
        // Clone the current test case object
        GWTTestCase testCase = outer.getNewTestCase();
        // Tell it what method name to run
        testCase.setName((String) result);
        // Launch it
        testCase.impl.runTest();
      }
    }
  }

  /**
   * A watchdog class for use with asynchronous mode. On construction,
   * immediately schedules itself for the specified timeout. If the timeout
   * expires before this timer is cancelled, causes the enclosing test case to
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
   * The remote service to communicate with.
   */
  private static final JUnitHostAsync junitHost = (JUnitHostAsync) GWT.create(JUnitHost.class);

  static {
    // Bind junitHost to the appropriate url.
    ServiceDefTarget endpoint = (ServiceDefTarget) junitHost;
    String url = GWT.getModuleBaseURL() + "junithost";
    endpoint.setServiceEntryPoint(url);

    // Null out the default uncaught exception handler since control it.
    GWT.setUncaughtExceptionHandler(null);
  }

  /**
   * The collected checkpoint messages.
   */
  private List checkPoints;

  /**
   * Handles all RPC responses.
   */
  private final JUnitHostListener junitHostListener = new JUnitHostListener();

  /**
   * Tracks whether the main test body has run (for asynchronous mode).
   */
  private boolean mainTestHasRun = false;

  /**
   * My paired (enclosing) {@link GWTTestCase}.
   */
  private final GWTTestCase outer;

  /**
   * Tracks whether this test is completely done.
   */
  private boolean testIsFinished = false;

  /**
   * If non-null, a timer to kill the current test case (for asynchronous mode).
   */
  private KillTimer timer;

  /**
   * Constructs a new GWTTestCaseImpl that is paired one-to-one with a
   * {@link GWTTestCase}.
   * 
   * @param outer The paired (enclosing) GWTTestCase.
   */
  public GWTTestCaseImpl(GWTTestCase outer) {
    this.outer = outer;
  }

  /**
   * Implementation of {@link GWTTestCase#addCheckpoint(String)}.
   */
  public void addCheckpoint(String msg) {
    if (checkPoints == null) {
      checkPoints = new ArrayList();
    }
    checkPoints.add(msg);
  }

  public void clearCheckpoints() {
    checkPoints = null;
  }

  /**
   * Implementation of {@link GWTTestCase#delayTestFinish(int)}.
   */
  public void delayTestFinish(int timeoutMillis) {
    if (timer != null) {
      // Cancel the pending timer
      timer.cancel();
    }

    // Set a new timer for the specified new timeout
    timer = new KillTimer(timeoutMillis);
  }

  /**
   * Implementation of {@link GWTTestCase#finishTest()}.
   */
  public void finishTest() {
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

  public String[] getCheckpoints() {
    if (checkPoints == null) {
      return new String[0];
    } else {
      int len = checkPoints.size();
      String[] result = new String[len];
      for (int i = 0; i < len; ++i) {
        result[i] = (String) checkPoints.get(i);
      }
      return result;
    }
  }

  /**
   * Implementation of {@link GWTTestCase#onModuleLoad()}.
   */
  public void onModuleLoad() {
    /*
     * Kick off the test running process by getting the first method to run from
     * the server.
     */
    junitHost.getFirstMethod(outer.getTestName(), junitHostListener);
  }

  /**
   * An uncaught exception escaped to the browser; what we should do depends on
   * what state we're in.
   */
  public void onUncaughtException(Throwable ex) {
    if (mainTestHasRun && timer != null) {
      // Asynchronous mode; uncaught exceptions cause an immediate failure.
      assert (!testIsFinished);
      reportResultsAndRunNextMethod(ex);
    } else {
      // just ignore it
    }
  }

  /**
   * Cleans up any outstanding state, reports ex to the remote runner, and kicks
   * off the next test.
   * 
   * @param ex The results of this test.
   */
  private void reportResultsAndRunNextMethod(Throwable ex) {
    testIsFinished = true;

    resetAsyncState();

    ExceptionWrapper ew = null;
    if (ex != null) {
      ew = new ExceptionWrapper(ex);
      if (checkPoints != null) {
        for (int i = 0, c = checkPoints.size(); i < c; ++i) {
          ew.message += "\n" + checkPoints.get(i);
        }
      }
    }
    junitHost.reportResultsAndGetNextMethod(outer.getTestName(), ew,
        junitHostListener);
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
    this.@com.google.gwt.junit.client.impl.GWTTestCaseImpl::outer.@junit.framework.TestCase::runBare()();
  }-*/;

  /**
   * Actually run the user's test.
   */
  private void runTest() {
    Throwable caught = null;
    if (shouldCatchExceptions()) {
      // Make sure no exceptions escape
      GWT.setUncaughtExceptionHandler(this);
      try {
        outer.runBare();
      } catch (Throwable e) {
        caught = e;
      }
    } else {
      // Special; make sure all exceptions escape to the browser (for debugging)
      GWT.setUncaughtExceptionHandler(null);
      runBareTestCaseAvoidingExceptionDecl();
    }

    // Mark that the main test body has now run. From this point, if
    // timer != null we are in true asynchronous mode.
    mainTestHasRun = true;

    if (caught != null) {
      // Test failed; finish test no matter what state we're in.
      reportResultsAndRunNextMethod(caught);
    } else if (timer != null) {
      // Test is still running; wait for asynchronous completion.
    } else {
      // Test is really done; report success.
      reportResultsAndRunNextMethod(null);
    }
  }

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
      return outer.catchExceptions();
    } catch (Throwable e) {
      return true;
    }
  }
}
