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
package com.google.gwt.emultest.java.lang;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Unit tests for the GWT emulation of java.lang.Throwable class.
 */
public class ThrowableTest extends GWTTestCase {

  // Line number for exception thrown in throwException below.
  private static final int THROWN_EXCEPTION_LINE_NUMBER = 36;

  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  private void throwException(String arg) {
    // prevent inlining by double referencing arg
    String result = arg.substring(1) + "test";
    // THROWN_EXCEPTION_LINE_NUMBER should point to this line below
    throw new RuntimeException(result.charAt(0) + result.substring(1));
  }

  public void testStackTraceContainsConstructorLineNumber() {
    // NOTE: changing the line number of this object creation will affect
    // the test case.
    final int lineNumber = 43;  // should be the next line.
    Throwable throwable = new Throwable("stacktrace");
    StackTraceElement[] trace = throwable.getStackTrace();
    assertNotNull(trace);
    String traceStr = getTraceAsString(trace);
    assertTrue("stack trace has length: " + trace.length + ", full trace: \n" +
        traceStr, trace.length > 2);
    StackTraceElement e = findTraceElementForThisFile(trace,
        (isObfuscated(trace) ? 7 : 3));
    assertNotNull("Unable to find trace element: \n" + traceStr, e);
    assertEquals(lineNumber, e.getLineNumber());
  }

  public void testGetStackTraceOnThrownException() {
    RuntimeException caughtException = null;
    try {
      throwException("Runtime Exception");
    } catch (RuntimeException exception) {
      caughtException = exception;
    }
    assertNotNull(caughtException);
    StackTraceElement[] trace = caughtException.getStackTrace();
    assertNotNull(trace);
    String traceStr =  getTraceAsString(trace);
    StackTraceElement e = findTraceElementForThisFile(trace,
        (isObfuscated(trace) ? 9 : 5));
    assertNotNull("Unable to find trace element: \n" + traceStr, e);
    assertEquals(traceStr, THROWN_EXCEPTION_LINE_NUMBER,
        e.getLineNumber());
    // Don't compare method names for obfuscated cases.
    if (!isObfuscated(trace)) {
      assertTrue("actual method is <<" + e.getMethodName() + ">>" +
          ", full trace is:\n" + traceStr,
          e.getMethodName().contains("throwException"));
    }
  }

  public void testSetStackTrace() {
    Throwable throwable = new Throwable("stacktrace");
    throwable.fillInStackTrace();
    StackTraceElement[] newStackTrace = new StackTraceElement[2];
    newStackTrace[0] = new StackTraceElement("TestClass", "testMethod", "fakefile", 10);
    newStackTrace[1] = new StackTraceElement("TestClass", "testCaller", "fakefile2", 97);
    throwable.setStackTrace(newStackTrace);
    StackTraceElement[] trace = throwable.getStackTrace();
    assertNotNull(trace);
    assertEquals(2, trace.length);
    assertEquals("TestClass", trace[0].getClassName());
    assertEquals("testMethod", trace[0].getMethodName());
    assertEquals("fakefile", trace[0].getFileName());
    assertEquals(10, trace[0].getLineNumber());
    assertEquals("TestClass.testMethod(fakefile:10)", trace[0].toString());
    assertEquals("TestClass.testCaller(fakefile2:97)", trace[1].toString());
  }

  // Returns true if stack trace is obfuscated.
  private boolean isObfuscated(StackTraceElement[] trace) {
    if (trace == null || trace.length == 0) {
      throw new RuntimeException("null trace");
    }
    return trace[0].getClassName().equals("Unknown");
  }

  // Finds first trace element that mentions this file, ThrowableTest, going
  // at most maxDepthToExamine depth.
  private StackTraceElement findTraceElementForThisFile(
      StackTraceElement[] trace, int maxDepthToExamine) {
    if (trace == null) {
      throw new RuntimeException("null trace");
    }
    for (int i = 0; i < maxDepthToExamine && i < trace.length; i++) {
      StackTraceElement e = trace[i];
      if (e.getFileName() != null &&
          e.getFileName().contains("ThrowableTest")) {
        return e;
      }
    }
    return null;
  }

  private String getTraceAsString(StackTraceElement[] trace) {
    if (trace == null) {
      throw new RuntimeException("null trace");
    }
    String result = "";
    for (StackTraceElement e : trace) {
      result += e.toString() + "\n";
    }
    return result;
  }
}
