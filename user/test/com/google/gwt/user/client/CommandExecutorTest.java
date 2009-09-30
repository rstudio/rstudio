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
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Test cases for {@link CommandExecutor}.
 */
public class CommandExecutorTest extends GWTTestCase {

  private static class NonRestartingCommandExecutor extends CommandExecutor {
    protected void maybeStartExecutionTimer() {
      // keeps the executing timer for interfering with the test
    }
  }

  private static class TestCommand implements Command {
    private boolean executed;

    public boolean didExecute() {
      return executed;
    }

    public void execute() {
      executed = true;
    }
  }

  private static class TestIncrementalCommand implements IncrementalCommand {
    private boolean done = false;
    private int executeCount;

    public boolean execute() {
      ++executeCount;

      return !isDone();
    }

    public int getExecuteCount() {
      return executeCount;
    }

    public boolean isDone() {
      return done;
    }

    public void setDone(boolean done) {
      this.done = done;
    }
  }

  /**
   * A sufficiently large delay to let the SSW triggers.
   */
  private static final int TEST_FINISH_DELAY_MILLIS = 40000;

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  /**
   * Test method for
   * {@link com.google.gwt.user.client.CommandExecutor#doExecuteCommands(int)}.
   * 
   * Checks that we can recover after a cancellation
   */
  public void testDoExecuteCommands_CancellationRecovery() {
    final CommandExecutor ce = new NonRestartingCommandExecutor();

    final Command c1 = new Command() {
      public void execute() {
      }
    };

    ce.setExecuting(true);
    ce.submit(c1);
    ce.setLast(0);

    final UncaughtExceptionHandler originalUEH = GWT.getUncaughtExceptionHandler();

    UncaughtExceptionHandler ueh1 = new UncaughtExceptionHandler() {
      public void onUncaughtException(Throwable e) {
        if (!(e instanceof CommandCanceledException)) {
          originalUEH.onUncaughtException(e);
          return;
        }

        CommandCanceledException cce = (CommandCanceledException) e;
        if (cce.getCommand() != c1) {
          fail("CommandCanceledException did not contain the correct failed command");
        }

        // Submit some more work and do another dispatch
        ce.submit(new IncrementalCommand() {
          public boolean execute() {
            return false;
          }
        });

        delayTestFinish(TEST_FINISH_DELAY_MILLIS);
        ce.submit(new Command() {
          public void execute() {
            finishTest();
          }
        });

        ce.doExecuteCommands(Duration.currentTimeMillis());
      }
    };

    GWT.setUncaughtExceptionHandler(ueh1);
    ce.doCommandCanceled();
  }

  /**
   * Test method for
   * {@link com.google.gwt.user.client.CommandExecutor#doExecuteCommands(int)}.
   * 
   * Checks Command cancellation detection
   */
  public void testDoExecuteCommands_CommandCancellation() {
    final CommandExecutor ce = new NonRestartingCommandExecutor();

    final Command c1 = new Command() {
      public void execute() {
      }
    };

    // Setup the cancellation state
    ce.setExecuting(true);
    ce.submit(c1);
    ce.setLast(0);

    final UncaughtExceptionHandler originalUEH = GWT.getUncaughtExceptionHandler();

    UncaughtExceptionHandler ueh1 = new UncaughtExceptionHandler() {
      public void onUncaughtException(Throwable e) {
        if (!(e instanceof CommandCanceledException)) {
          originalUEH.onUncaughtException(e);
          return;
        }

        CommandCanceledException cce = (CommandCanceledException) e;
        if (cce.getCommand() != c1) {
          fail("CommandCanceledException did not contain the correct failed command");
        }
      }
    };

    GWT.setUncaughtExceptionHandler(ueh1);
    ce.doCommandCanceled();
  }

  /**
   * Test method for
   * {@link com.google.gwt.user.client.CommandExecutor#doExecuteCommands(int)}.
   * 
   * Checks that calling {@link CommandExecutor#doExecuteCommands(double)} with no
   * items in the queue is safe
   */
  public void testDoExecuteCommands_emptyQueue() {
    final CommandExecutor ce = new NonRestartingCommandExecutor();

    ce.doExecuteCommands(Duration.currentTimeMillis());
  }

