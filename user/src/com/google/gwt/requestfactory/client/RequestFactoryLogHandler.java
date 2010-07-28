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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * A Handler that does Remote Logging for applications using Request Factory.
 */
public class RequestFactoryLogHandler extends Handler {
  class LoggingReceiver implements Receiver<Long> {

    public void onSuccess(Long response) {
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
  private List<LogRecord> records;
  private RequestFactory requestFactory;
  
  public RequestFactoryLogHandler(RequestFactory requestFactory) {
    this(requestFactory, Level.INFO);
  }
  
  /**
   * Since records from this handler go accross the wire, it should only be
   * used for important messages, and it's Level will often be higher than the
   * Level being used app-wide.
   * Do not set the level of this handler below FINER messages, since it logs
   * messages at that level to acknowledge success/failure, and would cause
   * an infinite loop.
   */
  public RequestFactoryLogHandler(RequestFactory requestFactory, Level level) {
    this.requestFactory = requestFactory;
    closed = false;
    records = new ArrayList<LogRecord>();
    setLevel(level);
  }

  @Override
  public void close() {
    flush();
    closed = true;
  }
  
  @Override
  public void flush() {
    if (!closed) {
      // We go ahead and just send a request for every message. The request
      // factory will take care of the batching for us. Once we can send
      // something more complex than Strings to the logMessage function, then
      // we can do batching here.
      for (LogRecord record : records) {
        Receiver loggingReciever = new LoggingReceiver();
        requestFactory.loggingRequest().logMessage(
            record.getLevel().toString(),
            record.getLoggerName(),
            record.getMessage()).to(loggingReciever).fire();
      }
    }
  }

  @Override
  public void publish(LogRecord record) {
    if (!closed && isLoggable(record)) {
      records.add(record);
      // For now, just flush every time since a new request is sent for every
      // record anyway.
      flush();
    }
  }

}
