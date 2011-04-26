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
package com.google.gwt.junit;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs via browsers managed by Selenium.
 */
public class RunStyleSelenium extends RunStyle {

  /**
   * The maximum amount of time that a selenia can take to start in
   * milliseconds. 10 minutes.
   */
  private static final int LAUNCH_TIMEOUT = 10 * 60 * 1000;

  /**
   * Wraps a Selenium instance.
   */
  protected static interface SeleniumWrapper {
    void createSelenium(String domain);

    Selenium getSelenium();

    String getSpecifier();
  }

  /**
   * Implements SeleniumWrapper using DefaultSelenium. Visible for testing.
   */
  static class RCSelenium implements SeleniumWrapper {

    private static final Pattern PATTERN = Pattern.compile("([\\w\\.-]+):([\\d]+)/(.+)");

    /*
     * Visible for testing.
     */
    String browser;
    String host;
    int port;

    private Selenium selenium;
    private final String specifier;

    public RCSelenium(String specifier) {
      this.specifier = specifier;
      parseSpecifier();
    }

    public void createSelenium(String domain) {
      this.selenium = new DefaultSelenium(host, port, browser, domain);
    }

    public Selenium getSelenium() {
      return selenium;
    }

    public String getSpecifier() {
      return specifier;
    }

    private void parseSpecifier() {
      Matcher matcher = PATTERN.matcher(specifier);
      if (!matcher.matches()) {
        throw new IllegalArgumentException("Unable to parse Selenium target "
            + specifier + " (expected format is [host]:[port]/[browser])");
      }
      this.browser = matcher.group(3);
      this.host = matcher.group(1);
      this.port = Integer.parseInt(matcher.group(2));
    }
  }

  /**
   * A {@link Thread} used to interact with {@link Selenium} instances. Selenium
   * does not support execution of multiple methods at the same time, so its
   * important to make sure that {@link SeleniumThread#isComplete()} returns
   * true before calling more methods in {@link Selenium}.
   */
  class SeleniumThread extends Thread {

    /**
     * {@link RunStyleSelenium#lock} is sometimes active when calling
     * {@link #isComplete()}, so we need a separate lock to avoid deadlock.
     */
    Object accessLock = new Object();

    /**
     * The exception thrown while running this thread, if any.
     */
    private Throwable exception;

    /**
     * True if the selenia has successfully completed the action. Protected by
     * {@link #accessLock}.
     */
    private boolean isComplete;

    private final SeleniumWrapper remote;

    /**
     * Construct a new {@link SeleniumThread}.
     * 
     * @param remote the {@link SeleniumWrapper} instance
     */
    public SeleniumThread(SeleniumWrapper remote) {
      this.remote = remote;
      setDaemon(true);
    }

    /**
     * Get the {@link Throwable} caused by the action.
     * 
     * @return the exception if one occurred, null if none occurred
     */
    public Throwable getException() {
      synchronized (accessLock) {
        return exception;
      }
    }

    public SeleniumWrapper getRemote() {
      return remote;
    }

    public boolean isComplete() {
      synchronized (accessLock) {
        return isComplete;
      }
    }

    protected void markComplete() {
      synchronized (accessLock) {
        isComplete = true;
      }
    }

    protected void setException(Throwable e) {
      synchronized (accessLock) {
        this.exception = e;
        isComplete = true;
      }
    }
  }

  /**
   * <p>
   * The {@link Thread} used to launch a module on a single Selenium target. We
   * launch {@link Selenium} instances in a separate thread because
   * {@link Selenium#start()} can hang if the browser cannot be opened
   * successfully. Instead of blocking the test indefinitely, we use a separate
   * thread and timeout if needed.
   * </p>
   * <p>
   * We wait until {@link LaunchThread#isComplete()} returns <code>true</code>
   * before starting the keep alive thread or creating a {@link StopThread}, so
   * no other thread can be accessing {@link Selenium} at the same time.
   * </p>
   */
  class LaunchThread extends SeleniumThread {

    private final String moduleName;

    /**
     * Construct a new {@link LaunchThread}.
     * 
     * @param remote the remote {@link SeleniumWrapper} instance
     * @param moduleName the module to load
     */
    public LaunchThread(SeleniumWrapper remote, String moduleName) {
      super(remote);
      this.moduleName = moduleName;
    }

