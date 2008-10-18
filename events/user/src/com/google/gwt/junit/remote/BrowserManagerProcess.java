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
   * {@link BrowserManagerProcess#killTask}.
   */
  private final class KillTask extends TimerTask {
    @Override
    public void run() {
      synchronized (BrowserManagerProcess.this) {
        /*
         * Verify we're still the active KillTask! If we're not the active
         * killTask, it means we've been rescheduled and a newer kill timer is
         * active.
         */
        if (killTask == this && !deadOrDying) {
          logger.info("Timeout expired for: " + token);
          process.destroy();
          deadOrDying = true;
        }
      }
    }
  }

  private static final Logger logger = Logger.getLogger(BrowserManagerProcess.class.getName());

  /**
   * Compute elapsed time.
   * 
   * @param startTime the time the process started
   * @return returns a string representing the number of seconds elapsed since
   *         the process started.
   */
  private static String getElapsed(long intervalMs) {
    NumberFormat nf = NumberFormat.getNumberInstance();
    nf.setMaximumFractionDigits(3);
    return nf.format(intervalMs / 1000.0);
  }

  /**
   * Set to 'true' when the process exits or starts being killed.
   */
  private boolean deadOrDying = false;

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
   * Timer instance passed in from BrowserManagerServer.
   */
  private final Timer timer;

  /**
   * The key associated with <code>process</code>.
   */
  private final int token;

  /**
   * Constructs a new ProcessManager for the specified process.
   * 
   * @param timer timer passed in from BrowserManagerServer instance.
   * @param token the key to be used to identify this process.
   * @param process the process being managed
   * @param initKeepAliveMs the initial time to wait before killing
   *          <code>process</code>
   */
  public BrowserManagerProcess(final ProcessExitCb cb, Timer timer,
      final int token, final Process process, long initKeepAliveMs) {
    this.process = process;
    this.timer = timer;
    this.token = token;

    final long startTime = System.currentTimeMillis();
    Thread cleanupThread = new Thread() {
      @Override
      public void run() {
        while (true) {
          try {
            int exitValue = process.waitFor();
            doCleanup(cb, exitValue, token, System.currentTimeMillis()
                - startTime);
            return;
          } catch (InterruptedException e) {
            logger.log(Level.WARNING,
                "Interrupted waiting for process exit of: " + token, e);
          }
        }
      }
    };

    cleanupThread.setDaemon(true);
    cleanupThread.setName("Browser-" + token + "-Wait");
    cleanupThread.start();
    keepAlive(initKeepAliveMs);
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
  public synchronized boolean keepAlive(long keepAliveMs) {
    assert (keepAliveMs > 0);
    if (!deadOrDying) {
      killTask = new KillTask();
      timer.schedule(killTask, keepAliveMs);
      return true;
    }
    return false;
  }

  /**
   * Kills the underlying browser process.
   */
  public synchronized void killBrowser() {
    if (!deadOrDying) {
      process.destroy();
      deadOrDying = true;
    }
  }

  /**
   * Cleans up when the underlying process terminates. The lock must not be held
   * when calling this method or deadlock could result.
   * 
   * @param cb the callback to fire
   * @param exitValue the exit value of the process
   * @param token the id of this browser instance
   * @param startTime the time the process started
   */
  private void doCleanup(ProcessExitCb cb, int exitValue, int token,
      long intervalMs) {
    synchronized (this) {
      deadOrDying = true;
    }
    if (exitValue != 0) {
      logger.warning("Browser: " + token + " exited with bad status: "
          + exitValue);
    } else {
      logger.info("Browser: " + token + " process exited normally after "
          + getElapsed(intervalMs) + "s");
    }
    cb.childExited(token, exitValue);
  }
}
