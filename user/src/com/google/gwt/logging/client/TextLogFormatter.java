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

package com.google.gwt.logging.client;

import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Formats LogRecords into 2 lines of text
 */
public class TextLogFormatter extends Formatter {

  @Override
  public String format(LogRecord event) {
    Date date = new Date(event.getMillis());
    StringBuilder message = new StringBuilder();
    message.append(date.toString());
    message.append(" ");
    message.append(event.getLoggerName());
    message.append("\n");
    message.append(event.getLevel().getName());
    message.append(": ");
    message.append(event.getMessage());
    if (event.getThrown() != null) {
      // TODO(unnurg): output throwables correctly
    }
    return message.toString();
  }
}
