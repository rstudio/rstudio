/*
 * Copyright 2010 Google Inc.
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

package com.google.gwt.logging.impl;

import com.google.gwt.core.client.GWT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * This class is used by the byte code rewriting which happens in DevMode only.
 * We rewrite all calls that create and access loggers by name to include a
 * magic, thread specific prefix, which creates a separate tree of loggers for
 * each thread in DevMode. This works around the issue where DevMode root
 * loggers are shared between threads and between client/server. These functions
 * will never be used in Production compiles.
 *
 * TODO(unnurg): Move this class into gwt-dev
 *
 * @see UseMirroredClasses
 */
public class DevModeLoggingFixes {
  private static Logger root = null;

  /**
   * Replaces all logRecord.getLoggerName() calls, removing a thread specific
   * prefix which is appended to all logger names in dev mode in order to
   * maintain a pseudo tree of loggers for each thread.
   */
  public static String getLoggerName(LogRecord record) {
    if (record.getLoggerName() != null) {
      return removeLoggerPrefix(record.getLoggerName());
    }
    return null;
  }

  /**
   * Replaces all logger.getName() calls, removing a thread specific prefix
   * which is appended to all logger names in dev mode in order to maintain
   * a pseudo tree of loggers for each thread.
   */
  public static String getName(Logger logger) {
    if (logger.getName() != null) {
      return removeLoggerPrefix(logger.getName());
    }
    return null;
  }

  /**
   * Replaces all Logger.getLogger(name) calls, adding a thread specific prefix
   * which is appended to all logger names in dev mode in order to maintain
   * a pseudo tree of loggers for each thread.
   */
  public static Logger loggerGetLogger(String name) {
    // If a library adds Loggers, but does not include the LogConfiguration
    // EntryPoint, then the separate root logger does not get created, and set
    // to ignore it's parent. In order to ensure that this happens, we do it
    // again here.
    if (root == null) {
      root = Logger.getLogger(getPrefixName());
      root.setUseParentHandlers(false);
    }
    return Logger.getLogger(addLoggerPrefix(name));
  }

  /**
   * Replaces all LogManager.getLogger(name) calls, adding a thread specific
   * prefix which is appended to all logger names in dev mode in order to
   * maintain a pseudo tree of loggers for each thread.
   */
  public static Logger logManagerGetLogger(LogManager manager, String name) {
    return manager.getLogger(addLoggerPrefix(name));
  }

  /**
   * Replaces all LogManager.getLoggerNames() calls, deleting the thread specific
   * prefix which is appended to all logger names in dev mode in order to
   * maintain a pseudo tree of loggers for each thread.  Also deletes all logger
   * names that do not start with the prefix since those belong to the server
   * and should not be returned in this function.
   */
  public static Enumeration<String> logManagerGetLoggerNames(LogManager manager) {
    Enumeration<String> loggerList = manager.getLoggerNames();
    List<String> newList = new ArrayList<String>();
    while (loggerList.hasMoreElements()) {
      String name = loggerList.nextElement();
      if (startsWithLoggerPrefix(name)) {
        newList.add(removeLoggerPrefix(name));
      }
    }
    return Collections.enumeration(newList);
  }

  private static String addLoggerPrefix(String name) {
    return getLoggerPrefix() + name;
  }

  private static String getLoggerPrefix() {
    return getPrefixName() + ".";
  }

  private static String getPrefixName() {
    return GWT.getUniqueThreadId();
  }

  private static String removeLoggerPrefix(String name) {
    return name.replaceFirst("^" + getLoggerPrefix(), "");
  }

  private static boolean startsWithLoggerPrefix(String name) {
    return name.startsWith(getLoggerPrefix());
  }
}
