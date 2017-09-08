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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 *  An emulation of the java.util.logging.Logger class. See
 *  <a href="http://java.sun.com/j2se/1.4.2/docs/api/java/util/logging/Logger.html">
 *  The Java API doc for details</a>
 */
public class Logger {
  public static final String GLOBAL_LOGGER_NAME = "global";

  private static final boolean LOGGING_OFF;
  private static final boolean ALL_ENABLED;
  private static final boolean INFO_ENABLED;
  private static final boolean WARNING_ENABLED;
  private static final boolean SEVERE_ENABLED;

  static {
    // '==' instead of equals makes it compile out faster.

    String level = System.getProperty("jre.logging.logLevel");
    if (level != "ALL"
        && level != "INFO"
        && level != "WARNING"
        && level != "SEVERE"
        && level != "OFF") {
      throw new AssertionError("Undefined value for jre.logging.logLevel: '" + level + "'");
    }

    LOGGING_OFF = level == "OFF";
    ALL_ENABLED = level == "ALL";
    INFO_ENABLED = level == "ALL" || level == "INFO";
    WARNING_ENABLED = level == "ALL" || level == "INFO" || level == "WARNING";
    SEVERE_ENABLED = level == "ALL" || level == "INFO" || level == "WARNING" || level == "SEVERE";
  }

  public static Logger getGlobal() {
    return getLogger(GLOBAL_LOGGER_NAME);
  }

  public static Logger getLogger(String name) {
    // Use shortcut if logging is disabled to avoid parent logger creations in LogManager
    if (LOGGING_OFF) {
      return new Logger(null, null);
    }
    return LogManager.getLogManager().ensureLogger(name);
  }

  private List<Handler> handlers;
  private Level level;
  private String name;
  private Logger parent;  // Should never be null except in the RootLogger
  private boolean useParentHandlers;

  protected Logger(String name, @SuppressWarnings("unused") String resourceName) {
    if (LOGGING_OFF) {
      return;
    }

    this.name = name;
    this.useParentHandlers = true;
    handlers = new ArrayList<Handler>();
  }

  public void addHandler(Handler handler) {
    if (LOGGING_OFF) {
      return;
    }
    handlers.add(handler);
  }

  public void config(String msg) {
    if (!ALL_ENABLED) {
      return;
    }
    log(Level.CONFIG, msg);
  }

  public void config(Supplier<String> msgSupplier) {
    if (!ALL_ENABLED) {
      return;
    }
    log(Level.CONFIG, msgSupplier);
  }

  public void fine(String msg) {
    if (!ALL_ENABLED) {
      return;
    }
    log(Level.FINE, msg);
  }

  public void fine(Supplier<String> msgSupplier) {
    if (!ALL_ENABLED) {
      return;
    }
    log(Level.FINE, msgSupplier);
  }

  public void finer(String msg) {
    if (!ALL_ENABLED) {
      return;
    }
    log(Level.FINER, msg);
  }

  public void finer(Supplier<String> msgSupplier) {
    if (!ALL_ENABLED) {
      return;
    }
    log(Level.FINER, msgSupplier);
  }

  public void finest(String msg) {
    if (!ALL_ENABLED) {
      return;
    }
    log(Level.FINEST, msg);
  }

  public void finest(Supplier<String> msgSupplier) {
    if (!ALL_ENABLED) {
      return;
    }
    log(Level.FINEST, msgSupplier);
  }

  public void info(String msg) {
    if (!INFO_ENABLED) {
      return;
    }
    log(Level.INFO, msg);
  }

  public void info(Supplier<String> msgSupplier) {
    if (!INFO_ENABLED) {
      return;
    }
    log(Level.INFO, msgSupplier);
  }

  public void warning(String msg) {
    if (!WARNING_ENABLED) {
      return;
    }
    log(Level.WARNING, msg);
  }

  public void warning(Supplier<String> msgSupplier) {
    if (!WARNING_ENABLED) {
      return;
    }
    log(Level.WARNING, msgSupplier);
  }

  public void severe(String msg) {
    if (!SEVERE_ENABLED) {
      return;
    }
    log(Level.SEVERE, msg);
  }

