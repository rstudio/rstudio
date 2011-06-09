/*
 * Copyright 2011 Google Inc.
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

package com.google.gwt.core.client.prefetch;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Exercises isLoaded() method in {@link RunAsyncCode}
 */
public class RunAsyncCodeTest extends GWTTestCase {
  /**
   * Used as a label for GWT.runAsync() call.
   */
  public static class IsLoadedMarker {
  }

  /**
   * Used as a label for GWT.runAsync() call.
   */
  public static class TestPrefetchMarker {
  }

  public static final int ASYNC_DELAY_MSEC = 10000;

  // Used to cancel the repeating timer in testPrefetch()
  boolean cancelPrefetchTest = false;

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Core";
  }

  @Override
  public void gwtTearDown() {
    cancelPrefetchTest = true;
  }

  /**
   * Checks the return of isLoaded() to make sure it returns
   * <code>false</false> for a yet to be referenced split point and <code>true</code>
   * after onSuccess is called.
   */
  public void testIsLoaded() {
    // This test is not applicable to DevMode
    if (!GWT.isScript()) {
      return;
    }

    delayTestFinish(ASYNC_DELAY_MSEC);
    final RunAsyncCode prefetchSplitPoint = RunAsyncCode.runAsyncCode(IsLoadedMarker.class);
    assertFalse(prefetchSplitPoint.isLoaded());

    GWT.runAsync(IsLoadedMarker.class, new RunAsyncCallback() {
      @Override
      public void onFailure(Throwable reason) {
        fail("runAsync() call failed.");
      }

      @Override
      public void onSuccess() {
        assertTrue(prefetchSplitPoint.isLoaded());
        finishTest();
      }
    });
  }

  /**
   * Make a call to {@link Prefetcher} to prefetch a split point and wait for it
   * to be loaded before invoking it.
   */
  public void testPrefetch() {
    cancelPrefetchTest = false;
    // This test is not applicable to DevMode
    if (!GWT.isScript()) {
      return;
    }

    delayTestFinish(ASYNC_DELAY_MSEC);
    final RunAsyncCode prefetchSplitPoint = RunAsyncCode.runAsyncCode(TestPrefetchMarker.class);
    Prefetcher.prefetch(prefetchSplitPoint);

    assertFalse(prefetchSplitPoint.isLoaded());
    Prefetcher.start();

    Scheduler.get().scheduleFixedDelay(new RepeatingCommand() {

      @Override
      public boolean execute() {
        // gwtTearDown() will set this flag to stop the repeating timer if
        // the test case is already finished due to a timeout.
        if (cancelPrefetchTest) {
          return false;
        }

        if (prefetchSplitPoint.isLoaded()) {
          GWT.runAsync(TestPrefetchMarker.class, new RunAsyncCallback() {

            @Override
            public void onFailure(Throwable reason) {
              fail("runAsync call failed.");
            }

            @Override
            public void onSuccess() {
              finishTest();
            }
          });
          return false;
        }
        return true;
      }
    }, 50);
  }
}
