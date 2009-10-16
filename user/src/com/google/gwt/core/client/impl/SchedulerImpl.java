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

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;

/**
 * This is used by Scheduler to collaborate with Impl in order to have
 * FinallyCommands executed.
 */
public class SchedulerImpl extends Scheduler {

  /**
   * Metadata bag for command objects. It's a JSO so that a lightweight JsArray
   * can be used instead of a Collections type.
   */
  static final class Task extends JavaScriptObject {
    public static native Task create(RepeatingCommand cmd) /*-{
      return [cmd, true];
    }-*/;

    public static native Task create(ScheduledCommand cmd) /*-{
      return [cmd, false];
    }-*/;

    protected Task() {
    }

    public boolean executeRepeating() {
      return getRepeating().execute();
    }

    public void executeScheduled() {
      getScheduled().execute();
    }

    /**
     * Has implicit cast.
     */
    private native RepeatingCommand getRepeating() /*-{
      return this[0];
    }-*/;

    /**
     * Has implicit cast.
     */
    private native ScheduledCommand getScheduled() /*-{
      return this[0];
    }-*/;

    private native boolean isRepeating() /*-{
      return this[1];
    }-*/;
  }

  /**
   * A RepeatingCommand that calls flushPostEventPumpCommands(). It repeats if
   * there are any outstanding deferred or incremental commands.
   */
  static final RepeatingCommand FLUSHER = new RepeatingCommand() {
    public boolean execute() {
      flushRunning = true;
      flushPostEventPumpCommands();
      /*
       * No finally here, we want this to be clear only on a normal exit. An
       * abnormal exit would indicate that an exception isn't being caught
       * correctly or that a slow script warning canceled the timer.
       */
      flushRunning = false;
      return shouldBeRunning = isWorkQueued();
    }
  };

  /**
   * This provides some backup for the main flusher task in case it gets shut
   * down by a slow-script warning.
   */
  static final RepeatingCommand RESCUE = new RepeatingCommand() {
    public boolean execute() {
      if (flushRunning) {
        /*
         * Since JS is single-threaded, if we're here, then than means that
         * FLUSHER.execute() started, but did not finish. Reschedule FLUSHER.
         */
        scheduleFixedDelayImpl(FLUSHER, FLUSHER_DELAY);
      }
      return shouldBeRunning;
    }
  };

  /**
   * Indicates the location of a previously-live command that has been removed
   * from the queue.
   */
  static final Task TOMBSTONE = JavaScriptObject.createObject().cast();

  /*
   * Work queues. Timers store their state on the function, so we don't need to
   * track them.
   */
  static final JsArray<Task> DEFERRED_COMMANDS = JavaScriptObject.createArray().cast();

  static final JsArray<Task> INCREMENTAL_COMMANDS = JavaScriptObject.createArray().cast();

  static final JsArray<Task> FINALLY_COMMANDS = JavaScriptObject.createArray().cast();

  /*
   * These two flags are used to control the state of the flusher and rescuer
   * commands.
   */
  private static boolean shouldBeRunning = false;

  private static boolean flushRunning = false;

  /**
   * The delay between flushing the task queues.
   */
  private static final int FLUSHER_DELAY = 1;
  /**
   * The delay between checking up on SSW problems.
   */
  private static final int RESCUE_DELAY = 50;
  /**
   * The amount of time that we're willing to spend executing
   * IncrementalCommands.
   */
  private static final double TIME_SLICE = 100;

  public static void scheduleDeferredImpl(ScheduledCommand cmd) {
    DEFERRED_COMMANDS.push(Task.create(cmd));
    maybeSchedulePostEventPumpCommands();
  }

  public static void scheduleFinallyImpl(ScheduledCommand cmd) {
    FINALLY_COMMANDS.push(Task.create(cmd));
  }

  public static native void scheduleFixedDelayImpl(RepeatingCommand cmd,
      int delayMs) /*-{
    $wnd.setTimeout(function() {
      // $entry takes care of uncaught exception handling
      var ret = $entry(@com.google.gwt.core.client.impl.SchedulerImpl::execute(Lcom/google/gwt/core/client/Scheduler$RepeatingCommand;))(cmd);
      if (ret) {
        $wnd.setTimeout(arguments.callee, delayMs);
      }
    }, delayMs);
  }-*/;

