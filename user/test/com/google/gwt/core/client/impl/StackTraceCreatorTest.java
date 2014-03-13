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
import com.google.gwt.core.client.impl.StackTraceCreator.Collector;
import com.google.gwt.core.client.impl.StackTraceCreator.CollectorChrome;
import com.google.gwt.core.client.impl.StackTraceCreator.CollectorChromeNoSourceMap;
import com.google.gwt.core.client.impl.StackTraceCreator.CollectorMoz;
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
      return GWT.<Collector> create(Collector.class).collect();
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
        new Collector().extractName("function(){}"));
    assertEquals("anonymous",
        new Collector().extractName("function (){}"));
    assertEquals("anonymous",
        new Collector().extractName("  function (){}"));
    assertEquals("anonymous",
        new Collector().extractName("function  (){}"));
    assertEquals("foo",
        new Collector().extractName("function foo(){}"));
    assertEquals("foo",
        new Collector().extractName("function foo (){}"));
    assertEquals("foo",
        new Collector().extractName("  function foo (){}"));
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
    
    // iOS style
    assertEquals("functionName@@file.js:1",
        c.extractName("functionName@file.js:1"));
  }

  public void testFirefox14ExtractName() {
    StackTraceCreator.CollectorMoz c = new StackTraceCreator.CollectorMoz();

    assertEquals("anonymous", c.extractName("@file.js:1"));
    assertEquals("functionName",
        c.extractName("functionName@file.js:1"));
  }

  public void testChrome_25() {
    StackTraceElement[] expected = new StackTraceElement[] {
        createSTE("$third", "http://www.example.com/test/ABCD.cache.js@10", 300),
        createSTE("$second", "http://www.example.com/test/ABCD.cache.js@10", 200),
        createSTE("$first", "http://www.example.com/test/ABCD.cache.js@10", 100),
        createSTE("$entry0", "http://www.example.com/test/ABCD.cache.js@10", 50),
    };
    assertStackTrace(StackTraceExamples.chrome_25(), new CollectorChrome(), expected);
  }

  public void testChrome_31_file() {
    StackTraceElement[] expected = new StackTraceElement[] {
        createSTE("dumpException6", "file:///E:/test/ExceptionLab.html@20", 82),
        createSTE("onclick", "file:///E:/test/ExceptionLab.html@122", 101),
    };
    assertStackTrace(StackTraceExamples.chrome_31_file(), new CollectorChrome(), expected);
  }

  // Asserts expected behavior but it is broken
  public void _disabled_testChrome_31_multiline() {
    StackTraceElement[] expected = new StackTraceElement[] {
        createSTE("dumpException6", "http://www.example.com/test/ExceptionLab.html@20", 82),
        createSTE("onclick", "http://www.example.com/test/ExceptionLab.html@122", 101),
    };
    assertStackTrace(StackTraceExamples.chrome_31_multiline(), new CollectorChrome(), expected);
  }

  // Asserts expected behavior but it is broken
  public void _disabled_testSafari_6() {
    StackTraceElement[] expected = new StackTraceElement[] {
        createSTE("anonymous", "file:///Users/test/ExceptionLab.html", 48),
        createSTE("dumpException3", "file:///Users/test/ExceptionLab.html", 52),
        createSTE("onclick", "file:///Users/test/ExceptionLab.html", 82),
        createSTE("anonymous", "Unknown", -1),
    };
    assertStackTrace(StackTraceExamples.safari_6(), new CollectorChromeNoSourceMap(), expected);
  }

  // Asserts current broken behavior
  public void testSafari_6_broken() {
    StackTraceElement[] expected = new StackTraceElement[] {
        createSTE("dumpException3", "file@52", -1),
        createSTE("onclick", "file@82", -1),
        createSTE("anonymous", "Unknown@-1", -1),
    };
    assertStackTrace(StackTraceExamples.safari_6(), new CollectorChromeNoSourceMap(), expected);
  }

  // Asserts expected behavior but it is broken
  public void _disabled_testSafari_6_ios() {
    StackTraceElement[] expected = new StackTraceElement[] {
        createSTE("$third", "http://www.example.com/test/ABCD.cache.js", 300),
        createSTE("$second", "http://www.example.com/test/ABCD.cache.js", 200),
        createSTE("$first", "http://www.example.com/test/ABCD.cache.js", 100),
        createSTE("$entry0", "http://www.example.com/test/ABCD.cache.js", 50),
        createSTE("anonymous", "http://www.example.com/test/ABCD.cache.js", 10),
        createSTE("anonymous", "http://www.example.com/test/ABCD.cache.js", 5),
        createSTE("anonymous", "Unknown", -1),
    };
    assertStackTrace(StackTraceExamples.safari_6_ios(), new CollectorChromeNoSourceMap(), expected);
  }

  // Asserts current broken behavior
  public void testSafari_6_ios_broken() {
    StackTraceElement[] expected = new StackTraceElement[] {
        createSTE("$third", "http@300", -1),
        createSTE("$second", "http@200", -1),
        createSTE("$first", "http@100", -1),
        createSTE("$entry0", "http@50", -1),
        createSTE("anonymous", "http@10", -1),
        createSTE("anonymous", "http@5", -1),
        createSTE("anonymous", "Unknown@-1", -1),
    };
    assertStackTrace(StackTraceExamples.safari_6_ios(), new CollectorChromeNoSourceMap(), expected);
  }

  // Asserts expected behavior but it is broken
  public void _disabled_testFirefox_3_6() {
    StackTraceElement[] expected = new StackTraceElement[] {
        createSTE("anonymous", "stacktrace.js", 44),
        createSTE("anonymous", "stacktrace.js", 31),
        createSTE("printStackTrace", "stacktrace.js", 18),
        createSTE("bar", "stacktrace.js", 13),
        createSTE("bar", "stacktrace.js", 16),
        createSTE("foo", "stacktrace.js", 20),
        createSTE("", "stacktrace.js", 24),
        createSTE("", "", -1),
    };
    assertStackTrace(StackTraceExamples.firefox_3_6(), new CollectorMoz(), expected);
  }

  // Asserts current broken behavior
  public void testFirefox_3_6_broken() {
    StackTraceElement[] expected = new StackTraceElement[] {
        createSTE("anonymous", null, -1),
        createSTE("anonymous", null, -1),
        createSTE("printStackTrace", null, -1),
        createSTE("bar", null, -1),
        createSTE("bar", null, -1),
        createSTE("foo", null, -1),
        createSTE("anonymous", null, -1),
        createSTE("anonymous", null, -1),
    };
    assertStackTrace(StackTraceExamples.firefox_3_6(), new CollectorMoz(), expected);
  }

  // Asserts expected behavior but it is broken
  public void _disabled_testFirefox_22() {
    StackTraceElement[] expected = new StackTraceElement[] {
        createSTE("anonymous", "http://www.example.com/ExceptionLab.html", 4),
        createSTE("createException", "http://www.example.com/ExceptionLab.html", 8),
        createSTE("createException4", "http://www.example.com/ExceptionLab.html", 56),
        createSTE("dumpException4", "http://www.example.com/ExceptionLab.html", 60),
        createSTE("onclick", "http://www.example.com/ExceptionLab.html", 1),
        createSTE("anonymous", "Unknown", -1),
    };
    assertStackTrace(StackTraceExamples.firefox_22(), new CollectorMoz(), expected);
  }

  // Asserts current broken behavior
  public void testFirefox_22_broken() {
    StackTraceElement[] expected = new StackTraceElement[] {
        createSTE("anonymous", null, -1),
        createSTE("createException", null, -1),
        createSTE("createException4", null, -1),
        createSTE("dumpException4", null, -1),
        createSTE("onclick", null, -1),
        createSTE("anonymous", null, -1),
    };
    assertStackTrace(StackTraceExamples.firefox_22(), new CollectorMoz(), expected);
  }

  // Asserts expected behavior but it is broken
  // TODO(goktug): IE10 actually uses default collector so we are manually generating the stack.
  public void _disabled_testIE_10() {
    StackTraceElement[] expected = new StackTraceElement[] {
        createSTE("anonymous", "http://www.example.com/ExceptionLab.html@13", 48),
        createSTE("dumpException4", "http://www.example.com/ExceptionLab.html@9", 46),
        createSTE("onclick", "http://www.example.com/ExceptionLab.html@1", 82),
    };
    assertStackTrace(StackTraceExamples.ie_10(), new CollectorMoz(), expected);
  }

  private static StackTraceElement createSTE(String methodName, String fileName, int lineNumber) {
    return new StackTraceElement("Unknown", methodName, fileName, lineNumber);
  }

  private static void assertStackTrace(JavaScriptObject exception, CollectorMoz collector,
      StackTraceElement[] expected) {
    JsArrayString stack = collector.inferFrom(exception);
    assertEquals(expected, collector.getStackTrace(stack));
  }

  private static void assertEquals(StackTraceElement[] expecteds, StackTraceElement[] actuals) {
    assertEquals("Traces differ in size", expecteds.length, actuals.length);

    for (int i = 0; i < expecteds.length; i++) {
      StackTraceElement expected = expecteds[i];
      StackTraceElement actual = actuals[i];

      // Equals is missing from StackTraceElement's emulation, we need to manually compare
      assertEquals(actual.toString(), expected.getClassName(), actual.getClassName());
      assertEquals(actual.toString(), expected.getMethodName(), actual.getMethodName());
      assertEquals(actual.toString(), expected.getFileName(), actual.getFileName());
      assertEquals(actual.toString(), expected.getLineNumber(), actual.getLineNumber());
    }
  }
}
