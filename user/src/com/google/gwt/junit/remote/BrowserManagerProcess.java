/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.junit.remote;

import java.text.NumberFormat;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages one web browser child process. This class contains a TimerTask which
 * tries to kill the managed process. A thread is created for each task to wait
 * for the process to exit and give a callback.
 * 
 * Invariants:
 * <ul>
 * <li> Most of this code executes in a separate thread per process. Thus, the
 * API entry points lock <code>this</code></li>
 * <li> The lock on this is removed before calling the <code>childExited</code>
 * callback. This prevents potential deadlock.
 * </ul>
 */
class BrowserManagerProcess {

  /**
   * Used to notify the caller of the constructor when a process exits. Note
   * that the childExited() method is called from a different thread than the
   * one that created the process.
   */
  public interface ProcessExitCb {
    void childExited(int key, int exitValue);
  }

  /**
   * Kills the child process when fired, unless it is no longer the active
   * {@link BrowserManagerProcess#killTask} in its outer ProcessManager.
   */
  private final class KillTask extends TimerTask {
    @Override
    public void run() {
      synchronized (BrowserManagerProcess.this) {
        /*
         * CORNER CASE: Verify we're still the active KillTask, because it's
         * possible we were bumped out by a keepAlive call after our execution
         * started but before we could grab the lock.
         */
        if (killTask == this) {
          logger.info("Timeout expired for task: " + token);
          doKill();
        }
      }
    }
  }

  private static final Logger logger = Logger.getLogger(BrowserManagerProcess.class.getName());

  /**
   * Exit callback when the process exits.
   */
  private final ProcessExitCb cb;

  /**
   * Set to 'true' when the process exits.
   */
  private boolean exited = false;

  /**
   * Set to the exitValue() of the process when it actually exits.
   */
  private int exitValue = -1;

  /**
   * The key associated with <code>process</code>.
   */
  private final int token;

  /**
   * If non-null, the active TimerTask which will kill <code>process</code>
   * when it fires.
   */
  private KillTask killTask;

  /**
   * The managed child process.
   */
  private final Process process;

  /**
   * Time the exec'ed child actually started.
   */
  private final long startTime;

  /**
   * Timer instance passed in from BrowserManagerServer.
   */
  private final Timer timer;

  /**
   * Constructs a new ProcessManager for the specified process.
   * 
   * @param timer timer passed in from BrowserManagerServer instance.
   * @param token the key to be used to identify this process.
   * @param process the process being managed
   * @param initKeepAliveMs the initial time to wait before killing
   *          <code>process</code>
   */
  BrowserManagerProcess(ProcessExitCb cb, Timer timer, final int token,
      final Process process, long initKeepAliveMs) {
    this.cb = cb;
    this.timer = timer;
    this.process = process;
    this.token = token;
    schedule(initKeepAliveMs);
    startTime = System.currentTimeMillis();

    Thread cleanupThread = new Thread() {
      @Override
      public void run() {
        try {
          exitValue = process.waitFor();
          if (cleanupBrowser() != 0) {
            logger.warning("Browser " + token + "exited with bad status: "
                + exitValue);
          } else {
            logger.info("Browser " + token + " process exited normally. "
                + getElapsed() + " milliseconds.");
          }
        } catch (InterruptedException e) {
          logger.log(Level.WARNING, "Couldn't wait for process exit. token: "
              + token, e);
        }
      }
    };

    cleanupThread.setDaemon(true);
    cleanupThread.setName("Browser-" + token + "-Wait");
    cleanupThread.start();
  }

  /**
   * Kills the managed process.
   * 
   * @return the exit value of the task.
   */
  public int doKill() {

    boolean doCleanup = false;
    synchronized (this) {
      if (!exited) {
        logger.info("Killing browser process for " + this.token);
        process.destroy();

        // Wait for the process to exit.
        try {
          exitValue = process.waitFor();
          doCleanup = true;
        } catch (InterruptedException ie) {
          logger.severe("Interrupted waiting for browser " + token
              + " exit during kill.");
        }
      }
    }

    // Run cleanupBrowser() outside the critical section.
    if (doCleanup) {
      if (cleanupBrowser() != 0) {
        logger.warning("Kill Browser " + token + "exited with bad status: "
            + exitValue);

      } else {
        logger.info("Kill Browser " + token + " process exited normally. "
            + getElapsed() + " milliseconds.");
      }
    }

    return exitValue;
  }

  /**
   * Keeps the underlying process alive for <code>keepAliveMs</code> starting
   * now. If the managed process is already dead, cleanup is performed and the
   * method return false.
   * 
   * @param keepAliveMs the time to wait before killing the underlying process
   * @return <code>true</code> if the process was successfully kept alive,
   *         <code>false</code> if the process is already dead.
   */
  public boolean keepAlive(long keepAliveMs) {
    synchronized (this) {
      try {
        /*
         * See if the managed process is still alive. WEIRD: The only way to
         * check the process's liveness appears to be asking for its exit status
         * and seeing whether it throws an IllegalThreadStateException.
         */
        process.exitValue();
      } catch (IllegalThreadStateException e) {
        // The process is still alive.
        schedule(keepAliveMs);
        return true;
      }
    }

    // The process is dead already; perform cleanup.
    cleanupBrowser();
    return false;
  }

  /**
   * Routine that informs the BrowserManagerServer of the exit status once and
   * only once.
   * 
   * This should be called WITHOUT the lock on BrowserManagerProcess.this being
   * held.
   * 
   * @return The exit value returned by the process when it exited.
   */
  private int cleanupBrowser() {
    boolean doCb = false;
    synchronized (this) {
      if (!exited) {
        exited = true;
        exitValue = process.exitValue();
        // Stop the timer for this thread.
        schedule(0);
        doCb = true;
      }
    }

    /*
     * Callback must occur without holding my own lock. This is because the
     * callee will try to acquire the lock on
     * BrowserManagerServer.processByToken. If another thread already has that
     * lock and is tries to lock me at the same time, a deadlock would ensure.
     */
    if (doCb) {
      cb.childExited(token, exitValue);
    }

    return exitValue;
  }

  /**
   * Compute elapsed time.
   * 
   * @return returns a string representing the number of seconds elapsed since
   *         the process started.
   */
  private synchronized String getElapsed() {
    NumberFormat nf = NumberFormat.getNumberInstance();
    nf.setMaximumFractionDigits(3);
    return nf.format((System.currentTimeMillis() - startTime) / 1000.0);
  }

  /**
   * Cancels any existing kill task and optionally schedules a new one to run
   * <code>keepAliveMs</code> from now.
   * 
   * @param keepAliveMs if > 0, schedules a new kill task to run in keepAliveMs
   *          milliseconds; if <= 0, a new kill task is not scheduled.
   */
  private void schedule(long keepAliveMs) {
    if (killTask != null) {
      killTask.cancel();
      killTask = null;
    }
    if (keepAliveMs > 0) {
      killTask = new KillTask();
      timer.schedule(killTask, keepAliveMs);
    }
  }
}
