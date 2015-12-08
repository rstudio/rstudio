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
import com.google.gwt.junit.client.GWTTestCase;

import javaemul.internal.annotations.DoNotInline;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * Tests JsType functionality.
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

  public void testClassLiterals() {
    assertEquals(JavaScriptObject.class, MyNativeJsType.class);
    assertEquals(JavaScriptObject.class, MyNativeJsTypeInterface.class);
    assertEquals(JavaScriptObject.class, MyNativeJsType[].class);
    assertEquals(JavaScriptObject.class, MyNativeJsTypeInterface[].class);
    assertEquals(JavaScriptObject.class, MyNativeJsType[][].class);
    assertEquals(JavaScriptObject.class, MyNativeJsTypeInterface[][].class);

    Object nativeObject = createNativeObjectWithoutToString();
    assertEquals(JavaScriptObject.class, nativeObject.getClass());
    assertEquals(JavaScriptObject.class, ((MyNativeJsTypeInterface) nativeObject).getClass());
  }

  public void testToString() {
    Object nativeObjectWithToString = createNativeObjectWithToString();
    assertEquals("Native type", nativeObjectWithToString.toString());

    Object nativeObjectWithoutToString = createNativeObjectWithoutToString();
    assertEquals("[object Object]", nativeObjectWithoutToString.toString());

    Object nativeArray = createNativeArray();
    assertEquals("", nativeArray.toString());
  }

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
}
