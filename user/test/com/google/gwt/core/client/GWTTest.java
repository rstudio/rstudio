/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.core.client;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests for the GWT class.
 */
public class GWTTest extends GWTTestCase {

  private static volatile int seven = 7;
  private static volatile int zero = 0;

  private static native boolean canCallNativeMethod() /*-{
    return true;
  }-*/;

  private static void jvmTests() {
    assertFalse(GWT.isProdMode());
    assertFalse(GWT.isScript());
    try {
      canCallNativeMethod();
      fail("Expected UnsatisfiedLinkError");
    } catch (Throwable expected) {
      assertEquals("java.lang.UnsatisfiedLinkError",
          expected.getClass().getName());
    }
    try {
      GWT.create(GWTTest.class);
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }
  }

  public GWTTest() {
    if (!GWT.isClient()) {
      jvmTests();
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Core";
  }

  public void testCreate() {
    Object created = GWT.create(GWTTest.class);
    assertSame(getClass(), created.getClass());
  }

  public void testGetModuleName() {
    assertEquals("com.google.gwt.core.Core.JUnit", GWT.getModuleName());
  }

  @SuppressWarnings("deprecation")
  public void testGetTypeName() {
    assertEquals(getClass().getName(), GWT.getTypeName(this));
  }

  public void testIsClient() {
    assertTrue(GWT.isClient());
    assertTrue(canCallNativeMethod());
  }

  @SuppressWarnings("unused")
  public void testIsProdMode() {
    if (GWT.isScript()) {
      assertTrue(GWT.isProdMode());
    }
    try {
      double d = seven / zero;
      if (!GWT.isProdMode()) {
        fail("Expected ArithmeticException");
      }
    } catch (ArithmeticException expected) {
    }
  }

  @SuppressWarnings("unused")
  public void testIsScript() {
    try {
      double d = seven / zero;
      if (!GWT.isScript()) {
        fail("Expected ArithmeticException");
      }
    } catch (ArithmeticException expected) {
      assertFalse(GWT.isScript());
    }
  }

  @Override
  protected void gwtSetUp() {
    assertTrue(GWT.isClient());
    assertTrue(canCallNativeMethod());
  }
}
