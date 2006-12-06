/*
 * Copyright 2006 Google Inc.
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

import java.util.Vector;

/**
 * This class allows you to execute code after all currently pending event
 * handlers have completed, using the {@link #add(Command)} method. This is
 * useful when you need to execute code outside of the context of the current
 * stack.
 */
public class DeferredCommand {

  /**
   * The list of commands to be processed the next time a timer event is
   * handled.
   */
  private static Vector deferredCommands = new Vector();

  /**
   * Records whether a timer is pending so we don't set multiple ones.
   */
  private static boolean timerIsActive = false;

  /**
   * Enqueues a {@link Command} to be fired after all current events have been
   * handled.
   * 
   * @param cmd the command to be fired. If cmd is null, a "pause" will be
   *          inserted into the queue. Any events added after the pause will
   *          wait for an additional cycle through the system event loop before
   *          executing. Pauses are cumulative.
   */
  public static void add(Command cmd) {
    deferredCommands.add(cmd);
    maybeSetDeferredCommandTimer();
  }

  /**
   * Executes the current set of deferred commands before returning.
   */
  private static void flushDeferredCommands() {
    /*
     * Only execute the commands present at the beginning, and always pull from
     * the beginning of the list. This ensures that if any commands are added
     * while executing the current ones, they will stay in the list for the next
     * pass (otherwise, they wouldn't appear deferred).
     * 
     * If a deferred command throws an exception, that's okay, we'll just pick
     * up where we left off on the next tick.
     */
    for (int i = 0, max = deferredCommands.size(); i < max; ++i) {
      Command current = (Command) deferredCommands.remove(0);
      if (current == null) {
        /*
         * This is an indication that we should defer everything else in the
         * list until the next tick. Leave everything else in the queue.
         */
        return;
      } else {
        current.execute();
      }
    }
  }

  private static void maybeSetDeferredCommandTimer() {
    if (!timerIsActive && !deferredCommands.isEmpty()) {
      // There are some deferred commands in the queue.
      // Make sure a timer will fire for them.
      new Timer() {
        public void run() {
          try {

            // execute the pending commands
            flushDeferredCommands();

          } finally {
            // this timer has now fired and will not fire again
            timerIsActive = false;

            // always setup the next timer, even if we're throwing an exception
            maybeSetDeferredCommandTimer();
          }
        }
      }.schedule(1);
      timerIsActive = true;
    }
  }
}
