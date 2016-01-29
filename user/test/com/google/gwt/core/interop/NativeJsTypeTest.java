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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.interop.JsTypeSpecialTypesTest.SomeFunctionalInterface;
import com.google.gwt.junit.client.GWTTestCase;

import javaemul.internal.annotations.DoNotInline;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * Tests native JsType functionality.
 */
@SuppressWarnings("cast")
public class NativeJsTypeTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Interop";
  }

  @JsType(isNative = true)
  static class MyNativeJsType {
    // TODO(rluble): these methods should be synthesized by the compiler.
    @Override
    public native String toString();
    @Override
    public native boolean equals(Object o);
    @Override
    public native int hashCode();
  }

  @JsType(isNative = true)
  interface MyNativeJsTypeInterface {
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  static class NativeObject implements MyNativeJsTypeInterface {
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  final static class FinalNativeObject implements MyNativeJsTypeInterface {
  }

  @JsType(isNative = true)
  interface MyNativeJsTypeInterfaceOnlyOneConcreteImplementor {
  }

  public void testClassLiterals() {
    assertEquals(JavaScriptObject.class, MyNativeJsType.class);
    assertEquals(JavaScriptObject.class, MyNativeJsTypeInterface.class);
    assertEquals(JavaScriptObject[].class, MyNativeJsType[].class);
    assertEquals(JavaScriptObject[].class, MyNativeJsTypeInterface[].class);
    assertEquals(JavaScriptObject[].class, MyNativeJsType[][].class);
    assertEquals(JavaScriptObject[].class, MyNativeJsTypeInterface[][].class);
    assertEquals(JavaScriptObject[].class, JavaScriptObject.createArray().getClass());
  }

  public void testGetClass() {
    Object object = createNativeObjectWithoutToString();
    assertEquals(JavaScriptObject.class, object.getClass());

    MyNativeJsTypeInterface nativeInterface =
        (MyNativeJsTypeInterface) createNativeObjectWithoutToString();
    assertEquals(JavaScriptObject.class, nativeInterface.getClass());

    // Test that the dispatch to getClass in not messed up by incorrectly marking nativeObject1 as
    // exact and inlining Object.getClass() implementation.
    NativeObject nativeObject1 = new NativeObject();
    assertEquals(JavaScriptObject.class, nativeObject1.getClass());

    // Test that the dispatch to getClass in not messed up by incorrectly marking nativeObject2 as
    // exact and inlining Object.getClass() implementation.
    FinalNativeObject nativeObject2 = createNativeObject();
    assertEquals(JavaScriptObject.class, nativeObject2.getClass());
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  final static class AnotherFinalNativeObject implements MyNativeJsTypeInterface {
  }

  private static boolean same(Object thisObject, Object thatObject) {
    return thisObject == thatObject;
  }

  public void testEqualityOptimization() {
    // Makes sure that == does not get optimized away due to static class incompatibility.

    FinalNativeObject finalNativeObject = new FinalNativeObject();

    AnotherFinalNativeObject anotherFinalNativeObject =
        (AnotherFinalNativeObject) (Object) finalNativeObject;
    // DeadCodeElimination could optimize statically to false due to type incompatibility, which
    // could happen if both variables were marked as exact.
    assertTrue(same(anotherFinalNativeObject, finalNativeObject));
  }

  public void testToString() {
    Object nativeObjectWithToString = createNativeObjectWithToString();
    assertEquals("Native type", nativeObjectWithToString.toString());

    Object nativeObjectWithoutToString = createNativeObjectWithoutToString();
    assertEquals("[object Object]", nativeObjectWithoutToString.toString());

    Object nativeArray = createNativeArray();
    assertEquals("", nativeArray.toString());
  }

  private static native FinalNativeObject createNativeObject() /*-{
    return {};
  }-*/;

  private static native MyNativeJsType createNativeObjectWithToString() /*-{
    return {toString: function() { return "Native type"; } };
  }-*/;

  private static native MyNativeJsType createNativeObjectWithoutToString() /*-{
    return {};
  }-*/;

  private static native Object createNativeArray() /*-{
    return [];
  }-*/;

  @JsType(isNative = true, namespace = GLOBAL, name = "Object")
  static class NativeJsTypeWithOverlay {

    @JsOverlay
    public static final int x = 2;

    public static native String[] keys(Object o);

    @JsOverlay @DoNotInline
    public static final boolean hasM(Object obj) {
      return keys(obj)[0].equals("m");
    }

    public native boolean hasOwnProperty(String name);

    @JsOverlay @DoNotInline
    public final boolean hasM() {
      return hasOwnProperty("m");
    }
  }

  private native NativeJsTypeWithOverlay createNativeJsTypeWithOverlay() /*-{
    return { m: function() { return 6; } };
  }-*/;

  public void testNativeJsTypeWithOverlay() {
    NativeJsTypeWithOverlay object = createNativeJsTypeWithOverlay();
    assertTrue(object.hasM());
    assertTrue(NativeJsTypeWithOverlay.hasM(object));
    assertEquals(2, NativeJsTypeWithOverlay.x);
  }

  @JsType(isNative = true)
  static class NativeJsTypeWithStaticInitializationAndFieldAccess {
    @JsOverlay
    public static Object object = new Integer(3);
  }

  @JsType(isNative = true)
  static class NativeJsTypeWithStaticInitializationAndStaticOverlayMethod {
    @JsOverlay
    public static Object object = new Integer(4);

    @JsOverlay
    public static Object getObject() {
      return object;
    }
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  static class NativeJsTypeWithStaticInitializationAndInstanceOverlayMethod {
    @JsOverlay
    public static Object object = new Integer(5);

    @JsOverlay
    public final Object getObject() {
      return object;
    }
  }

  public void testNativeJsTypeWithStaticIntializer() {
    assertEquals(new Integer(3), NativeJsTypeWithStaticInitializationAndFieldAccess.object);
    assertEquals(
        new Integer(4), NativeJsTypeWithStaticInitializationAndStaticOverlayMethod.getObject());
     assertEquals(new Integer(5),
         new NativeJsTypeWithStaticInitializationAndInstanceOverlayMethod().getObject());
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Function")
  static class NativeFunction {
  }

  private static native Object createFunction() /*-{
    return function() {};
   }-*/;

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Array")
  static class NativeArray {
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Number")
  static class NativeNumber {
  }

  private static native Object createNumber() /*-{
    return 1;
  }-*/;

  private static native Object createBoxedNumber() /*-{
    return new Number(1);
  }-*/;

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "String")
  static class NativeString {
  }

  private static native Object createBoxedString() /*-{
    return new String("hello");
  }-*/;

  @JsFunction
  interface SomeFunctionInterface {
    void m();
  }

  static class SomeFunction implements SomeFunctionInterface {
    public void m() {
    }
  }

  public void testSpecialNativeInstanceOf() {
    Object aJsFunction = new SomeFunction();
    // True cases.
    assertTrue(aJsFunction instanceof NativeFunction);
    assertTrue(aJsFunction instanceof SomeFunctionalInterface);
    // False cases.
    assertFalse(aJsFunction instanceof NativeObject);
    assertFalse(aJsFunction instanceof NativeArray);
    assertFalse(aJsFunction instanceof NativeNumber);
    assertFalse(aJsFunction instanceof NativeString);

    Object anotherFunction = createFunction();
    // True cases.
    assertTrue(anotherFunction instanceof NativeFunction);
    assertTrue(anotherFunction instanceof SomeFunctionalInterface);
    // False cases.
    assertFalse(anotherFunction instanceof NativeObject);
    assertFalse(anotherFunction instanceof NativeArray);
    assertFalse(anotherFunction instanceof NativeNumber);
    assertFalse(anotherFunction instanceof NativeString);

    Object aString = "Hello";
    // True cases.
    assertTrue(aString instanceof NativeString);
    // False cases.
    assertFalse(aString instanceof NativeFunction);
    assertFalse(aString instanceof NativeObject);
    assertFalse(aString instanceof NativeArray);
    assertFalse(aString instanceof NativeNumber);

    Object aBoxedString = createBoxedString();
    // True cases.
    // Note that boxed strings are (surprisingly) not strings but objects.
    assertTrue(aBoxedString instanceof NativeObject);
    // False cases.
    assertFalse(aBoxedString instanceof NativeFunction);
    assertFalse(aBoxedString instanceof NativeArray);
    assertFalse(aBoxedString instanceof NativeNumber);
    assertFalse(aBoxedString instanceof NativeString);

    Object anArray = new String[0];
    // True cases.
    assertTrue(anArray instanceof NativeArray);
    assertTrue(anArray instanceof NativeObject);
    // False cases.
    assertFalse(anArray instanceof NativeFunction);
    assertFalse(anArray instanceof NativeNumber);
    assertFalse(anArray instanceof NativeString);

    Object aNativeArray = JavaScriptObject.createArray();
    // True cases.
    assertTrue(aNativeArray instanceof NativeArray);
    assertTrue(anArray instanceof NativeObject);
    // False cases.
    assertFalse(aNativeArray instanceof NativeFunction);
    assertFalse(aNativeArray instanceof NativeNumber);
    assertFalse(aNativeArray instanceof NativeString);

    Object aNumber = new Double(3);
    // True cases.
    assertTrue(aNumber instanceof NativeNumber);
    // False cases.
    assertFalse(aNumber instanceof NativeArray);
    assertFalse(aNumber instanceof NativeObject);
    assertFalse(aNumber instanceof NativeFunction);
    assertFalse(aNumber instanceof NativeString);

    Object anotherNumber = createNumber();
    // True cases.
    assertTrue(anotherNumber instanceof NativeNumber);
    // False cases.
    assertFalse(anotherNumber instanceof NativeArray);
    assertFalse(anotherNumber instanceof NativeObject);
    assertFalse(anotherNumber instanceof NativeFunction);
    assertFalse(anotherNumber instanceof NativeString);

    Object aBoxedNumber = createBoxedNumber();
    // True cases.
    assertTrue(aBoxedNumber instanceof NativeObject);
    // False cases.
    assertFalse(aBoxedNumber instanceof NativeNumber);
    assertFalse(aBoxedNumber instanceof NativeArray);
    assertFalse(aBoxedNumber instanceof NativeFunction);
    assertFalse(aBoxedNumber instanceof NativeString);
  }
}
