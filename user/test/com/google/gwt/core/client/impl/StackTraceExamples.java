/*
 * Copyright 2014 Google Inc.
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

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Example stack traces from various browsers.
 * <p>
 * Some samples are extracted from
 * https://github.com/stacktracejs/stacktrace.js/blob/master/test/CapturedExceptions.js
 */
public class StackTraceExamples {

  // Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.22 (KHTML, like Gecko)
  // Chrome/25.0.1364.97 Safari/537.22
  public static native JavaScriptObject chrome_25() /*-{
    return {
        message: "Cannot read property 'length' of undefined",
        name: "TypeError",
        stack: "TypeError: Cannot read property 'length' of undefined\n" +
            "    at $third (http://www.example.com/test/ABCD.cache.js:300:10)\n" +
            "    at $second (http://www.example.com/test/ABCD.cache.js:200:10)\n" +
            "    at $first (http://www.example.com/test/ABCD.cache.js:100:10)\n" +
            "    at $entry0 (http://www.example.com/test/ABCD.cache.js:50:10)"
    };
  }-*/;

  public static native JavaScriptObject chrome_31_multiline() /*-{
    return {
      message: "Object function () {\n" +
          "                return {\n" +
          "                    name: \"provide multi-line source in exception\"\n" +
          "                };\n" +
          "            } has no method 'nonExistentMethod'",
      name: "TypeError",
      stack: "TypeError: Object function () {\n" +
          "                return {\n" +
          "                    name: \"provide multi-line source in exception\"\n" +
          "                };\n" +
          "            } has no method 'nonExistentMethod'\n" +
          "    at dumpException6 (http://www.example.com/test/ExceptionLab.html:82:20)\n" +
          "    at HTMLButtonElement.onclick (http://www.example.com/test/ExceptionLab.html:101:122)"
    };
  }-*/;

  public static native JavaScriptObject chrome_31_file() /*-{
    return {
      message: "N/A",
      name: "TypeError",
      stack: "TypeError: N/A\n" +
          "    at dumpException6 (file:///E:/test/ExceptionLab.html:82:20)\n" +
          "    at HTMLButtonElement.onclick (file:///E:/test/ExceptionLab.html:101:122)"
    };
  }-*/;

  public static native JavaScriptObject safari_6() /*-{
    return {
      message: "'null' is not an object (evaluating 'x.undef')",
      stack: "@file:///Users/test//ExceptionLab.html:48\n" +
          "dumpException3@file:///Users/test/ExceptionLab.html:52\n" +
          "onclick@file:///Users/test/ExceptionLab.html:82\n" +
          "[native code]",
      line: 48,
      sourceURL: "file:///Users/eric/src/javascript-stacktrace/test/functional/ExceptionLab.html"
    };
  }-*/;

  // Mozilla/5.0 (iPad; CPU OS 6_1 like Mac OS X) AppleWebKit/536.26 (KHTML, like Gecko)
  // Version/6.0 Mobile/10B141 Safari/8536.25
  public static native JavaScriptObject safari_6_ios() /*-{
    return {
        message: "Cannot read property 'length' of undefined",
        name: "TypeError",
        stack: "$third@http://www.example.com/test/ABCD.cache.js:300\n" +
            "$second@http://www.example.com/test/ABCD.cache.js:200\n" +
            "$first@http://www.example.com/test/ABCD.cache.js:100\n" +
            "$entry0@http://www.example.com/test/ABCD.cache.js:50\n" +
            "@http://www.example.com/test/ABCD.cache.js:10\n" +
            "@http://www.example.com/test/ABCD.cache.js:5\n" +
            "[native code]"
    };
  }-*/;

  public static native JavaScriptObject firefox_3_6() /*-{
    return {
      fileName: "http://127.0.0.1:8000/js/stacktrace.js",
      lineNumber: 44,
      message: "this.undef is not a function",
      name: "TypeError",
      stack: "()@http://127.0.0.1:8000/js/stacktrace.js:44\n" +
          "(null)@http://127.0.0.1:8000/js/stacktrace.js:31\n" +
          "printStackTrace()@http://127.0.0.1:8000/js/stacktrace.js:18\n" +
          "bar(1)@http://127.0.0.1:8000/js/test/functional/testcase1.html:13\n" +
          "bar(2)@http://127.0.0.1:8000/js/test/functional/testcase1.html:16\n" +
          "foo()@http://127.0.0.1:8000/js/test/functional/testcase1.html:20\n" +
          "@http://127.0.0.1:8000/js/test/functional/testcase1.html:24\n" +
          ""
    };
  }-*/;

  public static native JavaScriptObject firefox_22() /*-{
    return {
      message: "x is null",
      name: "TypeError",
      stack: "@http://www.example.com/ExceptionLab.js:4\n" +
          "createException@http://www.example.com/ExceptionLab.js:8\n" +
          "createException4@http://www.example.com/ExceptionLab.html:56\n" +
          "dumpException4@http://www.example.com/ExceptionLab.html:60\n" +
          "onclick@http://www.example.com/ExceptionLab.html:1\n" +
          "",
      fileName: "http://www.example.com/ExceptionLab.html",
      lineNumber: 4,
      columnNumber: 6
    };
  }-*/;

  public static native JavaScriptObject ie_10() /*-{
    return {
      message: "Unable to get property 'undef' of undefined or null reference",
      name: "TypeError",
      stack: "TypeError: Unable to get property 'undef' of undefined or null reference\n" +
          "   at Anonymous function (http://www.example.com/ExceptionLab.html:48:13)\n" +
          "   at dumpException3 (http://www.example.com/ExceptionLab.html:46:9)\n" +
          "   at onclick (http://www.example.com/ExceptionLab.html:82:1)",
      description: "Unable to get property 'undef' of undefined or null reference",
      number: -2146823281
    };
  }-*/;
}
