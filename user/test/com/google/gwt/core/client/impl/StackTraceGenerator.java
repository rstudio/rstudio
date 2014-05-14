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

import com.google.gwt.junit.client.GWTTestCase;

/**
 * A helper test to capture native stack traces from browsers via exception error message.
 */
public class StackTraceGenerator extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.StackTraceNoEmul";
  }

  public void testPrintStackTrace() {
    assertEquals("Intentionally failed to print stack trace", "fail", getTrace(3));
  }

  private String getTrace(int count) {
    return count > 1 ? getTrace(count - 1) : getTraceNative();
  }

  private static native String getTraceNative() /*-{
    function native1() {
      return native2();
    }
    function native2() {
      try {
        null.a();
        return null; // Shouldn't be reached
      } catch (e) {
        return e.stack;
      }
    }
    return native1();
  }-*/;
}
