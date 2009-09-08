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

/**
 * This test tests that specifying an initial sequence of runAsyncs works. It
 * has three callbacks which each have a runAsync in them. It then calls the
 * callbacks in a different order than predicted.
 */
public class InitialLoadSequenceTest extends GWTTestCase {
  private static final int TIMEOUT = 10000;

  /**
   * This class is used to mark the second runAsync call.
   */
  public static class Callback2Marker {
  }

  /**
   * The number of callbacks outstanding. When this gets to zero, the test
   * finishes.
   */
  private int callbacksOutstanding;

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.InitialLoadSequence";
  }

  public void testBackwards() {
    callbacksOutstanding = 3;
    delayTestFinish(TIMEOUT);
    callback3();
    callback2();
    callback1();
  }

  private void callback1() {
    GWT.runAsync(new RunAsyncCallback() {
      public void onFailure(Throwable reason) {
        fail(reason.toString());
      }

      public void onSuccess() {
        countCallback();
      }
    });
  }

  private void callback2() {
    GWT.runAsync(Callback2Marker.class, new RunAsyncCallback() {
      public void onFailure(Throwable reason) {
        fail(reason.toString());
      }

      public void onSuccess() {
        countCallback();
      }
    });
  }

  private void callback3() {
    GWT.runAsync(new RunAsyncCallback() {
      public void onFailure(Throwable reason) {
        fail(reason.toString());
      }

      public void onSuccess() {
        countCallback();
      }
    });
  }

  private void countCallback() {
    callbacksOutstanding--;
    if (callbacksOutstanding == 0) {
      finishTest();
    }
  }
}
