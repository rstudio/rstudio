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

import static jsinterop.annotations.JsPackage.GLOBAL;

import com.google.gwt.junit.client.GWTTestCase;

import javaemul.internal.annotations.DoNotAutobox;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsProperty;

/**
 * Tests JsMethod functionality.
 */
public class JsMethodTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Interop";
  }

  class MyObject {
    @JsProperty
    public int mine;

    @JsMethod
    public native boolean hasOwnProperty(String name);
  }

  public void testNativeJsMethod() {
    MyObject obj = new MyObject();
    obj.mine = 0;
    assertTrue(obj.hasOwnProperty("mine"));
    assertFalse(obj.hasOwnProperty("toString"));
  }

  @JsMethod(namespace = GLOBAL)
  private static native boolean isFinite(double d);

  public void testStaticNativeJsMethod() {
    assertFalse(isFinite(Double.POSITIVE_INFINITY));
    assertFalse(isFinite(Double.NEGATIVE_INFINITY));
    assertFalse(isFinite(Double.NaN));
    assertTrue(isFinite(0));
    assertTrue(isFinite(1));
  }

  @JsProperty(namespace = GLOBAL, name = "NaN")
  private static native double getNaN();

  @JsProperty(namespace = GLOBAL, name = "Infinity")
  private static native double infinity();

  public void testStaticNativeJsPropertyGetter() {
    assertTrue(getNaN() != getNaN());
    assertTrue(Double.isInfinite(infinity()));
    assertTrue(Double.isInfinite(-infinity()));
  }

  @JsProperty(namespace = GLOBAL)
  private static native void setJsInteropSecret(String magic);

  @JsProperty(namespace = GLOBAL)
  private static native String getJsInteropSecret();

  public void testStaticNativeJsPropertySetter() {
    setJsInteropSecret("very secret!");
    assertEquals("very secret!", getJsInteropSecret());
  }

  public static double returnWithoutBoxing(@DoNotAutobox Object object) {
    return (Double) object;
  }

  public static double sumWithoutBoxing(@DoNotAutobox Object... objects) {
    double sum = 0;
    for (Object o : objects) {
      sum += (Double) o;
    }
    return sum;
  }

  @JsMethod
  public static double sumWithoutBoxingJsVarargs(@DoNotAutobox Object... objects) {
    double sum = 0;
    for (Object o : objects) {
      sum += (Double) o;
    }
    return sum;
  }

  public void testDoNotAutobox() {
    assertEquals(3.0, returnWithoutBoxing(3));
    assertEquals(4.5, sumWithoutBoxing(1, 1.5, (short) 1, (byte) 1));
    assertEquals(4.5, sumWithoutBoxingJsVarargs(1, 1.5, (short) 1, (byte) 1));
    try {
      returnWithoutBoxing(Long.MAX_VALUE);
      fail("Should have thrown ClassCastException");
    } catch (ClassCastException expected) {
    }
  }
}
