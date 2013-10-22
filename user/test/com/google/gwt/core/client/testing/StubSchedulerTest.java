/*
 * Copyright 2013 Google Inc.
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

package com.google.gwt.core.client.testing;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Tests for {@link StubScheduler}.
 */
public class StubSchedulerTest extends TestCase {

  private StubScheduler scheduler;
  private List<String> events;
  private List<String> thrownExceptionMessages;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    scheduler = new StubScheduler();
    events = new ArrayList<String>();
    thrownExceptionMessages = new ArrayList<String>();
    GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
      @Override public void onUncaughtException(Throwable throwable) {
        if (throwable.getMessage().contains("Fake failure")) {
          thrownExceptionMessages.add(throwable.getMessage());
        } else {
          fail(throwable.getMessage());
        }
      }
    });
  }

  @Override
  protected void tearDown() throws Exception {
    GWT.setUncaughtExceptionHandler(null);
    checkEvents();
    checkThrownExceptionMessages();
    super.tearDown();
  }

  public void testMixedCommands() {
    scheduler.scheduleEntry(new FakeRepeatingCommand("repeating", 2, "repeating child"));
    scheduler.scheduleDeferred(new FakeScheduledCommand("scheduled", "scheduled child"));
    checkEvents();

    assertTrue(scheduler.executeCommands());
    checkEvents("repeating", "scheduled");

    assertTrue(scheduler.executeCommands());
    checkEvents("repeating child", "repeating", "scheduled child");

    assertTrue(scheduler.executeCommands());
    checkEvents("repeating child", "repeating child");

    assertFalse(scheduler.executeCommands());
    checkEvents("repeating child");

    checkCommands(scheduler.getRepeatingCommands());
    checkCommands(scheduler.getScheduledCommands());
  }

  public void testExceptions() {
    scheduler.scheduleEntry(new FakeRepeatingCommand("repeating ok", 1, null));
    scheduler.scheduleEntry(new FakeRepeatingCommand("repeating failing", 2, null));
    scheduler.scheduleDeferred(new FakeScheduledCommand("scheduled failing", null));
    scheduler.scheduleDeferred(new FakeScheduledCommand("scheduled ok", null));

    assertFalse(scheduler.executeCommands());

    checkEvents("repeating ok", "repeating failing", "scheduled failing", "scheduled ok");
    checkThrownExceptionMessages(
        "Fake failure: repeating failing",
        "Fake failure: scheduled failing");
  }

  public void testRepeatingCommands() {
    scheduler.scheduleEntry(new FakeRepeatingCommand("entry1", 1, null));
    scheduler.scheduleFinally(new FakeRepeatingCommand("finally1", 2, null));
    scheduler.scheduleFixedDelay(new FakeRepeatingCommand("delay1", 1, null), 42);
    scheduler.scheduleFixedPeriod(new FakeRepeatingCommand("period1", 2, "period1 child"), 42);
    scheduler.scheduleIncremental(new FakeRepeatingCommand("incremental1", 1, null));

    scheduler.scheduleEntry(new FakeRepeatingCommand("entry2", 2, null));
    scheduler.scheduleFinally(new FakeRepeatingCommand("finally2", 1, "finally2 child"));
    scheduler.scheduleFixedDelay(new FakeRepeatingCommand("delay2", 2, null), 42);
    scheduler.scheduleFixedPeriod(new FakeRepeatingCommand("period2", 1, null), 42);
    scheduler.scheduleIncremental(new FakeRepeatingCommand("incremental2", 2, null));

    scheduler.scheduleDeferred(new FakeScheduledCommand("scheduled", null)); // ignored
    checkEvents();

    checkCommands(scheduler.getRepeatingCommands(),
        "entry1", "finally1", "delay1", "period1", "incremental1",
        "entry2", "finally2", "delay2", "period2", "incremental2");
    assertTrue(scheduler.executeRepeatingCommands());
    checkEvents(
        "entry1", "finally1", "delay1", "period1", "incremental1",
        "entry2", "finally2", "delay2", "period2", "incremental2");

    checkCommands(scheduler.getRepeatingCommands(),
        "finally1", "period1 child", "period1",
        "entry2", "finally2 child", "delay2", "incremental2");
    assertTrue(scheduler.executeRepeatingCommands());
    checkEvents(
        "finally1", "period1 child", "period1",
        "entry2", "finally2 child", "delay2", "incremental2");

    checkCommands(scheduler.getRepeatingCommands(),
        "period1 child", "period1 child");
    assertTrue(scheduler.executeRepeatingCommands());
    checkEvents("period1 child", "period1 child");

    checkCommands(scheduler.getRepeatingCommands(),
        "period1 child");
    assertFalse(scheduler.executeRepeatingCommands());
    checkEvents("period1 child");

    checkCommands(scheduler.getRepeatingCommands());
  }

  public void testScheduledCommands() {
    scheduler.scheduleDeferred(new FakeScheduledCommand("deferred1", "deferred1 child"));
    scheduler.scheduleEntry(new FakeScheduledCommand("entry1", null));
    scheduler.scheduleFinally(new FakeScheduledCommand("finally1", null));

    scheduler.scheduleFinally(new FakeScheduledCommand("finally2", null));
    scheduler.scheduleEntry(new FakeScheduledCommand("entry2", "entry2 child"));
    scheduler.scheduleDeferred(new FakeScheduledCommand("deferred2", null));

    scheduler.scheduleEntry(new FakeRepeatingCommand("repeating", 1, null));  // ignored
    checkEvents();

    checkCommands(scheduler.getScheduledCommands(),
        "deferred1", "entry1", "finally1", "finally2", "entry2", "deferred2");
    assertTrue(scheduler.executeScheduledCommands());
    checkEvents("deferred1", "entry1", "finally1", "finally2", "entry2", "deferred2");

    checkCommands(scheduler.getScheduledCommands(), "deferred1 child", "entry2 child");
    assertFalse(scheduler.executeScheduledCommands());
    checkEvents("deferred1 child", "entry2 child");

    checkCommands(scheduler.getScheduledCommands());
  }

  private void checkEvents(String... expected) {
    assertEquals(Arrays.asList(expected), events);
    events.clear();
  }

  private void checkThrownExceptionMessages(String... expected) {
    assertEquals(Arrays.asList(expected), thrownExceptionMessages);
    thrownExceptionMessages.clear();
  }

  private void checkCommands(List<?> actual, String... expected) {
    List<String> actualStrings = new ArrayList<String>();
    for (Object command : actual) {
      actualStrings.add(command.toString());
    }
    assertEquals(Arrays.asList(expected), actualStrings);
  }

  private abstract class FakeCommand {

    protected final String id;
    @Nullable protected final String scheduledEntryId;
    protected int executionCount;

    public FakeCommand(String id, @Nullable String scheduledEntryId) {
      this.id = id;
      this.scheduledEntryId = scheduledEntryId;
    }

    protected void prepareExecute() {
      executionCount++;
      events.add(id);

      if (id.contains("failing")) {
        throw new RuntimeException("Fake failure: " + id);
      }
    }
  }

  private class FakeRepeatingCommand extends FakeCommand implements RepeatingCommand {

    private final int repeatCount;

    public FakeRepeatingCommand(String id, int repeatCount, @Nullable String scheduledEntryId) {
      super(id, scheduledEntryId);
      this.repeatCount = repeatCount;
    }

    @Override
    public boolean execute() {
      prepareExecute();
      assertTrue(executionCount <= repeatCount);

      if (scheduledEntryId != null) {
        scheduler.scheduleEntry(new FakeRepeatingCommand(scheduledEntryId, repeatCount, null));
      }

      return executionCount < repeatCount;
    }

    @Override
    public String toString() {
      return id;
    }
  }

  private class FakeScheduledCommand extends FakeCommand implements ScheduledCommand {

    public FakeScheduledCommand(String id, @Nullable String scheduledEntryId) {
      super(id, scheduledEntryId);
    }

    @Override
    public void execute() {
      prepareExecute();
      assertEquals(1, executionCount);

      if (scheduledEntryId != null) {
        scheduler.scheduleEntry(new FakeScheduledCommand(scheduledEntryId, null));
      }
    }

    @Override
    public String toString() {
      return id;
    }
  }
}
