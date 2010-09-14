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

import com.google.gwt.core.client.GWT;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * A Handler that prints logs to GWT.log, causing the messages to show up in
 * the Development Mode tab in Eclipse when running in Development mode.
 */
public class DevelopmentModeLogHandler extends Handler {
  
  public DevelopmentModeLogHandler() {
    setFormatter(new TextLogFormatter(false));
    setLevel(Level.ALL);
  }

  @Override
  public void close() {
    // No action needed
  }

  @Override
  public void flush() {
    // No action needed
  }

  @Override
  public void publish(LogRecord record) {
    if (!isSupported() || !isLoggable(record)) {
      return;
    }
    String msg = getFormatter().format(record);
    GWT.log(msg, record.getThrown());
  }
  
  private boolean isSupported() {
    return !GWT.isScript();
  }

}
