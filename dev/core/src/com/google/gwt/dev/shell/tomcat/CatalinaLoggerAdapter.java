// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.shell.tomcat;

import com.google.gwt.core.ext.TreeLogger;

import org.apache.catalina.logger.LoggerBase;

class CatalinaLoggerAdapter extends LoggerBase {

  private final TreeLogger fLogger;

  public CatalinaLoggerAdapter(TreeLogger logger) {
    fLogger = logger;
  }

  public void log(Exception exception, String msg) {
    fLogger.log(TreeLogger.WARN, msg, exception);
  }

  public void log(String msg) {
    fLogger.log(TreeLogger.INFO, msg, null);
  }

  public void log(String message, int verbosity) {
    TreeLogger.Type type = mapVerbosityToLogType(verbosity);
    fLogger.log(type, message, null);
  }

  public void log(String msg, Throwable throwable) {
    fLogger.log(TreeLogger.WARN, msg, throwable);
  }

  public void log(String message, Throwable throwable, int verbosity) {
    TreeLogger.Type type = mapVerbosityToLogType(verbosity);
    fLogger.log(type, message, throwable);
  }

  private TreeLogger.Type mapVerbosityToLogType(int verbosity) {
    switch (verbosity) {
      case LoggerBase.FATAL:
      case LoggerBase.ERROR:
      case LoggerBase.WARNING:
        return TreeLogger.WARN;

      case LoggerBase.INFORMATION:
        return TreeLogger.DEBUG;
      case LoggerBase.DEBUG:
        return TreeLogger.SPAM;

      default:
        // really, this was an unexpected type
        return TreeLogger.WARN;
    }
  }

}