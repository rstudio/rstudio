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
    expected.append("\tSuppressed: custom msg supressed 1\n");
    expected.append("\t\tat c5.m5(f5:5)\n");
    expected.append("\tCaused by: custom msg supressed 1 cause\n");
    expected.append("\t\tat c6.m6(f6:6)\n");
    expected.append("\tSuppressed: custom msg supressed 2\n");
    expected.append("\t\tat c7.m7(f7:7)\n");
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
    exception.initCause(cause);

    Exception supressed1 = new Exception() {
      @Override
      public String toString() {
        return "custom msg supressed 1";
      }
    };
    supressed1.setStackTrace(new StackTraceElement[] {new StackTraceElement("c5", "m5", "f5", 5)});

    Exception s1Cause = new Exception() {
      @Override
      public String toString() {
        return "custom msg supressed 1 cause";
      }
    };
    s1Cause.setStackTrace(new StackTraceElement[] {new StackTraceElement("c6", "m6", "f6", 6)});
    supressed1.initCause(s1Cause);

    exception.addSuppressed(supressed1);

    Exception s2 = new Exception() {
      @Override
      public String toString() {
        return "custom msg supressed 2";
      }
    };
    s2.setStackTrace(new StackTraceElement[] {new StackTraceElement("c7", "m7", "f7", 7)});
    exception.addSuppressed(s2);

    return exception;
  }
}
