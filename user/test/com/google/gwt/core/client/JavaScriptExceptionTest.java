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

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Core";
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
    Object o = new Object() {
      @Override public String toString() {
        return "myLameObject";
      }
    };
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

  public void testThrowable() {
    Throwable t = new Throwable();
    try {
      throwNative(t);
      fail();
    } catch (Throwable e) {
      assertSame(t, e);
    }
  }

  public void testJavaScriptException() {
    JavaScriptException t = new JavaScriptException((Object) "foo");
    assertTrue(t.isThrownSet());
    try {
      throwNative(t);
      fail();
    } catch (JavaScriptException e) {
      assertSame(t, e);
    }

    t = new JavaScriptException("exception message"); // Thrown is not set
    assertFalse(t.isThrownSet());
    try {
      throwNative(t);
      fail();
    } catch (JavaScriptException e) {
      assertSame(t, e);
    }
  }

  private static native void throwNative(Object e) /*-{
    throw e;
  }-*/;

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
