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
   * Calls {@link SchedulerImpl#flushPostEventPumpCommands()}.
   */
  private final class Flusher implements RepeatingCommand {
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
  }

  /**
   * Keeps {@link Flusher} running.
   */
  private final class Rescuer implements RepeatingCommand {
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
   * Extract boilerplate code.
   */
  private static JsArray<Task> createQueue() {
    return JavaScriptObject.createArray().cast();
  }

  /**
   * Called from scheduledFixedInterval to give $entry a static function.
   */
  private static boolean execute(RepeatingCommand cmd) {
    return cmd.execute();
  }

  /**
   * Provides lazy-init pattern for the task queues.
   */
  private static JsArray<Task> push(JsArray<Task> queue, Task task) {
    if (queue == null) {
      queue = createQueue();
    }
    queue.push(task);
    return queue;
  }

  /**
   * Execute a list of Tasks that hold RepeatingCommands.
   * 
   * @return A replacement array that is possibly a shorter copy of
   *         <code>tasks</code>
   */
  private static JsArray<Task> runRepeatingTasks(JsArray<Task> tasks) {
    assert tasks != null : "tasks";

    int length = tasks.length();
    if (length == 0) {
      return null;
    }

    boolean canceledSomeTasks = false;
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
      JsArray<Task> newTasks = createQueue();
      // Remove tombstones
      for (int i = 0; i < length; i++) {
        if (tasks.get(i) != null) {
          newTasks.push(tasks.get(i));
        }
      }
      assert newTasks.length() < length;
      return newTasks.length() == 0 ? null : newTasks;
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
   * 
   * @return <code>rescheduled</code> or a newly-allocated array if
   *         <code>rescheduled</code> is null.
   */
  private static JsArray<Task> runScheduledTasks(JsArray<Task> tasks,
      JsArray<Task> rescheduled) {
    assert tasks != null : "tasks";

    for (int i = 0, j = tasks.length(); i < j; i++) {
      assert tasks.length() == j : "Working array length changed "
          + tasks.length() + " != " + j;
      Task t = tasks.get(i);

      try {
        // Move repeating commands to incremental commands queue
        if (t.isRepeating()) {
          if (t.executeRepeating()) {
            rescheduled = push(rescheduled, t);
          }
        } else {
          t.executeScheduled();
        }
      } catch (Throwable e) {
        if (GWT.getUncaughtExceptionHandler() != null) {
          GWT.getUncaughtExceptionHandler().onUncaughtException(e);
        }
      }
    }
    return rescheduled;
  }

  private static native void scheduleFixedDelayImpl(RepeatingCommand cmd,
      int delayMs) /*-{
    $wnd.setTimeout(function() {
      // $entry takes care of uncaught exception handling
      var ret = $entry(@com.google.gwt.core.client.impl.SchedulerImpl::execute(Lcom/google/gwt/core/client/Scheduler$RepeatingCommand;))(cmd);
      if (!@com.google.gwt.core.client.GWT::isScript()()) {
        // Unwrap from Development Mode
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
        // Unwrap from Development Mode
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
  Flusher flusher;

  /**
   * This provides some backup for the main flusher task in case it gets shut
   * down by a slow-script warning.
   */
  Rescuer rescue;

  /*
   * Work queues. Timers store their state on the function, so we don't need to
   * track them. They are not final so that we don't have to shorten them.
   * Processing the values in the queues is a one-shot, and then the array is
   * discarded.
   */
  JsArray<Task> deferredCommands;
  JsArray<Task> entryCommands;
  JsArray<Task> finallyCommands;
  JsArray<Task> incrementalCommands;

  /*
   * These two flags are used to control the state of the flusher and rescuer
   * commands.
   */
  private boolean flushRunning = false;
  private boolean shouldBeRunning = false;

  /**
   * Called by {@link Impl#entry(JavaScriptObject)}.
   */
  public void flushEntryCommands() {
    if (entryCommands != null) {
      JsArray<Task> rescheduled = null;
      // This do-while loop handles commands scheduling commands
      do {
        JsArray<Task> oldQueue = entryCommands;
        entryCommands = null;
        rescheduled = runScheduledTasks(oldQueue, rescheduled);
      } while (entryCommands != null);
      entryCommands = rescheduled;
    }
  }

  /**
   * Called by {@link Impl#entry(JavaScriptObject)}.
   */
  public void flushFinallyCommands() {
    if (finallyCommands != null) {
      JsArray<Task> rescheduled = null;
      // This do-while loop handles commands scheduling commands
      do {
        JsArray<Task> oldQueue = finallyCommands;
        finallyCommands = null;
        rescheduled = runScheduledTasks(oldQueue, rescheduled);
      } while (finallyCommands != null);
      finallyCommands = rescheduled;
    }
  }

  @Override
  public void scheduleDeferred(ScheduledCommand cmd) {
    deferredCommands = push(deferredCommands, Task.create(cmd));
    maybeSchedulePostEventPumpCommands();
  }

  @Override
  public void scheduleEntry(RepeatingCommand cmd) {
    entryCommands = push(entryCommands, Task.create(cmd));
  }

  @Override
  public void scheduleEntry(ScheduledCommand cmd) {
    entryCommands = push(entryCommands, Task.create(cmd));
  }

  @Override
  public void scheduleFinally(RepeatingCommand cmd) {
    finallyCommands = push(finallyCommands, Task.create(cmd));
  }

  @Override
  public void scheduleFinally(ScheduledCommand cmd) {
    finallyCommands = push(finallyCommands, Task.create(cmd));
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
    deferredCommands = push(deferredCommands, Task.create(cmd));
    maybeSchedulePostEventPumpCommands();
  }

  /**
   * Called by Flusher.
   */
  void flushPostEventPumpCommands() {
    if (deferredCommands != null) {
      JsArray<Task> oldDeferred = deferredCommands;
      deferredCommands = null;

      /* We might not have any incremental commands queued. */
      if (incrementalCommands == null) {
        incrementalCommands = createQueue();
      }
      runScheduledTasks(oldDeferred, incrementalCommands);
    }

    if (incrementalCommands != null) {
      incrementalCommands = runRepeatingTasks(incrementalCommands);
    }
  }

  boolean isWorkQueued() {
    return deferredCommands != null || incrementalCommands != null;
  }

  private void maybeSchedulePostEventPumpCommands() {
    if (!shouldBeRunning) {
      shouldBeRunning = true;

      if (flusher == null) {
        flusher = new Flusher();
      }
      scheduleFixedDelayImpl(flusher, FLUSHER_DELAY);

      if (rescue == null) {
        rescue = new Rescuer();
      }
      scheduleFixedDelayImpl(rescue, RESCUE_DELAY);
    }
  }
}
