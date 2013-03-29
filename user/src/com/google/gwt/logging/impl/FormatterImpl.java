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

import java.io.PrintStream;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Base class for Formatters - provides common functionality.
 */
public abstract class FormatterImpl extends Formatter {

  /**
   * Note that this format is likely to change in the future.
   * Outputs the meta information in the log record in the following format:
   * <pre>Date LoggerName\nLEVEL:</pre>
   * Most formatters will append the message right after the colon
   */
  protected String getRecordInfo(LogRecord event, String newline) {
    Date date = new Date(event.getMillis());
    StringBuilder s = new StringBuilder();
    s.append(date.toString());
    s.append(" ");
    s.append(event.getLoggerName());
    s.append(newline);
    s.append(event.getLevel().getName());
    s.append(": ");
    return s.toString();
  }

  /**
   * @deprecated Use {@link Throwable#printStackTrace(PrintStream)} with
   *             {@link StackTracePrintStream} instead.
   */
  @Deprecated
  protected String getStackTraceAsString(Throwable e, final String newline, final String indent) {
    if (e == null) {
      return "";
    }
    final StringBuilder builder = new StringBuilder();
    PrintStream stream = new StackTracePrintStream(builder) {
      @Override
      public void append(String str) {
        builder.append(str.replaceAll("\t", indent));
      }

      @Override
      public void newLine() {
        builder.append(newline);
      }
    };
    e.printStackTrace(stream);
    return builder.toString();
  }
}