  public void severe(Supplier<String> msgSupplier) {
    if (!SEVERE_ENABLED) {
      return;
    }
    log(Level.SEVERE, msgSupplier);
  }

  public Handler[] getHandlers() {
    if (LOGGING_OFF) {
      return new Handler[0];
    }

    return handlers.toArray(new Handler[handlers.size()]);
  }

  public Level getLevel() {
    return LOGGING_OFF ? null : level;
  }

  public String getName() {
    return LOGGING_OFF ? null : name;
  }

  public Logger getParent() {
    return LOGGING_OFF ? null : parent;
  }

  public boolean getUseParentHandlers() {
    return LOGGING_OFF ? false : useParentHandlers;
  }

  public boolean isLoggable(Level messageLevel) {
    if (ALL_ENABLED) {
      return messageLevel.intValue() >= getEffectiveLevel().intValue();
    } else if (INFO_ENABLED) {
      return messageLevel.intValue() >= Level.INFO.intValue();
    } else if (WARNING_ENABLED) {
      return messageLevel.intValue() >= Level.WARNING.intValue();
    } else if (SEVERE_ENABLED) {
      return messageLevel.intValue() >= Level.SEVERE.intValue();
    } else {
      return false;
    }
  }

  public void log(Level level, String msg) {
    log(level, msg, null);
  }

  public void log(Level level, Supplier<String> msgSupplier) {
    log(level, null, msgSupplier);
  }

  public void log(Level level, String msg, Throwable thrown) {
    if (isLoggable(level)) {
      actuallyLog(level, msg, thrown);
    }
  }

  public void log(Level level, Throwable thrown, Supplier<String> msgSupplier) {
    if (isLoggable(level)) {
      actuallyLog(level, msgSupplier.get(), thrown);
    }
  }

  public void log(LogRecord record) {
    if (isLoggable(record.getLevel())) {
      actuallyLog(record);
    }
  }

  public void removeHandler(Handler handler) {
    if (LOGGING_OFF) {
      return;
    }
    handlers.remove(handler);
  }

  public void setLevel(Level newLevel) {
    if (LOGGING_OFF) {
      return;
    }
    this.level = newLevel;
  }

  public void setParent(Logger newParent) {
    if (LOGGING_OFF) {
      return;
    }
    if (newParent != null) {
      parent = newParent;
    }
  }

  public void setUseParentHandlers(boolean newUseParentHandlers) {
    if (LOGGING_OFF) {
      return;
    }
    this.useParentHandlers = newUseParentHandlers;
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

  private void actuallyLog(Level level, String msg, Throwable thrown) {
    LogRecord record = new LogRecord(level, msg);
    record.setThrown(thrown);
    record.setLoggerName(getName());
    actuallyLog(record);
  }

  private void actuallyLog(LogRecord record) {
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

  /* Not Implemented */
  // public static Logger getAnonymousLogger() {
  // public static Logger getAnonymousLogger(String resourceBundleName) {}
  // public Filter getFilter() {}
  // public static Logger getLogger(String name, String resourceBundleName) {}
  // public ResourceBundle getResourceBundle() {}
  // public String getResourceBundleName() {}
  // public void setFilter(Filter newFilter) {}
  // public void entering(String sourceClass, String sourceMethod) {}
  // public void entering(String sourceClass, String sourceMethod, Object param1) {}
  // public void entering(String sourceClass, String sourceMethod, Object[] params) {}
  // public void exiting(String sourceClass, String sourceMethod, Object result) {}
  // public void exiting(String sourceClass, String sourceMethod) {}
  // public void log(Level level, String msg, Object param1) {}
  // public void log(Level level, String msg, Object[] params) {}
  // public void logp(Level level, String sourceClass, String sourceMethod, String msg) {}
  // public void logp(Level level, String sourceClass, String sourceMethod, String msg, Object param1) {}
  // public void logp(Level level, String sourceClass, String sourceMethod, String msg, Object[] params) {}
  // public void logp(Level level, String sourceClass, String sourceMethod, String msg, Throwable thrown) {}
  // public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg) {}
  // public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg, Object param1) {}
  // public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg, Object[] params) {}
  // public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg, Throwable thrown) {}
  // public void throwing(String sourceClass, String sourceMethod, Throwable thrown) {}
}
