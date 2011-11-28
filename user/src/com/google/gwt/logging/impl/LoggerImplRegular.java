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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Implementation for the Logger class when logging is enabled.
 */
public class LoggerImplRegular implements LoggerImpl {
  private List<Handler> handlers;
  private Level level = null;
  private String name;
  private Logger parent;  // Should never be null except in the RootLogger
  private boolean useParentHandlers;

  public LoggerImplRegular() {
    this.useParentHandlers = true;
    handlers = new ArrayList<Handler>();
  }

  public void addHandler(Handler handler) {
    handlers.add(handler);
  }

  public void config(String msg) {
    log(Level.CONFIG, msg);
  }

  public void fine(String msg) {
    log(Level.FINE, msg);
  }

  public void finer(String msg) {
    log(Level.FINER, msg);
  }

  public void finest(String msg) {
    log(Level.FINEST, msg);
  }

  public Handler[] getHandlers() {
    return handlers.toArray(new Handler[handlers.size()]);
  }

  public Level getLevel() {
    return level;
  }

  public Logger getLoggerHelper(String name) {
    // Ideally, we'd just return LogManager.getLogManager.getOrAddLogger(name)
    // here, since the code is basically the same, except that code gets to call
    // addLoggerWithoutDuplicationChecking, which makes it somewhat more
    // efficient. However, that means adding a public method to LogManager which
    // is not in the API which is frowned upon.
    LogManager manager = LogManager.getLogManager();
    Logger logger = manager.getLogger(name);
    if (logger == null) {
      Logger newLogger = new LoggerWithExposedConstructor(name);
      manager.addLogger(newLogger);
      return newLogger;
    }
    return logger;
  }

  public String getName() {
    return name;
  }

  public Logger getParent() {
    return parent;
  }

  public boolean getUseParentHandlers() {
    return useParentHandlers;
  }

  public void info(String msg) {
    log(Level.INFO, msg);
  }

  public boolean isLoggable(Level messageLevel) {
    return getEffectiveLevel().intValue() <= messageLevel.intValue();
  }

  public void log(Level level, String msg) {
    log(level, msg, null);
  }

  public void log(Level level, String msg, Throwable thrown) {
    if (isLoggable(level)) {
      LogRecord record = new LogRecord(level, msg);
      record.setThrown(thrown);
      record.setLoggerName(getName());
      log(record);
    }
  }

  public void log(LogRecord record) {
    if (isLoggable(record.getLevel())) {
      for (Handler handler : getHandlers()) {
        handler.publish(record);
      }
      Logger logger = getUseParentHandlers() ? getParent() : null;
      while (logger != null) {
        for (Handler handler : logger.getHandlers()) {
          handler.publish(record);
        }
        logger = logger.getUseParentHandlers() ? logger.getParent() : null;
      }
    }
  }

  public void removeHandler(Handler handler) {
    handlers.remove(handler);
  }

  public void setLevel(Level newLevel) {
    level = newLevel;
  }

  public void setName(String newName) {
    name = newName;
  }

  public void setParent(Logger newParent) {
    if (newParent != null) {
      parent = newParent;
    }
  }

  public void setUseParentHandlers(boolean newUseParentHandlers) {
    useParentHandlers = newUseParentHandlers;
  }

  public void severe(String msg) {
    log(Level.SEVERE, msg);
  }

  public void warning(String msg) {
    log(Level.WARNING, msg);
  }

  private Level getEffectiveLevel() {
    if (level != null) {
      return level;
    }
    Logger logger = getParent();
    while (logger != null) {
      Level effectiveLevel = logger.getLevel();
      if (effectiveLevel != null) {
        return effectiveLevel;
      }
      logger = logger.getParent();
    }
    return Level.INFO;
  }
}
