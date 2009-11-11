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
package com.google.gwt.core.client.impl;

import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests that poke at the internal state of SchedulerImpl.
 */
public class SchedulerImplTest extends GWTTestCase {

  static class ArraySetterCommand implements ScheduledCommand {
    private final boolean[] values;

    public ArraySetterCommand(boolean[] values) {
      this.values = values;
    }

    public void execute() {
      values[0] = true;
    }
  }

  static class CountingCommand implements RepeatingCommand {
    public final int[] values;

    public CountingCommand(int[] values) {
      assertEquals(2, values.length);
      this.values = values;
    }

    public boolean execute() {
      assertTrue("Called too many times", values[0] < values[1]);
      values[0] = values[0] + 1;
      return values[0] < values[1];
    }
  }

  /**
   * A no-op command used to test internal datastructures.
   */
  static class NullCommand implements ScheduledCommand {
    public void execute() {
    }
  }

  private static final int TEST_DELAY = 5000;

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Core";
  }

  public void testDeferredCommands() {
    final SchedulerImpl impl = new SchedulerImpl();

    final boolean[] values = {false};
    impl.scheduleDeferred(new ArraySetterCommand(values));

    assertEquals(1, impl.deferredCommands.length());

    ScheduledCommand nullCommand = new NullCommand();
    impl.scheduleDeferred(nullCommand);
    assertEquals(2, impl.deferredCommands.length());
    assertSame(nullCommand, impl.deferredCommands.get(1).getScheduled());

    impl.scheduleDeferred(new ScheduledCommand() {
      public void execute() {
        assertTrue(values[0]);
        assertEquals(0, impl.deferredCommands.length());
        finishTest();
      }
    });

    delayTestFinish(TEST_DELAY);
  }

  public void testFinallyCommands() {
    SchedulerImpl impl = new SchedulerImpl();

    final boolean[] values = {false};
    impl.scheduleFinally(new ArraySetterCommand(values));

    assertEquals(1, impl.finallyCommands.length());

    ScheduledCommand nullCommand = new NullCommand();
    impl.scheduleFinally(nullCommand);
    assertEquals(2, impl.finallyCommands.length());
    assertSame(nullCommand, impl.finallyCommands.get(1).getScheduled());

    impl.flushFinallyCommands();

    assertTrue(values[0]);
    assertEquals(0, impl.finallyCommands.length());
  }

  public void testFixedDelayCommands() {
    final SchedulerImpl impl = new SchedulerImpl();
    final int[] values = {0, 4};

    impl.scheduleFixedDelay(new CountingCommand(values), 20);
    // Scheduler doesn't need to maintain state for this kind of command
    assertFalse(impl.isWorkQueued());

    // Busy wait for the counter
    impl.scheduleDeferred(new ScheduledCommand() {
      public void execute() {
        if (values[0] == values[1]) {
          finishTest();
        } else {
          impl.scheduleDeferred(this);
        }
      }
    });

    delayTestFinish(TEST_DELAY);
  }

  public void testFixedPeriodCommands() {
    final SchedulerImpl impl = new SchedulerImpl();
    final int[] values = {0, 4};

    impl.scheduleFixedPeriod(new CountingCommand(values), 20);
    // Scheduler doesn't need to maintain state for this kind of command
    assertFalse(impl.isWorkQueued());

    // Busy wait for the counter
    impl.scheduleDeferred(new ScheduledCommand() {
      public void execute() {
        if (values[0] == values[1]) {
          finishTest();
        } else {
          impl.scheduleDeferred(this);
        }
      }
    });

    delayTestFinish(TEST_DELAY);
  }

  public void testIncrementalCommands() {
    final SchedulerImpl impl = new SchedulerImpl();

    final int[] values = {0, 4};
    final CountingCommand counter = new CountingCommand(values);
    impl.scheduleIncremental(counter);

    // The first pass is scheduled as a deferred command
    assertEquals(1, impl.deferredCommands.length());

    impl.scheduleDeferred(new ScheduledCommand() {
      public void execute() {
        // After the incremental command has fired, it's moved to a new queue
        assertEquals(0, impl.deferredCommands.length());
        assertTrue(String.valueOf(values[0]), values[0] <= values[1]);

        if (values[0] == values[1]) {
          assertEquals(0, impl.incrementalCommands.length());
          finishTest();
        } else {
          assertEquals(1, impl.incrementalCommands.length());
          assertSame(counter, impl.incrementalCommands.get(0).getRepeating());
          impl.scheduleDeferred(this);
        }
      }
    });

    assertEquals(2, impl.deferredCommands.length());

    delayTestFinish(TEST_DELAY);
  }
}
