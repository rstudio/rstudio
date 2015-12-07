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

import static com.google.gwt.core.client.impl.StackTraceCreator.extractFunctionName;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.impl.StackTraceCreator.CollectorModern;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests different {@link StackTraceCreator.Collector} implementations.
 */
@DoNotRunWith(Platform.Devel)
public class StackTraceCreatorCollectorTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Core";
  }

  public void testExtractName() {
    assertEquals("anonymous", extractFunctionName("function(){}"));
    assertEquals("anonymous", extractFunctionName("function (){}"));
    assertEquals("anonymous", extractFunctionName("  function (){}"));
    assertEquals("anonymous", extractFunctionName("function  (){}"));
    assertEquals("foo", extractFunctionName("function foo(){}"));
    assertEquals("foo", extractFunctionName("function foo (){}"));
    assertEquals("foo", extractFunctionName("  function foo (){}"));
    // In an unlikely case if somebody overrides fn.toString
    assertEquals("anonymous", extractFunctionName("abc"));
  }

  public void testChrome_25() {
    StackTraceElement[] expected = new StackTraceElement[] {
        createSTE("$third", "http://www.example.com/test/ABCD.cache.js@10", 300),
        createSTE("$second", "http://www.example.com/test/ABCD.cache.js@10", 200),
        createSTE("$first", "http://www.example.com/test/ABCD.cache.js@10", 100),
        createSTE("$entry0", "http://www.example.com/test/ABCD.cache.js@10", 50),
    };
    assertStackTrace(StackTraceExamples.chrome_25(), expected);
  }

  public void testChrome_31_file() {
    StackTraceElement[] expected = new StackTraceElement[] {
        createSTE("dumpException6", "file:///E:/test/ExceptionLab.html@20", 82),
        createSTE("onclick", "file:///E:/test/ExceptionLab.html@122", 101),
    };
    assertStackTrace(StackTraceExamples.chrome_31_file(), expected);
  }

  // Asserts expected behavior but it is broken
  public void _disabled_testChrome_31_multiline() {
    StackTraceElement[] expected = new StackTraceElement[] {
        createSTE("dumpException6", "http://www.example.com/test/ABCD.cache.js@20", 82),
        createSTE("onclick", "http://www.example.com/test/ABCD.cache.js@122", 101),
    };
    assertStackTrace(StackTraceExamples.chrome_31_multiline(), expected);
  }

  public void testAndroid_gingerbread() {
    StackTraceElement[] expected = new StackTraceElement[] {
        createSTE("Kj", "http://www.example.com/test/ABCD.cache.js@9", 300),
        createSTE("$third", "http://www.example.com/test/ABCD.cache.js@10", 300),
        createSTE("$second", "http://www.example.com/test/ABCD.cache.js@10", 200),
        createSTE("$first", "http://www.example.com/test/ABCD.cache.js@10", 100),
        createSTE("$entry0", "http://www.example.com/test/ABCD.cache.js@10", 50),
        createSTE("anonymous", "http://www.example.com/test/ABCD.cache.js@10", 40),
    };
    assertStackTrace(StackTraceExamples.android_gingerbread(), expected);
  }

  // Asserts expected behavior but it is broken
  public void _disabled_testSafari_6() {
    StackTraceElement[] expected = new StackTraceElement[] {
        createSTE("anonymous", "http://www.example.com/test/ABCD.cache.js", 48),
        createSTE("dumpException3", "http://www.example.com/test/ABCD.cache.js", 52),
        createSTE("onclick", "http://www.example.com/test/ABCD.cache.js", 82),
        createSTE("anonymous", "Unknown", -1),
    };
    assertStackTrace(StackTraceExamples.safari_6(), expected);
  }

  // Asserts current broken behavior
  public void testSafari_6_broken() {
    StackTraceElement[] expected = new StackTraceElement[] {
        createSTE("dumpException3", "http@52", -1),
        createSTE("onclick", "http@82", -1),
        createSTE("anonymous", "Unknown@-1", -1),
    };
    assertStackTrace(StackTraceExamples.safari_6(), expected);
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
    assertStackTrace(StackTraceExamples.safari_6_ios(), expected);
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
    assertStackTrace(StackTraceExamples.safari_6_ios(), expected);
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
        createSTE("anonymous", "stacktrace.js", 24),
        createSTE("anonymous", "stacktrace.js", -1),
    };
    assertStackTrace(StackTraceExamples.firefox_3_6(), expected);
  }

  // Asserts current broken behavior
  public void testFirefox_3_6_broken() {
    StackTraceElement[] expected = new StackTraceElement[] {
        createSTE("anonymous", "Unknown@-1", -1),
        createSTE("printStackTrace", "Unknown@-1", -1),
        createSTE("bar", "Unknown@-1", -1),
        createSTE("bar", "Unknown@-1", -1),
        createSTE("foo", "Unknown@-1", -1),
        createSTE("anonymous", "http@24", -1),
        createSTE("anonymous", "Unknown@-1", -1),
        createSTE("anonymous", "Unknown@-1", -1),
    };
    assertStackTrace(StackTraceExamples.firefox_3_6(), expected);
  }

  // Asserts expected behavior but it is broken
  public void _disabled_testFirefox_22() {
    StackTraceElement[] expected = new StackTraceElement[] {
        createSTE("anonymous", "http://www.example.com/test/ABCD.cache.js", 4),
        createSTE("createException", "http://www.example.com/test/ABCD.cache.js", 8),
        createSTE("createException4", "http://www.example.com/test/ABCD.cache.js", 56),
        createSTE("dumpException4", "http://www.example.com/test/ABCD.cache.js", 60),
        createSTE("Ul", "http://www.example.com/test/ABCD.cache.js", 7),
        createSTE("onclick", "http://www.example.com/test/ABCD.cache.js", 1),
        createSTE("anonymous", "Unknown", -1),
    };
    assertStackTrace(StackTraceExamples.firefox_22(), expected);
  }

  // Asserts current broken behavior
  public void testFirefox_22_broken() {
    StackTraceElement[] expected = new StackTraceElement[] {
        createSTE("createException", "http@8", -1),
        createSTE("createException4", "http@56", -1),
        createSTE("dumpException4", "http@60", -1),
        createSTE("xyz", "http@7", -1),
        createSTE("onclick", "http@1", -1),
        createSTE("anonymous", "Unknown@-1", -1),
    };
    assertStackTrace(StackTraceExamples.firefox_22(), expected);
  }

  // Asserts expected behavior but it is broken
  public void testIE_10() {
    StackTraceElement[] expected = new StackTraceElement[] {
        createSTE("anonymous", "http://www.example.com/test/ABCD.cache.js@13", 48),
        createSTE("dumpException3", "http://www.example.com/test/ABCD.cache.js@9", 46),
        createSTE("onclick", "http://www.example.com/test/ABCD.cache.js@1", 82),
    };
    assertStackTrace(StackTraceExamples.ie_10(), expected);
  }

  private static StackTraceElement createSTE(String methodName, String fileName, int lineNumber) {
    return new StackTraceElement("Unknown", methodName, fileName, lineNumber);
  }

  private static void assertStackTrace(JavaScriptObject jsError, StackTraceElement[] expected) {
    assertEquals(expected, new CollectorModern().getStackTrace(link(new Throwable(), jsError)));
  }

  private static native Object link(Throwable t, JavaScriptObject jsError) /*-{
    t.@Throwable::backingJsObject = jsError;
    return t;
  }-*/;

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
