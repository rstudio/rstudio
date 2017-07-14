/*
 * Copyright 2017 Google Inc.
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
package com.google.gwt.core.client;

import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Timer;

/** Test for window.onerror reporting to {@link UncaughtExceptionHandler}. */
public class WindowOnErrorTest extends GWTTestCase {

  private int reportedJsExceptionCount;

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Core";
  }

  @Override
  protected void reportUncaughtException(Throwable ex) {
    // We need to distinguish here between the exceptions that we are deliberately creating
    // and ones that should make the test fail.
    // All our exceptions will contain "from_js" in their message
    if (ex.getMessage().contains("from_js")) {
      // Do not let the test fail
      reportedJsExceptionCount++;
      return;
    }
    super.reportUncaughtException(ex);
  }

  // Does not work in dev mode, since JNSI code for setting up window.onerror needs Throwable.of
  // from super sourced code.
  @DoNotRunWith({Platform.Devel})
  public void testFailViaWindowOnError() {
    delayTestFinish(2000);

    new Timer() {
      @Override
      public void run() {
        assertEquals(1, reportedJsExceptionCount);
        finishTest();
      }
    }.schedule(1000);

    throwInNonEntryMethod();
  }

  private native void throwInNonEntryMethod() /*-{
    $wnd.setTimeout(function() {
      throw new Error("from_js");
    }, 0);
  }-*/;
}
