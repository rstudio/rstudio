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
import com.google.gwt.logging.shared.RemoteLoggingService;
import com.google.gwt.logging.shared.RemoteLoggingServiceAsync;
import com.google.gwt.logging.shared.SerializableLogRecord;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * A very simple handler which sends messages to the server via GWT RPC to be
 * logged. Note that this logger should not be used in production. It does not
 * do any intelligent batching of RPC's, nor does it disable when the RPC
 * calls fail repeatedly.
 */
public final class SimpleRemoteLogHandler extends Handler {
  private static Logger logger =
    Logger.getLogger(SimpleRemoteLogHandler.class.getName());

  class DefaultCallback implements AsyncCallback<String> {
    public void onFailure(Throwable caught) {
      logger.severe("Remote logging failed: " + caught.toString());
    }
    public void onSuccess(String result) {
      if (result.length() > 0) {
        logger.severe("Remote logging failed: " + result);
      } else {
        logger.finest("Remote logging message acknowledged");
      }
    }
  }
  
  private RemoteLoggingServiceAsync service;
  private AsyncCallback<String> callback;

  public SimpleRemoteLogHandler() {
    service = (RemoteLoggingServiceAsync) GWT.create(RemoteLoggingService.class);
    this.callback = new DefaultCallback();
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
    if (record.getLoggerName().equals(logger.getName())) {
      // We don't want to propagate our own messages to the server since it
      // would lead to an infinite loop.
      return;
    }
    service.logOnServer(new SerializableLogRecord(
        record, GWT.getPermutationStrongName()), callback);
  }
}