  public static native void scheduleFixedPeriodImpl(RepeatingCommand cmd,
      int delayMs) /*-{
    var fn = function() {
      // $entry takes care of uncaught exception handling
      var ret = $entry(@com.google.gwt.core.client.impl.SchedulerImpl::execute(Lcom/google/gwt/core/client/Scheduler$RepeatingCommand;))(cmd);
      if (!ret) {
        // Either canceled or threw an exception
        $wnd.clearInterval(arguments.callee.token);
      }
    };
    fn.token = $wnd.setInterval(fn, delayMs);
  }-*/;

  public static void scheduleIncrementalImpl(RepeatingCommand cmd) {
    // Push repeating commands onto the same initial queue for relative order
    DEFERRED_COMMANDS.push(Task.create(cmd));
    maybeSchedulePostEventPumpCommands();
  }

  /**
   * Called by {@link Impl#entry(JavaScriptObject)}.
   */
  static void flushFinallyCommands() {
    runScheduledTasks(FINALLY_COMMANDS, FINALLY_COMMANDS);
  }

  /**
   * Called by Flusher.
   */
  static void flushPostEventPumpCommands() {
    runScheduledTasks(DEFERRED_COMMANDS, INCREMENTAL_COMMANDS);
    runRepeatingTasks(INCREMENTAL_COMMANDS);
  }

  static boolean isWorkQueued() {
    return DEFERRED_COMMANDS.length() > 0 || INCREMENTAL_COMMANDS.length() > 0;
  }

  /**
   * Called from scheduledFixedInterval to give $entry a static function.
   */
  @SuppressWarnings("unused")
  private static boolean execute(RepeatingCommand cmd) {
    return cmd.execute();
  }

  private static void maybeSchedulePostEventPumpCommands() {
    if (!shouldBeRunning) {
      shouldBeRunning = true;
      scheduleFixedDelayImpl(FLUSHER, FLUSHER_DELAY);
      scheduleFixedDelayImpl(RESCUE, RESCUE_DELAY);
    }
  }

  /**
   * Execute a list of Tasks that hold RepeatingCommands.
   */
  private static void runRepeatingTasks(JsArray<Task> tasks) {
    boolean canceledSomeTasks = false;
    int length = tasks.length();
    double start = Duration.currentTimeMillis();

    while (Duration.currentTimeMillis() - start < TIME_SLICE) {
      for (int i = 0; i < length; i++) {
        Task t = tasks.get(i);
        if (t == TOMBSTONE) {
          continue;
        }

        assert t.isRepeating() : "Found a non-repeating Task";

        if (!t.executeRepeating()) {
          tasks.set(i, TOMBSTONE);
          canceledSomeTasks = true;
        }
      }
    }

    if (canceledSomeTasks) {
      // Remove tombstones
      int last = 0;
      for (int i = 0; i < length; i++) {
        if (tasks.get(i) == TOMBSTONE) {
          continue;
        }
        tasks.set(last++, tasks.get(i));
      }
      tasks.setLength(last + 1);
    }
  }

  /**
   * Execute a list of Tasks that hold both ScheduledCommands and
   * RepeatingCommands. Any RepeatingCommands in the <code>tasks</code> queue
   * that want to repeat will be pushed onto the <code>rescheduled</code> queue.
   */
  private static void runScheduledTasks(JsArray<Task> tasks,
      JsArray<Task> rescheduled) {
    // Use the while-shift pattern in case additional commands are enqueued
    while (tasks.length() > 0) {
      Task t = tasks.shift();

      try {
        // Move repeating commands to incremental commands queue
        if (t.isRepeating()) {
          if (t.executeRepeating()) {
            rescheduled.push(t);
          }
        } else {
          t.executeScheduled();
        }
      } catch (RuntimeException e) {
        if (GWT.getUncaughtExceptionHandler() != null) {
          GWT.getUncaughtExceptionHandler().onUncaughtException(e);
        }
      }
    }
  }

  @Override
  public void scheduleDeferred(ScheduledCommand cmd) {
    scheduleDeferredImpl(cmd);
  }

  @Override
  public void scheduleFinally(ScheduledCommand cmd) {
    scheduleFinallyImpl(cmd);
  }

  @Override
  public void scheduleFixedDelay(RepeatingCommand cmd, int delayMs) {
    scheduleFixedDelayImpl(cmd, delayMs);
  }

  @Override
  public void scheduleFixedPeriod(RepeatingCommand cmd, int delayMs) {
    scheduleFixedPeriodImpl(cmd, delayMs);
  }

  @Override
  public void scheduleIncremental(RepeatingCommand cmd) {
    scheduleIncrementalImpl(cmd);
  }
}
