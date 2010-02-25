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
package com.google.gwt.core.client;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * This is a black-box test of the Scheduler API.
 */
public class SchedulerTest extends GWTTestCase {

  private static final int TEST_DELAY = 500000;

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Core";
  }

  /**
   * Tests that an entry command can schedule a finally command where the whole
   * thing is kicked off by a deferred command.
   */
  public void testEndToEnd() {
    final boolean[] ranEntry = {false};

    final ScheduledCommand finallyCommand = new ScheduledCommand() {
      public void execute() {
        assertTrue(ranEntry[0]);
        finishTest();
      }
    };

    Scheduler.get().scheduleEntry(new ScheduledCommand() {
      public void execute() {
        ranEntry[0] = true;
        Scheduler.get().scheduleFinally(finallyCommand);
      }
    });

    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      public void execute() {
        assertTrue(ranEntry[0]);
      }
    });

    delayTestFinish(TEST_DELAY);
  }
}
