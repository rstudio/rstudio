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
import jsinterop.annotations.JsProperty;
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

    public int k;

    @JsOverlay
    public final NativeJsTypeWithOverlay setK(int k) {
      this.k = k;
      return this;
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
    assertEquals(42, object.setK(3).setK(42).k);
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

    static {
      clinitCalled++;
    }
  }

  private static int clinitCalled = 0;

  public void testNativeJsTypeWithStaticIntializer() {
    assertEquals(new Integer(3), NativeJsTypeWithStaticInitializationAndFieldAccess.object);
    assertEquals(0, clinitCalled);
    assertEquals(
        new Integer(4), NativeJsTypeWithStaticInitializationAndStaticOverlayMethod.getObject());
     assertEquals(new Integer(5),
         new NativeJsTypeWithStaticInitializationAndInstanceOverlayMethod().getObject());
    assertEquals(1, clinitCalled);
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

  static final class SomeFunction implements SomeFunctionInterface {
    public void m() {
    }
  }

  public void testSpecialNativeInstanceOf() {
    Object aJsFunction = new SomeFunction();
    // True cases.
    assertTrue(aJsFunction instanceof NativeFunction);
    assertTrue(aJsFunction instanceof SomeFunctionalInterface);
    assertTrue(aJsFunction instanceof NativeObject);
    // False cases.
    assertFalse(aJsFunction instanceof NativeArray);
    assertFalse(aJsFunction instanceof NativeNumber);
    assertFalse(aJsFunction instanceof NativeString);

    Object anotherFunction = createFunction();
    // True cases.
    assertTrue(anotherFunction instanceof NativeFunction);
    assertTrue(anotherFunction instanceof SomeFunctionalInterface);
    assertTrue(anotherFunction instanceof NativeObject);
    // False cases.
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

    Object nullObject = null;

    assertFalse(nullObject instanceof NativeObject);
    assertFalse(nullObject instanceof NativeArray);
    assertFalse(nullObject instanceof NativeFunction);
    assertFalse(nullObject instanceof NativeString);
    assertFalse(nullObject instanceof NativeNumber);

    Object undefined = getUndefined();
    assertFalse(undefined instanceof NativeObject);
    assertFalse(undefined instanceof NativeArray);
    assertFalse(undefined instanceof NativeFunction);
    assertFalse(undefined instanceof NativeString);
    assertFalse(undefined instanceof NativeNumber);
  }

  private static native Object getUndefined() /*-{
  }-*/;

  @JsType(isNative = true)
  private static class UnreferencedNativeType { }

  private static native Object createArray() /*-{
    return [];
  }-*/;

  public void testUnreferencedNativeArrayInstanceOf() {
    assertTrue(createArray() instanceof UnreferencedNativeType[]);
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  interface NativeInterface {
    void add(String element);
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  static class NativeSuperClass {
    public native void add(String element);
    public native boolean remove(String element);
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  static class NativeSubClassAccidentalOverride
      extends NativeSuperClass implements NativeInterface {
  }

  public native NativeSubClassAccidentalOverride createNativeSubclass() /*-{
    return {
        add:
            function(e) {
              this[0] = e;
            },
        remove:
            function(e) {
              var ret = this[0] == e;
              this[0] = undefined;
              return ret;
            }
      };
  }-*/;

  public void testForwaringMethodsOnNativeClasses() {
    NativeSubClassAccidentalOverride subClass = createNativeSubclass();
    subClass.add("Hi");
    assertTrue(subClass.remove("Hi"));
    assertFalse(subClass.remove("Hi"));
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  static class NativeClassWithStaticOverlayFields {
    @JsOverlay
    static String uninitializedString;
    @JsOverlay
    static int uninitializedInt;
    @JsOverlay
    static int initializedInt = 5;
  }

  public void testUninitializedStaticOverlayField() {
    assertEquals(0, NativeClassWithStaticOverlayFields.uninitializedInt);
    assertEquals(5, NativeClassWithStaticOverlayFields.initializedInt);
    assertNull(NativeClassWithStaticOverlayFields.uninitializedString);
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "window")
  private static class MainWindow {
    public static Object window;
  }

  // <window> is a special qualifier that allows referencing the iframe window instead of the main
  // window.
  @JsType(isNative = true, namespace = "<window>", name = "window")
  private static class IFrameWindow {
    public static Object window;
  }

  @JsType(isNative = true)
  private static class AlsoMainWindow {
    @JsProperty(namespace = JsPackage.GLOBAL)
    public static Object window;
  }

  @JsType(isNative = true)
  private static class AlsoIFrameWindow {
    @JsProperty(namespace = "<window>")
    public static Object window;
  }

  public void testMainWindowIsNotIFrameWindow() {
    assertSame(IFrameWindow.window, AlsoIFrameWindow.window);
    assertNotSame(AlsoIFrameWindow.window, AlsoMainWindow.window);
    assertNotSame(IFrameWindow.window, MainWindow.window);
    assertSame(MainWindow.window, AlsoMainWindow.window);
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Error")
  private static class NativeError {
    public String message;
  }

  private static final String ERROR_FROM_NATIVE_ERROR_SUBCLASS = "error from NativeErrorSubclass";

  private static class NativeErrorSubclass extends NativeError {
    public NativeErrorSubclass() {
      message = ERROR_FROM_NATIVE_ERROR_SUBCLASS;
    }
  }

  public void testObjectPropertiesAreCopied() {
    Object error = new NativeErrorSubclass();
    assertTrue(error instanceof NativeError);
    // Make sure the subclass is a proper Java object (the typeMarker should be one of the
    // properties copied from java.lang.Object).
    assertFalse(error instanceof JavaScriptObject);
    assertTrue(error.toString().contains(ERROR_FROM_NATIVE_ERROR_SUBCLASS));
  }
}
