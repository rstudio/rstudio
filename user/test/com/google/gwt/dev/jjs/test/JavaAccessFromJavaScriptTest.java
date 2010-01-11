/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.jjs.test;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests JSNI references in their main use of accessing Java code from
 * JavaScript.
 */
public class JavaAccessFromJavaScriptTest extends GWTTestCase {
  private static class Adder implements HasAddOne {
    /**
     * This overloaded version is present in the class but not the interface.
     */
    public double addOne(double x) {
      return x + 1.01;
    }

    public int addOne(int x) {
      return x + 1;
    }
  }

  private interface HasAddOne {
    int addOne(int x);
  }

  public static int addOne(int x) {
    return x + 1;
  }

  /**
   * Accesses the virtual addOne() method.
   */
  public static native int useAddOne(HasAddOne adder, int y) /*-{
    return adder.@com.google.gwt.dev.jjs.test.JavaAccessFromJavaScriptTest.HasAddOne::addOne(I)(y);
  }-*/;

  /**
   * Accesses the static addOne() method.
   */
  public static native int useAddOne(int y) /*-{
    return @com.google.gwt.dev.jjs.test.JavaAccessFromJavaScriptTest::addOne(I)(y);
  }-*/;

  /**
   * Accesses the virtual addOne() method via the wildcard notation to specify a non-overloaded
   * method.
   */
  public static native int useAddOneUsingWildcard(HasAddOne adder, int y) /*-{
    return adder.@com.google.gwt.dev.jjs.test.JavaAccessFromJavaScriptTest.HasAddOne::addOne(*)(y);
  }-*/;

  /**
   * Accesses the addOne() method via the wildcard notation to specify a non-overloaded
   * method.
   */
  public static native int useAddOneUsingWildcard(int y) /*-{
    return @com.google.gwt.dev.jjs.test.JavaAccessFromJavaScriptTest::addOne(*)(y);
  }-*/;

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testBasics() {
    assertEquals(11, useAddOne(10));
    assertEquals(11, useAddOneUsingWildcard(10));
  }

  public void testInterface() {
    HasAddOne adder = new Adder();
    assertEquals(11, useAddOne(adder, 10));
    assertEquals(11, useAddOneUsingWildcard(adder, 10));
  }
}
