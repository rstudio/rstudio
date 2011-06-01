/*
 * Copyright 2011 Google Inc.
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

package com.google.gwt.user.client.rpc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Test cases for Generic Collections in GWT RPC.
 * 
 */
public class LoggingRPCTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.LoggingRPCSuite";
  }
  
  private static final LogRecord expectedLogRecord = createLogRecord();

  public static boolean isValid(LogRecord value) {
    if (!expectedLogRecord.getLevel().toString().equals(value.getLevel().toString())) {
      return false;
    }

    if (!expectedLogRecord.getMessage().equals(value.getMessage())) {
      return false;
    }

    if (expectedLogRecord.getMillis() != value.getMillis()) {
      return false;
    }

    if (!expectedLogRecord.getLoggerName().equals(value.getLoggerName())) {
      return false;
    }

    Throwable expectedCause = expectedLogRecord.getThrown();
    Throwable valueCause = value.getThrown();
    while (expectedCause != null) {
      if (valueCause == null) {
        return false;
      }

      if (!expectedCause.getMessage().equals(valueCause.getMessage())) {
        return false;
      }

      // Do not compare trace as it is not stable across RPC.
      
      expectedCause = expectedCause.getCause();
      valueCause = valueCause.getCause();
    }
    if (valueCause != null) {
      return false;
    }

    return true;
  }

  private static LogRecord createLogRecord() {
    LogRecord result = new LogRecord(Level.INFO, "Test Log Record");

    // Only set serialized fields.

    result.setLoggerName("Test Logger Name");
    result.setMillis(1234567);

    Throwable thrown = new Throwable("Test LogRecord Throwable 1");
    thrown.initCause(new Throwable("Test LogRecord Throwable cause"));
    result.setThrown(thrown);

    return result;
  }

  private LoggingRPCTestServiceAsync loggingRPCTestService;

  public void testLogRecord() {
    LoggingRPCTestServiceAsync service = getServiceAsync();
    delayTestFinish(15000);
    service.echoLogRecord(expectedLogRecord, new AsyncCallback<LogRecord>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(LogRecord result) {
        assertNotNull(result);
        assertTrue(isValid(result));
        finishTest();
      }
    });
    finishTest();
  }

  private LoggingRPCTestServiceAsync getServiceAsync() {
    if (loggingRPCTestService == null) {
      loggingRPCTestService = (LoggingRPCTestServiceAsync) GWT.create(LoggingRPCTestService.class);
    }
    return loggingRPCTestService;
  }
}
