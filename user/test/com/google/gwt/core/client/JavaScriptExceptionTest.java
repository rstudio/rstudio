/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.core.client;

import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.useragent.client.UserAgent;

/**
 * Any JavaScript exceptions occurring within JSNI methods are wrapped as this
 * class when caught in Java code. The wrapping does not occur until the
 * exception passes out of JSNI into Java. Before that, the thrown object
 * remains a native JavaScript exception object, and can be caught in JSNI as
 * normal.
 */
public class JavaScriptExceptionTest extends GWTTestCase {

  private static native JavaScriptObject makeJSO() /*-{
    return {
      toString:function() {
        return "jso";
      },
      name: "myName",
      message: "myDescription",
      extraField: "extraData"
    };
  }-*/;

  private static Object makeJavaObject() {
    Object o = new Object() {
      @Override public String toString() {
        return "myLameObject";
      }
    };
    return o;
  }

  private static native void throwNative(Object e) /*-{
    throw e;
  }-*/;

  private static native void throwTypeError() /*-{
    "dummy".notExistsWillThrowTypeError();
  }-*/;

  private static native void throwSvgError() /*-{
    // In Firefox, this throws an object (not Error):
    $doc.createElementNS("http://www.w3.org/2000/svg", "text").getBBox();

    // For other browsers, make sure an exception is thrown to keep the test simple
    throw new Error;
  }-*/;

  private static void throwSandwichJava(Object e) {
    throwNative(e);
  }

  private static Throwable catchJava(Runnable runnable) {
    try {
      runnable.run();
    } catch (Throwable e) {
      return e;
    }
    return null;
  }

  private static native Object catchNative(Runnable runnable) /*-{
    try {
      runnable.@java.lang.Runnable::run()();
    } catch(e) {
      return e;
    }
  }-*/;

  private static Runnable createThrowRunnable(final Throwable e) {
    return new Runnable() {
      @Override public void run() {
        throw sneakyThrow(e);
      }

      @SuppressWarnings("unchecked")
      <T extends RuntimeException> T sneakyThrow(Throwable e) {
        return (T) e;
      }
    };
  }

  private static Runnable createThrowNativeRunnable(final Object e) {
    return new Runnable() {
      @Override public void run() {
        throwNative(e);
      }
    };
  }

  private static void assertJavaScriptException(Object expected, Throwable exception) {
    assertTrue(exception instanceof JavaScriptException);
    assertEquals(expected, ((JavaScriptException) exception).getThrown());
  }

