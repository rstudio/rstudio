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

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Implementation for the LogManager class when logging is enabled.
 */
public class LogManagerImplRegular {
  
  /** 
   * Since the Impl class is in a different package than the LogManager class,
   * we need to work around the fact that the Impl class cannot access the
   * protected constructor.
   */
  private static class LogManagerWithExposedConstructor extends LogManager {
    public LogManagerWithExposedConstructor() {
      super();
    }
  }
  
  /** 
   * Since the Logger constructor is protected, the LogManager cannot create
   * one directly, so we create a RootLogger which has an exposed constructor.
   */
  private class RootLogger extends Logger {
    public RootLogger() {
      super("", null);
      setLevel(Level.ALL);
    }
  }

  private static LogManager singleton;
  
  public static LogManager getLogManager() {
    if (singleton == null) {
      singleton = new LogManagerWithExposedConstructor();
    }
    return singleton;
  }
  
  private HashMap<String, Logger> loggerList;
  private Logger rootLogger;
  
  public LogManagerImplRegular() {
    loggerList = new HashMap<String, Logger>();
    rootLogger = new RootLogger();
    loggerList.put("", rootLogger);
  }
  
  public boolean addLogger(Logger logger) {
    if (getLogger(logger.getName()) != null) {
      return false;
    }
    addLoggerWithoutDuplicationChecking(logger);
    return true;
  }

  public Logger getLogger(String name) {
    return loggerList.get(name);
  }
  
  /**
   *  Helper function to add a logger when we have already determined that it
   *  does not exist.  When we add a logger, we recursively add all of it's
   *  ancestors. Since loggers do not get removed, logger creation is cheap, 
   *  and there are not usually too many loggers in an ancestry chain,
   *  this is a simple way to ensure that the parent/child relationships are
   *  always correctly set up.
   */
  private void addLoggerWithoutDuplicationChecking(Logger logger) {
    String name = logger.getName();
    String parentName = name.substring(0, Math.max(0, name.lastIndexOf('.')));
    Logger parent = getOrAddLogger(parentName);
    loggerList.put(logger.getName(), logger);
    logger.setParent(parent);
  }
  
  /**
   *  Helper function to create a logger if it does not exist since the public
   *  APIs for getLogger and addLogger make it difficult to use those functions
   *  for this.
   */ 
  private Logger getOrAddLogger(String name) {
    Logger logger = getLogger(name);
    if (logger == null) {
      Logger newLogger = new LoggerWithExposedConstructor(name);
      addLoggerWithoutDuplicationChecking(newLogger);
      return newLogger;
    }
    return logger;
  }
}
