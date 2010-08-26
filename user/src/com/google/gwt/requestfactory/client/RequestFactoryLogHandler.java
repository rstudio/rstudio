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

import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestFactory;
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
  class LoggingReceiver implements Receiver<Long> {
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
  
  private boolean closed;
  private RequestFactory requestFactory;
  private String ignoredLoggerSubstring;
  
  /**
   * Since records from this handler go accross the wire, it should only be
   * used for important messages, and it's Level will often be higher than the
   * Level being used app-wide. This handler also takes a string which it will
   * use to exclude the messages from some loggers. This usually includes the
   * name of the logger(s) which will be used to log acknowledgements of
   * activity going accross the wire. If we did not exclude these loggers, an
   * infinite loop would occur.
   */
  public RequestFactoryLogHandler(RequestFactory requestFactory, Level level,
      String ignoredLoggerSubstring) {
    this.requestFactory = requestFactory;
    this.ignoredLoggerSubstring = ignoredLoggerSubstring;
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
    
    Receiver<Long> loggingReceiver = new LoggingReceiver();
    requestFactory.loggingRequest().logMessage(
        record.getLevel().toString(),
        record.getLoggerName(),
        record.getMessage()).fire(loggingReceiver);
  }

}
