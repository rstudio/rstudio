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

import com.google.gwt.core.client.impl.SerializableThrowable;

import java.util.Date;
import java.util.HashSet;
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
  
  // This method is borrowed from AbstractTreeLogger. 
  // TODO(unnurg): once there is a clear place where code used by gwt-dev and
  // gwt-user can live, move this function there.
  protected String getStackTraceAsString(Throwable e, String newline,
      String indent) {
    if (e == null) {
      return "";
    }
    // For each cause, print the requested number of entries of its stack
    // trace, being careful to avoid getting stuck in an infinite loop.
    //
    StringBuffer s = new StringBuffer(newline);
    Throwable currentCause = e;
    String causedBy = "";
    HashSet<Throwable> seenCauses = new HashSet<Throwable>();
    while (currentCause != null && !seenCauses.contains(currentCause)) {
      seenCauses.add(currentCause);
      s.append(causedBy);
      causedBy = newline + "Caused by: "; // after 1st, all say "caused by"
      if (currentCause instanceof SerializableThrowable.ThrowableWithClassName) {
        s.append(((SerializableThrowable.ThrowableWithClassName) currentCause).getExceptionClass());
      } else {
        s.append(currentCause.getClass().getName());
      }
      s.append(": " + currentCause.getMessage());
      StackTraceElement[] stackElems = currentCause.getStackTrace();
      if (stackElems != null) {
        for (int i = 0; i < stackElems.length; ++i) {
          s.append(newline + indent + "at ");
          s.append(stackElems[i].toString());
        }
      }

      currentCause = currentCause.getCause();
    }
    return s.toString();
  }

}
