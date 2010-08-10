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

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class which executes {@link Command}s and {@link IncrementalCommand}s after
 * all currently pending event handlers have completed. This class attempts to
 * protect against slow script warnings by running commands in small time
 * increments.
 * 
 * <p>
 * It is still possible that a poorly written command could cause a slow script
 * warning which a user may choose to cancel. In that event, a
 * {@link CommandCanceledException} or an
 * {@link IncrementalCommandCanceledException} is reported through the current
 * {@link UncaughtExceptionHandler} depending on the type of command which
 * caused the warning. All other commands will continue to be executed.
 * </p>
 * 
 * TODO(mmendez): Can an SSW be detected without using a timer? Currently, if a
 * {@link Command} or an {@link IncrementalCommand} calls either
 * {@link Window#alert(String)} or the JavaScript <code>alert(String)</code>
 * methods directly or indirectly then the  cancellation timer can fire,
 * resulting in a false SSW cancellation detection.
 */
class CommandExecutor {

  /**
   * A circular iterator used by this class. This iterator will wrap back to
   * zero when it hits the end of the commands.
   */
  private class CircularIterator implements Iterator<Object> {
    /**
     * Index of the element where this iterator should wrap back to the
     * beginning of the collection.
     */
    private int end;

    /**
     * Index of the last item returned by {@link #next()}.
     */
    private int last = -1;

    /**
     * Index of the next command to execute.
     */
    private int next = 0;

    /**
     * Returns <code>true</code> if there are more commands in the queue.
     * 
     * @return <code>true</code> if there are more commands in the queue.
     */
    public boolean hasNext() {
      return next < end;
    }

    /**
     * Returns the next command from the queue. When the end of the dispatch
     * region is reached it will wrap back to the start.
     * 
     * @return next command from the queue.
     */
    public Object next() {
      last = next;
      Object command = commands.get(next++);
      if (next >= end) {
        next = 0;
      }

      return command;
    }

    /**
     * Removes the command which was previously returned by {@link #next()}.
     * 
     */
    public void remove() {
      assert (last >= 0);

      commands.remove(last);
      --end;

      if (last <= next) {
        if (--next < 0) {
          next = 0;
        }
      }

      last = -1;
    }

    /**
     * Returns the last element returned by {@link #next()}.
     * 
     * @return last element returned by {@link #next()}
     */
    private Object getLast() {
      assert (last >= 0);
      return commands.get(last);
    }

    private void setEnd(int end) {
      assert (end >= next);

      this.end = end;
    }

    private void setLast(int last) {
      this.last = last;
    }

    private boolean wasRemoved() {
      return last == -1;
    }
  }

  /**
   * Default amount of time to wait before assuming that a script cancellation
   * has taken place. This should be a platform dependent value, ultimately we
   * may need to acquire this value based on a rebind decision. For now, we
   * chose the smallest value known to cause an SSW.
   */
  private static final int DEFAULT_CANCELLATION_TIMEOUT_MILLIS = 10000;

  /**
   * Default amount of time to spend dispatching commands before we yield to the
   * system.
   */
  private static final int DEFAULT_TIME_SLICE_MILLIS = 100;

  /**
   * Returns true the end time has been reached or exceeded.
   * 
   * @param currentTimeMillis current time in milliseconds
   * @param startTimeMillis end time in milliseconds
   * @return true if the end time has been reached
   */
  private static boolean hasTimeSliceExpired(double currentTimeMillis,
      double startTimeMillis) {
    return currentTimeMillis - startTimeMillis >= DEFAULT_TIME_SLICE_MILLIS;
  }

  /**
   * Timer used to recover from script cancellations arising from slow script
   * warnings.
   */
  private final Timer cancellationTimer = new Timer() {
    @Override
    public void run() {
      if (!isExecuting()) {
        /*
         * If we are not executing, then the cancellation timer expired right
         * about the time that the command dispatcher finished -- we are okay so
         * we just exit.
         */
        return;
      }

      doCommandCanceled();
    }
  };

  /**
   * Commands that need to be executed.
   */
  private final List<Object> commands = new ArrayList<Object>();

  /**
   * Set to <code>true</code> when we are actively dispatching commands.
   */
  private boolean executing = false;

  /**
   * Timer used to drive the dispatching of commands in the background.
   */
  private final Timer executionTimer = new Timer() {
    @Override
    public void run() {
      assert (!isExecuting());

      setExecutionTimerPending(false);

      doExecuteCommands(Duration.currentTimeMillis());
    }
  };

  /**
   * Set to <code>true</code> when we are waiting for a dispatch timer event
   * to fire.
   */
  private boolean executionTimerPending = false;

  /**
   * The single circular iterator instance that we use to iterate over the
   * collection of commands.
   */
  private final CircularIterator iterator = new CircularIterator();

  /**
   * Submits a {@link Command} for execution.
   * 
   * @param command command to submit
   */
  public void submit(Command command) {
    commands.add(command);

    maybeStartExecutionTimer();
  }

  /**
   * Submits an {@link IncrementalCommand} for execution.
   * 
   * @param command command to submit
   */
  public void submit(IncrementalCommand command) {
    commands.add(command);

    maybeStartExecutionTimer();
  }

  /**
   * Reports either a {@link CommandCanceledException} or an
   * {@link IncrementalCommandCanceledException} back through the
   * {@link UncaughtExceptionHandler} if one is set.
   */
  protected void doCommandCanceled() {
    Object cmd = iterator.getLast();
    iterator.remove();
    assert (cmd != null);

    RuntimeException ex = null;
    if (cmd instanceof Command) {
      ex = new CommandCanceledException((Command) cmd);
    } else if (cmd instanceof IncrementalCommand) {
      ex = new IncrementalCommandCanceledException((IncrementalCommand) cmd);
    }

    if (ex != null) {
      UncaughtExceptionHandler ueh = GWT.getUncaughtExceptionHandler();
      if (ueh != null) {
        ueh.onUncaughtException(ex);
      }
    }

    setExecuting(false);

    maybeStartExecutionTimer();
  }

  /**
   * This method will dispatch commands from the command queue. It will dispatch
   * commands until one of the following conditions is <code>true</code>:
   * <ul>
   * <li>It consumed its dispatching time slice
   * {@value #DEFAULT_TIME_SLICE_MILLIS}</li>
   * <li>It encounters a <code>null</code> in the command queue</li>
   * <li>All commands which were present at the start of the dispatching have
   * been removed from the command queue</li>
   * <li>The command that it was processing was canceled due to a false
   * cancellation -- in this case we exit without updating any state</li>
   * </ul>
   * 
   * @param startTimeMillis the time when this method started
   */
  protected void doExecuteCommands(double startTimeMillis) {
    assert (!isExecutionTimerPending());

    boolean wasCanceled = false;
    try {
      setExecuting(true);

      iterator.setEnd(commands.size());

      cancellationTimer.schedule(DEFAULT_CANCELLATION_TIMEOUT_MILLIS);

      while (iterator.hasNext()) {
        Object element = iterator.next();

        boolean removeCommand = true;
        try {
          if (element == null) {
            // null forces a yield or pause in execution
            return;
          }

          if (element instanceof Command) {
            Command command = (Command) element;
            command.execute();
          } else if (element instanceof IncrementalCommand) {
            IncrementalCommand incrementalCommand = (IncrementalCommand) element;
            removeCommand = !incrementalCommand.execute();
          }

        } finally {
          wasCanceled = iterator.wasRemoved();
          if (!wasCanceled) {
            if (removeCommand) {
              iterator.remove();
            }
          }
        }

        if (hasTimeSliceExpired(Duration.currentTimeMillis(), startTimeMillis)) {
          // the time slice has expired
          return;
        }
      }
    } finally {
      if (!wasCanceled) {
        cancellationTimer.cancel();

        setExecuting(false);

        maybeStartExecutionTimer();
      }
    }
  }

  /**
   * Starts the dispatch timer if there are commands to dispatch and we are not
   * waiting for a dispatch timer and we are not actively dispatching.
   */
  protected void maybeStartExecutionTimer() {
    if (!commands.isEmpty() && !isExecutionTimerPending() && !isExecuting()) {
      setExecutionTimerPending(true);
      executionTimer.schedule(1);
    }
  }

  /**
   * This method is for testing only.
   */
  List<Object> getPendingCommands() {
    return commands;
  }

  /**
   * This method is for testing only.
   */
  void setExecuting(boolean executing) {
    this.executing = executing;
  }

  /**
   * This method is for testing only.
   */
  void setLast(int last) {
    iterator.setLast(last);
  }

  /**
   * Returns <code>true</code> if this instance is currently dispatching
   * commands.
   * 
   * @return <code>true</code> if this instance is currently dispatching
   *         commands
   */
  private boolean isExecuting() {
    return executing;
  }

  /**
   * Returns <code>true</code> if a the dispatch timer was scheduled but it
   * still has not fired.
   * 
   * @return <code>true</code> if a the dispatch timer was scheduled but it
   *         still has not fired
   */
  private boolean isExecutionTimerPending() {
    return executionTimerPending;
  }

  private void setExecutionTimerPending(boolean pending) {
    executionTimerPending = pending;
  }
}