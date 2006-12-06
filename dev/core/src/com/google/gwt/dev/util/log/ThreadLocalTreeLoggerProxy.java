// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.log;

import com.google.gwt.core.ext.TreeLogger;

/**
 * An internal implementation support class that creates a
 * {@link com.google.gwt.server.internal.TreeLogger} that wraps another
 * TreeLogger to allow for the underlying logger to be redirected per thread. It
 * can be useful for situations where it is not practical to pass in a logger as
 * a parameter, such as when interfacing with third-party classes.
 */
public final class ThreadLocalTreeLoggerProxy implements TreeLogger {

  private static final ThreadLocal perThreadLogger = new ThreadLocal();

  public ThreadLocalTreeLoggerProxy() {
    this(null);
  }

  public ThreadLocalTreeLoggerProxy(TreeLogger logger) {
    set(logger);
  }

  /**
   * Sets the logger to which calls are redirected for the current thread.
   */
  public void set(TreeLogger logger) {
    perThreadLogger.set(logger);
  }

  /**
   * Delegates the check to the thread-local logger if one is present.
   * 
   * @return relays the return value of the wrapped logger if one exists, or
   *         returns <code>false</code> otherwise
   */
  public boolean isLoggable(Type type) {
    TreeLogger logger = (TreeLogger) perThreadLogger.get();
    if (logger != null)
      return logger.isLoggable(type);
    else
      return false;
  }

  /**
   * Delegates the log to the thread-local logger if one is present. Otherwise,
   * the log entry is discarded.
   */
  public void log(Type type, String msg, Throwable caught) {
    TreeLogger logger = (TreeLogger) perThreadLogger.get();
    if (logger != null)
      logger.log(type, msg, caught);
  }

  /**
   * Delegates the branch to the thread-local logger if one is present.
   * Otherwise, the log entry is discarded and <code>this</code> is returned.
   */
  public TreeLogger branch(Type type, String msg, Throwable caught) {
    TreeLogger logger = (TreeLogger) perThreadLogger.get();
    if (logger != null)
      return logger.branch(type, msg, caught);
    else
      return this;
  }
}
