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

package com.google.web.bindery.requestfactory.gwt.client;

import com.google.gwt.logging.client.JsonLogRecordClientUtil;
import com.google.gwt.logging.client.RemoteLogHandlerBase;
import com.google.web.bindery.requestfactory.shared.LoggingRequest;
import com.google.web.bindery.requestfactory.shared.Receiver;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * A Handler that does remote logging for applications using RequestFactory.
 */
public class RequestFactoryLogHandler extends RemoteLogHandlerBase {

  /**
   * Provides a logging request.
   */
  public static interface LoggingRequestProvider {
    /**
     * Returns the logging request.
     * 
     * @return a {@link LoggingRequest} instance
     */
    LoggingRequest getLoggingRequest();
  }

  private LoggingRequestProvider requestProvider;

  /**
   * Since records from this handler go accross the wire, it should only be used
   * for important messages, and it's Level will often be higher than the Level
   * being used app-wide. This handler also takes string which it will use to
   * exclude the messages from some loggers. This usually includes the name of
   * the logger(s) which will be used to log acknowledgements of activity going
   * accross the wire. If we did not exclude these loggers, an infinite loop
   * would occur.
   * 
   * @param requestProvider a {@link LoggingRequestProvider} instance
   * @param level a logging {@link Level}
   * @param ignoredLoggerNames a List of Strings
   */
  public RequestFactoryLogHandler(LoggingRequestProvider requestProvider, Level level,
      List<String> ignoredLoggerNames) {
    super(ignoredLoggerNames);
    this.requestProvider = requestProvider;
    setLevel(level);
  }

  @Override
  public void publish(LogRecord record) {
    if (!isLoggable(record)) {
      return;
    }
    String json = JsonLogRecordClientUtil.logRecordAsJson(record);
    requestProvider.getLoggingRequest().logMessage(json).fire(new Receiver<Void>() {
      @Override
      public void onSuccess(Void response) {
        // Do nothing
      }
    });
  }
}
