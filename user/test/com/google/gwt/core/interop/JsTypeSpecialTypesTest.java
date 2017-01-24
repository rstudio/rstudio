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

import com.google.gwt.junit.client.GWTTestCase;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * Tests special JsType functionality.
 */
@SuppressWarnings("cast")
public class JsTypeSpecialTypesTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Interop";
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Array")
  static class NativeArray {
  }

  public void testNativeArray() {
    Object object = new Object[10];

    assertNotNull((NativeArray) object);
    assertTrue(object instanceof NativeArray);

    Object nativeArray = new NativeArray();
    assertNotNull((NativeArray[]) nativeArray);
    assertTrue(nativeArray instanceof NativeArray[]);
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Function")
  static class NativeFunction {
  }

  @JsFunction
  interface  SomeFunctionalInterface {
    void m();
  }

  public void testNativeFunction() {
    Object object = new SomeFunctionalInterface() {
          @Override
          public void m() {
          }
        };

    assertNotNull((NativeFunction) object);
    assertTrue(object instanceof NativeFunction);

    SomeFunctionalInterface nativeFunction = (SomeFunctionalInterface) new NativeFunction();
    assertTrue(nativeFunction instanceof SomeFunctionalInterface);
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Number")
  static class NativeNumber {
    public NativeNumber(double number) { }
    public native NativeNumber valueOf();
  }

  public void testNativeNumber() {
    Object object = new Double(1);

    assertNotNull((NativeNumber) object);
    assertTrue(object instanceof NativeNumber);

    // new NativeString() returns a boxed JS number. Java Double object are only interchangeable
    // with unboxed JS numbers.
    Object nativeNumber = new NativeNumber(10.0).valueOf();
    assertNotNull((Double) nativeNumber);
    assertTrue(nativeNumber instanceof Double);
    assertEquals(10.0, (Double) nativeNumber);
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "String")
  static class NativeString {
    public NativeString(String someString) { }
    public native NativeString valueOf();
  }

  public void testNativeString() {
    Object object = "Hello";

    assertNotNull((NativeString) object);
    assertTrue(object instanceof NativeString);

    // new NativeString() returns a boxed JS string. Java String objects are only interchangeable
    // with unboxed JS strings.
    Object nativeString = new NativeString("Hello").valueOf();
    assertNotNull((String) nativeString);
    assertTrue(nativeString instanceof String);
    assertEquals("Hello", nativeString);
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  static class NativeObject {
  }

  public void testNativeObject() {
    Object object = new Object();

    assertNotNull((NativeObject) object);
    assertTrue(object instanceof NativeObject);

    Object nativeObject = new NativeObject();
    assertNotNull((Object) nativeObject);
    assertTrue(nativeObject instanceof Object);
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "*")
  interface Star {
  }

  public void testStar() {
    Object object = new Object();

    assertNotNull((Star) object);

    object = Double.valueOf(3.0);
    assertNotNull((Star) object);
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
  interface Wildcard {
  }

  public void testWildcard() {
    Object object = new Object();

    assertNotNull((Wildcard) object);

    object = Double.valueOf(3.0);
    assertNotNull((Wildcard) object);
  }
}
