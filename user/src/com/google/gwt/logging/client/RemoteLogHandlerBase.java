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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Base class for Logging handlers that send records to the server.
 */
public abstract class RemoteLogHandlerBase extends Handler {
  protected static final String WIRE_LOGGER_NAME = "WireActivityLogger";
  
  // A separate logger for wire activity, which does not get logged
  // by the remote log handler, so we avoid infinite loops.
  protected static Logger wireLogger = Logger.getLogger(WIRE_LOGGER_NAME);
  
  private boolean closed = false;
  
  private List<String> excludedLoggerNames;
  protected RemoteLogHandlerBase() {
    excludedLoggerNames = new ArrayList<String>();
    excludedLoggerNames.add(WIRE_LOGGER_NAME);
  }

  protected RemoteLogHandlerBase(List<String> excludedLoggerNames) {
    this.excludedLoggerNames = excludedLoggerNames;
    this.excludedLoggerNames.add(WIRE_LOGGER_NAME);
  }

  @Override
  public void close() {
    closed = true;
  }

  @Override
  public void flush() {
    // No action needed
  }
  
  @Override
  public boolean isLoggable(LogRecord record) {
    // The number of excludedLoggerNames is expected to be small (2-3 at most)
    // but in theory, clients could put lots of names in the list.
    // TODO(unnurg): consider implementing this search with a map rather than
    // a list, depending on the JS size implications of including a map.
    return (!closed && super.isLoggable(record) &&
        !excludedLoggerNames.contains(record.getLoggerName()));
  }
}
