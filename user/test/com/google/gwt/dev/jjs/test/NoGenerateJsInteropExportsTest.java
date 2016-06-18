/*
 * Copyright 2016 Google Inc.
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

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsType;

/**
 * Tests for JsInterop that require -nogenerateJsInteropExports.
 */
@DoNotRunWith(Platform.Devel)
public class NoGenerateJsInteropExportsTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  @JsType
  static class A {
    @JsMethod(name = "method")
    public void m() {
    }
  }

  @JsType(isNative = true)
  interface ObjectWithMethod {
    void method();
  }

  public void testJsMethodNameNotHonored() {
    Object o = new A();
    try {
      // As this test runs with -nogenerateJsInteropExports, all non native types
      // should not respect methods JsNames.
      ((ObjectWithMethod) o).method();
      fail("Should have failed");
    } catch (JavaScriptException expected) {
    }
  }
}