  /**
   * This test doesn't work in Development Mode yet; we'd need a way to throw
   * true native objects as exceptions. Windows/IE is the deal killer right now
   * on really making this work since there's no way to raise an exception of a
   * true JS value. We could use JS lambdas around Java calls to get around this
   * restriction.
   */
  public native void disabledTestJsExceptionSandwich() /*-{
    var e = { };
    try {
      @com.google.gwt.core.client.JavaScriptExceptionTest::throwSandwichJava(Ljava/lang/Object;)(e);
    } catch (t) {
      @junit.framework.Assert::assertSame(Ljava/lang/Object;Ljava/lang/Object;)(e, t);
    }
  }-*/;

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Core";
  }

  public void testCatch() {
    RuntimeException e = new RuntimeException();
    assertSame(e, catchJava(createThrowRunnable(e)));

    JavaScriptObject jso = makeJSO();
    e = new JavaScriptException(jso);
    assertJavaScriptException(jso, catchJava(createThrowRunnable(e)));
  }

  // java throw -> jsni catch -> jsni throw -> java catch
  public void testJavaNativeJavaSandwichCatch() {
    RuntimeException e = new RuntimeException();
    assertSame(e, javaNativeJavaSandwich(e));

    JavaScriptObject jso = makeJSO();
    e = new JavaScriptException(jso);
    assertJavaScriptException(jso, javaNativeJavaSandwich(e));
  }

  private Throwable javaNativeJavaSandwich(RuntimeException e) {
    return catchJava(createThrowNativeRunnable(catchJava(createThrowRunnable(e))));
  }

  public void testCatchThrowNative() {
    Object e;

    e = makeJSO();
    assertJavaScriptException(e, catchJava(createThrowNativeRunnable(e)));

    e = "testing";
    assertJavaScriptException(e, catchJava(createThrowNativeRunnable(e)));

    e = null;
    assertJavaScriptException(e, catchJava(createThrowNativeRunnable(e)));

    e = makeJavaObject();
    assertJavaScriptException(e, catchJava(createThrowNativeRunnable(e)));

    e = new RuntimeException();
    assertSame(e, catchJava(createThrowNativeRunnable(e)));

    e = new JavaScriptException("exception message"); // Thrown is not set
    assertSame(e, catchJava(createThrowNativeRunnable(e)));

    e = new JavaScriptException(makeJSO());
    assertSame(e, catchJava(createThrowNativeRunnable(e)));
  }

  // jsni throw -> java catch -> java throw -> jsni catch
  public void testNativeJavaNativeSandwichCatch() {
    Object e;

    e = makeJSO();
    assertSame(e, nativeJavaNativeSandwich(e));

    e = "testing";
    assertEquals(e, nativeJavaNativeSandwich(e));
    if (GWT.isScript()) { // Devmode will not preserve the same String instance
      assertSame(e, nativeJavaNativeSandwich(e));
    }

    e = null;
    assertSame(e, nativeJavaNativeSandwich(e));

    e = makeJavaObject();
    assertSame(e, nativeJavaNativeSandwich(e));

    e = new RuntimeException();
    assertSame(e, nativeJavaNativeSandwich(e));

    e = new JavaScriptException("exception message"); // Thrown is not set
    assertSame(e, nativeJavaNativeSandwich(e));

    JavaScriptObject jso = makeJSO();
    e = new JavaScriptException(jso);
    assertSame(jso, nativeJavaNativeSandwich(e));
  }

  private Object nativeJavaNativeSandwich(Object e) {
    return catchNative(createThrowRunnable(catchJava(createThrowNativeRunnable(e))));
  }

  public void testJso() {
    JavaScriptObject jso = makeJSO();
    try {
      throwNative(jso);
      fail();
    } catch (JavaScriptException e) {
      assertEquals("myName", e.getName());
      assertDescription(e, "myDescription");
      assertTrue(e.isThrownSet());
      assertSame(jso, e.getThrown());
      assertMessage(e);
    }
  }

  public void testNull() {
    try {
      throwNative(null);
      fail();
    } catch (JavaScriptException e) {
      assertEquals("null", e.getName());
      assertDescription(e, "null");
      assertTrue(e.isThrownSet());
      assertEquals(null, e.getThrown());
      assertMessage(e);
    }
  }

  public void testObject() {
    Object o = makeJavaObject();
    try {
      throwNative(o);
      fail();
    } catch (JavaScriptException e) {
      assertEquals(o.getClass().getName(), e.getName());
      assertDescription(e, "myLameObject");
      assertTrue(e.isThrownSet());
      assertEquals(o, e.getThrown());
      assertMessage(e);
    }
  }

  public void testString() {
    try {
      throwNative("foobarbaz");
      fail();
    } catch (JavaScriptException e) {
      assertEquals("String", e.getName());
      assertDescription(e, "foobarbaz");
      assertTrue(e.isThrownSet());
      assertEquals("foobarbaz", e.getThrown());
      assertMessage(e);
    }
  }

  @DoNotRunWith(Platform.HtmlUnitUnknown)
  public void testTypeError() {
    try {
      throwTypeError();
      fail();
    } catch (JavaScriptException e) {
      assertTypeError(e);
      e = (JavaScriptException) javaNativeJavaSandwich(e);
      assertTypeError(e);
    }
  }

  public void testSvgError() {
    try {
      throwSvgError();
      fail();
    } catch (JavaScriptException e) {
      assertTrue(e.isThrownSet());
      e = (JavaScriptException) javaNativeJavaSandwich(e);
      assertTrue(e.isThrownSet());
    }
  }

  private static void assertTypeError(JavaScriptException e) {
    assertEquals("TypeError", e.getName());
    assertTrue("e.isThrownSet() for a TypeError", e.isThrownSet());
    assertMessage(e);

    if (isIE8()) {
      return;
    }

    String description = e.getDescription();
    // The description depends on the browser.
    // On Chrome 34, the description is: "undefined is not a function".
    if (description.isEmpty()) {
      fail("TypeError description should not be empty");
    }
  }

  private static boolean isIE8() {
    UserAgent userAgent = GWT.create(UserAgent.class);
    return userAgent.getCompileTimeValue().equals("ie8");
  }

  private static void assertDescription(JavaScriptException e, String description) {
    if (!GWT.isScript()) {
      assertTrue("Should start with method name",
          e.getDescription().startsWith(
              "@com.google.gwt.core.client.JavaScriptExceptionTest::"
                  + "throwNative(Ljava/lang/Object;)"));
    }
    assertTrue("Should end with " + e.getDescription(),
        e.getDescription().endsWith(description));
  }

  private static void assertMessage(JavaScriptException e) {
    assertEquals("(" + e.getName() + ") " + e.getDescription(), e.getMessage());
  }
}
