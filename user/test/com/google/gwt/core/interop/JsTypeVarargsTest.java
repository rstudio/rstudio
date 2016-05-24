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

import javaemul.internal.annotations.DoNotInline;
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
  @DoNotInline
  public static native int varargsLengthThruArguments(Object... varargs) /*-{
    return arguments.length;
  }-*/;

  @JsMethod
  @DoNotInline
  public static int varargsLength(Object... varargs) {
    return varargs.length;
  }

  @JsMethod
  @DoNotInline
  public static int stringVarargsLength(String... varargs) {
    return varargs.length;
  }

  @JsMethod
  @DoNotInline
  public static int stringVarargsLengthV2(int i,  String... varargs) {
    return varargs.length;
  }

  @JsMethod(namespace = JsPackage.GLOBAL)
  @DoNotInline
  public static Object getVarargsSlot(int slot, Object... varargs) {
    return varargs[slot];
  }

  @JsMethod(namespace = JsPackage.GLOBAL)
  @DoNotInline
  public static Object[] clrearVarargsSlot(int slot, Object... varargs) {
    varargs[slot] = null;
    return varargs;
  }

  @JsMethod(namespace = JsPackage.GLOBAL)
  @DoNotInline
  public static Class<?> getVarargsArrayClass(String... varargs) {
    return varargs.getClass();
  }

  private static native Object callGetVarargsSlotUsingJsName() /*-{
    return $global.getVarargsSlot(2, "1", "2", "3", "4");
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
      super(0, new Object[0]);
    }

    Object varargsMethod(int i, Object... args) {
      return super.varargsMethod(i, args);
    }

    Object nonJsVarargsMethod() {
      return super.varargsMethod(1, null ,this);
    }
  }

  public void testVarargsCall_regularMethods() {
    assertEquals(3, varargsLengthThruArguments("A", "B", "C"));
    assertEquals(4, varargsLength("A", "B", "C", "D"));
    assertEquals(2, varargsLengthThruArguments(new NativeJsType[]{null, null}));
    assertEquals(5, varargsLength(new NativeJsType[]{null, null, null, null, null}));
    assertEquals("C", getVarargsSlot(2, "A", "B", "C", "D"));
    assertEquals("3", callGetVarargsSlotUsingJsName());
    assertNull(clrearVarargsSlot(1, "A", "B", "C")[1]);
    assertEquals("A", clrearVarargsSlot(1, "A", "B", "C")[0]);
    assertEquals(3, clrearVarargsSlot(1, "A", "B", "C").length);
    assertSame(String[].class, getVarargsArrayClass("A", "B", "C"));
  }
  public void testVarargsCall_edgeCases() {
    assertSame(String[].class, getVarargsArrayClass());
    assertSame(String[].class, getVarargsArrayClass(new String[0]));
    assertSame(String[].class, getVarargsArrayClass((String) null));
    try {
      assertSame(String[].class, getVarargsArrayClass(null));
      fail("Should have thrown exception");
    } catch (NullPointerException expected) {
    }
    try {
      assertSame(String[].class, getVarargsArrayClass((String[]) null));
      fail("Should have thrown exception");
    } catch (NullPointerException expected) {
    }

    assertEquals(0, stringVarargsLength());
    assertEquals(0, stringVarargsLength(new String[0]));
    assertEquals(1, stringVarargsLength((String) null));
    try {
      assertEquals(0, stringVarargsLength(null));
      fail("Should have thrown exception");
    } catch (NullPointerException expected) {
    }
    try {
      assertEquals(0, stringVarargsLength((String[]) null));
      fail("Should have thrown exception");
    } catch (NullPointerException expected) {
    }

    // Test with an additional parameter as it results in a slightly different call site.
    assertEquals(0, stringVarargsLengthV2(0));
    assertEquals(0, stringVarargsLengthV2(0, new String[0]));
    assertEquals(1, stringVarargsLengthV2(0, (String) null));
    try {
      assertEquals(0, stringVarargsLengthV2(0, null));
      fail("Should have thrown exception");
    } catch (NullPointerException expected) {
    }
    try {
      assertEquals(0, stringVarargsLengthV2(0, (String[]) null));
      fail("Should have thrown exception");
    } catch (NullPointerException expected) {
    }
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
    SubclassNativeWithVarargsConstructor object =
        new SubclassNativeWithVarargsConstructor(0, new Object[0]);
    sideEffectCount = 0;
    Object[] params = new Object[] { object, null };
    assertSame(object, doSideEffect(object).varargsMethod(0, params));
    assertSame(1, sideEffectCount);
  }

  static class UninstantiatedClass {
  }

  @JsMethod(namespace = JsPackage.GLOBAL)
  public static int varargJsMethodUninstantiatedVararg(
      UninstantiatedClass... varargs) {
    return varargs.length;
  }

  public native void testVarargsCall_uninstantiatedVararg() /*-{
    @GWTTestCase::assertEquals(II)(0, $global.varargJsMethodUninstantiatedVararg());
  }-*/;
}
