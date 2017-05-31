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

import com.google.gwt.testing.TestUtils;

import jsinterop.annotations.JsType;

/**
 * Unit tests for the GWT emulation of java.lang.Throwable class.
 */
public class ThrowableTest extends ThrowableTestBase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testStackTrace() {
    Throwable e = new Throwable("<my msg>");
    assertTrue(e.getStackTrace().length > 0);

    e = new Throwable("<my msg>") {
      public Throwable fillInStackTrace() {
        // Replace fill in stack trace with no-op.
        return this;
      }
    };
    assertEquals(0, e.getStackTrace().length);

    e = new Throwable("<my msg>", null, true, false) { };
    assertEquals(0, e.getStackTrace().length);
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

  public void testCatchJava() {
    Throwable e = new Throwable();
    assertSame(e, catchJava(createThrower(e)));
  }

  public void testCatchNative() {
    if (TestUtils.isJvm()) {
      return;
    }
    Throwable e = new Throwable("<my msg>");
    Object caughtNative = catchNative(createThrower(e));
    assertTrue(caughtNative instanceof Error);
    assertTrue(caughtNative.toString().contains("<my msg>"));
    assertTrue(caughtNative.toString().contains(Throwable.class.getName()));
  }

  public void testCatchNativeWithFillInStackTraceOverride() {
    if (TestUtils.isJvm()) {
      return;
    }
    Throwable e = new Throwable("<my msg>") {
      public Throwable fillInStackTrace() {
        // Replace fill in stack trace with no-op.
        return this;
      }
    };

    Object caughtNative = catchNative(createThrower(e));
    assertTrue(caughtNative instanceof Error);
    assertTrue(caughtNative.toString().contains("<my msg>"));
    assertTrue(caughtNative.toString().contains(e.getClass().getName()));
  }

  public void testJavaNativeJavaSandwichCatch() {
    if (TestUtils.isJvm()) {
      return;
    }
    Throwable e = new Throwable();
    assertSame(e, javaNativeJavaSandwich(e));
  }

  @JsType(isNative = true, namespace = "<window>")
  private static class Error { }
}
