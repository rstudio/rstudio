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

import junit.framework.AssertionFailedError;

/**
 * Tests {@link StackTraceCreator}.
 */
public class StackTraceCreatorTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.StackTraceNoEmul";
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

    assertTrace(expected, t, 0);
  }

  @DoNotRunWith(Platform.Devel)
  public void testTraceRecursion() {
    Throwable t = null;
    try {
      throwExceptionRecursive(3);
      fail("No exception thrown");
    } catch (Throwable e) {
      t = e;
    }

    final String[] expectedModern = {
        Impl.getNameOf("@java.lang.Throwable::new(Ljava/lang/String;)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceCreatorTest::throwException3(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceCreatorTest::throwException2(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceCreatorTest::throwException1(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceCreatorTest::throwExceptionRecursive(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceCreatorTest::throwExceptionRecursive(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceCreatorTest::throwExceptionRecursive(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceCreatorTest::testTraceRecursion()"),
    };

    final String[] expectedLegacy = {
        Impl.getNameOf("@java.lang.Throwable::new(Ljava/lang/String;)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceCreatorTest::throwException3(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceCreatorTest::throwException2(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceCreatorTest::throwException1(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceCreatorTest::throwExceptionRecursive(*)"),
    };

    final String[] expected = isLegacyCollector() ? expectedLegacy : expectedModern;
    assertTrace(expected, t, 0);
  }

  @DoNotRunWith(Platform.Devel)
  public void testTraceNative() {
    Throwable t = null;
    try {
      throwException1(true /* throw js exception */);
      fail("No exception thrown");
    } catch (Throwable e) {
      t = e;
    }

    String[] nativeMethodNames = throwNative(false /* don't throw - collect mode */);
    final String[] expectedModern = {
        nativeMethodNames[0],
        nativeMethodNames[1],
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceCreatorTest::throwNative(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceCreatorTest::throwException3(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceCreatorTest::throwException2(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceCreatorTest::throwException1(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceCreatorTest::testTraceNative()"),
    };

    final String[] expectedLegacy = {
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceCreatorTest::testTraceNative()"),
    };

    final String[] expected = isLegacyCollector() ? expectedLegacy : expectedModern;

    int offset = getTraceOffset(t.getStackTrace(), expected[0]);
    assertTrace(expected, t, offset);
  }

  private void assertTrace(String[] expected, Throwable t, int offset) {
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

  private static boolean isLegacyCollector() {
    return StackTraceCreator.collector instanceof CollectorLegacy;
  }

  private static void throwExceptionRecursive(int count) throws Throwable {
    if (count > 1) {
      throwExceptionRecursive(count - 1);
    } else {
      throwException1(false);
    }
  }

  private static void throwException1(boolean throwNative) throws Throwable {
    throwException2(throwNative);
  }

  private static void throwException2(boolean throwNative) throws Throwable {
    throwException3(throwNative);
  }

  private static void throwException3(boolean throwNative) throws Throwable {
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

      return [
        @StackTraceCreator::getFunctionName(*)(arguments.callee),
        @StackTraceCreator::getFunctionName(*)(arguments.callee.caller),
      ];
    }
    return native1();
  }-*/;
}
