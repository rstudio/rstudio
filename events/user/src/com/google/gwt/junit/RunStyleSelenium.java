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
import com.thoughtworks.selenium.SeleniumException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs in web mode via browsers managed by Selenium.
 */
public class RunStyleSelenium extends RunStyleRemote {

  private static class RCSelenium {
    final String browser;
    final String host;
    final int port;
    Selenium selenium;

    public RCSelenium(String browser, String host, int port) {
      this.browser = browser;
      this.host = host;
      this.port = port;
    }

    public void createSelenium(String domain) {
      this.selenium = new DefaultSelenium(host, port, browser, domain);
    }

    public String getBrowser() {
      return browser;
    }

    public String getHost() {
      return host;
    }

    public int getPort() {
      return port;
    }

    public Selenium getSelenium() {
      return selenium;
    }
  }

  public static RunStyle create(JUnitShell shell, String[] targetsIn) {
    RCSelenium targets[] = new RCSelenium[targetsIn.length];

    Pattern pattern = Pattern.compile("([\\w\\.-]+):([\\d]+)/([\\w\\s\\*]+)");
    for (int i = 0; i < targets.length; ++i) {
      Matcher matcher = pattern.matcher(targetsIn[i]);
      if (!matcher.matches()) {
        throw new JUnitFatalLaunchException("Unable to parse Selenium target "
            + targetsIn[i] + " (expected format is [host]:[port]/[browser])");
      }
      RCSelenium instance = new RCSelenium(matcher.group(3), matcher.group(1),
          Integer.parseInt(matcher.group(2)));
      targets[i] = instance;
    }

    return new RunStyleSelenium(shell, targets);
  }

  private RCSelenium remotes[];

  /**
   * Whether one of the remote browsers was interrupted.
   */
  private boolean wasInterrupted;

  /**
   * A separate lock to control access to {@link #wasInterrupted}. This keeps
   * the main thread calls into {@link #wasInterrupted()} from having to be
   * synchronized on the containing instance and potentially block on RPC calls.
   * It is okay to take the {@link #wasInterruptedLock} while locking the
   * containing instance; it is NOT okay to do the opposite or deadlock could
   * occur.
   */
  private final Object wasInterruptedLock = new Object();

  public RunStyleSelenium(final JUnitShell shell, RCSelenium targets[]) {

    super(shell);
    this.remotes = targets;

    // Install a shutdown hook that will close all of our outstanding Selenium
    // sessions.
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        for (RCSelenium remote : remotes) {
          if (remote.getSelenium() != null) {
            try {
              remote.getSelenium().stop();
            } catch (SeleniumException se) {
              shell.getTopLogger().log(TreeLogger.WARN,
                  "Error stoping selenium session", se);
            }
          }
        }
      }
    });

    // Crank up the keep-alive thread. This will periodically check for failure
    // of the Selenium session and stop the test if something goes wrong.
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

  @Override
  public synchronized void launchModule(String moduleName)
      throws UnableToCompleteException {
    // Get the localhost address.
    String domain;
    try {
      String localhost = InetAddress.getLocalHost().getHostAddress();
      domain = "http://" + localhost + ":" + shell.getPort() + "/";
    } catch (UnknownHostException e) {
      throw new RuntimeException("Unable to determine my ip address", e);
    }

    // Startup all the selenia and point them at the module url.
    for (RCSelenium remote : remotes) {
      try {
        shell.getTopLogger().log(TreeLogger.TRACE,
            "Starting with domain: " + domain 
            + " Opening URL: " + getMyUrl(moduleName)); 
        remote.createSelenium(domain);
        remote.getSelenium().start();
        remote.getSelenium().open(getMyUrl(moduleName));
      } catch (Exception e) {
        shell.getTopLogger().log(TreeLogger.ERROR,
            "Error launching browser via Selenium-RC at " + remote.getHost(), e);
      }
    }
  }

  @Override
  public boolean wasInterrupted() {
    synchronized (wasInterruptedLock) {
      return wasInterrupted;
    }
  }

  private synchronized boolean doKeepAlives() {
    if (remotes != null) {
      for (RCSelenium remote : remotes) {
        // Use getTitle() as a cheap way to see if the Selenium server's still
        // responding (Selenium seems to provide no way to check the server
        // status directly).
        try {
          if (remote.getSelenium() != null) {
            remote.getSelenium().getTitle();
          }
        } catch (Throwable e) {
          setWasInterrupted(true);
        }
      }
    }

    return !wasInterrupted();
  }

  private void setWasInterrupted(boolean wasInterrupted) {
    synchronized (wasInterruptedLock) {
      this.wasInterrupted = wasInterrupted;
    }
  }
}
