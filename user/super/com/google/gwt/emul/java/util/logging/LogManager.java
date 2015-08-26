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

package java.util.logging;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;

/**
 *  An emulation of the java.util.logging.LogManager class. See
 *  <a href="http://java.sun.com/j2se/1.4.2/docs/api/java/util/logging/LogManger.html">
 *  The Java API doc for details</a>
 */
public class LogManager {

  private static LogManager singleton;

  public static LogManager getLogManager() {
    if (singleton == null) {
      singleton = new LogManager();
      Logger rootLogger = new Logger("", null);
      rootLogger.setLevel(Level.INFO);
      singleton.addLoggerImpl(rootLogger);
    }
    return singleton;
  }

  private HashMap<String, Logger> loggerMap = new HashMap<String, Logger>();

  protected LogManager() { }

  public boolean addLogger(Logger logger) {
    if (getLogger(logger.getName()) != null) {
      return false;
    }
    addLoggerAndEnsureParents(logger);
    return true;
  }

  public Logger getLogger(String name) {
    return loggerMap.get(name);
  }

  public Enumeration<String> getLoggerNames() {
    return Collections.enumeration(loggerMap.keySet());
  }

  /**
   *  Helper function to add a logger when we have already determined that it
   *  does not exist.  When we add a logger, we recursively add all of it's
   *  ancestors. Since loggers do not get removed, logger creation is cheap,
   *  and there are not usually too many loggers in an ancestry chain,
   *  this is a simple way to ensure that the parent/child relationships are
   *  always correctly set up.
   */
  private void addLoggerAndEnsureParents(Logger logger) {
    String name = logger.getName();
    String parentName = name.substring(0, Math.max(0, name.lastIndexOf('.')));
    logger.setParent(ensureLogger(parentName));
    addLoggerImpl(logger);
  }

  private void addLoggerImpl(Logger logger) {
    if (System.getProperty("gwt.logging.simpleConsoleHandler", "ENABLED").equals("ENABLED")) {
      if (logger.getName().isEmpty()) {
        logger.addHandler(new SimpleConsoleLogHandler());
      }
    }
    loggerMap.put(logger.getName(), logger);
  }

  /**
   *  Helper function to create a logger if it does not exist since the public
   *  APIs for getLogger and addLogger make it difficult to use those functions
   *  for this.
   */
  Logger ensureLogger(String name) {
    Logger logger = getLogger(name);
    if (logger == null) {
      Logger newLogger = new Logger(name, null);
      addLoggerAndEnsureParents(newLogger);
      return newLogger;
    }
    return logger;
  }

  /* Not Implemented */
  // public void addPropertyChangeListener(PropertyChangeListener l) {}
  // public void checkAccess() {}
  // public String getProperty(String name) {}
  // public void readConfiguration() {}
  // public void readConfiguration(InputStream ins) {}
  // public void removePropertyChangeListener(PropertyChangeListener l) {}
  // public void reset() {}
}
