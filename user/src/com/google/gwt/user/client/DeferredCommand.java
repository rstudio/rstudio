/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.user.client;

/**
 * This class allows you to execute code after all currently pending event
 * handlers have completed, using the {@link #addCommand(Command)} or
 * {@link #addCommand(IncrementalCommand)} methods. This is useful when you need
 * to execute code outside of the context of the current stack.
 * 
 * @deprecated Replaced by
 *             {@link com.google.gwt.core.client.Scheduler#scheduleDeferred
 *             Scheduler.scheduleDeferred()} because the static nature of this
 *             API prevents effective mocking for JRE-only tests.
 */
@Deprecated
public class DeferredCommand {
  private static final CommandExecutor commandExecutor = new CommandExecutor();

  /**
   * Enqueues a {@link Command} to be fired after all current events have been
   * handled.
   * 
   * @param cmd the command to be fired. If cmd is null, a "pause" will be
   *          inserted into the queue. Any events added after the pause will
   *          wait for an additional cycle through the system event loop before
   *          executing. Pauses are cumulative.
   * 
   * @deprecated As of release 1.4, replaced by {@link #addCommand(Command)}
   */
  @Deprecated
  public static void add(Command cmd) {
    commandExecutor.submit(cmd);
  }

  /**
   * Enqueues a {@link Command} to be fired after all current events have been
   * handled.
   * 
   * Note that the {@link Command} should not perform any blocking operations.
   * 
   * @param cmd the command to be fired
   * @throws NullPointerException if cmd is <code>null</code>
   */
  public static void addCommand(Command cmd) {
    if (cmd == null) {
      throw new NullPointerException("cmd cannot be null");
    }

    commandExecutor.submit(cmd);
  }

  /**
   * Enqueues an {@link IncrementalCommand} to be fired after all current events
   * have been handled.
   * 
   * Note that the {@link IncrementalCommand} should not perform any blocking
   * operations.
   * 
   * @param cmd the command to be fired
   * @throws NullPointerException if cmd is <code>null</code>
   */
  public static void addCommand(IncrementalCommand cmd) {
    if (cmd == null) {
      throw new NullPointerException("cmd cannot be null");
    }

    commandExecutor.submit(cmd);
  }

  /**
   * Adds a "pause" to the queue of {@link DeferredCommand}s. Any
   * {@link DeferredCommand}s or pauses that are added after this pause will
   * wait for an additional cycle through the system event loop before
   * executing.
   * 
   * @deprecated with no replacement because the presence of this method causes
   *             arbitrary scheduling decisions
   */
  @Deprecated
  public static void addPause() {
    commandExecutor.submit((Command) null);
  }
}
