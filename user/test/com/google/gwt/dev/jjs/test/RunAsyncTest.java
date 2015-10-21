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
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.junit.client.GWTTestCase;

import javaemul.internal.annotations.DoNotInline;

/**
 * Tests runAsync in various ways.
 */
public class RunAsyncTest extends GWTTestCase {
  private static final String HELLO = "hello";

  private static final String LONG_INTERNED_STRING = "abcdefghijklmnopqrstuvwxyz";

  private static final int RUNASYNC_TIMEOUT = 10000;

  private static String staticWrittenInBaseButReadLater;

  private static int staticWrittenByAsync;

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  @DoNotInline
  public String getTestString() {
    return LONG_INTERNED_STRING;
  }

  public void testBasic() {
    delayTestFinish(RUNASYNC_TIMEOUT);

    GWT.runAsync(new RunAsyncCallback() {
      @Override
      public void onFailure(Throwable caught) {
        throw new RuntimeException(caught);
      }

      @Override
      public void onSuccess() {
        finishTest();
      }
    });
  }

  /**
   * Only tests the XSLinker/CrossSiteRunAsyncSuite.
   * A string which is only referenced in > 0 fragment which gets interned needs to be
   * processed by HandleCrossFragmentReferences, but JsLiteralInterner was running after
   * HandleCrossFragmentReferences.
   */
  public void testHandleCrossFragmentReference() {
    delayTestFinish(RUNASYNC_TIMEOUT);

    GWT.runAsync(new RunAsyncCallback() {
      @Override
      public void onFailure(Throwable caught) {
        throw new RuntimeException(caught);
      }

      @Override
      public void onSuccess() {
        assertEquals(LONG_INTERNED_STRING, getTestString());
        GWT.runAsync(new RunAsyncCallback() {
          @Override
          public void onFailure(Throwable caught) {
            throw new RuntimeException(caught);
          }

          @Override
          public void onSuccess() {
            assertEquals(LONG_INTERNED_STRING, getTestString());
            finishTest();
          }
        });
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
      @Override
      public void onFailure(Throwable caught) {
        throw new RuntimeException(caught);
      }

      @Override
      public void onSuccess() {
        // This read happens later
        assertEquals(HELLO, staticWrittenInBaseButReadLater);
        finishTest();
      }
    });
  }

  /**
   * Test runAsync always runs async.
   */
  public void testAsyncIsAlwaysAsync() {
    delayTestFinish(RUNASYNC_TIMEOUT);
    staticWrittenByAsync = 0;

    assertRunAsyncIsAsync();

    // Give it little bit more time to loaded and try runAsync again
    Scheduler.get().scheduleFixedPeriod(new RepeatingCommand() {
      @Override public boolean execute() {
        if (staticWrittenByAsync == 0) {
          return true;
        }

        // Code is loaded, let's assert it still runs async
        assertRunAsyncIsAsync();

        finishTest();
        return false;
      }
    }, 100);
  }

  private void assertRunAsyncIsAsync() {
    final int lastValue = staticWrittenByAsync;
    GWT.runAsync(RunAsyncTest.class, new RunAsyncCallback() {
      @Override
      public void onFailure(Throwable caught) {
        throw new RuntimeException(caught);
      }
      @Override
      public void onSuccess() {
        staticWrittenByAsync++;
      }
    });
    assertEquals(lastValue, staticWrittenByAsync);
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

          @Override
          public void onFailure(Throwable caught) {
            throw new RuntimeException(caught);
          }

          @Override
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

  @Override
  protected void reportUncaughtException(Throwable ex) {
    if (!(ex.getMessage().equals("_expected_"))) {
      super.reportUncaughtException(ex);
    }
  }

  public void testExceptionsThrownFromOnSuccessReported() {
    // Create an exception that will be thrown from an onSuccess method
    final RuntimeException toThrow = new RuntimeException("_expected_");

    // set a handler that looks for toThrow
    GWT.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
      @Override
      public void onUncaughtException(Throwable e) {
        if (e == toThrow) {
          // expected
          finishTest();
        }
      }
    });
    delayTestFinish(RUNASYNC_TIMEOUT);

    GWT.runAsync(new RunAsyncCallback() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess() {
        throw toThrow;
      }
    });
  }
}
