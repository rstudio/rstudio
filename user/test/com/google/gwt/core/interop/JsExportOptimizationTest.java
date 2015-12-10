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
package com.google.gwt.core.interop;

import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.junit.client.GWTTestCase;

import javaemul.internal.annotations.DoNotInline;
import jsinterop.annotations.JsMethod;

/**
 * Tests JsInterop performs correctly under tricky optimization scenarios.
 */
public class JsExportOptimizationTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Interop";
  }

  @Override
  public void gwtSetUp() throws Exception {
    setupGlobal();
  }

  // $global always points to scope of exports
  private native void setupGlobal() /*-{
    $global = window.goog && window.goog.global || $wnd;
    $wnd.$global = $global;
  }-*/;

  public void testNoTypeTightenParams() {
    // If we type-tighten, java side will see no calls and think that parameter could only be null.
    // As a result, it will be optimized to null.nullMethod().
    ScriptInjector.fromString("$global.callBar($global.newA());").inject();
    assertTrue(MyClassExportsMethod.calledFromBar);

    // If we type-tighten, java side will only see a call to subclass and think that parameter could
    // be optimized to that one. As a result, the method call will be inlined.
    MyClassExportsMethod.callFoo(new MyClassExportsMethod.SubclassOfA());
    ScriptInjector.fromString("$global.callFoo($global.newA());").inject();
    assertTrue(MyClassExportsMethod.calledFromFoo);
  }

  public void testNoSameParameterValueOptimization() {
    assertEquals("L", X.m("L"));
    assertEquals("M", callM("M"));
  }

  static class X {
    @JsMethod
    @DoNotInline
    public static String m(String s) {
      return s;
    }
  }

  @JsMethod(namespace = "$global.woo.JsExportOptimizationTest.X", name = "m")
  private static native String callM(String s);
}
