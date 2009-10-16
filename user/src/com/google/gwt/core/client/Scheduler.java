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

import com.google.gwt.core.client.impl.SchedulerImpl;

/**
 * This class provides low-level task scheduling primitives. Any exceptions
 * thrown by the command objects executed by the scheduler will be passed to the
 * {@link GWT.UncaughtExceptionHandler} if one is installed.
 */
public abstract class Scheduler {

  /**
   * General-purpose Command interface for tasks that repeat.
   */
  public interface RepeatingCommand {
    /**
     * Returns true if the RepeatingCommand should be invoked again.
     */
    boolean execute();
  }

  /**
   * General-purpose Command interface.
   */
  public interface ScheduledCommand {
    /**
     * Invokes the command.
     */
    void execute();
  }

  /**
   * Use a GWT.create() here to make it simple to hijack the default
   * implementation.
   */
  private static final Scheduler IMPL = GWT.create(SchedulerImpl.class);

  /**
   * Returns the default implementation of the Scheduler API.
   */
  public static Scheduler get() {
    return IMPL;
  }

  /**
   * A deferred command is executed after the browser event loop returns.
   */
  public abstract void scheduleDeferred(ScheduledCommand cmd);

  /**
   * A "finally" command will be executed before GWT-generated code returns
   * control to the browser's event loop. This type of command is used to
   * aggregate small amounts of work before performing a non-recurring,
   * heavyweight operation.
   * <p>
   * Consider the following:
   * 
   * <pre>
   * try {
   *   nativeEventCallback(); // Calls scheduleFinally one or more times
   * } finally {
   *   executeFinallyCommands();
   * }
   * </pre>
   * 
   * @see com.google.gwt.dom.client.StyleInjector
   */
  public abstract void scheduleFinally(ScheduledCommand cmd);

  /**
   * Schedules a repeating command that is scheduled with a constant delay. That
   * is, the next invocation of the command will be scheduled for
   * <code>delayMs</code> milliseconds after the last invocation completes.
   * <p>
   * For example, assume that a command takes 30ms to run and a 100ms delay is
   * provided. The second invocation of the command will occur at 130ms after
   * the first invocation starts.
   * 
   * @param cmd the command to execute
   * @param delayMs the amount of time to wait after one invocation ends before
   *          the next invocation
   */
  public abstract void scheduleFixedDelay(RepeatingCommand cmd, int delayMs);

  /**
   * Schedules a repeating command that is scheduled with a constant
   * periodicity. That is, the command will be invoked every
   * <code>delayMs</code> milliseconds, regardless of how long the previous
   * invocation took to complete.
   * 
   * @param cmd the command to execute
   * @param delayMs the period with which the command is executed
   */
  public abstract void scheduleFixedPeriod(RepeatingCommand cmd, int delayMs);

  /**
   * Schedules a repeating command that performs incremental work. This type of
   * command is encouraged for long-running processes that perform computation
   * or that manipulate the DOM. The commands in this queue are invoked many
   * times in rapid succession and are then deferred to allow the browser to
   * process its event queue.
   * 
   * @param cmd the command to execute
   */
  public abstract void scheduleIncremental(RepeatingCommand cmd);
}
