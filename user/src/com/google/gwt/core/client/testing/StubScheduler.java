/*
 * Copyright 2010 Google Inc.
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
import com.google.gwt.core.client.Scheduler;

import java.util.ArrayList;
import java.util.List;

/**
 * A fake scheduler that records scheduled commands and can execute them when asked to.
 *
 * <p>Typical usage:
 * <pre>
 *   scheduleCommands(scheduler);
 *   assertFalse(scheduler.executeCommands());
 * </pre>
 */
public class StubScheduler extends Scheduler {

  private final List<RepeatingCommand> repeatingCommands = new ArrayList<RepeatingCommand>();
  private final List<ScheduledCommand> scheduledCommands = new ArrayList<ScheduledCommand>();

  /**
   * Returns the currently scheduled {@link RepeatingCommand}s that would be executed
   * by {@link #executeRepeatingCommands()}.
   */
  public List<RepeatingCommand> getRepeatingCommands() {
    return repeatingCommands;
  }

  /**
   * Executes all scheduled commands once. Equivalent to calling {@link #executeRepeatingCommands}
   * then {@link #executeScheduledCommands}.
   *
   * <p>Caveat: executes once any scheduled command created when executing the repeating commands.
   *
   * @return whether some repeating commands are still scheduled (returned {@code true}) or
   *   new commands have been scheduled after the execution
   */
  public boolean executeCommands() {
    boolean repeatingRemaining = executeRepeatingCommands();
    boolean scheduledRemaining = executeScheduledCommands();
    return repeatingRemaining || scheduledRemaining;
  }

  /**
   * Executes all scheduled {@link RepeatingCommand}s once. Does not execute the commands
   * newly scheduled by the initial commands. Removes the commands that returned {@code false}.
   *
   * <p>After this method completes, {@link #getRepeatingCommands} returns only the commands
   * that are still scheduled.
   *
   * @return whether some commands are still scheduled (returned {@code true}) or
   *   new commands have been scheduled after the execution
   */
  public boolean executeRepeatingCommands() {
    List<RepeatingCommand> commands = new ArrayList<RepeatingCommand>(repeatingCommands);
    repeatingCommands.clear();
    for (RepeatingCommand command : commands) {
      boolean reschedule;
      try {
        reschedule = command.execute();
      } catch (Throwable e) {
        reschedule = false;
        GWT.maybeReportUncaughtException(e);
      }
      if (reschedule) {
        repeatingCommands.add(command);
      }
    }
    return !repeatingCommands.isEmpty();
  }

  /**
   * Returns the currently scheduled {@link ScheduledCommand}s that would be executed
   * by {@link #executeScheduledCommands}.
   */
  public List<ScheduledCommand> getScheduledCommands() {
    return scheduledCommands;
  }

  /**
   * Executes all scheduled {@link ScheduledCommand}s that have been passed to this scheduler,
   * then removes all commands.
   *
   * <p>After this method completes, {@link #getScheduledCommands} returns only the commands
   * that have been scheduled by the initial commands.
   *
   * @return whether new commands have been scheduled after the execution
   */
  public boolean executeScheduledCommands() {
    List<ScheduledCommand> commands = new ArrayList<ScheduledCommand>(scheduledCommands);
    scheduledCommands.clear();
    for (ScheduledCommand command : commands) {
      try {
        command.execute();
      } catch (Throwable e) {
        GWT.maybeReportUncaughtException(e);
      }
    }
    return !scheduledCommands.isEmpty();
  }

  @Override
  public void scheduleDeferred(ScheduledCommand cmd) {
    scheduledCommands.add(cmd);
  }

  @Override
  public void scheduleEntry(RepeatingCommand cmd) {
    repeatingCommands.add(cmd);
  }

  @Override
  public void scheduleEntry(ScheduledCommand cmd) {
    scheduledCommands.add(cmd);
  }

  @Override
  public void scheduleFinally(RepeatingCommand cmd) {
    repeatingCommands.add(cmd);
  }

  @Override
  public void scheduleFinally(ScheduledCommand cmd) {
    scheduledCommands.add(cmd);
  }

  @Override
  public void scheduleFixedDelay(RepeatingCommand cmd, int delayMs) {
    repeatingCommands.add(cmd);
  }

  @Override
  public void scheduleFixedPeriod(RepeatingCommand cmd, int delayMs) {
    repeatingCommands.add(cmd);
  }

  @Override
  public void scheduleIncremental(RepeatingCommand cmd) {
    repeatingCommands.add(cmd);
  }
}
