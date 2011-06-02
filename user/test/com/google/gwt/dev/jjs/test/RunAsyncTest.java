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
package com.google.gwt.dev.jjs.test;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests runAsync in various ways.
 */
public class RunAsyncTest extends GWTTestCase {
  private static final String HELLO = "hello";

  private static final int RUNASYNC_TIMEOUT = 10000;

  private static String staticWrittenInBaseButReadLater;

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testBasic() {
    delayTestFinish(RUNASYNC_TIMEOUT);

    GWT.runAsync(new RunAsyncCallback() {
      public void onFailure(Throwable caught) {
        throw new RuntimeException(caught);
      }

      public void onSuccess() {
        finishTest();
      }
    });
  }

  /**
   * Unlike with pruning, writing to a field should rescue it for code-splitting
   * purposes.
   */
  public void testFieldWrittenButNotRead() {
    delayTestFinish(RUNASYNC_TIMEOUT);

    // This write happens in the base fragment
    staticWrittenInBaseButReadLater = HELLO;

    GWT.runAsync(new RunAsyncCallback() {
      public void onFailure(Throwable caught) {
        throw new RuntimeException(caught);
      }

      public void onSuccess() {
        // This read happens later
        assertEquals(HELLO, staticWrittenInBaseButReadLater);
        finishTest();
      }
    });
  }

  /**
   * Test that callbacks are called in the order they are posted.
   */
  public void testOrder() {
    class Util {
      int callNumber = 0;
      int seen = 0;

      void scheduleCallback() {
        final int thisCallNumber = callNumber++;

        GWT.runAsync(new RunAsyncCallback() {

          public void onFailure(Throwable caught) {
            throw new RuntimeException(caught);
          }

          public void onSuccess() {
            assertEquals(seen, thisCallNumber);
            seen++;
            if (seen == 3) {
              finishTest();
            }
          }
        });
      }
    }
    Util util = new Util();

    delayTestFinish(RUNASYNC_TIMEOUT);

    util.scheduleCallback();
    util.scheduleCallback();
    util.scheduleCallback();
  }

  public void testUnhandledExceptions() {
    // Create an exception that will be thrown from an onSuccess method
    final RuntimeException toThrow =
        new RuntimeException("Should be caught by the uncaught exception handler");

    // save the original handler
    final UncaughtExceptionHandler originalHandler = GWT.getUncaughtExceptionHandler();

    // set a handler that looks for toThrow
    GWT.UncaughtExceptionHandler myHandler = new GWT.UncaughtExceptionHandler() {
      public void onUncaughtException(Throwable e) {
        GWT.setUncaughtExceptionHandler(originalHandler);
        if (e == toThrow) {
          // expected
          finishTest();
        } else {
          // some other exception; pass it on
          throw new RuntimeException(e);
        }
      }
    };
    GWT.setUncaughtExceptionHandler(myHandler);
    delayTestFinish(RUNASYNC_TIMEOUT);

    try {
      GWT.runAsync(new RunAsyncCallback() {
        public void onFailure(Throwable caught) {
        }

        public void onSuccess() {
          throw toThrow;
        }
      });
    } catch (Throwable e) {
      // runAsync can either throw immediately, or throw uncaught.
      myHandler.onUncaughtException(e);
    }
  }
}