    @Override
    public void run() {
      SeleniumWrapper remote = getRemote();
      try {
        String domain = "http://" + getLocalHostName() + ":" + shell.getPort()
            + "/";
        String url = shell.getModuleUrl(moduleName);

        // Create the selenium instance and open the browser.
        if (shell.getTopLogger().isLoggable(TreeLogger.TRACE)) {
          shell.getTopLogger().log(TreeLogger.TRACE,
              "Starting with domain: " + domain + " Opening URL: " + url);
        }
        remote.createSelenium(domain);
        remote.getSelenium().start();

        // We set the speed to 1000ms as a workaround a bug where Selenium#open
        // can hang.
        remote.getSelenium().setSpeed("1000");
        remote.getSelenium().open(url);
        remote.getSelenium().setSpeed("0");

        markComplete();
      } catch (Throwable e) {
        shell.getTopLogger().log(
            TreeLogger.ERROR,
            "Error launching browser via Selenium-RC at "
                + remote.getSpecifier(), e);
        setException(e);
      }
    }
  }

  /**
   * <p>
   * The {@link Thread} used to stop a selenium instance.
   * </p>
   * <p>
   * We stop the keep alive thread before creating {@link StopThread}s, and we
   * do not create {@link StopThread}s if a {@link LaunchThread} is still
   * running for a {@link Selenium} instance, so no other thread can possible be
   * accessing {@link Selenium} at the same time.
   * </p>
   */
  class StopThread extends SeleniumThread {

    public StopThread(SeleniumWrapper remote) {
      super(remote);
    }

    @Override
    public void run() {
      SeleniumWrapper remote = getRemote();
      try {
        remote.getSelenium().stop();
        markComplete();
      } catch (Throwable e) {
        shell.getTopLogger().log(TreeLogger.WARN,
            "Error stopping selenium session at " + remote.getSpecifier(), e);
        setException(e);
      }
    }
  }

  /**
   * The list of hosts that were interrupted. Protected by {@link #lock}.
   */
  private Set<String> interruptedHosts;

  /**
   * We keep a list of {@link LaunchThread} instances so that we know which
   * selenia successfully started. Only selenia that have been successfully
   * started should be stopped when the test is finished. Protected by
   * {@link #lock};
   */
  private List<LaunchThread> launchThreads = new ArrayList<LaunchThread>();

  /**
   * Indicates that testing has stopped, and we no longer need to run keep alive
   * checks. Protected by {@link #lock}.
   */
  private boolean stopped;

  private SeleniumWrapper remotes[];

  /**
   * A separate lock to control access to {@link Selenium}, {@link #stopped},
   * {@link #remotes}, and {@link #interruptedHosts}. This ensures that the
   * keepAlive thread doesn't call getTitle after the shutdown thread calls
   * {@link Selenium#stop()}.
   */
  private final Object lock = new Object();

  public RunStyleSelenium(final JUnitShell shell) {
    super(shell);
  }

  @Override
  public String[] getInterruptedHosts() {
    synchronized (lock) {
      if (interruptedHosts == null) {
        return null;
      }
      return interruptedHosts.toArray(new String[interruptedHosts.size()]);
    }
  }

