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

/**
 * Tests {@link StackTraceCreator} in the emulated mode.
 */
public class StackTraceEmulTest extends StackTraceNativeTest {

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Core";
  }

  /**
   * Verifies throw/try/catch doesn't poison the emulated stack frames.
   */
  public static void testViaSample() {
    StackTraceElement[] start = sample();

    Exception e = StackTraceExamples.getLiveException(JAVA);
    assertTrue(e.getStackTrace().length > 0);

    StackTraceElement[] end = sample();
    assertEquals(start, end);
  }

  /**
   * Verifies throw/try/catch with JSE doesn't poison the emulated stack frames.
   */
  public void testJseViaSample() {
    StackTraceElement[] start = sample();

    Exception e = StackTraceExamples.getLiveException(TYPE_ERROR);
    assertTrue(e.getStackTrace().length > 0);

    StackTraceElement[] end = sample();
    assertEquals(start, end);
  }

  private static void assertEquals(StackTraceElement[] start, StackTraceElement[] end) {
    assertEquals("length", start.length, end.length);
    for (int i = 0, j = start.length; i < j; i++) {
      assertEquals("frame " + i, start[i].getMethodName(), end[i].getMethodName());
    }
  }

  private static StackTraceElement[] sample() {
    return new Throwable().getStackTrace();
  }
}
