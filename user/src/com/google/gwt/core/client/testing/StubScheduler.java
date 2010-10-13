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

import com.google.gwt.core.client.Scheduler;

import java.util.ArrayList;
import java.util.List;

/**
 * A no-op implementation of Scheduler that simply records its arguments.
 */
public class StubScheduler extends Scheduler {
  private final List<RepeatingCommand> repeatingCommands = new ArrayList<RepeatingCommand>();

  private final List<ScheduledCommand> scheduledCommands = new ArrayList<ScheduledCommand>();

  /**
   * Returns the RepeatingCommands that have been passed into the MockScheduler.
   */
  public List<RepeatingCommand> getRepeatingCommands() {
    return repeatingCommands;
  }

  /**
   * Returns the ScheduledCommands that have been passed into the MockScheduler.
   */
  public List<ScheduledCommand> getScheduledCommands() {
    return scheduledCommands;
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