  @Override
  public int initialize(String args) {
    if (args == null || args.length() == 0) {
      getLogger().log(TreeLogger.ERROR,
          "Selenium runstyle requires comma-separated Selenium-RC targets");
      return -1;
    }
    String[] targetsIn = args.split(",");
    SeleniumWrapper targets[] = new SeleniumWrapper[targetsIn.length];

    for (int i = 0; i < targets.length; ++i) {
      try {
        targets[i] = createSeleniumWrapper(targetsIn[i]);
      } catch (IllegalArgumentException e) {
        getLogger().log(TreeLogger.ERROR, e.getMessage());
        return -1;
      }
    }

    // We don't need a lock at this point because we haven't started the keep-
    // alive thread.
    this.remotes = targets;

    // Install a shutdown hook that will close all of our outstanding Selenium
    // sessions. The hook is only executed if the JVM is exited normally. If the
    // process is terminated, the shutdown hook will not run, which leaves
    // browser instances open on the Selenium server. We'll need to modify
    // Selenium Server to do its own cleanup after a timeout.
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        List<StopThread> stopThreads = new ArrayList<StopThread>();
        synchronized (lock) {
          stopped = true;
          for (LaunchThread launchThread : launchThreads) {
            // Closing selenium instances that have not successfully started
            // results in an error on the selenium client. By doing this check,
            // we are ensuring that no other calls to the remote instance are
            // being done by another thread.
            if (launchThread.isComplete()) {
              StopThread stopThread = new StopThread(launchThread.getRemote());
              stopThreads.add(stopThread);
              stopThread.start();
            }
          }
        }

        // Wait for all threads to stop.
        try {
          waitForThreadsToComplete(stopThreads, false, "stop", 500);
        } catch (UnableToCompleteException e) {
          // This should never happen.
        }
      }
    });
    return targets.length;
  }

  @Override
  public void launchModule(String moduleName) throws UnableToCompleteException {
    // Startup all the selenia and point them at the module url.
    for (SeleniumWrapper remote : remotes) {
      LaunchThread thread = new LaunchThread(remote, moduleName);
      synchronized (lock) {
        launchThreads.add(thread);
      }
      thread.start();
    }

    // Wait for all selenium targets to start.
    waitForThreadsToComplete(launchThreads, true, "start", 1000);

    // Check if any threads have thrown an exception. We wait until all threads
    // have had a change to start so that we don't shutdown while some threads
    // are still starting.
    synchronized (lock) {
      for (LaunchThread thread : launchThreads) {
        if (thread.getException() != null) {
          // The thread has already logged the exception.
          throw new UnableToCompleteException();
        }
      }
    }

    // Start the keep alive thread.
    start();
  }

  /**
   * Factory method for {@link SeleniumWrapper}.
   * 
   * @param seleniumSpecifier Specifies the Selenium instance to create
   * @return an instance of {@link SeleniumWrapper}
   */
  protected SeleniumWrapper createSeleniumWrapper(String seleniumSpecifier) {
    return new RCSelenium(seleniumSpecifier);
  }

  /**
   * Create the keep-alive thread.
   */
  protected void start() {
    // This will periodically check for failure of the Selenium session and stop
    // the test if something goes wrong.
    Thread keepAliveThread = new Thread() {
      @Override
      public void run() {
        do {
          try {
            Thread.sleep(1000);
          } catch (InterruptedException ignored) {
            break;
          }
        } while (doKeepAlives());
      }
    };
    keepAliveThread.setDaemon(true);
    keepAliveThread.start();
  }

  private boolean doKeepAlives() {
    synchronized (lock) {
      if (remotes != null) {
        // If the shutdown thread has already executed, then we can stop this
        // thread.
        if (stopped) {
          return false;
        }

        for (SeleniumWrapper remote : remotes) {
          // Use getTitle() as a cheap way to see if the Selenium server's still
          // responding (Selenium seems to provide no way to check the server
          // status directly).
          try {
            if (remote.getSelenium() != null) {
              remote.getSelenium().getTitle();
            }
          } catch (Throwable e) {
            // If we ask for the title of the page while a new module is
            // loading, IE will throw a permission denied exception.
            String message = e.getMessage();
            if (message == null
                || !message.toLowerCase().contains("permission denied")) {
              if (interruptedHosts == null) {
                interruptedHosts = new HashSet<String>();
              }
              interruptedHosts.add(remote.getSpecifier());
            }
          }
        }
      }
      return interruptedHosts == null;
    }
  }

  /**
   * Get the display list of specifiers for threads that did not complete.
   * 
   * @param threads the list of threads
   * @return a list of specifiers
   */
  private <T extends SeleniumThread> String getIncompleteSpecifierList(
      List<T> threads) {
    String list = "";
    for (SeleniumThread thread : threads) {
      if (!thread.isComplete()) {
        list += "  " + thread.getRemote().getSpecifier() + "\n";
      }
    }
    return list;
  }

  /**
   * Iterate over a list of {@link SeleniumThread}s, waiting for them to finish.
   * 
   * @param <T> the thread type
   * @param threads the list of threads
   * @param fatalExceptions true to treat all exceptions as errors, false to
   *          treat exceptions as warnings
   * @param action the action being performed by the thread
   * @param sleepTime the amount of time to sleep in milliseconds
   * @throws UnableToCompleteException if the thread times out and
   *           fatalExceptions is true
   */
  private <T extends SeleniumThread> void waitForThreadsToComplete(
      List<T> threads, boolean fatalExceptions, String action, int sleepTime)
      throws UnableToCompleteException {
    boolean allComplete;
    long endTime = System.currentTimeMillis() + LAUNCH_TIMEOUT;
    do {
      try {
        Thread.sleep(sleepTime);
      } catch (InterruptedException e) {
        // This should not happen.
        throw new UnableToCompleteException();
      }

      allComplete = true;
      synchronized (lock) {
        for (SeleniumThread thread : threads) {
          if (!thread.isComplete()) {
            allComplete = false;
          }
        }
      }

      // Check if we have timed out.
      if (!allComplete && endTime < System.currentTimeMillis()) {
        allComplete = true;
        String message = "The following Selenium instances did not " + action
            + " within " + LAUNCH_TIMEOUT + "ms:\n";
        synchronized (lock) {
          message += getIncompleteSpecifierList(threads);
        }
        if (fatalExceptions) {
          shell.getTopLogger().log(TreeLogger.ERROR, message);
          throw new UnableToCompleteException();
        } else {
          shell.getTopLogger().log(TreeLogger.WARN, message);
        }
      }
    } while (!allComplete);
  }
}
