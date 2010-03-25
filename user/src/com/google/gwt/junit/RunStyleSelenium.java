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

import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;
import com.thoughtworks.selenium.SeleniumException;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs via browsers managed by Selenium.
 */
public class RunStyleSelenium extends RunStyle {
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
   * The list of hosts that were interrupted. Protected by {@link #lock}.
   */
  private Set<String> interruptedHosts;

  /**
   * Indicates that testing has stopped, and we no longer need to run keep
   * alive checks. Protected by {@link #lock}.
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
        synchronized (lock) {
          stopped = true;
          for (SeleniumWrapper remote : remotes) {
            if (remote.getSelenium() != null) {
              try {
                remote.getSelenium().stop();
              } catch (SeleniumException se) {
                shell.getTopLogger().log(TreeLogger.WARN,
                    "Error stopping selenium session", se);
              }
            }
          }
        }
      }
    });
    start();
    return targets.length;
  }

  @Override
  public void launchModule(String moduleName) {
    // Get the localhost address.
    String domain = "http://" + getLocalHostName() + ":" + shell.getPort()
        + "/";

    // Startup all the selenia and point them at the module url.
    synchronized (lock) {
      for (SeleniumWrapper remote : remotes) {
        try {
          String url = shell.getModuleUrl(moduleName);
          shell.getTopLogger().log(TreeLogger.TRACE,
              "Starting with domain: " + domain + " Opening URL: " + url);
          remote.createSelenium(domain);
          remote.getSelenium().start();
          remote.getSelenium().open(url);
        } catch (Exception e) {
          shell.getTopLogger().log(
              TreeLogger.ERROR,
              "Error launching browser via Selenium-RC at "
                  + remote.getSpecifier(), e);
        }
      }
    }
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
            if (message != null
                && message.toLowerCase().contains("permission denied")) {
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
}
