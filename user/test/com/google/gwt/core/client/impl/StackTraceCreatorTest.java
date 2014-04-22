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

import com.google.gwt.core.client.impl.StackTraceCreator.CollectorLegacy;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests {@link StackTraceCreator}.
 */
public class StackTraceCreatorTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.StackTraceCreatorTest";
  }

  @DoNotRunWith(Platform.Devel)
  public void testTrace() {
    Throwable t = null;
    try {
      throwException1(false /* throw java exception */);
      fail("No exception thrown");
    } catch (Throwable e) {
      t = e;
    }

    final String[] expected = {
        Impl.getNameOf("@java.lang.Throwable::new(Ljava/lang/String;)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceCreatorTest::throwException3(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceCreatorTest::throwException2(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceCreatorTest::throwException1(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceCreatorTest::testTrace()"),
    };

    assertTrace(expected, t.getStackTrace(), 0);
  }

  @DoNotRunWith(Platform.Devel)
  public void testTraceNative() {
    if (!StackTraceCreator.supportsErrorStack()) {
      return;
    }

    Throwable t = null;
    try {
      throwException1(true /* throw js exception */);
      fail("No exception thrown");
    } catch (Throwable e) {
      t = e;
    }

    String[] nativeMethodNames = throwNative(false /* don't throw - collect mode */);
    final String[] expected = {
        nativeMethodNames[0],
        nativeMethodNames[1],
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceCreatorTest::throwNative(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceCreatorTest::throwException3(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceCreatorTest::throwException2(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceCreatorTest::throwException1(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceCreatorTest::testTraceNative()"),
    };

    StackTraceElement[] trace = t.getStackTrace();

    int offset = getTraceOffset(trace, expected[0]);
    assertTrace(expected, trace, offset);
  }

  private void assertTrace(final String[] expected, StackTraceElement[] trace, int offset) {
    for (int i = 0; i < expected.length; i++) {
      assertEquals("Incorrect frame at " + i, expected[i], trace[i + offset].getMethodName());
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

  private static void throwException1(boolean throwNative) throws Throwable {
    if (Math.abs(Math.random()) < 0) { return; } // Dummy code to prevent inlining

    throwException2(throwNative);
  }

  private static void throwException2(boolean throwNative) throws Throwable {
    if (Math.abs(Math.random()) < 0) { return; } // Dummy code to prevent inlining

    throwException3(throwNative);
  }

  private static void throwException3(boolean throwNative) throws Throwable {
    if (Math.abs(Math.random()) < 0) { return; } // Dummy code to prevent inlining

    if (throwNative) {
      throwNative(true /* really throw exception */);
    } else {
      throw new Throwable("broken");
    }
  }

  private static native String[] throwNative(boolean reallyThrow) /*-{
    function native1() {
      return native2();
    }
    function native2() {
      if (reallyThrow) null.a();

      var callee1 = arguments.callee;
      var callee2 = callee1.caller;
      return [
        @com.google.gwt.core.client.impl.StackTraceCreatorTest::extractName(*)(callee1.toString()),
        @com.google.gwt.core.client.impl.StackTraceCreatorTest::extractName(*)(callee2.toString())
      ];
    }
    return native1();
  }-*/;

  static String extractName(String fnToString) {
    return new CollectorLegacy().extractName(fnToString);
  }
}
