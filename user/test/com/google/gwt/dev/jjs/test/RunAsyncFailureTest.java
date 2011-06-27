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
package com.google.gwt.dev.jjs.test;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.core.client.impl.LoadingStrategyBase;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Timer;

/**
 * Tests runAsync server/network failure handling.
 *
 * This is skipped in dev mode because runAsync never fails in dev mode.
 */
@DoNotRunWith(Platform.Devel)
public class RunAsyncFailureTest extends GWTTestCase {

  abstract static class MyRunAsyncCallback implements RunAsyncCallback {
    private static int sToken = 0;
    private int attempt;
    private int expectedSuccessfulAttempt;
    private int token;
    
    public MyRunAsyncCallback(int attempt, int expectedSuccessfulAttempt) {
      this.attempt = attempt;
      this.expectedSuccessfulAttempt = expectedSuccessfulAttempt;
      token = sToken++;
    }
    
    public int getToken() {
      return token;
    }
    
    public boolean onSuccessHelper(String test) {
      int token = getToken();
      log("onSuccess: attempt = " + attempt + ", token = " + token);
      if (attempt == expectedSuccessfulAttempt) {
        return true;
      } else {
        // We don't really care about the test string, but we need to use it
        // somewhere so it doesn't get dead stripped out.  Each test passes
        // in a unique string so it ends up in it's fragment.
        fail(test + " - Succeeded on attempt: " + attempt + 
            " but should have succeeded on attempt: " + expectedSuccessfulAttempt);
      }
      return false;
    }
    
    protected void onFailureHelper(Throwable caught, Timer t) {
      // Fail the test if too many attempts have taken place.
      if (attempt > 5) {
        fail("Too many failures");
      }
      
      int token = getToken();
      log("onFailure: attempt = " + attempt + ", token = " + token
          + ", caught = " + caught);
      t.schedule(100);
    }

    private native void log(String message) /*-{
      // Enable this for testing on Safari/WebKit browsers
      // $wnd.console.log(message);
     }-*/;
  }

  private static final int RUNASYNC_TIMEOUT = 30000;
  
  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.RunAsyncFailure";
  }
  
  /**
   * Some subclasses of this test use linkers that do not support retries, in
   * which case the expected number of manual retries before success will always
   * be 4.
   */
  protected boolean supportsRetries() {
    return true;
  }
  
  // Note that each tests needs a new subclass of MyRunAsyncCallback because
  // otherwise the runAsync code will cache the fragment and later tests will
  // always succeed on the first try.
  
  private void runAsync1(final int attempt, final int expectedSuccessfulAttempt) {
    GWT.runAsync(new MyRunAsyncCallback(attempt, expectedSuccessfulAttempt) {
      public void onFailure(Throwable caught) {
        onFailureHelper(caught, new Timer() {
          @Override
          public void run() {
            runAsync1(attempt + 1, expectedSuccessfulAttempt);
          }
        });
      }

      public void onSuccess() {
        if (onSuccessHelper("DOWNLOAD_FAILURE_TEST_1")) { finishTest(); }
      }
    });
  }
  
  private void runAsync2(final int attempt, final int expectedSuccessfulAttempt) {
    GWT.runAsync(new MyRunAsyncCallback(attempt, expectedSuccessfulAttempt) {
      public void onFailure(Throwable caught) {
        onFailureHelper(caught, new Timer() {
          @Override
          public void run() {
            runAsync2(attempt + 1, expectedSuccessfulAttempt);
          }
        });
      }

      public void onSuccess() {
        if (onSuccessHelper("DOWNLOAD_FAILURE_TEST_2")) { finishTest(); }
      }
    });
  }
  
  private void runAsync3(final int attempt, final int expectedSuccessfulAttempt) {
    GWT.runAsync(new MyRunAsyncCallback(attempt, expectedSuccessfulAttempt) {
      public void onFailure(Throwable caught) {
        onFailureHelper(caught, new Timer() {
          @Override
          public void run() {
            runAsync3(attempt + 1, expectedSuccessfulAttempt);
          }
        });
      }

      public void onSuccess() {
        if (onSuccessHelper("DOWNLOAD_FAILURE_TEST_3")) { finishTest(); }
      }
    });
  }
  
  private void runAsync4() {
    GWT.runAsync(new RunAsyncCallback() {
      public void onFailure(Throwable caught) {
        // This call should fail since no retries are done if the code downloads
        // successfully, but fails to install.
        finishTest();
      }
      public void onSuccess() {
        // Use the string "INSTALL_FAILURE_TEST" so we can identify this
        // fragment on the server.  In the fail message is good enough.
        fail("INSTALL_FAILURE_TEST - Code should have failed to install!");
      }
    });
  }
  
  /**
   * Test the basic functionality of retrying runAsync until is succeeds.
   * A Timer is used to avoid nesting GWT.runAsync calls.
   */
  public void testHttpFailureRetries() {
    delayTestFinish(RUNASYNC_TIMEOUT);
    // Default is 3, but we set it explicitly, since other tests may run first
    LoadingStrategyBase.MAX_AUTO_RETRY_COUNT = 2;
    // In RunAsyncFailureServlet, the 5th time is the charm, but the code
    // by default retries 3 times every time we call runAsync, so this
    // should succeed on the second runAsync call, which is attempt #1.
    runAsync1(0, supportsRetries() ? 1 : 4);
  }
  
  public void testHttpFailureRetries2() {
    delayTestFinish(RUNASYNC_TIMEOUT);
    LoadingStrategyBase.MAX_AUTO_RETRY_COUNT = 0;
    // In RunAsyncFailureServlet, the 5th time is the charm, so if we do not
    // let the code do any retries, we'll need to retry 4 times before succeeding
    runAsync2(0, 4);
  }
  
  public void testBuiltInRetries() {
    delayTestFinish(RUNASYNC_TIMEOUT);
    LoadingStrategyBase.MAX_AUTO_RETRY_COUNT = 4;
    // In RunAsyncFailureServlet, the 5th time is the charm, so retrying 4 times
    // should be enough to get it on the first try.
    runAsync3(0, supportsRetries() ? 0 : 4);
  }
  
  public void testDownloadSuccessButInstallFailure() {
    delayTestFinish(RUNASYNC_TIMEOUT);
    LoadingStrategyBase.MAX_AUTO_RETRY_COUNT = 3;
    runAsync4();
  }
}
