// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.shell.tomcat;

import com.google.gwt.core.ext.TreeLogger;

public class CommonsLoggerAdapter implements org.apache.commons.logging.Log {

  public CommonsLoggerAdapter(String name) {
    // NOTE: this is ugly, but I don't know of any other way to get a 
    // non-static log to which we can delegate.
    //
    fLog = EmbeddedTomcatServer.sTomcat.getLogger();
  }

  public void debug(Object message) {
    doLog(TreeLogger.SPAM, message, null);
  }

  public void debug(Object message, Throwable t) {
    doLog(TreeLogger.SPAM, message, t);
  }

  public void error(Object message) {
    doLog(TreeLogger.WARN, message, null);
  }

  public void error(Object message, Throwable t) {
    doLog(TreeLogger.WARN, message, t);
  }

  public void fatal(Object message) {
    doLog(TreeLogger.WARN, message, null);
  }

  public void fatal(Object message, Throwable t) {
    doLog(TreeLogger.WARN, message, t);
  }

  public void info(Object message) {
    // Intentionally low-level to us.
    doLog(TreeLogger.TRACE, message, null);
  }

  public void info(Object message, Throwable t) {
    // Intentionally low-level to us.
    doLog(TreeLogger.TRACE, message, t);
  }

  public boolean isDebugEnabled() {
    return fLog.isLoggable(TreeLogger.SPAM);
  }

  public boolean isErrorEnabled() {
    return fLog.isLoggable(TreeLogger.WARN);
  }

  public boolean isFatalEnabled() {
    return fLog.isLoggable(TreeLogger.WARN);
  }

  public boolean isInfoEnabled() {
    // Intentionally low-level to us.
    return fLog.isLoggable(TreeLogger.TRACE);
  }

  public boolean isTraceEnabled() {
    // Intentionally low-level to us.
    return fLog.isLoggable(TreeLogger.SPAM);
  }

  public boolean isWarnEnabled() {
    return fLog.isLoggable(TreeLogger.WARN);
  }

  public void trace(Object message) {
    // Intentionally low-level to us.
    doLog(TreeLogger.DEBUG, message, null);
  }

  public void trace(Object message, Throwable t) {
    // Intentionally low-level to us.
    doLog(TreeLogger.DEBUG, message, t);
  }

  public void warn(Object message) {
    doLog(TreeLogger.WARN, message, null);
  }

  public void warn(Object message, Throwable t) {
    doLog(TreeLogger.WARN, message, t);
  }

  private void doLog(TreeLogger.Type type, Object message, Throwable t) {
    String msg = message.toString();
    fLog.log(type, msg, t);
  }

  private TreeLogger fLog;
}