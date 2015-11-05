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
package com.google.gwt.core.client.interop;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.junit.client.GWTTestCase;

import jsinterop.annotations.JsType;

/**
 * Tests JsType functionality.
 */
@SuppressWarnings("cast")
public class NativeJsTypeTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Core";
  }

  @JsType(isNative = true)
  static class MyNativeJsType {
    // TODO(rluble): these methods should be synthesyzed by the compiler.
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
}
