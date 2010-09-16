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

package com.google.gwt.requestfactory.client;

import com.google.gwt.logging.client.JsonLogRecordClientUtil;
import com.google.gwt.logging.shared.SerializableLogRecord;
import com.google.gwt.requestfactory.shared.LoggingRequest;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.SyncResult;

import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * A Handler that does remote logging for applications using RequestFactory.
 */
public class RequestFactoryLogHandler extends Handler {
  
  /** 
   * Provides a logging request
   */
  public static interface LoggingRequestProvider {
    LoggingRequest getLoggingRequest();
  }
  
  private class LoggingReceiver extends Receiver<Long> {
    public void onSuccess(Long response, Set<SyncResult> syncResults) {
      if (response > 0) {
        logger.finest("Remote logging successful");
      } else {
        logger.finest("Remote logging failed");
      }
    }
  }
  
  private static Logger logger = 
    Logger.getLogger(RequestFactoryLogHandler.class.getName());
  
  // A separate logger for wire activity, which does not get logged
  // by the remote log handler, so we avoid infinite loops.
  private static Logger wireLogger = Logger.getLogger("WireActivityLogger");
  
  private boolean closed;
  private LoggingRequestProvider requestProvider;
  private String ignoredLoggerSubstring;
  private String strongName;
  
  /**
   * Since records from this handler go accross the wire, it should only be
   * used for important messages, and it's Level will often be higher than the
   * Level being used app-wide. This handler also takes a string which it will
   * use to exclude the messages from some loggers. This usually includes the
   * name of the logger(s) which will be used to log acknowledgements of
   * activity going accross the wire. If we did not exclude these loggers, an
   * infinite loop would occur.
   */
  public RequestFactoryLogHandler(LoggingRequestProvider requestProvider,
      Level level, String ignoredLoggerSubstring, String strongName) {
    this.requestProvider = requestProvider;
    this.ignoredLoggerSubstring = ignoredLoggerSubstring;
    this.strongName = strongName;
    closed = false;
    setLevel(level);
  }

  @Override
  public void close() {
    closed = true;
  }
  
  @Override
  public void flush() {
    // Do nothing
  }

  @Override
  public void publish(LogRecord record) {
    if (closed || !isLoggable(record)) {
      return;
    }
    if (record.getLoggerName().contains(ignoredLoggerSubstring)) {
      return;
    }
    SerializableLogRecord slr =
      new SerializableLogRecord(record, strongName);
    String json = JsonLogRecordClientUtil.serializableLogRecordAsJson(slr);
    requestProvider.getLoggingRequest().logMessage(json).fire(
        new Receiver<Boolean>() {
          @Override
          public void onSuccess(Boolean response, Set<SyncResult> syncResults) {
            if (!response) {
              wireLogger.severe("Remote Logging failed to parse JSON");
            }
          }
        });
  }
}
