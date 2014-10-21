/*
 * Copyright 2014 Google Inc.
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

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * A simple {@link LoggerConfigurator} that configures the root logger to log to the console.
 * <p>
 * This is only used when the application doesn't depend on com.google.gwt.logging.Logging.
 */
class LoggerConfiguratorConsole implements LoggerConfigurator {

  @Override
  public void configure(Logger logger) {
    if (logger.getName().isEmpty()) {
      logger.addHandler(new SimpleConsoleLogHandler());
    }
  }

  private static class SimpleConsoleLogHandler extends Handler {
    @Override
    public void publish(LogRecord record) {
      if (!isSupported()) {
        return;
      }

      int val = record.getLevel().intValue();
      if (val >= Level.SEVERE.intValue()) {
        error(record.getMessage());
      } else if (val >= Level.WARNING.intValue()) {
        warn(record.getMessage());
      } else if (val >= Level.INFO.intValue()) {
        info(record.getMessage());
      } else {
        log(record.getMessage());
      }
      Throwable e = record.getThrown();
      if (e != null) {
        logException(e);
      }
    }

    private native boolean isSupported() /*-{
      return console != null;
    }-*/;

    private native void error(String message) /*-{
      console.error(message);
    }-*/;

    private native void warn(String message) /*-{
      console.warn(message);
    }-*/;

    private native void info(String message) /*-{
      console.info(message);
    }-*/;

    private native void log(String message) /*-{
      console.log(message);
    }-*/;

    private native void logException(Throwable t) /*-{
      // Not all browsers support grouping:
      var groupStart = console.groupCollapsed || console.group || console.log;
      var groupEnd = console.groupEnd || function(){};
      var backingError = t.__gwt$backingJsError;

      groupStart(t.toString());
      console.log(backingError && backingError.stack);
      groupEnd();
    }-*/;

    @Override
    public void close() {
      // No action needed
    }

    @Override
    public void flush() {
      // No action needed
    }
  }
}
