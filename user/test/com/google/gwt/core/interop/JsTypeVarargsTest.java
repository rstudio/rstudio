/*
 * Copyright 2013 Google Inc.
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

import static jsinterop.annotations.JsPackage.GLOBAL;

import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.junit.client.GWTTestCase;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * Tests JsType functionality.
 */
@SuppressWarnings("cast")
public class JsTypeVarargsTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Interop";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    ScriptInjector.fromString(
        "function JsTypeVarargsTest_MyNativeJsType() {}\n"
            + "function JsTypeVarargsTest_MyNativeJsTypeVarargsConstructor(i) {"
            + " this.a = arguments[i]; this.b = arguments.length; }\n")
        .setWindow(ScriptInjector.TOP_WINDOW)
        .inject();
    setupGlobal();
  }

  // $global always points to scope of exports
  private native void setupGlobal() /*-{
    $global = window.goog && window.goog.global || $wnd;
    $wnd.$global = $global;
  }-*/;

  @JsMethod
  public static native int varargsMethod1(Object... varargs) /*-{
    return arguments.length;
  }-*/;

  @JsMethod
  public static int varargsMethod2(Object... varargs) {
    return varargs.length;
  }

  @JsMethod(namespace = JsPackage.GLOBAL)
  public static Object varargsMethod3(int slot, Object... varargs) {
    return varargs[slot];
  }

  @JsMethod(namespace = JsPackage.GLOBAL)
  public static Object[] varargsMethod4(int slot, Object... varargs) {
    varargs[slot] = null;
    return varargs;
  }

  private static native Object callVarargsMethod3FromJSNI() /*-{
    return $global.varargsMethod3(2, "1", "2", "3", "4");
  }-*/;

  @JsType(isNative = true, namespace = GLOBAL, name = "Object")
  static class NativeJsType {
  }

  @JsType(isNative = true, namespace = GLOBAL,
      name = "JsTypeVarargsTest_MyNativeJsTypeVarargsConstructor")
  static class NativeJsTypeWithVarargsConstructor {
    public Object a;
    public int b;
    NativeJsTypeWithVarargsConstructor(int i, Object... args) { }
  }

  static class SubclassNativeWithVarargsConstructor extends NativeJsTypeWithVarargsConstructor {
    SubclassNativeWithVarargsConstructor(String s, Object... args) {
      super(1, args[0], args[1], null);
    }

    SubclassNativeWithVarargsConstructor(int i, Object... args) {
      super(i, args);
    }

    @JsMethod
    Object varargsMethod(int i, Object... args) {
      return args[i];
    }
  }

  static class SubSubclassNativeWithVarargsConstructor
      extends SubclassNativeWithVarargsConstructor {
    SubSubclassNativeWithVarargsConstructor() {
      super(0, null);
    }

    Object varargsMethod(int i, Object... args) {
      return super.varargsMethod(i, args);
    }

    Object nonJsVarargsMethod() {
      return super.varargsMethod(1, null ,this);
    }
  }

  public void testVarargsCall_regularMethods() {
    assertEquals(3, varargsMethod1("A", "B", "C"));
    assertEquals(4, varargsMethod2("A", "B", "C", "D"));
    assertEquals(2, varargsMethod1(new NativeJsType[]{null, null}));
    assertEquals(5, varargsMethod2(new NativeJsType[]{null, null, null, null, null}));
    assertEquals("C", varargsMethod3(2, "A", "B", "C", "D"));
    assertEquals("3", callVarargsMethod3FromJSNI());
    assertNull(varargsMethod4(1, "A", "B", "C")[1]);
    assertEquals("A", varargsMethod4(1, "A", "B", "C")[0]);
    assertEquals(3, varargsMethod4(1, "A", "B", "C").length);
  }

  public void testVarargsCall_constructors() {
    NativeJsType someNativeObject = new NativeJsType();
    NativeJsTypeWithVarargsConstructor object =
        new NativeJsTypeWithVarargsConstructor(1, someNativeObject, null);

    assertSame(someNativeObject, object.a);
    assertEquals(3, object.b);

    Object[] params = new Object[] { someNativeObject, null };
    object = new NativeJsTypeWithVarargsConstructor(1, params);

    assertSame(someNativeObject, object.a);
    assertEquals(3, object.b);

    object = new SubclassNativeWithVarargsConstructor("", someNativeObject, null);

    assertSame(someNativeObject, object.a);
    assertEquals(4, object.b);

    object = new SubclassNativeWithVarargsConstructor(1, someNativeObject, null);

    assertSame(someNativeObject, object.a);
    assertEquals(3, object.b);
  }

  @JsMethod(namespace = JsPackage.GLOBAL)
  public static Double sumAndMultiply(Double multiplier, Double... numbers) {
    double result = 0.0d;
    for (double d : numbers) {
      result += d;
    }
    result *= multiplier;
    return result;
  }

  @JsMethod(namespace = JsPackage.GLOBAL)
  public static int sumAndMultiplyInt(int multiplier, int... numbers) {
    int result = 0;
    for (int d : numbers) {
      result += d;
    }
    result *= multiplier;
    return result;
  }

  @JsFunction
  interface Function {
    Object f(int i, Object... args);
  }

  static class AFunction implements Function {

    @Override
    public Object f(int i, Object... args) {
      return args[i];
    }
    static Function create() {
      return new AFunction();
    }
  }

  public native void testVarargsCall_fromJavaScript() /*-{
    @GWTTestCase::assertEquals(DDD)(60, $global.sumAndMultiply(2, 10, 20), 0);
    @GWTTestCase::assertEquals(II)(30, $global.sumAndMultiplyInt(3, 2, 8));
    var f = @JsTypeVarargsTest.AFunction::create()()
    @GWTTestCase::assertSame(Ljava/lang/Object;Ljava/lang/Object;)(
        f, f(2, null, null,  f,  null));
  }-*/;

  public void testVarargsCall_jsFunction() {
    Function function = new AFunction();
    assertSame(function, function.f(2, null, null, function, null));
    assertSame(null, function.f(1, null, null, function, null));
  }

  public void testVarargsCall_superCalls() {
    SubSubclassNativeWithVarargsConstructor object = new SubSubclassNativeWithVarargsConstructor();
    assertSame(object, object.nonJsVarargsMethod());
    assertSame(object, object.varargsMethod(1, null, object, null));
  }

  private static int sideEffectCount;
  private SubclassNativeWithVarargsConstructor doSideEffect(
      SubclassNativeWithVarargsConstructor obj) {
    sideEffectCount++;
    return obj;
  }
  public void testVarargsCall_sideEffectingInstance() {
    SubclassNativeWithVarargsConstructor object = new SubclassNativeWithVarargsConstructor(0, null);
    sideEffectCount = 0;
    Object[] params = new Object[] { object, null };
    assertSame(object, doSideEffect(object).varargsMethod(0, params));
    assertSame(1, sideEffectCount);
  }
}
