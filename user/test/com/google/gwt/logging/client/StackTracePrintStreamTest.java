/*
 * Copyright 2013 Google Inc.
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

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.logging.impl.StackTracePrintStream;

/**
 * Tests {@link StackTracePrintStream}.
 */
public class StackTracePrintStreamTest extends GWTTestCase {
  @Override
  public String getModuleName() {
    return "com.google.gwt.logging.Logging";
  }

  public void testPrintStackTrace() throws Exception {
    StringBuilder actual = new StringBuilder();
    createTestException().printStackTrace(new StackTracePrintStream(actual));

    StringBuilder expected = new StringBuilder();
    expected.append("custom msg\n");
    expected.append("\tat c1.m1(f1:1)\n");
    expected.append("\tat c2.m2(f2:2)\n");
    expected.append("Caused by: custom msg cause\n");
    expected.append("\tat c3.m3(f3:3)\n");
    expected.append("\tat c4.m4(f4:4)\n");

    assertEquals(expected.toString(), actual.toString());
  }

  private Throwable createTestException() {
    Exception exception = new Exception() {
      @Override
      public String toString() {
        return "custom msg";
      }
    };
    exception.setStackTrace(new StackTraceElement[] {
        new StackTraceElement("c1", "m1", "f1", 1), new StackTraceElement("c2", "m2", "f2", 2)});

    Exception cause = new Exception() {
      @Override
      public String toString() {
        return "custom msg cause";
      }
    };
    cause.setStackTrace(new StackTraceElement[] {
        new StackTraceElement("c3", "m3", "f3", 3), new StackTraceElement("c4", "m4", "f4", 4)});

    return exception.initCause(cause);
  }
}
