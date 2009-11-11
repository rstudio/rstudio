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
    public native RepeatingCommand getRepeating() /*-{
      return this[0];
    }-*/;

    /**
     * Has implicit cast.
     */
    public native ScheduledCommand getScheduled() /*-{
      return this[0];
    }-*/;

    public native boolean isRepeating() /*-{
      return this[1];
    }-*/;
  }

  /**
   * Use a GWT.create() here to make it simple to hijack the default
   * implementation.
   */
  public static final SchedulerImpl INSTANCE = GWT.create(SchedulerImpl.class);

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

  /**
   * Called from scheduledFixedInterval to give $entry a static function.
   */
  @SuppressWarnings("unused")
  private static boolean execute(RepeatingCommand cmd) {
    return cmd.execute();
  }

  /**
   * Execute a list of Tasks that hold RepeatingCommands.
   * 
   * @return A replacement array that is possibly a shorter copy of
   *         <code>tasks</code>
   */
  private static JsArray<Task> runRepeatingTasks(JsArray<Task> tasks) {
    boolean canceledSomeTasks = false;
    int length = tasks.length();
    double start = Duration.currentTimeMillis();

    while (Duration.currentTimeMillis() - start < TIME_SLICE) {
      for (int i = 0; i < length; i++) {
        assert tasks.length() == length : "Working array length changed "
            + tasks.length() + " != " + length;
        Task t = tasks.get(i);
        if (t == null) {
          continue;
        }

        assert t.isRepeating() : "Found a non-repeating Task";

        if (!t.executeRepeating()) {
          tasks.set(i, null);
          canceledSomeTasks = true;
        }
      }
    }

    if (canceledSomeTasks) {
      JsArray<Task> newTasks = JavaScriptObject.createArray().cast();
      // Remove tombstones
      for (int i = 0; i < length; i++) {
        if (tasks.get(i) == null) {
          continue;
        }
        newTasks.push(tasks.get(i));
      }
      assert newTasks.length() < length;
      return newTasks;
    } else {
      return tasks;
    }
  }

  /**
   * Execute a list of Tasks that hold both ScheduledCommands and
   * RepeatingCommands. Any RepeatingCommands in the <code>tasks</code> queue
   * that want to repeat will be pushed onto the <code>rescheduled</code> queue.
   * The contents of <code>tasks</code> may not be altered while this method is
   * executing.
   */
  private static void runScheduledTasks(JsArray<Task> tasks,
      JsArray<Task> rescheduled) {
    for (int i = 0, j = tasks.length(); i < j; i++) {
      assert tasks.length() == j : "Working array length changed "
          + tasks.length() + " != " + j;
      Task t = tasks.get(i);

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

  private static native void scheduleFixedDelayImpl(RepeatingCommand cmd,
      int delayMs) /*-{
    $wnd.setTimeout(function() {
      // $entry takes care of uncaught exception handling
      var ret = $entry(@com.google.gwt.core.client.impl.SchedulerImpl::execute(Lcom/google/gwt/core/client/Scheduler$RepeatingCommand;))(cmd);
      if (!@com.google.gwt.core.client.GWT::isScript()()) {
        // Unwrap from hosted mode
        ret = ret == true;
      }
      if (ret) {
        $wnd.setTimeout(arguments.callee, delayMs);
      }
    }, delayMs);
  }-*/;

  private static native void scheduleFixedPeriodImpl(RepeatingCommand cmd,
      int delayMs) /*-{
    var fn = function() {
      // $entry takes care of uncaught exception handling
      var ret = $entry(@com.google.gwt.core.client.impl.SchedulerImpl::execute(Lcom/google/gwt/core/client/Scheduler$RepeatingCommand;))(cmd);
      if (!@com.google.gwt.core.client.GWT::isScript()()) {
        // Unwrap from hosted mode
        ret = ret == true;
      }
      if (!ret) {
        // Either canceled or threw an exception
        $wnd.clearInterval(arguments.callee.token);
      }
    };
    fn.token = $wnd.setInterval(fn, delayMs);
  }-*/;

  /**
   * A RepeatingCommand that calls flushPostEventPumpCommands(). It repeats if
   * there are any outstanding deferred or incremental commands.
   */
  final RepeatingCommand flusher = new RepeatingCommand() {
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
  final RepeatingCommand rescue = new RepeatingCommand() {
    public boolean execute() {
      if (flushRunning) {
        /*
         * Since JS is single-threaded, if we're here, then than means that
         * FLUSHER.execute() started, but did not finish. Reschedule FLUSHER.
         */
        scheduleFixedDelay(flusher, FLUSHER_DELAY);
      }
      return shouldBeRunning;
    }
  };

  /*
   * Work queues. Timers store their state on the function, so we don't need to
   * track them. They are not final so that we don't have to shorten them.
   * Processing the values in the queues is a one-shot, and then the array is
   * discarded.
   */
  JsArray<Task> deferredCommands = JavaScriptObject.createArray().cast();
  JsArray<Task> incrementalCommands = JavaScriptObject.createArray().cast();
  JsArray<Task> finallyCommands = JavaScriptObject.createArray().cast();

  /*
   * These two flags are used to control the state of the flusher and rescuer
   * commands.
   */
  private boolean shouldBeRunning = false;
  private boolean flushRunning = false;

  /**
   * Called by {@link Impl#entry(JavaScriptObject)}.
   */
  public void flushFinallyCommands() {
    JsArray<Task> oldFinally = finallyCommands;
    finallyCommands = JavaScriptObject.createArray().cast();
    runScheduledTasks(oldFinally, finallyCommands);
  }

  @Override
  public void scheduleDeferred(ScheduledCommand cmd) {
    deferredCommands.push(Task.create(cmd));
    maybeSchedulePostEventPumpCommands();
  }

  @Override
  public void scheduleFinally(ScheduledCommand cmd) {
    finallyCommands.push(Task.create(cmd));
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
    // Push repeating commands onto the same initial queue for relative order
    deferredCommands.push(Task.create(cmd));
    maybeSchedulePostEventPumpCommands();
  }

  /**
   * Called by Flusher.
   */
  void flushPostEventPumpCommands() {
    JsArray<Task> oldDeferred = deferredCommands;
    deferredCommands = JavaScriptObject.createArray().cast();

    runScheduledTasks(oldDeferred, incrementalCommands);
    incrementalCommands = runRepeatingTasks(incrementalCommands);
  }

  boolean isWorkQueued() {
    return deferredCommands.length() > 0 || incrementalCommands.length() > 0;
  }

  private void maybeSchedulePostEventPumpCommands() {
    if (!shouldBeRunning) {
      shouldBeRunning = true;
      scheduleFixedDelayImpl(flusher, FLUSHER_DELAY);
      scheduleFixedDelayImpl(rescue, RESCUE_DELAY);
    }
  }
}
