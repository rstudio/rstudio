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
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Window;

/**
 * Tests runAsync in various ways.
 */
public class RunAsyncTest extends GWTTestCase {
  private static final int RUNASYNC_TIMEOUT = 10000;

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  private static final String HELLO = "hello";

  private static String staticWrittenInBaseButReadLater;

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

}
