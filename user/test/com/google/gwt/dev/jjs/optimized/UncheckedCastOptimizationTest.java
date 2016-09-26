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
package com.google.gwt.dev.jjs.optimized;

import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;

import javaemul.internal.annotations.UncheckedCast;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * Tests that unchecked casts allow other optimizations to happen.
 */
@DoNotRunWith(Platform.Devel)
public class UncheckedCastOptimizationTest extends OptimizationTestBase {

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Array")
  private static class JsArray {
    public int length;
  }

  @JsType(isNative = true)
  private interface NativeObject {
  }

  @UncheckedCast
  public static <T> T uncheckedCast(Object o) {
    return (T) o;
  }
  public static int unckeckedCastOperation() {
    JsArray array = uncheckedCast(new NativeObject[] {null, null});
    return array.length;
  }

  private static native String getGeneratedUncheckedCastFunctionDefinition() /*-{
    return function() {
      @com.google.gwt.dev.jjs.optimized.UncheckedCastOptimizationTest::unckeckedCastOperation()();
    }.toString();
  }-*/;

  public void testUncheckedCastAllowsOptimizaionsC() throws Exception {
    String functionDef = getGeneratedUncheckedCastFunctionDefinition();
    assertFunctionMatches(functionDef, "var<obf>;<obf>=[null,null]");
  }
}
