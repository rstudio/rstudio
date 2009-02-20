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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.impl.StackTraceCreator.Collector;
import com.google.gwt.core.client.impl.StackTraceCreator.CollectorMoz;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests StackTraceCreator in web mode. The methods in this test class are
 * static so that their names can be reliably determined in web mode.
 */
public class StackTraceCreatorTest extends GWTTestCase {
  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Core";
  }

  public static void testJavaScriptException() {
    StackTraceElement[] stack = null;
    try {
      throwNative();
    } catch (JavaScriptException e) {
      /*
       * Some browsers may or may not be able to implement this at all, so we'll
       * at least make sure that an array is returned;
       */
      stack = e.getStackTrace();
    }
    assertNotNull(stack);

    String myName = null;
    if (!GWT.isScript()) {
      myName = "testJavaScriptException";
    } else if (GWT.<Collector> create(Collector.class) instanceof CollectorMoz) {
      myName = throwNativeName();
    }

    if (myName != null) {
      boolean found = false;
      for (StackTraceElement elt : stack) {
        if (elt.getMethodName().equals(myName)) {
          found = true;
          break;
        }
      }
      assertTrue("Did not find " + myName + " in the stack " + stack, found);
    }
  }

  /**
   * Just make sure that reentrant behavior doesn't fail.
   */
  public static void testReentrantCalls() {
    if (!GWT.isScript()) {
      // StackTraceCreator.createStackTrace() is useless in hosted mode
      return;
    }

    JsArrayString stack = countDown(5);
    assertNotNull(stack);
    assertTrue(stack.length() > 0);
  }

  public static void testStackTraces() {
    if (!GWT.isScript()) {
      // StackTraceCreator.createStackTrace() is useless in hosted mode
      return;
    }

    // Since we're in web mode, we can find the name of this method's function
    String myName = testStackTracesName();

    StackTraceElement[] stack;
    try {
      throw new RuntimeException();
    } catch (Throwable t) {
      stack = t.getStackTrace();
    }

    assertNotNull(stack);
    assertTrue(stack.length > 0);

    boolean found = false;
    for (int i = 0, j = stack.length; i < j; i++) {
      StackTraceElement elt = stack[i];
      String value = elt.getMethodName();
      assertNotNull(value);
      assertTrue(value.length() > 0);
      assertEquals(value.length(), value.trim().length());

      found |= myName.equals(value);
    }

    assertTrue("Did not find " + myName + " in the stack " + stack, found);
  }

  private static JsArrayString countDown(int count) {
    if (count > 0) {
      return countDown(count - 1);
    } else {
      return StackTraceCreator.createStackTrace();
    }
  }

  private static native String testStackTracesName() /*-{
    var fn = @com.google.gwt.core.client.impl.StackTraceCreatorTest::testStackTraces();
    return @com.google.gwt.core.client.impl.StackTraceCreator::extractNameFromToString(Ljava/lang/String;)(fn.toString());
  }-*/;

  private static native void throwNative() /*-{
    throw new Error();
  }-*/;

  private static native String throwNativeName() /*-{
    var fn = @com.google.gwt.core.client.impl.StackTraceCreatorTest::throwNative();
    return @com.google.gwt.core.client.impl.StackTraceCreator::extractNameFromToString(Ljava/lang/String;)(fn.toString());
  }-*/;

  public void testExtractName() {
    assertEquals("anonymous",
        StackTraceCreator.extractNameFromToString("function(){}"));
    assertEquals("anonymous",
        StackTraceCreator.extractNameFromToString("function (){}"));
    assertEquals("anonymous",
        StackTraceCreator.extractNameFromToString("function  (){}"));
    assertEquals("foo",
        StackTraceCreator.extractNameFromToString("function foo(){}"));
    assertEquals("foo",
        StackTraceCreator.extractNameFromToString("function foo (){}"));
  }
}
