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
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Timer;

/**
 * Tests runAsync server/network failure handling.
 */
public class RunAsyncFailureTest extends GWTTestCase {

  abstract static class MyRunAsyncCallback implements RunAsyncCallback {
    private static int sToken = 0;
    private int token;
    
    public MyRunAsyncCallback() {
      token = sToken++;
    }
    
    public int getToken() {
      return token;
    }
  }

  private static final int RUNASYNC_TIMEOUT = 30000;
  
  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.RunAsyncFailure";
  }
  
  // Repeated runAsync using a Timer
  private void runAsync1(final int attempt) {
    log("runAsync1: attempt = " + attempt);
    GWT.runAsync(new MyRunAsyncCallback() {
      public void onFailure(Throwable caught) {
        // Fail the test if too many attempts have taken place.
        if (attempt > 20) {
          fail();
        }
        
        int token = getToken();
        log("onFailure: attempt = " + attempt + ", token = " + token
            + ", caught = " + caught);
        new Timer() {
          @Override
          public void run() {
            runAsync1(attempt + 1);
          }
        }.schedule(100);
      }

      public void onSuccess() {
        int token = getToken();
        log("onSuccess: attempt = " + attempt + ", token = " + token);
        finishTest();
      }
    });
  }
  
  /**
   * Test the basic functionality of retrying runAsync until is succeeds.
   * A Timer is used to avoid nesting GWT.runAsync calls.
   */
  public void testHttpFailureRetries() {
    delayTestFinish(RUNASYNC_TIMEOUT);
    runAsync1(0);
  }

  private native void log(String message) /*-{
    // Enable this for testing on Safari/WebKit browsers
    // $wnd.console.log(message);
  }-*/;
}
