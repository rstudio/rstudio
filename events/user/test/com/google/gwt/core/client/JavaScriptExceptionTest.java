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

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Any JavaScript exceptions occurring within JSNI methods are wrapped as this
 * class when caught in Java code. The wrapping does not occur until the
 * exception passes out of JSNI into Java. Before that, the thrown object
 * remains a native JavaScript exception object, and can be caught in JSNI as
 * normal.
 */
public class JavaScriptExceptionTest extends GWTTestCase {

  static native JavaScriptObject makeJSO() /*-{
    return {
      toString:function() {
        return "jso";
      },
      name: "myName",
      message: "myDescription",
      extraField: "extraData"
    };
  }-*/;

  static void throwJava(Throwable t) throws Throwable {
    throw t;
  }

  static native void throwNative(Object e) /*-{
    throw e;
  }-*/;

  static void throwSandwichJava(Object e) {
    throwNative(e);
  }

  static native void throwSandwichNative(Throwable t) /*-{
    @com.google.gwt.core.client.JavaScriptExceptionTest::throwJava(Ljava/lang/Throwable;)(t);
  }-*/;

  /**
   * This test doesn't work in hosted mode yet; we'd need a way to throw true
   * native objects as exceptions. Windows/IE is the deal killer right now on
   * really making this work since there's no way to raise an exception of a
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

  public void testJavaExceptionSandwich() {
    RuntimeException e = new RuntimeException();
    try {
      throwSandwichNative(e);
    } catch (Throwable t) {
      assertSame(e, t);
    }
  }

  public void testJso() {
    JavaScriptObject jso = makeJSO();
    try {
      throwNative(jso);
      fail();
    } catch (JavaScriptException e) {
      assertEquals("myName", e.getName());
      assertEquals("myDescription", e.getDescription());
      assertSame(jso, e.getException());
      assertTrue(e.getMessage().contains("myName"));
      assertTrue(e.getMessage().contains("myDescription"));
      assertTrue(e.getMessage().contains("extraField"));
      assertTrue(e.getMessage().contains("extraData"));
    }
  }

  public void testNull() {
    try {
      throwNative(null);
      fail();
    } catch (JavaScriptException e) {
      assertEquals("null", e.getName());
      assertEquals("null", e.getDescription());
      assertEquals(null, e.getException());
      assertTrue(e.getMessage().contains("null"));
    }
  }

  public void testObject() {
    Object o = new Object() {
      @Override
      public String toString() {
        return "myLameObject";
      }
    };
    try {
      throwNative(o);
      fail();
    } catch (JavaScriptException e) {
      assertEquals(o.getClass().getName(), e.getName());
      assertEquals("myLameObject", e.getDescription());
      assertEquals(null, e.getException());
      assertTrue(e.getMessage().contains(o.getClass().getName()));
      assertTrue(e.getMessage().contains("myLameObject"));
    }
  }

  public void testString() {
    try {
      throwNative("foobarbaz");
      fail();
    } catch (JavaScriptException e) {
      assertEquals("String", e.getName());
      assertEquals("foobarbaz", e.getDescription());
      assertEquals(null, e.getException());
      assertTrue(e.getMessage().contains("foobarbaz"));
    }
  }
}
