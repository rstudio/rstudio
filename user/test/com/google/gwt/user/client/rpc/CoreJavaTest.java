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
import com.google.gwt.logging.impl.LevelImplRegular;

import java.math.MathContext;
import java.math.RoundingMode;
import java.util.logging.LogRecord;

/**
 * Test cases for Generic Collections in GWT RPC.
 * 
 */
public class CoreJavaTest extends RpcTestBase {

  private static final LogRecord expectedLogRecord = createLogRecord();

  public static boolean isValid(LogRecord value, boolean compareTrace) {
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

      if (compareTrace) {
        StackTraceElement[] expectedTrace = expectedCause.getStackTrace();
        StackTraceElement[] valueTrace = valueCause.getStackTrace();
        if ((expectedTrace == null) != (valueTrace == null)) {
          return false;
        }
        if (expectedTrace != null) {
          if (expectedTrace.length != valueTrace.length) {
            return false;
          }
          for (int i = 0; i < expectedTrace.length; ++i) {
            StackTraceElement expectedElement = expectedTrace[i];
            StackTraceElement valueElement = valueTrace[i];

            if (!expectedElement.equals(valueElement)) {
              return false;
            }
          }
        }
      }

      expectedCause = expectedCause.getCause();
      valueCause = valueCause.getCause();
    }
    if (valueCause != null) {
      return false;
    }

    return true;
  }

  public static boolean isValid(MathContext value) {
    return createMathContext().equals(value);
  }

  private static LogRecord createLogRecord() {
    /*
     * A LevelImplRegular log level is created here in order to circumvent
     * normal logging system behavior. Without this, the Level is null unless
     * logging is active, which we do not want for testing. Standard usage
     * is new LogRecord(Level.INFO, "Test Log Record");
     */
    LevelImplRegular logLevel = new LevelImplRegular();
    LogRecord result = new LogRecord(logLevel.info(), "Test Log Record");

    // Only set serialized fields.

    result.setLoggerName("Test Logger Name");
    result.setMillis(1234567);

    Throwable thrown = new Throwable("Test LogRecord Throwable 1");
    thrown.initCause(new Throwable("Test LogRecord Throwable cause"));
    result.setThrown(thrown);

    return result;
  }

  private static MathContext createMathContext() {
    return new MathContext(5, RoundingMode.CEILING);
  }

  private CoreJavaTestServiceAsync coreJavaTestService;

  public void testLogRecord() {
    CoreJavaTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echoLogRecord(expectedLogRecord, new AsyncCallback<LogRecord>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(LogRecord result) {
        assertNotNull(result);
        assertTrue(isValid(result, true));
        finishTest();
      }
    });
    finishTest();
  }

  public void testMathContext() {
    CoreJavaTestServiceAsync service = getServiceAsync();
    final MathContext expected = createMathContext();

    delayTestFinishForRpc();
    service.echoMathContext(expected, new AsyncCallback<MathContext>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(MathContext result) {
        assertNotNull(result);
        assertTrue(isValid(result));
        finishTest();
      }
    });
  }

  private CoreJavaTestServiceAsync getServiceAsync() {
    if (coreJavaTestService == null) {
      coreJavaTestService = (CoreJavaTestServiceAsync) GWT.create(CoreJavaTestService.class);
    }
    return coreJavaTestService;
  }
}
