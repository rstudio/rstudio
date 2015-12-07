/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.core.client.impl;

import static com.google.gwt.core.client.impl.StackTraceExamples.JAVA;
import static com.google.gwt.core.client.impl.StackTraceExamples.TYPE_ERROR;

import junit.framework.AssertionFailedError;

/**
 * Tests {@link StackTraceCreator} in the emulated mode.
 */
public class StackTraceEmulTest extends StackTraceNativeTest {

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Core";
  }

  @Override
  protected String[] getTraceJse(Object thrown) {
    return super.getTraceJse(TYPE_ERROR);
  }

  @Override
  public void testCollectorType() {
    assertTrue(StackTraceCreator.collector instanceof StackTraceCreator.CollectorEmulated);
  }

  /**
   * Verifies that the line numbers are recorded accurately in emulation for JavaScript exceptions.
   */
  public void testJseLineNumbers() {
    Exception exception = StackTraceExamples.getLiveException(TYPE_ERROR);
    String[] methodNames = getTraceJse(TYPE_ERROR);

    StackTraceElement[] expectedTrace = new StackTraceElement[] {
        createSTE(methodNames[0], "StackTraceExamples.java", 83),
        createSTE(methodNames[1], "StackTraceExamples.java", 79),
        createSTE(methodNames[2], "StackTraceExamples.java", 95),
        createSTE(methodNames[3], "StackTraceExamples.java", 61),
        createSTE(methodNames[4], "StackTraceExamples.java", 52),
        createSTE(methodNames[5], "StackTraceExamples.java", 40)
    };

    assertTrace(expectedTrace, exception);
  }

  /**
   * Verifies that the line numbers are recorded accurately in emulation for Java exceptions.
   */
  public void testJavaLineNumbers() {
    Exception exception = StackTraceExamples.getLiveException(JAVA);
    String[] methodNames = getTraceJava();

    StackTraceElement[] expectedTrace = new StackTraceElement[] {
        createSTE(methodNames[0], "Throwable.java", 68),
        createSTE(methodNames[1], "Exception.java", 29),
        createSTE(methodNames[2], "StackTraceExamples.java", 57),
        createSTE(methodNames[3], "StackTraceExamples.java", 52),
        createSTE(methodNames[4], "StackTraceExamples.java", 40)
    };

    assertTrace(expectedTrace, exception);
  }
  /**
   * Verifies throw/try/catch doesn't poison the emulated stack frames.
   */
  public void testViaSample() {
    StackTraceElement[] start = sample();

    Exception e = StackTraceExamples.getLiveException(JAVA);
    assertTrue(e.getStackTrace().length > 0);

    StackTraceElement[] end = sample();
    assertTraceMethodNames(start, end);
  }

  /**
   * Verifies throw/try/catch with JSE doesn't poison the emulated stack frames.
   */
  public void testJseViaSample() {
    StackTraceElement[] start = sample();

    Exception e = StackTraceExamples.getLiveException(TYPE_ERROR);
    assertTrue(e.getStackTrace().length > 0);

    StackTraceElement[] end = sample();
    assertTraceMethodNames(start, end);
  }

  private static void assertTraceMethodNames(StackTraceElement[] start, StackTraceElement[] end) {
    assertEquals("length", start.length, end.length);
    for (int i = 0, j = start.length; i < j; i++) {
      assertEquals("frame " + i, start[i].getMethodName(), end[i].getMethodName());
    }
  }

  private void assertTrace(StackTraceElement[] expected, Exception t) {
    StackTraceElement[] trace = t.getStackTrace();

    for (int i = 0; i < expected.length; i++) {
      StackTraceElement actualElement = trace[i];
      if (actualElement.equals(expected[i])) {
        continue;
      }
      AssertionFailedError e = new AssertionFailedError("Incorrect frame at " + i + " - "
          + " Expected: " + expected[i] + " Actual: " + actualElement);
      e.initCause(t);
      throw e;
    }
  }

  private static StackTraceElement[] sample() {
    return new Throwable().getStackTrace();
  }

  private static StackTraceElement createSTE(String methodName, String fileName, int lineNumber) {
    methodName = methodName.startsWith("?") ? methodName.substring(1) : methodName;
    return new StackTraceElement("Unknown", methodName, fileName, lineNumber);
  }
}
