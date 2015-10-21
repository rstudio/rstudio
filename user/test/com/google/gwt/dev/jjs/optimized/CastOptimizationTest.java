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
package com.google.gwt.dev.jjs.optimized;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;

import java.util.Random;

import jsinterop.annotations.JsType;

/**
 * Tests cast checks are optimized out when cast checking is disabled.
 */
@DoNotRunWith(Platform.Devel)
public class CastOptimizationTest extends OptimizationTestBase {

  private static class TestObject { }

  @JsType(isNative = true)
  private interface JsTypeTestInterface { }

  private interface DualJsoTestInterface { }

  private static class JsoTestObject extends JavaScriptObject implements DualJsoTestInterface {
    protected JsoTestObject() { }
  }

  private static Object field;

  @Override
  protected void gwtSetUp() throws Exception {
    field = createField();
  }

  private static Object createField() {
    // Makes sure that field type is not upgradable even the compiler becomes really smart and also
    // no types are pruned otherwise casts can be statically evaluated.
    switch (new Random().nextInt(42)) {
      case 0:
        return new TestObject();
      case 1:
        return JavaScriptObject.createObject();
      case 2:
        return new DualJsoTestInterface() { };
      case 3:
        return "Some string";
      default:
        return null;
    }
  }

  public static TestObject castOp() {
    return ((TestObject) field);
  }

  public static JavaScriptObject castOpJso() {
    return ((JavaScriptObject) field);
  }

  public static DualJsoTestInterface castOpDualJso() {
    return ((DualJsoTestInterface) field);
  }

  public static JavaScriptObject castOpJsType() {
    return ((JavaScriptObject) field);
  }

  public static String castOpString() {
    return ((String) field);
  }

  private static native String getGeneratedFunctionDefinition() /*-{
    return function() {
      @CastOptimizationTest::castOp()();
      @CastOptimizationTest::castOpJso()();
      @CastOptimizationTest::castOpDualJso()();
      @CastOptimizationTest::castOpJsType()();
      @CastOptimizationTest::castOpString()();
    }.toString();
  }-*/;

  public void testCastsAreRemoved() throws Exception {
    String functionDef = getGeneratedFunctionDefinition();
    assertFunctionMatches(functionDef, "");
  }
}
