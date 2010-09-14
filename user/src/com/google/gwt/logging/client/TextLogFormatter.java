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

import com.google.gwt.logging.impl.FormatterImpl;

import java.util.logging.LogRecord;

/**
 * Formats LogRecords into 2 lines of text.
 */
public class TextLogFormatter extends FormatterImpl {
  private boolean showStackTraces;
  
  public TextLogFormatter(boolean showStackTraces) {
    this.showStackTraces = showStackTraces;
  }

  @Override
  public String format(LogRecord event) {
    StringBuilder message = new StringBuilder();
    message.append(getRecordInfo(event, "\n"));
    message.append(event.getMessage());
    if (showStackTraces) {
      message.append(getStackTraceAsString(event.getThrown(), "\n", "\t"));
    }
    return message.toString();
  }
}
