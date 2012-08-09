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
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.impl.StackTraceCreator.CollectorChrome;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests StackTraceCreator in Production Mode. The methods in this test class
 * are static so that their names can be reliably determined in Production Mode.
 */
public class StackTraceCreatorTest extends GWTTestCase {
  
  public static void testJavaScriptException() {
    JsArrayString start = sample();
    Throwable t = null;
    try {
      throwNative();
      fail("No exception thrown");
    } catch (JavaScriptException e) {
      /*
       * Some browsers may or may not be able to implement this at all, so we'll
       * at least make sure that an array is returned;
       */
      assertNotNull(e.getStackTrace());
      if (e.getStackTrace().length == 0) {
        assertTrue("Development Mode", GWT.isScript());
        return;
      } else {
        t = e;
      }
    }

    String myName = null;
    if (!GWT.isScript()) {
      myName = "testJavaScriptException";
    } else {
      myName = Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceCreatorTest::testJavaScriptException()");
    }

    checkStack(myName, t);

    JsArrayString end = sample();
    assertEquals(start, end);
  }

  /**
   * Just make sure that reentrant behavior doesn't fail.
   */  
  public static void testReentrantCalls() {
    if (!GWT.isScript()) {
      // sample is useless in Development Mode
      return;
    }

    JsArrayString start = sample();

    JsArrayString stack = countDown(5);
    assertNotNull(stack);
    assertTrue(stack.length() > 0);

    JsArrayString end = sample();
    assertEquals(start, end);
  }

  public static void testStackTraces() {
    JsArrayString start = sample();

    Throwable t;
    try {
      throw new RuntimeException();
    } catch (Throwable t2) {
      t = t2;
    }

    String myName = null;
    if (!GWT.isScript()) {
      myName = "testStackTraces";
    } else {
      myName = Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceCreatorTest::testStackTraces()");
    }

    checkStack(myName, t);

    JsArrayString end = sample();
    assertEquals(start, end);
  }

  private static void assertEquals(JsArrayString start, JsArrayString end) {
    assertEquals("length", start.length(), end.length());
    for (int i = 0, j = start.length(); i < j; i++) {
      assertEquals("frame " + i, start.get(i), end.get(i));
    }
  }

  private static void checkStack(String myName, Throwable t) {
    assertNotNull("myName", myName);
    assertNotNull("t", t);

    assertEquals("Needs a trim()", myName.trim(), myName);
    assertFalse("function", myName.startsWith("function"));
    assertFalse("(", myName.contains("("));

    StackTraceElement[] stack = t.getStackTrace();
    assertNotNull("stack", stack);
    assertTrue("stack.length", stack.length > 0);

    boolean found = false;
    StringBuilder observedStack = new StringBuilder();
    for (int i = 0, j = stack.length; i < j; i++) {
      StackTraceElement elt = stack[i];
      String value = elt.getMethodName();

      assertNotNull("value", value);
      assertTrue("value.length", value.length() > 0);
      assertEquals("value.trim()", value.length(), value.trim().length());

      observedStack.append("\n").append(value);
      found |= myName.equals(value);
    }

    assertTrue("Did not find " + myName + " in the stack " + observedStack,
        found);
  }

  private static JsArrayString countDown(int count) {
    if (count > 0) {
      return countDown(count - 1);
    } else {
      return sample();
    }
  }

  private static JsArrayString sample() {
    if (GWT.isScript()) {
      return StackTraceCreator.createStackTrace();
    } else {
      return JavaScriptObject.createArray().cast();
    }
  }

  private static native void throwNative() /*-{
    null.a();
  }-*/;

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Core";
  }

  public void testExtractName() {
    assertEquals("anonymous",
        StackTraceCreator.extractNameFromToString("function(){}"));
    assertEquals("anonymous",
        StackTraceCreator.extractNameFromToString("function (){}"));
    assertEquals("anonymous",
        StackTraceCreator.extractNameFromToString("  function (){}"));
    assertEquals("anonymous",
        StackTraceCreator.extractNameFromToString("function  (){}"));
    assertEquals("foo",
        StackTraceCreator.extractNameFromToString("function foo(){}"));
    assertEquals("foo",
        StackTraceCreator.extractNameFromToString("function foo (){}"));
    assertEquals("foo",
        StackTraceCreator.extractNameFromToString("  function foo (){}"));
  }

  public void testChromeExtractName() {
    CollectorChrome c = new CollectorChrome();

    assertEquals("anonymous@@file.js:1:2", c.extractName(" at file.js:1:2"));
    assertEquals("functionName@@file.js:1:2",
        c.extractName(" at functionName (file.js:1:2)"));
    assertEquals("functionName@@file.js:1:2",
        c.extractName(" at Type.functionName (file.js:1:2)"));
    assertEquals("functionName@@file.js:1:2",
        c.extractName(" at Type.functionName [as methodName] (file.js:1:2)"));
  }

  public void testFirefox14ExtractName() {
    StackTraceCreator.CollectorMoz c = new StackTraceCreator.CollectorMoz();

    assertEquals("anonymous", c.extractName("@file.js:1"));
    assertEquals("functionName",
        c.extractName("functionName@file.js:1"));
  }
}
