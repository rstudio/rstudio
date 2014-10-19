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

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testStackTraceContainsConstructorLineNumber() {

    final int lineNumber1 = 33; // should be the next line.
    Throwable throwable = new Throwable("stacktrace");
    StackTraceElement[] trace = throwable.getStackTrace();
    assertTrace(trace, lineNumber1, isObfuscated(trace) ? 7 : 3);

    final int lineNumber2 = 38; // should be the next line.
    throwable.fillInStackTrace();
    assertTrace(throwable.getStackTrace(), lineNumber2, 2);

    // break fillinstacktrace and test again
  }

  private void assertTrace(StackTraceElement[] trace, final int lineNumber, int maxDepthToExamine) {
    assertNotNull(trace);
    assertTrue("StackTrace too short: \n" + getTraceAsString(trace), trace.length > 2);
    StackTraceElement e = getFirstElementMentionsTest(trace, maxDepthToExamine);
    assertEquals(lineNumber, e.getLineNumber());
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

  private boolean isObfuscated(StackTraceElement[] trace) {
    assertTrue(trace.length > 0);
    return trace[0].getClassName().equals("Unknown");
  }

  private StackTraceElement getFirstElementMentionsTest(StackTraceElement[] trace,
      int maxDepthToExamine) {
    for (int i = 0; i < maxDepthToExamine && i < trace.length; i++) {
      StackTraceElement e = trace[i];
      if (e.getFileName().contains("ThrowableTest")) {
        return e;
      }
    }
    fail("Unable to find trace element: \n" + getTraceAsString(trace));
    return null; // shouldn't happen
  }

  private String getTraceAsString(StackTraceElement[] trace) {
    String result = "";
    for (StackTraceElement e : trace) {
      result += e.toString() + "\n";
    }
    return result;
  }
}
