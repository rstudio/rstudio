/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.emultest.java.lang;

import static com.google.gwt.emultest.java.lang.JsExceptionViolator.createJsException;
import static com.google.gwt.emultest.java.lang.JsExceptionViolator.getBackingJsObject;

import com.google.gwt.testing.TestUtils;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * Unit tests for JsException behavior.
 */
public class JsExceptionTest extends ThrowableTestBase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  @Override
  public void runTest() throws Throwable {
    // Do not run  these tests in JVM.
    if (TestUtils.isJvm()) {
      return;
    }
  }

  public void testCatchJava() {
    Object obj = new Object();
    Exception e = createJsException(obj);
    assertJsException(obj, catchJava(createThrower(e)));
  }

  public void testCatchNative() {
    Object obj = new Object();
    Exception e = createJsException(obj);
    assertSame(obj, catchNative(createThrower(e)));
  }

  public void testCatchNativePropagatedFromFinally() {
    Object obj = new Object();
    Exception e = createJsException(obj);
    assertSame(obj, catchNative(wrapWithFinally(createThrower(e))));
    assertTrue(keepFinallyAlive);
  }

  private static boolean keepFinallyAlive = false;

  private static Thrower wrapWithFinally(final Thrower thrower) {
    return new Thrower() {
      @Override public void throwException() throws Throwable {
        try {
          thrower.throwException();
        } finally {
          keepFinallyAlive = true;
        }
      }
    };
  }

  public void testJavaNativeJavaSandwichCatch() {
    Object obj = new Object();
    Exception e = createJsException(obj);
    assertJsException(obj, javaNativeJavaSandwich(e));
  }

  public void testCatchThrowNative() {
    Object e;

    e = "testing";
    assertJsException(e, catchJava(createNativeThrower(e)));

    e = null;
    assertJsException(e, catchJava(createNativeThrower(e)));

    e = new Object();
    assertJsException(e, catchJava(createNativeThrower(e)));
  }

  // jsni throw -> java catch -> java throw -> jsni catch
  public void testNativeJavaNativeSandwichCatch() {
    Object e;

    e = "testing";
    assertEquals(e, nativeJavaNativeSandwich(e));
    // Devmode will not preserve the same String instance
    assertEquals(e, nativeJavaNativeSandwich(e));

    e = null;
    assertSame(e, nativeJavaNativeSandwich(e));

    e = new Object();
    assertSame(e, nativeJavaNativeSandwich(e));
  }

  private Object nativeJavaNativeSandwich(Object e) {
    return catchNative(createThrower(catchJava(createNativeThrower(e))));
  }

  public void testTypeError() {
    try {
      throwTypeError();
      fail();
    } catch (RuntimeException e) {
      assertTypeError(e);
      e = (RuntimeException) javaNativeJavaSandwich(e);
      assertTypeError(e);
    }
  }

  private static void throwTypeError() {
    Object nullObject = null;
    nullObject.toString();
  }

  @JsType(isNative = true, namespace = "<window>")
  private static class TypeError { }

  protected static void assertTypeError(RuntimeException e) {
    assertTrue(getBackingJsObject(e) instanceof TypeError);
    assertTrue(e.toString().contains("TypeError"));
  }

  public void testSvgError() {
    try {
      throwSvgError();
      fail();
    } catch (RuntimeException e) {
      e = (RuntimeException) javaNativeJavaSandwich(e);
    }
  }

  private static void throwSvgError() {
    // In old Firefox, this throws an object (not Error):
    createElementNS("http://www.w3.org/2000/svg", "text").getBBox();

    // For other browsers, make sure an exception is thrown to keep the test simple
    throw new RuntimeException("NS_ERROR_FAILURE");
  }

  @JsMethod(name = "document.createElementNS", namespace = JsPackage.GLOBAL)
  private static native SVGElement createElementNS(String arg1, String arg2);

  @JsType(isNative = true, namespace = JsPackage.GLOBAL)
  private interface SVGElement {
    void getBBox();
  }

  private static void assertJsException(Object expected, Throwable exception) {
    assertTrue(exception instanceof RuntimeException);
    assertEquals(expected, getBackingJsObject(exception));
  }
}
