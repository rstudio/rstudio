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
package com.google.gwt.dev.shell.tomcat;

import com.google.gwt.core.ext.TreeLogger;

/**
 * Maps Tomcat's commons logger onto the GWT shell's tree logger.
 */
public class CommonsLoggerAdapter implements org.apache.commons.logging.Log {

  private TreeLogger log;

  /**
   * @param name unused
   */
  public CommonsLoggerAdapter(String name) {
    // NOTE: this is ugly, but I don't know of any other way to get a
    // non-static log to which we can delegate.
    //
    log = EmbeddedTomcatServer.sTomcat.getLogger();
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
    return log.isLoggable(TreeLogger.SPAM);
  }

  public boolean isErrorEnabled() {
    return log.isLoggable(TreeLogger.WARN);
  }

  public boolean isFatalEnabled() {
    return log.isLoggable(TreeLogger.WARN);
  }

  public boolean isInfoEnabled() {
    // Intentionally low-level to us.
    return log.isLoggable(TreeLogger.TRACE);
  }

  public boolean isTraceEnabled() {
    // Intentionally low-level to us.
    return log.isLoggable(TreeLogger.SPAM);
  }

  public boolean isWarnEnabled() {
    return log.isLoggable(TreeLogger.WARN);
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
    log.log(type, msg, t);
  }
}