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

import java.util.HashSet;
import java.util.Set;

/**
 * Runs in Production Mode via browsers managed as an external process. This
 * feature is experimental and is not officially supported.
 */
class RunStyleExternalBrowser extends RunStyle {

  private static class ExternalBrowser {
    String browserPath;
    Process process;

    public ExternalBrowser(String browserPath) {
      this.browserPath = browserPath;
    }

    public String getPath() {
      return browserPath;
    }

    public Process getProcess() {
      return process;
    }

    public void setProcess(Process process) {
      this.process = process;
    }
  }

  /**
   * Registered as a shutdown hook to make sure that any browsers that were not
   * finished are killed.
   */
  private class ShutdownCb extends Thread {

    @Override
    public void run() {
      for (ExternalBrowser browser : externalBrowsers) {
        if (browser.getProcess() != null) {
          try {
            browser.getProcess().exitValue();
          } catch (IllegalThreadStateException e) {
            // The process is still active. Kill it.
            browser.getProcess().destroy();
          }
        }
      }
    }
  }

  private ExternalBrowser[] externalBrowsers;

  /**
   * @param shell the containing shell
   */
  public RunStyleExternalBrowser(JUnitShell shell) {
    super(shell);
  }

  @Override
  public String[] getInterruptedHosts() {
    // Make sure all browsers are still running
    Set<String> toRet = null;
    for (ExternalBrowser browser : externalBrowsers) {
      try {
        browser.getProcess().exitValue();

        // The host exited, so return its path.
        if (toRet == null) {
          toRet = new HashSet<String>();
        }
        toRet.add(browser.getPath());
      } catch (IllegalThreadStateException e) {
        // The process is still active, keep looking.
      }
    }
    return toRet == null ? null : toRet.toArray(new String[toRet.size()]);
  }

  @Override
  public int initialize(String args) {
    if (args == null || args.length() == 0) {
      getLogger().log(
          TreeLogger.ERROR,
          "ExternalBrowser runstyle requires an "
              + "argument listing one or more executables of external browsers to "
              + "launch");
      return -1;
    }
    String browsers[] = args.split(",");
    synchronized (this) {
      this.externalBrowsers = new ExternalBrowser[browsers.length];
      for (int i = 0; i < browsers.length; ++i) {
        externalBrowsers[i] = new ExternalBrowser(browsers[i]);
      }
    }
    Runtime.getRuntime().addShutdownHook(new ShutdownCb());
    return browsers.length;
  }

  @Override
  public synchronized void launchModule(String moduleName)
      throws UnableToCompleteException {
    String commandArray[] = new String[2];
    // construct the URL for the browser to hit
    commandArray[1] = shell.getModuleUrl(moduleName);

    Process child = null;
    for (ExternalBrowser browser : externalBrowsers) {
      try {
        commandArray[0] = browser.getPath();

        child = Runtime.getRuntime().exec(commandArray);
        if (child == null) {
          getLogger().log(TreeLogger.ERROR,
              "Problem exec()'ing " + commandArray[0]);
          throw new UnableToCompleteException();
        }
      } catch (Exception e) {
        getLogger().log(TreeLogger.ERROR,
            "Error launching external browser at " + browser.getPath(), e);
        throw new UnableToCompleteException();
      }
      browser.setProcess(child);
    }
  }
}
