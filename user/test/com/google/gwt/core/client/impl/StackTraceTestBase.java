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
    assertTrace(getTraceJava(), t);
  }

  protected abstract String[] getTraceJava();

  public void testTraceRecursion() {
    Exception t = StackTraceExamples.getLiveException(RECURSION);
    assertTrace(getTraceRecursion(), t);
  }

  protected abstract String[] getTraceRecursion();

  public void testTraceTypeError() {
    assertJse(TYPE_ERROR);
  }

  public void testTraceString() {
    assertJse("testing");
  }

  public void testTraceStringMultiline() {
    assertJse("multi\nline\nerror\nmessage");
  }

  public void testTraceNull() {
    assertJse(null);
  }

  private void assertJse(Object whatToThrow) {
    Exception t = StackTraceExamples.getLiveException(whatToThrow);

    String[] expected = getTraceJse(whatToThrow);
    assertTrace(expected, t);
  }

  protected abstract String[] getTraceJse(Object whatToThrow);

  private void assertTrace(String[] expected, Exception t) {
    int i = 0;
    StackTraceElement[] trace = t.getStackTrace();
    for (String expectedMethodName : expected) {
      boolean optionalFrame = expectedMethodName.startsWith("?");
      if (optionalFrame) {
        expectedMethodName = expectedMethodName.substring(1);
      }
      StackTraceElement actualElement = trace[i];
      String methodName = actualElement == null ? "!MISSING!" : actualElement.getMethodName();
      if (expectedMethodName.equals(methodName)) {
        i++;
        continue;
      }
      if (optionalFrame) {
        continue;
      }
      AssertionFailedError e = new AssertionFailedError("Incorrect frame at " + i + " - "
          + " Expected: " + expectedMethodName + " Actual: " + methodName);
      e.initCause(t);
      throw e;
    }
  }
}
