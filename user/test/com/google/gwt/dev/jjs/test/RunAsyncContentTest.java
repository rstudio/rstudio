/*
 * Copyright 2013 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.jjs.test;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests that content unique to the execution of a particular fragment is not physically included in
 * the shared leftOver fragment. Also tests that shared content does physically exist in the shared
 * leftOver fragment.
 * 
 * Skipped in dev mode because of its different runAsync loading strategy.
 */
@DoNotRunWith(Platform.Devel)
public class RunAsyncContentTest extends GWTTestCase {

  private static final int RUNASYNC_TIMEOUT = 30000;

  @Override
  public String getModuleName() {
    // References the .gwt.xml file that sets up deferred binding for LoggingXhrLoadingStrategy.
    return "com.google.gwt.dev.jjs.RunAsyncContent";
  }

  public void testSharedContent() {
    delayTestFinish(RUNASYNC_TIMEOUT);

    GWT.runAsync(new RunAsyncCallback() {
      @Override
      public void onFailure(Throwable caught) {
        reportUncaughtException(caught);
      }

      @Override
      public void onSuccess() {
        String sharedContent = "Same String in multiple fragments.";
        assertTrue(LoggingXhrLoadingStrategy.getLeftOverFragmentText().contains(sharedContent));
        // Doesn't matter which one finishes first since that is not taken into account in code
        // splitting logic.
        finishTest();
      }
    });
    GWT.runAsync(new RunAsyncCallback() {
      @Override
      public void onFailure(Throwable caught) {
        reportUncaughtException(caught);
      }

      @Override
      public void onSuccess() {
        String sharedContent = "Same String in multiple fragments.";
        assertTrue(LoggingXhrLoadingStrategy.getLeftOverFragmentText().contains(sharedContent));
        // Doesn't matter which one finishes first since that is not taken into account in code
        // splitting logic.
        finishTest();
      }
    });
  }

  public void testUniqueContent() {
    delayTestFinish(RUNASYNC_TIMEOUT);
    GWT.runAsync(new RunAsyncCallback() {
      @Override
      public void onFailure(Throwable caught) {
        reportUncaughtException(caught);
      }

      @Override
      public void onSuccess() {
        String uniqueContent = "Fragment From Regular RunAsync";
        assertFalse(LoggingXhrLoadingStrategy.getLeftOverFragmentText().contains(uniqueContent));
        finishTest();
      }
    });
  }

  public void testUniqueContentWithClassLiteral() {
    delayTestFinish(RUNASYNC_TIMEOUT);
    GWT.runAsync(RunAsyncContentTest.class, new RunAsyncCallback() {
      @Override
      public void onFailure(Throwable caught) {
        reportUncaughtException(caught);
      }

      @Override
      public void onSuccess() {
        String uniqueContent = "Fragment From RunAsync With Class Literal";
        assertFalse(LoggingXhrLoadingStrategy.getLeftOverFragmentText().contains(uniqueContent));
        finishTest();
      }
    });
  }
}