  /**
   * Test method for
   * {@link com.google.gwt.user.client.CommandExecutor#doExecuteCommands(int)}.
   * 
   * Checks IncrementalCommand cancellation detection
   */
  public void testDoExecuteCommands_IncrementalCommandCancellation() {
    final CommandExecutor ce = new NonRestartingCommandExecutor();

    final IncrementalCommand ic = new IncrementalCommand() {
      public boolean execute() {
        return false;
      }
    };

    // setup the cancellation state
    ce.setExecuting(true);
    ce.submit(ic);
    ce.setLast(0);

    final UncaughtExceptionHandler originalUEH = GWT.getUncaughtExceptionHandler();

    UncaughtExceptionHandler ueh1 = new UncaughtExceptionHandler() {
      public void onUncaughtException(Throwable e) {
        if (!(e instanceof IncrementalCommandCanceledException)) {
          originalUEH.onUncaughtException(e);
          return;
        }

        IncrementalCommandCanceledException icce = (IncrementalCommandCanceledException) e;
        if (icce.getCommand() != ic) {
          fail("IncrementalCommandCanceledException did not contain the correct failed command");
        }
      }
    };

    GWT.setUncaughtExceptionHandler(ueh1);
    ce.doCommandCanceled();
  }

  /**
   * Test method for
   * {@link com.google.gwt.user.client.CommandExecutor#doExecuteCommands(int)}.
   * 
   * Checks that an incremental command executes and is removed from the queue
   * when it is done
   */
  public void testDoExecuteCommands_IncrementalCommands() {
    TestIncrementalCommand tic = new TestIncrementalCommand();
    final CommandExecutor ce = new NonRestartingCommandExecutor();

    tic.setDone(true);
    ce.submit(tic);
    ce.doExecuteCommands(Duration.currentTimeMillis());
    assertTrue(tic.getExecuteCount() > 0);
    assertTrue(ce.getPendingCommands().isEmpty());
  }

  /**
   * Test method for
   * {@link com.google.gwt.user.client.CommandExecutor#doExecuteCommands(int)}.
   * 
   * Checks that null does in fact cause a pause.
   */
  public void testDoExecuteCommands_pause() {
    final CommandExecutor ce = new NonRestartingCommandExecutor();

    TestCommand tc1 = new TestCommand();
    TestCommand tc2 = new TestCommand();

    ce.submit(tc1);
    ce.submit((Command) null);
    ce.submit(tc2);

    ce.doExecuteCommands(Duration.currentTimeMillis());

    assertTrue(tc1.didExecute() && !tc2.didExecute());
    assertEquals(1, ce.getPendingCommands().size());
    assertTrue(ce.getPendingCommands().contains(tc2));
  }

  /**
   * Test method for
   * {@link com.google.gwt.user.client.CommandExecutor#doExecuteCommands(int)}.
   * 
   * Checks that after one pass dispatch pass, we still have the incremental
   * command in the queue
   */
  public void testDoExecuteCommands_timeSliceUsage() {
    final CommandExecutor ce = new NonRestartingCommandExecutor();

    Command tc = new TestCommand();
    ce.submit(tc);

    TestIncrementalCommand tic = new TestIncrementalCommand();
    ce.submit(tic);
    ce.doExecuteCommands(Duration.currentTimeMillis());

    assertEquals(1, ce.getPendingCommands().size());
    assertTrue(ce.getPendingCommands().contains(tic));
    assertTrue(tic.getExecuteCount() > 0);
  }

  /**
   * Test method for
   * {@link com.google.gwt.user.client.CommandExecutor#submit(com.google.gwt.user.client.Command)}.
   * 
   * <p/> Cases:
   * <ul>
   * <li>Submit <code>null</code></li>
   * <li>Submit {@link Command} and make sure that it fires</li>
   * </ul>
   */
  public void testSubmitCommand() {
    CommandExecutor ce = new CommandExecutor();
    ce.submit((Command) null);

    delayTestFinish(TEST_FINISH_DELAY_MILLIS);

    ce.submit(new Command() {
      public void execute() {
        finishTest();
      }
    });
  }

  /**
   * Test method for
   * {@link com.google.gwt.user.client.CommandExecutor#submit(com.google.gwt.user.client.IncrementalCommand)}.
   * 
   * <p/> Cases:
   * <ul>
   * <li>Submit <code>null</code></li>
   * <li>Submit {@link IncrementalCommand} and make sure that it fires as many
   * times as we want it to</li>
   * </ul>
   */
  public void testSubmitIncrementalCommand() {
    CommandExecutor ce = new CommandExecutor();
    ce.submit((Command) null);

    delayTestFinish(TEST_FINISH_DELAY_MILLIS);

    ce.submit(new IncrementalCommand() {
      private int executionCount = 0;

      public boolean execute() {
        if (++executionCount > 10) {
          fail("IncrementalCommand was fired more than 10 times");
        }

        if (executionCount == 10) {
          finishTest();
        }

        return executionCount < 10;
      }
    });
  }
}
