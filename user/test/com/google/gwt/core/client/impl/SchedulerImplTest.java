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

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.core.client.impl.SchedulerImpl.Task;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * This is a white-box test of the Scheduler API implementation.
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

  /**
   * The EntryCommand and FinallyCommand queues should have the same behavior,
   * so we use this interface to reuse the same test logic.
   */
  interface QueueTester {
    void flush();

    JsArray<Task> queue();

    void schedule(RepeatingCommand cmd);

    void schedule(ScheduledCommand cmd);
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
        assertNull(impl.deferredCommands);
        finishTest();
      }
    });

    delayTestFinish(TEST_DELAY);
  }

  public void testEntryCommands() {
    final SchedulerImpl impl = new SchedulerImpl();

    testQueue(new QueueTester() {
      public void flush() {
        impl.flushEntryCommands();
      }

      public JsArray<Task> queue() {
        return impl.entryCommands;
      }

      public void schedule(RepeatingCommand cmd) {
        impl.scheduleEntry(cmd);
      }

      public void schedule(ScheduledCommand cmd) {
        impl.scheduleEntry(cmd);
      }
    });
  }

  public void testFinallyCommands() {
    final SchedulerImpl impl = new SchedulerImpl();

    testQueue(new QueueTester() {
      public void flush() {
        impl.flushFinallyCommands();
      }

      public JsArray<Task> queue() {
        return impl.finallyCommands;
      }

      public void schedule(RepeatingCommand cmd) {
        impl.scheduleFinally(cmd);
      }

      public void schedule(ScheduledCommand cmd) {
        impl.scheduleFinally(cmd);
      }
    });
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
        assertNull(impl.deferredCommands);
        assertTrue(String.valueOf(values[0]), values[0] <= values[1]);

        if (values[0] == values[1]) {
          // Haven't yet cleared the queue, still in flushPostEventPumpCommands
          assertNotNull(impl.incrementalCommands);
          assertEquals(0, impl.incrementalCommands.length());
          finishTest();
        } else {
          assertNotNull(impl.incrementalCommands);
          assertEquals(1, impl.incrementalCommands.length());
          assertSame(counter, impl.incrementalCommands.get(0).getRepeating());
          impl.scheduleDeferred(this);
        }
      }
    });

    assertEquals(2, impl.deferredCommands.length());

    delayTestFinish(TEST_DELAY);
  }

  private void testQueue(final QueueTester impl) {
    boolean[] oneShotValues = {false};
    final boolean[] chainedValues = {false};
    int[] counterValues = {0, 2};

    impl.schedule(new ArraySetterCommand(oneShotValues));
    impl.schedule(new CountingCommand(counterValues));
    impl.schedule(new ScheduledCommand() {
      public void execute() {
        // Schedule another entry
        impl.schedule(new ArraySetterCommand(chainedValues));
      }
    });

    assertEquals(3, impl.queue().length());

    ScheduledCommand nullCommand = new NullCommand();
    impl.schedule(nullCommand);
    assertEquals(4, impl.queue().length());
    assertSame(nullCommand, impl.queue().get(3).getScheduled());

    impl.flush();

    // Ensure the command-schedules-command case has been executed
    assertTrue(chainedValues[0]);

    // Test that the RepeatingCommand is still scheduled
    assertEquals(1, counterValues[0]);
    assertEquals(1, impl.queue().length());
    impl.flush();

    // Everything should be finished now
    assertEquals(2, counterValues[0]);
    assertTrue(oneShotValues[0]);
    assertNull(impl.queue());
  }
}
