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
package com.google.gwt.dev.shell;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.HelpInfo;
import com.google.gwt.dev.shell.CheckForUpdates.UpdateResult;

import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Performs platform-specific class selection.
 */
public class PlatformSpecific {

  /**
   * All of these classes must extend CheckForUpdates. Note that currently only
   * IE has a custom implementation (to handle proxies) and that CheckForUpdates
   * must be the last one in the list.
   */
  private static final String[] updaterClassNames = new String[] {
      "com.google.gwt.dev.shell.ie.CheckForUpdatesIE6",
      "com.google.gwt.dev.shell.CheckForUpdates"};

  public static FutureTask<UpdateResult> checkForUpdatesInBackgroundThread(
      final TreeLogger logger, final long minCheckMillis) {
    final String entryPoint = PlatformSpecific.computeEntryPoint();
    FutureTask<UpdateResult> task = new FutureTask<UpdateResult>(
        new Callable<UpdateResult>() {
          public UpdateResult call() throws Exception {
            final CheckForUpdates updateChecker = createUpdateChecker(logger,
                entryPoint);
            return updateChecker == null ? null
                : updateChecker.check(minCheckMillis);
          }
        });
    Thread checkerThread = new Thread(task, "GWT Update Checker");
    checkerThread.setDaemon(true);
    checkerThread.start();
    return task;
  }

  /**
   * Find the first method named "main" on the call stack and use its class as
   * the entry point.
   */
  public static String computeEntryPoint() {
    Throwable t = new Throwable();
    for (StackTraceElement stackTrace : t.getStackTrace()) {
      if (stackTrace.getMethodName().equals("main")) {
        // Strip package name from main's class
        String className = stackTrace.getClassName();
        int i = className.lastIndexOf('.');
        if (i >= 0) {
          return className.substring(i + 1);
        }
        return className;
      }
    }
    return null;
  }

  public static CheckForUpdates createUpdateChecker(TreeLogger logger) {
    return createUpdateChecker(logger, computeEntryPoint());
  }

  public static CheckForUpdates createUpdateChecker(TreeLogger logger,
      String entryPoint) {
    try {
      for (int i = 0; i < updaterClassNames.length; i++) {
        try {
          Class<? extends CheckForUpdates> clazz = Class.forName(
              updaterClassNames[i]).asSubclass(CheckForUpdates.class);
          Constructor<? extends CheckForUpdates> ctor = clazz.getDeclaredConstructor(new Class[] {
              TreeLogger.class, String.class});
          CheckForUpdates checker = ctor.newInstance(new Object[] {
              logger, entryPoint});
          return checker;
        } catch (Exception e) {
          // Other exceptions can occur besides ClassNotFoundException,
          // so ignore them all so we can find a functional updater.
        }
      }
    } catch (Throwable e) {
      // silently ignore any errors
    }
    return null;
  }

  public static void logUpdateAvailable(TreeLogger logger,
      FutureTask<UpdateResult> updater) {
    if (updater != null && updater.isDone()) {
      UpdateResult result = null;
      try {
        result = updater.get(0, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        // Silently ignore exception
      } catch (ExecutionException e) {
        // Silently ignore exception
      } catch (TimeoutException e) {
        // Silently ignore exception
      }
      logUpdateAvailable(logger, result);
    }
  }

  public static void logUpdateAvailable(TreeLogger logger, UpdateResult result) {
    if (result != null) {
      final URL url = result.getURL();
      logger.log(TreeLogger.WARN, "A new version of GWT ("
          + result.getNewVersion() + ") is available", null, new HelpInfo() {
        @Override
        public URL getURL() {
          return url;
        }
      });
    }
  }
}
