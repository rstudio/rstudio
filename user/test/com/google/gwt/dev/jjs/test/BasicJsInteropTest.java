/*
 * Copyright 2015 Google Inc.
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
package com.google.gwt.dev.jjs.test;

import com.google.gwt.junit.client.GWTTestCase;

import jsinterop.annotations.JsType;

/**
 * Tests for JsInterop that should work with -nogenerateJsInteropExports.
 */
public class BasicJsInteropTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  /**
   * Tests that a native field that is not written to in Java code (but has some initial value in
   * the native implementation) is not assumed to be null.
   */
  @JsType(isNative = true)
  static class A {
    public String field;
  }

  private native A createA() /*-{
    return { field: "AA" };
  }-*/;

  public void testANotPruned() {
    A a = createA();
    assertEquals("aa", a.field.toLowerCase());
  }
}
