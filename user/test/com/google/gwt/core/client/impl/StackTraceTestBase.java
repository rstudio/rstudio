/*
 * Copyright 2014 Google Inc.
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
import static com.google.gwt.core.client.impl.StackTraceExamples.RECURSION;
import static com.google.gwt.core.client.impl.StackTraceExamples.TYPE_ERROR;

import com.google.gwt.junit.client.GWTTestCase;

import junit.framework.AssertionFailedError;

/**
 * Tests {@link StackTraceCreator}.
 */
public abstract class StackTraceTestBase extends GWTTestCase {

  public void testTraceJava() {
    Exception t = StackTraceExamples.getLiveException(JAVA);
    assertTrace(getTraceJava(), t, 0);
  }

  protected abstract String[] getTraceJava();

  public void testTraceRecursion() {
    Exception t = StackTraceExamples.getLiveException(RECURSION);
    assertTrace(getTraceRecursion(), t, 0);
  }

  protected abstract String[] getTraceRecursion();

  public void testTraceTypeError() {
    assertJse(TYPE_ERROR);
  }

  public void testTraceString() {
    assertJse("testing");
  }

  public void testTraceNull() {
    assertJse(null);
  }

  private void assertJse(Object whatToThrow) {
    Exception t = StackTraceExamples.getLiveException(whatToThrow);

    String[] expected = getTraceJse(whatToThrow);
    int offset = getTraceOffset(t.getStackTrace(), expected[0]);
    assertTrace(expected, t, offset);
  }

  protected abstract String[] getTraceJse(Object whatToThrow);

  private void assertTrace(String[] expected, Exception t, int offset) {
    StackTraceElement[] trace = t.getStackTrace();
    for (int i = 0; i < expected.length; i++) {
      StackTraceElement actualElement = trace[i + offset];
      String methodName = actualElement == null ? "!MISSING!" : actualElement.getMethodName();
      if (expected[i].equals(methodName)) {
        continue;
      }
      AssertionFailedError e = new AssertionFailedError("Incorrect frame at " + i + " - "
          + " Expected: " + expected[i] + " Actual: " + methodName);
      e.initCause(t);
      throw e;
    }
  }

  protected int getTraceOffset(StackTraceElement[] trace, String firstFrame) {
    // TODO(goktug): Native exception traces are not properly stripped,
    // Working around that for now by calculating an offset.
    int offset = 0;
    for (StackTraceElement ste : trace) {
      if (ste.getMethodName().equals(firstFrame)) {
        break;
      }
      offset++;
    }
    return offset;
  }
}
