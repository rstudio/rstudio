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
package com.google.gwt.examples;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Timer;

public class AsyncJUnitExample extends GWTTestCase {

  @Override
  public String getModuleName() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * Tests the Timer class asynchronously.
   */
  public void testTimer() {

    // Set a delay period significantly longer than the
    // event is expected to take.
    delayTestFinish(500);

    // Setup an asynchronous event handler.
    Timer timer = new Timer() {
      @Override
      public void run() {
        // do some validation logic

        // tell the test system the test is now done
        finishTest();
      }
    };

    // Schedule the event and return control to the test system.
    timer.schedule(100);
  }

}
